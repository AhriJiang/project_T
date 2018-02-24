package com.juntao.commons.util;

import com.juntao.commons.consts.SConsts;
import com.juntao.commons.function.UsefulFunctions;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by major on 2017/9/26.
 */
public class SFileUtil {
	private static final Logger log = LoggerFactory.getLogger(SFileUtil.class);


	public static final Set<String> deleteUselessFile(File rootDir, Set<File> usefulFileSet) throws Exception {
		return deleteUselessFile(rootDir, usefulFileSet, null);
	}

	/**
	 * 递归删除某个目录下所有的空子目录， 并将所有空文件和不在白名单里的所有文件剪切到/tmp目录下
	 *
	 * @param rootDir               递归根目录
	 * @param usefulFileSet         所有白名单文件路径
	 * @param destDirParentPathName 移动文件的目标路径
	 * @return 所有被删除掉的目录和被剪切掉的文件的列表
	 * @throws Exception
	 */
	public static final Set<String> deleteUselessFile(File rootDir, Set<File> usefulFileSet, String destDirParentPathName) throws Exception {
		if (null == rootDir || !rootDir.exists() || !rootDir.isDirectory()) {
			log.warn("bad rootDir!: {}",Optional.ofNullable(rootDir).map(File::getPath).orElse(StringUtils.EMPTY));
			return Collections.emptySet();
		}

		File[] files = rootDir.listFiles();
		if (ArrayUtils.isEmpty(files) || Stream.of(files).allMatch(UsefulFunctions.isNull)) {
			log.warn("empty rootDir!: {}",Optional.ofNullable(rootDir).map(File::getPath).orElse(StringUtils.EMPTY));
			return Collections.emptySet();
		}

		if (CollectionUtils.isEmpty(usefulFileSet) || usefulFileSet.stream().allMatch(UsefulFunctions.isNull)) {
			log.warn("it will delete all in case of empty white list!  no!");
			return Collections.emptySet();
		}

		Map<Boolean, List<File>> existFlag_fileList = usefulFileSet.stream().collect(Collectors.partitioningBy(usefulFile -> !usefulFile.exists()));
		if (null != existFlag_fileList.get(true)) { // some white list file not exists!
			existFlag_fileList.get(true).stream().peek(filePath ->  log.warn("file not exists: {}",filePath)).count();
		}
		Set<File> existUsefulFileSet = new HashSet<>();
		if (null != existFlag_fileList.get(false)) { // some white list file exists!
			existUsefulFileSet.addAll(existFlag_fileList.get(false).stream().collect(Collectors.toSet()));
		}

//		if (!usefulFileSet.stream().allMatch(File::isFile)) {
//			log.warn("white list should contains file only,  no directory!");
//			return Collections.emptySet();
//		}

		if (StringUtils.isNotBlank(destDirParentPathName)
				&& StringUtils.startsWith(getPathName(new File(destDirParentPathName)), getPathName(rootDir))) {
			log.warn("destDirParentPathName should not be children of rootDir!");
			return Collections.emptySet();
		}
		destDirParentPathName = new StringBuilder(StringUtils.stripToEmpty(destDirParentPathName)).append(File.separatorChar)
				.append("tmp").append(File.separator).append(DateFormatUtils.format(new Date(), SConsts.yyyyMMddHHmmss))
				.append(File.separator).toString();

		Set<File> allDeletedSet = new HashSet<>();
		ForkJoinPool pool = new ForkJoinPool();
		while (true) {
			ForkJoinTask<Set<File>> task = pool.submit(new DeleteUselessFileOfDirTask(rootDir,
					existUsefulFileSet.stream().filter(UsefulFunctions.notNull).map(File::toPath).collect(Collectors.toSet()),
					destDirParentPathName));
			do {
				try {
					TimeUnit.MILLISECONDS.sleep(5);
				} catch (InterruptedException e) {
					log.error("sleep error!!", e);
				}
			} while (!task.isDone());

			Set<File> deletedSet = task.get();
			if (CollectionUtils.isEmpty(deletedSet)) {
				break;
			} else {
				allDeletedSet.addAll(deletedSet);
			}
		}
		pool.shutdown();

		SortedSet<String> result = allDeletedSet.stream().map(SFileUtil::getPathName).collect(Collectors.toCollection(TreeSet::new));
		log.info(StringUtils.join(result.stream().toArray(), SConsts.SEMICOLON + StringUtils.SPACE));
		return result;
	}

	private static final class DeleteUselessFileOfDirTask extends RecursiveTask<Set<File>> {
		private File dir;
		private Set<Path> usefulPathSet;
		private String destParentDirPathName;

		public DeleteUselessFileOfDirTask(File dir, Set<Path> usefulPathSet, String destParentDirPathName) {
			this.dir = dir;
			this.usefulPathSet = usefulPathSet;
			this.destParentDirPathName = destParentDirPathName;
		}

		public Set<File> compute() {
			Set<File> deletedSet = new HashSet<>();
			File[] files = dir.listFiles();
			if (ArrayUtils.isEmpty(files) || Stream.of(files).allMatch(UsefulFunctions.isNull)) {
				FileUtils.deleteQuietly(dir);
				deletedSet.add(dir);
				return deletedSet;
			}

			Map<Boolean, List<File>> isDirectory_fileList = Stream.of(files).filter(UsefulFunctions.notNull)
					.collect(Collectors.partitioningBy(File::isDirectory));
			List<DeleteUselessFileOfDirTask> childrenDirTaskList = Optional.ofNullable(isDirectory_fileList.get(true)).orElse(Collections.emptyList())
					.stream().map(d -> new DeleteUselessFileOfDirTask(d, usefulPathSet, destParentDirPathName)).collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(childrenDirTaskList)) {
				childrenDirTaskList.forEach(DeleteUselessFileOfDirTask::fork);
			}

			for (File file : Optional.ofNullable(isDirectory_fileList.get(false)).orElse(Collections.emptyList())) {
				String pathName = getPathName(file);
				if (usefulPathSet.stream().noneMatch(usefulPath -> {
					try {
						return Files.isSameFile(usefulPath, file.toPath());
					} catch (IOException e) {
						log.error("Files.isSameFile error!! usefulPath= " + getPathName(usefulPath.toFile()) + ",  file= " + pathName, e);
						return false;
					}
				})
						|| file.length() == 0L
						) {
					try {
						int indexOfFileSeperator = pathName.indexOf(File.separator);
						FileUtils.moveFile(file, new File(destParentDirPathName
								+ (0 >= indexOfFileSeperator ? pathName : pathName.substring(indexOfFileSeperator))));
					} catch (Exception e) {
						log.error("moveFile error! file= ", e);
					}
					deletedSet.add(file);
				}
			}

			if (!CollectionUtils.isEmpty(childrenDirTaskList)) {
				childrenDirTaskList.stream().map(DeleteUselessFileOfDirTask::join).forEach(deletedSet::addAll);
			}

			return deletedSet;
		}
	}

	private static final String getPathName(File file) {
		try {
			return file.getCanonicalPath().toLowerCase();
		} catch (IOException e) {
			String absolutePath = file.getAbsolutePath().toLowerCase();
			log.error("getCanonicalPath error! absolutePath= " + absolutePath);
			return absolutePath;
		}
	}

	/**
	 * 判断offspringFile是不是在ancestorDir目录之下
	 *
	 * @param ancestorDir  上级目录
	 * @param offspringFile   子孙目录
	 * @return
	 */
	public static boolean isSameDirBranch(File ancestorDir, File offspringFile) {
		if (Stream.of(ancestorDir, offspringFile).anyMatch(file -> null == file || !file.exists()) || !ancestorDir.isDirectory()) {
			log.warn("bad file!!");
			return false;
		}

		final Path ancestorPath = ancestorDir.toPath();
		File parentDir = offspringFile;
		while (true) {
			if (null == parentDir) {
				return false;
			}

			try {
				if (Files.isSameFile(ancestorPath, parentDir.toPath())) {
					return true;
				}
			} catch (IOException e) {
				log.error("isSameFile error!", e);
				return false;
			}

			parentDir = parentDir.getParentFile();
		}
	}

	public static void main(String[] args) throws Exception {
		Stream.of("d:/testdel/3/a.txt", "d:/testdel/3/5/b.txt", "d:/testdel/3/5/9/c.txt")
				.map(File::new).forEach(file -> {
			try {
				FileUtils.writeLines(file, Arrays.asList("5656"));
			} catch (Exception e) {
				log.error("", e);
			}
		});


		Stream.of("d:/testdel/22", "d:/testdel/3/44", "d:/testdel/3/22/00", "d:/testdel/3/5/88", "d:/testdel/3/5/9/66")
				.map(File::new).forEach(File::mkdirs);


		deleteUselessFile(new File("d:/testdel"), Stream.of("d:/testdel/3/5/b.txt", "d:/testdel/3/a.txt","d:/testdel/7/a.txt")
						.map(File::new).collect(Collectors.toSet()),
				"d:\\sadf");
	}
}
