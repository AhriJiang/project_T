package com.juntao.commons.autocoder;

import net.sf.cglib.beans.BeanGenerator;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.juntao.commons.util.SCollectionUtil;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoCodeWriter {
	private static final String COMMON_PACKAGE = "com.cmbchina.o2o.funmall.rg.commons.";

	private static final String BASE_PACKAGE = "com.cmbchina.o2o.funmall.rg.";
	private static final String SCHEMA = "rg";
	private static final boolean SCHEMA_IN_CODE = false;
	private static final String TABLE_FIX = "";
	private static final String COLUMN_FIX = "";

	private static String SRC_DIR = "\\funmall-rg-dao\\src\\main\\java";
	private static String RESOURCES_DIR = "\\funmall-rg-dao\\src\\main\\resources";

	private static final Set<String> duplicateIngoreColumns = Stream.of("creator", "created").collect(Collectors.toSet());

	private static void init(String workspaceDir, boolean shouldClearAll) {
		SRC_DIR = workspaceDir + SRC_DIR;
		RESOURCES_DIR = workspaceDir + RESOURCES_DIR;
		for (String s : BASE_PACKAGE.split("\\.")) {
			if (StringUtils.isNotBlank(s)) {
				SRC_DIR = SRC_DIR + File.separatorChar + s;
				RESOURCES_DIR = RESOURCES_DIR + File.separatorChar + s;
			}
		}

		// try {
		// if (shouldClearAll) {
		// FileUtils.deleteDirectory(new File(SRC_DIR));
		// }
		//
		// FileUtils.copyDirectory(new File(".\\src\\major\\frameclass"), new File(SRC_DIR + "\\frameclass"));
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

	private static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	// private static final String DB_URL =
	// "jdbc:mysql://mysql.piao.ip:8066/CMBCHINA?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false";
//    private static final String DB_URL = "jdbc:mysql://mysql.galemall.ip:8066/" + SCHEMA
//	private static final String DB_URL = "jdbc:mysql://99.48.210.175:8066/" + SCHEMA
	//private static final String DB_URL = "jdbc:mysql://172.30.11.175:8066/" + SCHEMA
	private static final String DB_URL = "jdbc:mysql://127.0.0.1:43062/" + SCHEMA
			+ "?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false";
	private static final String DB_USER = "monitor";
	private static final String DB_PASSWORD = "Mon_Admin*2017";

	static class ColumnTypeQuerier implements ResultSetHandler<Triple<String, Boolean, Map<String, Pair<String, String>>>> {

		@Override
		public Triple<String, Boolean, Map<String, Pair<String, String>>> handle(ResultSet rs) throws SQLException {
			String idClassSimpleName = null;
			boolean isAutoIncrementId = false;
			Map<String, Pair<String, String>> column_type_comment = new LinkedHashMap<String, Pair<String, String>>();
			while (rs.next()) {
				String column = rs.getString(1).toLowerCase();
				String type = rs.getString(2).toLowerCase();
				String key = rs.getString(5);
				String extra = rs.getString(7);
				String comment = StringUtils.replace(StringUtils.replace(rs.getString(9), "\r\n", ""), "\n", "");
				if (StringUtils.isNotBlank(key) && StringUtils.containsIgnoreCase(key, "PRI")) {
					if (null != idClassSimpleName) {
						System.out.println("error error error error error error, only single primary key allowed!!");
						return null;
					}

					if (StringUtils.containsIgnoreCase(extra, "auto_increment")) {
						isAutoIncrementId = true;
					}

					if (StringUtils.containsIgnoreCase(type, "char")) {
						idClassSimpleName = "String";
					} else if (StringUtils.containsIgnoreCase(type, "int")) {
						if (StringUtils.containsIgnoreCase(type, "big")) {
							idClassSimpleName = "Long";
						} else {
							idClassSimpleName = "Integer";
						}
					}
				}

				column_type_comment.put(column, Pair.of(type, comment));
				System.out.println(column + " " + type);
			}

			return Triple.of(idClassSimpleName, isAutoIncrementId, column_type_comment);
		}
	}

	private static final String convertName(String src, String toBeRemoved) {
		if (StringUtils.isNotBlank(toBeRemoved)) {
			src = StringUtils.removeStart(src, toBeRemoved);
		}

		StringBuilder sb = new StringBuilder();
		for (String s : src.split("_")) {
			if (0 < sb.length()) {
				s = StringUtils.capitalize(s);
			}
			sb.append(s);
		}

		return sb.toString();
	}

	private static final Dto resolveFieldClass(
			String table,
			Triple<String, Boolean, Map<String, Pair<String, String>>> idClassSimpleName_isAutoIncrId_column_type_comment) {
		String idClassSimpleName = idClassSimpleName_isAutoIncrId_column_type_comment.getLeft();
		boolean isAutoIncrId = idClassSimpleName_isAutoIncrId_column_type_comment.getMiddle();
		Map<String, String> column_comment = new HashMap<>();
		List<Triple<String, String, Class>> column_field_class = new ArrayList<Triple<String, String, Class>>();
		for (Map.Entry<String, Pair<String, String>> entry : idClassSimpleName_isAutoIncrId_column_type_comment.getRight().entrySet()) {
			String column = entry.getKey();
			String type = entry.getValue().getLeft();
			String comment = entry.getValue().getRight();

			column_comment.put(column, comment);

			String field = convertName(column, COLUMN_FIX);
			Class clazz = null;
			if (StringUtils.contains(type, "int")) {
				if (StringUtils.containsIgnoreCase(type, "big")) {
					clazz = Long.class;
				} else {
					clazz = Integer.class;
				}
			} else if (StringUtils.contains(type, "char") || StringUtils.contains(type, "text")) {
				clazz = String.class;
			} else if (StringUtils.contains(type, "time") || StringUtils.contains(type, "date")) {
				clazz = Date.class;
			} else if (StringUtils.contains(type, "float") || StringUtils.contains(type, "double") || StringUtils.contains(type, "decimal")) {
				clazz = BigDecimal.class;
			} else {
				System.out.println("null clazz!!! table= " + table + ",  column= " + column + ",  type= " + type);
				System.exit(-1);
			}
			column_field_class.add(Triple.of(column, field, clazz));
			System.out.println(field + " " + clazz.getSimpleName());
		}

		Dto dto = new Dto();
		dto.setPo(StringUtils.capitalize(convertName(table, TABLE_FIX)));
		dto.setIdClassSimpleName(idClassSimpleName);
		dto.setIsAutoIncrId(isAutoIncrId);
		dto.setColumn_comment(column_comment);
		dto.setColumn_field_class(column_field_class);
		return dto;
	}

	private static final Triple<String, Boolean, Map<String, Pair<String, String>>> queryColumnType(String table) {
		DbUtils.loadDriver(DB_DRIVER);
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

			return new QueryRunner().query(conn, "SHOW FULL COLUMNS FROM " + table, new ColumnTypeQuerier());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			DbUtils.closeQuietly(conn);
		}
	}

	private static final void saveFile(String sub, String fileName, List<String> lines) {
		try {
			File file = new File((fileName.endsWith(".java") ? SRC_DIR : RESOURCES_DIR) + File.separatorChar + sub
					+ File.separatorChar
//					+ SCHEMA + File.separatorChar
					+ fileName);
			if ("po".equals(sub) && file.exists()) {
				try {
					poStaticConst(file, lines);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			FileUtils.writeLines(file, lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final void poStaticConst(File file, List<String> lines) throws Exception {
		List<String> staticLines = new ArrayList<>();
		boolean staticBegin = false;
		for (String line : FileUtils.readLines(file)) {
			if (staticBegin) {
				if (0 <= line.indexOf("private")) {
					break;
				} else {
					staticLines.add(line);
				}
			} else {
				if (0 <= line.indexOf("serialVersionUID")) {
					staticBegin = true;
				}
			}
		}

		int staticLinesSize = staticLines.size();
		if (1 < staticLinesSize) {
			if (StringUtils.isBlank(staticLines.get(staticLinesSize - 1)) && StringUtils.isBlank(staticLines.get(staticLinesSize - 2))) {
				staticLines.remove(staticLinesSize - 1);
			}

			int targetIndex = 0;
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				if (0 <= line.indexOf("serialVersionUID")) {
					targetIndex = i;
					break;
				}
			}

			lines.addAll(targetIndex + 1, staticLines);
		}
	}

	public static final void po(Dto dto) {
		String sub = "po";
		String po = dto.getPo();
		String idClassSimpleName = dto.getIdClassSimpleName();
		Map<String, String> column_comment = dto.getColumn_comment();
		List<Triple<String, String, Class>> column_field_class = dto.getColumn_field_class();
		Set<Class> classSet = column_field_class.stream().map(triple -> triple.getRight()).collect(Collectors.toSet());

		List<String> lines = new ArrayList<String>();

		BeanGenerator beanGenerator = new BeanGenerator();
		beanGenerator.setSuperclass(BlankPo.class);

		for (Triple<String, String, Class> triple : column_field_class) {
			beanGenerator.addProperty(triple.getMiddle(), triple.getRight());
		}

		Long serialVersionUID = null;
		try {
			serialVersionUID = ObjectStreamClass.lookup(beanGenerator.create().getClass()).getSerialVersionUID();
			System.out.println(serialVersionUID);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		lines.add("package " + BASE_PACKAGE + sub + (SCHEMA_IN_CODE ? "." + SCHEMA : "") + ";");
		lines.add("");
		if (classSet.contains(BigDecimal.class)) {
			lines.add("import java.math.BigDecimal;");
		}
		if (classSet.contains(Date.class)) {
			lines.add("import java.util.Date;");
		}
		lines.add("import java.util.Map;");
		lines.add("import java.util.stream.Collectors;");
		lines.add("import java.util.stream.Stream;");
		lines.add("");
		lines.add("import " + COMMON_PACKAGE + "consts.ToStringObject;");
		lines.add("import " + COMMON_PACKAGE + "po.IPo;");
		lines.add("import org.apache.commons.lang3.tuple.Pair;");
		lines.add("");
		lines.add("public class " + po + " extends ToStringObject implements IPo<" + idClassSimpleName + "> {");
		lines.add("\tprivate static final long serialVersionUID = " + serialVersionUID + "L;");
		lines.add("");
		for (Triple<String, String, Class> triple : column_field_class) {
			lines.add("\tprivate " + triple.getRight().getSimpleName() + " " + triple.getMiddle() + "; // " + column_comment.get(triple.getLeft()));
		}
		lines.add("");
		for (Triple<String, String, Class> triple : column_field_class) {
			String field = triple.getMiddle();
			lines.add("\tpublic " + triple.getRight().getSimpleName() + " get" + StringUtils.capitalize(field) + "() {");
			lines.add("\t\treturn " + field + ";");
			lines.add("\t}");
			lines.add("");
			lines.add("\tpublic void set" + StringUtils.capitalize(field) + "(" + triple.getRight().getSimpleName() + " " + field + ") {");
			lines.add("\t\tthis." + field + " = " + field + ";");
			lines.add("\t}");
			lines.add("");
		}
		String idField = dto.getColumn_field_class().get(0).getMiddle();
		if (!"id".equals(idField)) {
			lines.add("\t@Override");
			lines.add("\tpublic " + dto.getColumn_field_class().get(0).getRight().getSimpleName() + " getId() {");
			lines.add("\t\treturn " + idField + ";");
			lines.add("\t}");
			lines.add("");
			lines.add("\t@Override");
			lines.add("\tpublic void setId(" + dto.getColumn_field_class().get(0).getRight().getSimpleName() + " " + idField + ") {");
			lines.add("\t\tthis." + idField + " = " + idField + ";");
			lines.add("\t}");
			lines.add("");
		}
		lines.add("}");
		saveFile(sub, po + ".java", lines);
	}

	public static final void mapper(Dto dto, String table, boolean needDelete, boolean needInsertIgnore, boolean needSelectForUpdate, boolean needUnion) {
		mapperJava(dto, table, needDelete, needInsertIgnore, needSelectForUpdate, needUnion);
		mapperXml(dto, table, needDelete, needInsertIgnore, needSelectForUpdate, needUnion);
	}

	private static final void mapperJava(Dto dto, String table, boolean needDelete, boolean needInsertIgnore, boolean needSelectForUpdate, boolean needUnion) {
		String sub = "mapper";
		String po = dto.getPo();
		String idClassSimpleName = dto.getIdClassSimpleName();
		List<Triple<String, String, Class>> column_field_class = dto.getColumn_field_class();

		List<String> lines = new ArrayList<String>();
		lines.add("package " + BASE_PACKAGE + sub + (SCHEMA_IN_CODE ? "." + SCHEMA : "") + ";");
		lines.add("");
		if (needDelete) {
			lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlDeletableMapper;");
		}
		if (needInsertIgnore) {
			lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlInsertIgnoreMapper;");
		}
		lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlMapper;");
		if (needSelectForUpdate) {
			lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlSelectForUpdateMapper;");
		}
		if (needUnion) {
			lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlUnionMapper;");
		}
		lines.add("import " + BASE_PACKAGE + "po." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + ";");
		lines.add("import org.springframework.stereotype.Repository;");
		lines.add("");
		lines.add("@Repository");
		String template = idClassSimpleName + ", " + po;
		String parentMapper = "IMysqlMapper<" + template + ">";
		if (needDelete) {
			parentMapper += ", IMysqlDeletableMapper<" + template + ">";
		}
		if (needInsertIgnore) {
			parentMapper += ", IMysqlInsertIgnoreMapper<" + template + ">";
		}
		if (needSelectForUpdate) {
			parentMapper += ", IMysqlSelectForUpdateMapper<" + template + ">";
		}
		if (needUnion) {
			parentMapper += ", IMysqlUnionMapper<" + template + ">";
		}
		lines.add("public interface " + po + "Mapper extends " + parentMapper + " {");
		lines.add("\tString TABLE_NAME = \"" + table.toLowerCase() + "\";");
		lines.add("");
		int i = 0;
		for (Triple<String, String, Class> triple : column_field_class) {
			String columnLowerCase = triple.getLeft().toLowerCase();
			lines.add("\tString " + columnLowerCase + " = \"" + columnLowerCase + "\";");
			i++;
		}
		lines.add("}");
		saveFile(sub, po + "Mapper.java", lines);
	}

	private static final void mapperXml(Dto dto, String table, boolean needDelete, boolean needInsertIgnore, boolean needSelectForUpdate, boolean needUnion) {
		String sub = "mapper";
		String po = dto.getPo();
		String idClassSimpleName = dto.getIdClassSimpleName();
		boolean isAutoIncrId = dto.getIsAutoIncrId();
		List<Triple<String, String, Class>> column_field_class = dto.getColumn_field_class();

		List<String> lines = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();

		lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		lines.add("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">");
		lines.add("<mapper namespace=\"" + BASE_PACKAGE + "mapper." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + "Mapper\">");
		lines.add("\t<!-- <cache eviction=\"LRU\" flushInterval=\"300000\" size=\"128\" readOnly=\"true\" /> -->");
		lines.add("\t<resultMap type=\"" + BASE_PACKAGE + "po." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + "\" id=\"" + StringUtils.uncapitalize(po) + "\">");
		int i = 0;
		String keyColumn = null;
		String keyField = null;
		for (Triple<String, String, Class> triple : column_field_class) {
			String temp = "result";
			if (0 == i) {
				keyColumn = triple.getLeft();
				keyField = triple.getMiddle();
				temp = "id";
			}
			lines.add("\t\t<" + temp + " column=\"" + triple.getLeft() + "\" property=\"" + triple.getMiddle() + "\" />");
			i++;
		}
		lines.add("\t</resultMap>");

		lines.add("");
		lines.add("\t<sql id=\"sqlKey\">`" + keyColumn + "`</sql>");

		lines.add("");
		lines.add("\t<sql id=\"sqlTable\">`" + table + "`</sql>");

		lines.add("");
		lines.add("\t<sql id=\"sqlColumns\">");
		i = 0;
		for (Triple<String, String, Class> triple : column_field_class) {
			if (0 == i) {
				sb.append("\t\t");
				i++;
			}
			if (2 <= i) {
				sb.append(", ");
			}
			sb.append("`").append(triple.getLeft()).append("`");
			i++;
		}
		lines.add(sb.toString());
		sb.delete(0, sb.length());
		lines.add("\t</sql>");

		lines.add("");
		lines.add("\t<sql id=\"sqlVaryingColumns\">");
		lines.add("\t\t<choose>");
		lines.add("\t\t\t<when test=\"selectColumnList == null\">");
		lines.add("\t\t\t\t<include refid=\"sqlColumns\" />");
		lines.add("\t\t\t</when>");
		lines.add("\t\t\t<otherwise>");
		lines.add("\t\t\t\t<foreach collection=\"selectColumnList\" item=\"column\" separator=\",\">");
		lines.add("\t\t\t\t\t`${column}`");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</otherwise>");
		lines.add("\t\t</choose>");
		lines.add("\t</sql>");

		lines.add("");
		lines.add("\t<sql id=\"sqlInsert\">");
		lines.add("\t\tINSERT INTO <include refid=\"sqlTable\" />");
		lines.add("\t\t(<include refid=\"sqlColumns\" />)");
		lines.add("\t\tVALUES");
		lines.add("\t</sql>");

		if (needInsertIgnore) {
			lines.add("");
			lines.add("\t<sql id=\"sqlInsertIgnore\">");
			lines.add("\t\tINSERT IGNORE INTO <include refid=\"sqlTable\" />");
			lines.add("\t\t(<include refid=\"sqlColumns\" />)");
			lines.add("\t\tVALUES");
			lines.add("\t</sql>");
		}

		lines.add("");
		lines.add("\t<sql id=\"sqlFields\">");
		sb.append("\t\t(");
		i = 0;
		for (Triple<String, String, Class> triple : column_field_class) {
			if (0 == i) {
				i++;
			}
			if (2 <= i) {
				sb.append(", ");
			}
			sb.append("#{").append(triple.getMiddle()).append("}");
			i++;
		}
		lines.add(sb.append(")").toString());
		sb.delete(0, sb.length());
		lines.add("\t</sql>");

		lines.add("");
		lines.add("\t<sql id=\"sqlFieldsList\">");
		lines.add("\t\t<foreach collection=\"list\" item=\"po\" separator=\",\">");
		i = 0;
		for (Triple<String, String, Class> triple : column_field_class) {
			if (0 == i) {
				sb.append("\t\t\t(");
				i++;
			}
			if (2 <= i) {
				sb.append(", ");
			}
			sb.append("#{po.").append(triple.getMiddle()).append("}");
			i++;
		}
		lines.add(sb.append(")").toString());
		sb.delete(0, sb.length());
		lines.add("\t\t</foreach>");
		lines.add("\t</sql>");

		lines.add("");
		lines.add("\t<sql id=\"sqlOnDuplicateKeyUpdate\">");
		lines.add("\t\tON DUPLICATE KEY UPDATE");
		i = 0;
		for (Triple<String, String, Class> triple : column_field_class) {
			if (0 == i) {
				sb.append("\t\t");
				i++;
			}
			if (duplicateIngoreColumns.contains(triple.getLeft())) {
				continue;
			}
			if (2 <= i) {
				sb.append(", ");
			}
			sb.append("`").append(triple.getLeft()).append("`=values(`").append(triple.getLeft()).append("`)");
			i++;
		}
		lines.add(sb.toString());
		sb.delete(0, sb.length());
		lines.add("\t</sql>");

		lines.add("");
		lines.add("\t<sql id=\"sqlWhere\">");
		lines.add("\t\t<where>");
		lines.add("\t\t\t<if test=\"_eq != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_eq.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} = #{entry.value}");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t\t<if test=\"_ne != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_ne.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} &lt;&gt; #{entry.value}");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t\t<if test=\"_like != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_like.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} LIKE #{entry.value}");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t\t<if test=\"_gt != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_gt.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} &gt; #{entry.value}");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t\t<if test=\"_gte != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_gte.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} &gt;= #{entry.value}");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t\t<if test=\"_lt != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_lt.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} &lt; #{entry.value}");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t\t<if test=\"_lte != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_lte.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} &lt;= #{entry.value}");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t\t<if test=\"_in != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_in.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} IN");
		lines.add("\t\t\t\t\t<foreach collection=\"entry.value\" item=\"item\" separator=\",\" open=\"(\" close=\")\">");
		lines.add("\t\t\t\t\t\t#{item}");
		lines.add("\t\t\t\t\t</foreach>");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t\t<if test=\"_isNull != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_isNull.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} IS NULL");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t\t<if test=\"_isNotNull != null\">AND");
		lines.add("\t\t\t\t<foreach collection=\"_isNotNull.entrySet()\" item=\"entry\" separator=\"AND\">");
		lines.add("\t\t\t\t\t${entry.key} IS NOT NULL");
		lines.add("\t\t\t\t</foreach>");
		lines.add("\t\t\t</if>");
		lines.add("\t\t</where>");
		lines.add("\t</sql>");

		lines.add("");
		lines.add("\t<sql id=\"sqlOrderBy\">");
		lines.add("\t\t<if test=\"orderByMap != null\">");
		lines.add("\t\tORDER BY");
		lines.add("\t\t\t<foreach collection=\"orderByMap.entrySet()\" item=\"orderByEntry\" separator=\",\">");
		lines.add("\t\t\t\t${orderByEntry.key} ${orderByEntry.value}");
		lines.add("\t\t\t</foreach>");
		lines.add("\t\t</if>");
		lines.add("\t\tLIMIT #{start}, #{offset}");
		lines.add("\t</sql>");

		lines.add("");
		lines.add(
				"\t<insert id=\"insert\" parameterType=\"" + BASE_PACKAGE + "po." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + "\" useGeneratedKeys=\"true\" keyProperty=\""
						+ keyField + "\">");
		lines.add("\t\t<include refid=\"sqlInsert\" />");
		lines.add("\t\t<include refid=\"sqlFields\" />");
		lines.add("\t</insert>");

		lines.add("");
		lines.add("\t<insert id=\"insertList\">");
		lines.add("\t\t<include refid=\"sqlInsert\" />");
		lines.add("\t\t<include refid=\"sqlFieldsList\" />");
		lines.add("\t</insert>");

		lines.add("");
		lines.add(
				"\t<insert id=\"save\" parameterType=\"" + BASE_PACKAGE + "po." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + "\" useGeneratedKeys=\"true\" keyProperty=\"" + keyField
						+ "\">");
		lines.add("\t\t<include refid=\"sqlInsert\" />");
		lines.add("\t\t<include refid=\"sqlFields\" />");
		lines.add("\t\t<include refid=\"sqlOnDuplicateKeyUpdate\" />");
		lines.add("\t</insert>");

		lines.add("");
		lines.add("\t<insert id=\"saveList\">");
		lines.add("\t\t<include refid=\"sqlInsert\" />");
		lines.add("\t\t<include refid=\"sqlFieldsList\" />");
		lines.add("\t\t<include refid=\"sqlOnDuplicateKeyUpdate\" />");
		lines.add("\t</insert>");

		if (needInsertIgnore) {
			lines.add("");
			lines.add(
					"\t<insert id=\"insertIgnore\" parameterType=\"" + BASE_PACKAGE + "po." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + "\" useGeneratedKeys=\"true\" keyProperty=\"" + keyField
							+ "\">");
			lines.add("\t\t<include refid=\"sqlInsertIgnore\" />");
			lines.add("\t\t<include refid=\"sqlFields\" />");
			lines.add("\t</insert>");

			lines.add("");
			lines.add("\t<insert id=\"insertIgnoreList\">");
			lines.add("\t\t<include refid=\"sqlInsertIgnore\" />");
			lines.add("\t\t<include refid=\"sqlFieldsList\" />");
			lines.add("\t</insert>");
		}

		if (needDelete) {
			lines.add("");
			lines.add("\t<delete id=\"delete\" parameterType=\"" + idClassSimpleName + "\">");
			lines.add("\t\tDELETE FROM <include refid=\"sqlTable\" />");
			lines.add("\t\tWHERE <include refid=\"sqlKey\" /> = #{" + keyField + "}");
			lines.add("\t</delete>");

			lines.add("");
			lines.add("\t<delete id=\"deleteList\" parameterType=\"java.util.Map\">");
			lines.add("\t\tDELETE");
			lines.add("\t\tFROM <include refid=\"sqlTable\" />");
			lines.add("\t\t<include refid=\"sqlWhere\" />");
			lines.add("\t\tAND 1 = 1");
			lines.add("\t</delete>");

			lines.add("");
			lines.add("\t<update id=\"updateList\" parameterType=\"java.util.Map\">");
			lines.add("\t\tUPDATE <include refid=\"sqlTable\" />");
			lines.add("\t\tSET");
			lines.add("\t\t<foreach collection=\"updateSetMap.entrySet()\" item=\"updateSetEntry\" separator=\",\">");
			lines.add("\t\t\t${updateSetEntry.key} = #{updateSetEntry.value}");
			lines.add("\t\t</foreach>");
			lines.add("\t\t<include refid=\"sqlWhere\" />");
			lines.add("\t\tAND 1 = 1");
			lines.add("\t</update>");
		}

		lines.add("");
		lines.add("\t<update id=\"update\" parameterType=\"" + BASE_PACKAGE + "po." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + "\">");
		lines.add("\t\tUPDATE <include refid=\"sqlTable\" />");
		lines.add("\t\tSET");
		i = 0;
		for (Triple<String, String, Class> triple : column_field_class) {
			String column = triple.getLeft();
			String field = triple.getMiddle();
			if (0 == i) {
				lines.add("\t\t\t<include refid=\"sqlKey\" /> = #{" + keyField + "}");
			} else {
				lines.add("\t\t\t<if test=\"" + field + " != null\">, `" + column + "` = #{" + field + "}</if>");
			}
			i++;
		}
		lines.add("\t\tWHERE <include refid=\"sqlKey\" /> = #{" + keyField + "}");
		lines.add("\t</update>");

		lines.add("");
		lines.add("\t<select id=\"select\" parameterType=\"" + idClassSimpleName + "\" resultMap=\"" + StringUtils.uncapitalize(po) + "\">");
		lines.add("\t\tSELECT <include refid=\"sqlColumns\" />");
		lines.add("\t\tFROM <include refid=\"sqlTable\" />");
		lines.add("\t\tWHERE <include refid=\"sqlKey\" /> = #{" + keyField + "}");
		lines.add("\t</select>");

		if (needSelectForUpdate) {
			lines.add("");
			lines.add("\t<select id=\"selectForUpdate\" parameterType=\"" + idClassSimpleName + "\" resultMap=\"" + StringUtils.uncapitalize(po) + "\">");
			lines.add("\t\tSELECT <include refid=\"sqlColumns\" />");
			lines.add("\t\tFROM <include refid=\"sqlTable\" />");
			lines.add("\t\tWHERE <include refid=\"sqlKey\" /> = #{" + keyField + "}");
			lines.add("\t\tFOR UPDATE");
			lines.add("\t</select>");
		}

		lines.add("");
		lines.add("\t<select id=\"selectList\" parameterType=\"java.util.Map\" resultMap=\"" + StringUtils.uncapitalize(po) + "\">");
		lines.add("\t\tSELECT <include refid=\"sqlVaryingColumns\" />");
		lines.add("\t\tFROM <include refid=\"sqlTable\" />");
		lines.add("\t\t<include refid=\"sqlWhere\" />");
		lines.add("\t\t<include refid=\"sqlOrderBy\" />");
		lines.add("\t</select>");

		lines.add("");
		lines.add("\t<select id=\"selectIdList\" parameterType=\"java.util.Map\" resultType=\"java.lang." + idClassSimpleName + "\">");
		lines.add("\t\tSELECT <include refid=\"sqlKey\" />");
		lines.add("\t\tFROM <include refid=\"sqlTable\" />");
		lines.add("\t\t<include refid=\"sqlWhere\" />");
		lines.add("\t\t<include refid=\"sqlOrderBy\" />");
		lines.add("\t</select>");

		lines.add("");
		lines.add("\t<select id=\"count\" parameterType=\"java.util.Map\" resultType=\"int\">");
		lines.add("\t\tSELECT COUNT(<include refid=\"sqlKey\" />)");
		lines.add("\t\tFROM <include refid=\"sqlTable\" />");
		lines.add("\t\t<include refid=\"sqlWhere\" />");
		lines.add("\t</select>");

		if (needUnion) {
			lines.add("");
			lines.add("\t<sql id=\"sqlUnion\">");
			lines.add("\t\t<choose>");
			lines.add("\t\t\t<when test=\"_unionlist != null\">");
			lines.add("\t\t\t\t<foreach collection=\"_unionlist\" item=\"unionMap\" separator=\"UNION\" open=\"(\" close=\") t\">");
			lines.add("\t\t\t\t\tSELECT <include refid=\"sqlVaryingColumns\" />");
			lines.add("\t\t\t\t\tFROM <include refid=\"sqlTable\" />");
			lines.add("\t\t\t\t\t<where>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_eq'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_eq'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} = #{entry.value}");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_ne'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_ne'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} &lt;&gt; #{entry.value}");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_like'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_like'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} LIKE #{entry.value}");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_gt'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_gt'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} &gt; #{entry.value}");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_gte'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_gte'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} &gt;= #{entry.value}");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_lt'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_lt'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} &lt; #{entry.value}");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_lte'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_lte'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} &lt;= #{entry.value}");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_in'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_in'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} IN");
			lines.add("\t\t\t\t\t\t\t\t<foreach collection=\"entry.value\" item=\"item\" separator=\",\" open=\"(\" close=\")\">");
			lines.add("\t\t\t\t\t\t\t\t\t#{item}");
			lines.add("\t\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_isNull'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_isNull'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} IS NULL");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t\t<if test=\"unionMap['_isNotNull'] != null\">AND");
			lines.add("\t\t\t\t\t\t\t<foreach collection=\"unionMap['_isNotNull'].entrySet()\" item=\"entry\" separator=\"AND\">");
			lines.add("\t\t\t\t\t\t\t\t${entry.key} IS NOT NULL");
			lines.add("\t\t\t\t\t\t\t</foreach>");
			lines.add("\t\t\t\t\t\t</if>");
			lines.add("\t\t\t\t\t</where>");
			lines.add("\t\t\t\t</foreach>");
			lines.add("\t\t\t</when>");
			lines.add("\t\t\t<otherwise>");
			lines.add("\t\t\t\t<include refid=\"sqlTable\" />");
			lines.add("\t\t\t</otherwise>");
			lines.add("\t\t</choose>");
			lines.add("\t</sql>");
			lines.add("");
			lines.add("\t<select id=\"selectListUnion\" parameterType=\"java.util.Map\" resultMap=\"" + StringUtils.uncapitalize(po) + "\">");
			lines.add("\t\tSELECT <include refid=\"sqlVaryingColumns\" />");
			lines.add("\t\tFROM");
			lines.add("\t\t<include refid=\"sqlUnion\" />");
			lines.add("\t\t<include refid=\"sqlOrderBy\" />");
			lines.add("\t</select>");
			lines.add("");
			lines.add("\t<select id=\"selectIdListUnion\" parameterType=\"java.util.Map\" resultType=\"java.lang." + idClassSimpleName + "\">");
			lines.add("\t\tSELECT <include refid=\"sqlKey\" />");
			lines.add("\t\tFROM");
			lines.add("\t\t<include refid=\"sqlUnion\" />");
			lines.add("\t\t<include refid=\"sqlOrderBy\" />");
			lines.add("\t</select>");
			lines.add("");
			lines.add("\t<select id=\"countUnion\" parameterType=\"java.util.Map\" resultType=\"int\">");
			lines.add("\t\tSELECT COUNT(1)");
			lines.add("\t\tFROM");
			lines.add("\t\t<include refid=\"sqlUnion\" />");
			lines.add("\t</select>");
		}

		lines.add("");
		lines.add("</mapper>");
		saveFile(sub, po + "Mapper.xml", lines);
	}

	public static final void dao(Dto dto, boolean needDelete, boolean needInsertIgnore, boolean needSelectForUpdate, boolean needUnion) {
		String sub = "dao";
		String po = dto.getPo();
		String idClassSimpleName = dto.getIdClassSimpleName();

		List<String> lines = new ArrayList<String>();
		lines.add("package " + BASE_PACKAGE + sub + (SCHEMA_IN_CODE ? "." + SCHEMA : "") + ";");
		lines.add("");
		lines.add("import org.slf4j.Logger;");
		lines.add("import org.slf4j.LoggerFactory;");
		lines.add("import org.springframework.beans.factory.annotation.Autowired;");
		lines.add("import org.springframework.stereotype.Repository;");
		lines.add("");
		lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlMapper;");
		if (needDelete) {
			lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlDeletableMapper;");
		}
		if (needInsertIgnore) {
			lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlInsertIgnoreMapper;");
		}
		if (needSelectForUpdate) {
			lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlSelectForUpdateMapper;");
		}
		if (needUnion) {
			lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlUnionMapper;");
		}
		lines.add("import " + COMMON_PACKAGE + "dao.ADao;");
		if (needDelete) {
			lines.add("import " + COMMON_PACKAGE + "dao.IDeletableDao;");
		}
		if (needInsertIgnore) {
			lines.add("import " + COMMON_PACKAGE + "dao.IInsertIgnoreDao;");
		}
		if (needSelectForUpdate) {
			lines.add("import " + COMMON_PACKAGE + "dao.ISelectForUpdateDao;");
		}
		if (needUnion) {
			lines.add("import " + COMMON_PACKAGE + "dao.IUnionDao;");
		}
		lines.add("import " + BASE_PACKAGE + "mapper." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + "Mapper;");
		lines.add("import " + BASE_PACKAGE + "po." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + ";");
		lines.add("");
		lines.add("@Repository");
		String template = idClassSimpleName + ", " + po;
		String parentInterfaceDaos = "";
		if (needDelete || needInsertIgnore || needSelectForUpdate || needUnion) {
			parentInterfaceDaos = " implements ";
			List<String> parentInterfaceList = new ArrayList<>();
			if (needDelete) {
				parentInterfaceList.add("IDeletableDao<" + template + ">");
			}
			if (needInsertIgnore) {
				parentInterfaceList.add("IInsertIgnoreDao<" + template + ">");
			}
			if (needSelectForUpdate) {
				parentInterfaceList.add("ISelectForUpdateDao<" + template + ">");
			}
			if (needUnion) {
				parentInterfaceList.add("IUnionDao<" + template + ">");
			}
			parentInterfaceDaos += StringUtils.join(parentInterfaceList, ", ");
		}
		lines.add("public class " + po + "Dao extends ADao<" + template + ">" + parentInterfaceDaos + " {");
		lines.add("\tprivate static final Logger log = LoggerFactory.getLogger(" + po + "Dao.class);");
		lines.add("");
		lines.add("\t@Autowired");
		String mapperInstanceName = StringUtils.uncapitalize(po) + "Mapper;";
		lines.add("\tprivate " + po + "Mapper " + mapperInstanceName);
		lines.add("");
		lines.add("\t@Override");
		lines.add("\tpublic Class<" + po + "> getPoClass() {");
		lines.add("\t\treturn " + po + ".class;");
		lines.add("\t}");
		lines.add("");
		lines.add("\t@Override");
		lines.add("\tpublic IMysqlMapper<" + template + "> getMapper() {");
		lines.add("\t\treturn " + mapperInstanceName);
		lines.add("\t}");
		if (needDelete) {
			lines.add("");
			lines.add("\t@Override");
			lines.add("\tpublic IMysqlDeletableMapper<" + template + "> getDeletableMapper() {");
			lines.add("\t\treturn " + mapperInstanceName);
			lines.add("\t}");
		}
		if (needInsertIgnore) {
			lines.add("");
			lines.add("\t@Override");
			lines.add("\tpublic IMysqlInsertIgnoreMapper<" + template + "> getIMysqlInsertIgnoreMapper() {");
			lines.add("\t\treturn " + mapperInstanceName);
			lines.add("\t}");
		}
		if (needSelectForUpdate) {
			lines.add("");
			lines.add("\t@Override");
			lines.add("\tpublic IMysqlSelectForUpdateMapper<" + template + "> getSelectForUpdateMapper() {");
			lines.add("\t\treturn " + mapperInstanceName);
			lines.add("\t}");
		}
		if (needUnion) {
			lines.add("");
			lines.add("\t@Override");
			lines.add("\tpublic IMysqlUnionMapper<" + template + "> getUnionMapper() {");
			lines.add("\t\treturn " + mapperInstanceName);
			lines.add("\t}");
		}
		lines.add("}");
		saveFile(sub, po + "Dao.java", lines);
	}

	private static final Map<String, String> idClassSimpleName_str2IdFunc = SCollectionUtil.toMap(
			"Integer", "return Integer::valueOf;",
			"Long", "return Long::valueOf;",
			"String", "return idStr -> idStr;"
	);

	public static final void redisCacheDao(Dto dto) {
		String sub = "dao";
		String po = dto.getPo();
		String idClassSimpleName = dto.getIdClassSimpleName();

		List<String> lines = new ArrayList<String>();
		lines.add("package " + BASE_PACKAGE + sub + (SCHEMA_IN_CODE ? "." + SCHEMA : "") + ";");
		lines.add("");
		lines.add("import " + BASE_PACKAGE + "mapper." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + "Mapper;");
		lines.add("import " + BASE_PACKAGE + "po." + (SCHEMA_IN_CODE ? SCHEMA + "." : "") + po + ";");
		lines.add("import " + COMMON_PACKAGE + "dao.ARedisCachePoDao;");
		lines.add("import " + COMMON_PACKAGE + "mapper.IMysqlMapper;");
		lines.add("import " + COMMON_PACKAGE + "util.MySqlParamMapBuilder;");
		lines.add("import org.slf4j.Logger;");
		lines.add("import org.slf4j.LoggerFactory;");
		lines.add("import org.springframework.beans.factory.annotation.Autowired;");
		lines.add("import org.springframework.stereotype.Repository;");
		lines.add("");
		lines.add("import java.util.List;");
		lines.add("import java.util.Map;");
		lines.add("import java.util.function.Function;");
		lines.add("");
		lines.add("@Repository");
		String template = idClassSimpleName + ", " + po;
		lines.add("public class " + po + "Dao extends ARedisCachePoDao<" + template + "> {");
		lines.add("\tprivate static final Logger log = LoggerFactory.getLogger(" + po + "Dao.class);");
		lines.add("");
		lines.add("\t@Autowired");
		String mapperInstanceName = StringUtils.uncapitalize(po) + "Mapper";
		lines.add("\tprivate " + po + "Mapper " + mapperInstanceName + ";");
		lines.add("");
		lines.add("\t@Override");
		lines.add("\tpublic Class<" + po + "> getPoClass() {");
		lines.add("\t\treturn " + po + ".class;");
		lines.add("\t}");
		lines.add("");
		lines.add("\t@Override");
		lines.add("\tpublic IMysqlMapper<" + template + "> getMapper() {");
		lines.add("\t\treturn " + mapperInstanceName + ";");
		lines.add("\t}");
		lines.add("");
		lines.add("\t@Override");
		lines.add("\tprotected List<" + po + "> getAllAvailableList() {");
		lines.add("\t\treturn " + mapperInstanceName + ".selectList(MySqlParamMapBuilder.createSelectAll());");
		lines.add("\t}");
		lines.add("");
		lines.add("\t@Override");
		lines.add("\tprotected Function<String, " + idClassSimpleName + "> getStr2IdFunc() {");
		lines.add("\t\t" + idClassSimpleName_str2IdFunc.get(idClassSimpleName));
		lines.add("\t}");
		lines.add("");
		lines.add("\t@Override");
		lines.add("\tprotected void initIdxCacheMap(Map<String, Function<" + po + ", String>> redisHashKey_fieldGroupClassifierFunc) {");
		lines.add("\t}");
		lines.add("}");
		saveFile(sub, po + "Dao.java", lines);
	}

	private static class Dto {
		private String po;
		private String idClassSimpleName;
		private boolean isAutoIncrId;
		private Map<String, String> column_comment = new HashMap<>();
		private List<Triple<String, String, Class>> column_field_class;

		public String getPo() {
			return po;
		}

		public void setPo(String po) {
			this.po = po;
		}

		public String getIdClassSimpleName() {
			return idClassSimpleName;
		}

		public void setIdClassSimpleName(String idClassSimpleName) {
			this.idClassSimpleName = idClassSimpleName;
		}

		public boolean getIsAutoIncrId() {
			return isAutoIncrId;
		}

		public void setIsAutoIncrId(boolean isAutoIncrId) {
			this.isAutoIncrId = isAutoIncrId;
		}

		public Map<String, String> getColumn_comment() {
			return column_comment;
		}

		public void setColumn_comment(Map<String, String> column_comment) {
			this.column_comment = column_comment;
		}

		public List<Triple<String, String, Class>> getColumn_field_class() {
			return column_field_class;
		}

		public void setColumn_field_class(List<Triple<String, String, Class>> column_field_class) {
			this.column_field_class = column_field_class;
		}
	}

	public static void main(String[] args) {
		init(args[0], false);

		for (Object[] table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache : new Object[][]{
				// "briefing", "cmm_task", "limited_keyword", "limited_url",
				// "media_industry",
				// "print_media", "sys_group", "users", "watch_channel",
				// "watch_company_note", "watch_geographies",
				// "watch_industries", "watch_list_page", "watch_log",
				// "watch_release_industries", "watch_result",
				// "watch_result_cmm", "watch_result_mobile", "watch_task",
				// "watch_thread", "watch_website",
				// "watch_website_types"

				// new Object[]{"t_mer_merch_info", false, false, false},
				// new Object[]{"t_mer_store_info", false, false, false},
//				 new Object[]{"t_prd_base_book", true, false, false},
//				 new Object[]{"t_prd_bak_book", true, false, false},
				// new Object[]{"t_approve_status_log", false, false, false},
				// new Object[]{"t_delete_flag_log", false, false, false},
				// new Object[]{"t_oms_user", true,true,true},
				// new Object[]{"t_oms_role", false, false, false},
				// new Object[]{"t_mer_merch_approve_info", true, true, false},
				// new Object[]{"t_mer_store_approve_info", true, true, false},
//				 new Object[]{"t_prd_aprv_list", true, true, false},
				// new Object[]{"t_auth_department", false, true, false},
				// new Object[]{"t_mer_reltype_info", true, false, false},
				// new Object[]{"t_mer_merchant_type_info", false, false, false},
				// new Object[]{"t_role_menu", false, false, false},
				// new Object[]{"t_auth_user_info", false, false, false},
				// new Object[]{"t_mer_reltype_approve_info", true, false, false},
				// new Object[]{"t_config", false, false, false},
				// new Object[]{"t_mer_acc_approve_info", false, true, false},
				// new Object[]{"t_mer_acc_info", false, false, false},
				// new Object[]{"t_mer_relacct_his", false, false, false},
				// new Object[]{"t_mer_relclear_approve_info", true, false, false},
				// new Object[]{"t_mer_relclear_info", true, false, false},
				// new Object[]{"t_mer_attach_approve_info", true, false, false},
				// new Object[]{"t_mer_link_bank", false, false, false},
				// new Object[]{"t_mer_relacct_his", false, false, false},
				// new Object[]{"t_bas_city_info", false, false, false},
				// new Object[]{"t_bas_distinct_info", false, false, false},
				// new Object[]{"t_bas_region_info", false, false, false},
				// new Object[]{"t_prd_labe_sale_info", true, false, false},
//				 new Object[]{"t_prd_merch_info", true, false, true},
				// new Object[]{"t_prd_label_info", false, false, false},
				// new Object[]{"t_prd_prod_stock", false, true, false},
				// new Object[]{"t_prd_rush_stock", false, false, false},
				// new Object[]{"t_prd_sold_info", false, false, false},
				// new Object[]{"t_prd_activity", true, false, false},
//				 new Object[]{"t_test", true, true, true},
				// new Object[]{"t_prd_adver_map_info", true, false, false},
				// new Object[]{"t_prd_advertise_info", false, true, true},
				// new Object[]{"t_prd_adver_city", true, false, false},
				// new Object[]{"t_mer_onego_rate_approve_info", false, false, false},
//				 new Object[]{"t_mer_onego_rate_info", false, false, false},
//				 new Object[]{"t_mer_prd_score_rule", false, false, false},
//				 new Object[]{"t_mer_prd_info", false, false, false},
//			new Object[]{"t_mer_netpay_info", false, false, true},
//			new Object[]{"t_vou_voucher_info", false, true, false},
//		 new Object[]{"t_vou_arithmetic", false, false, false},
//			new Object[]{"t_check_point", false, false, false},
//				new Object[]{"t_quartz_log", false, false, false},
//				new Object[]{"t_prd_json_cache_info", false, false, false},
//				new Object[]{"t_quartz_info", false, false, false},
//				new Object[]{"t_update_solr_queue", true, false, false},
//				new Object[]{"t_subsidy_party", false, false, false},
//				new Object[]{"t_chnl_shopwindow_info", false, true, false},
//				new Object[]{"t_chnl_shopwindow_pic_list", true, false, false},
//				new Object[]{"t_chnl_shopwindow_city_list", true, false, false},
//				new Object[]{"t_oms_user_petition", false, true, false},
				//
				//
				//
				// new Object[]{"t_cou_code_info", false, false, false},
				// new Object[]{"t_cou_cop_list", false, false, false},
				// new Object[]{"t_cou_vfc_list", false, false, false},
				// new Object[]{"t_ord_onego_list", false, false, false},
				// new Object[]{"t_ord_order_info", false, false, false},
				// new Object[]{"t_ord_tran_list", false, false, false},
//			new Object[]{"t_vou_code_info", false, false, false},
				//
				//
				//
				// new Object[]{"t_cop_code_org", true, false, false},
//				new Object[]{"t_cop_fill_code", true, false, false},
//				new Object[]{"t_cop_coupon", false, false, false},
//				new Object[]{"t_test_cop", false, false, false},
//				new Object[]{"t_cop_org_coupon", false, false, false},
//				new Object[]{"t_cop_org", false, false, false},
//				new Object[]{"t_cop_code", false, false, false},
				//
				//
				//
//				new Object[]{"t_etl_prd_list", false, false, false},


//				new Object[]{"cogs_expend",false,false,false,false,false},
//				new Object[]{"cogs_ori",false,false,false,false,false},
//				new Object[]{"cogs_recv",false,false,false,false,false},
//				new Object[]{"pay_dtl_cmbmb",true,false,false,false,false},
//				new Object[]{"conf_alias",true,false,false,false,false},
//				new Object[]{"conf_biz_mod",false,false,false,false,true},
//				new Object[]{"conf_province",false,false,false,false,true},
//				new Object[]{"conf_city",false,false,false,false,true},
//				new Object[]{"conf_cmb_branch",false,false,false,false,true},
//				new Object[]{"conf_district",false,false,false,false,true},
//				new Object[]{"conf_chnl",false,false,false,false,true},
//				new Object[]{"conf_chnl_pay_way",false,false,false,false,true},
//				new Object[]{"conf_dict",false,false,false,false,true},
//				new Object[]{"conf_pay_way",false ,false ,false,false,true},
//				new Object[]{"conf_platform",false,false,false,false,true},
//				new Object[]{"conf_platform_reg",false,false,false,false,true},
//				new Object[]{"conf_platform_stl_subject",false,false,false,false,true},
//				new Object[]{"conf_ship_carrier",false,false,false,false,true},
//				new Object[]{"conf_sku_category",false,false,false,false,true},
//				new Object[]{"conf_stl_diff", false, false, false, false, true},
//				new Object[]{"conf_stl_diff_except", false, false, false, false, true},
//				new Object[]{"conf_stl_diff_relation", false, false, false, false, true},
//				new Object[]{"conf_stl_except", false, false, false, false, true},
//				new Object[]{"conf_stl_opt", false, false, false, false, true},
//				new Object[]{"config",false ,false ,false,false,true},

//				new Object[]{"cust_address",true,false,false,false,false},
//				new Object[]{"cust_cart",true ,false ,false,false,false},
//				new Object[]{"cust_cart_his",false,false,false,false,false},

//				new Object[]{"cust_info",false,false,false,true,false},
//				new Object[]{"inv_sku_chk", true, true, false, false, false},
//              new Object[]{"inv_change_petition",false,false,false,false,false},
//				new Object[]{"inv_log",false,false,false,false,false},
//				new Object[]{"inv_log_queue",true ,false ,false,false,false},
//				new Object[]{"inv_remaining",false,true,false,false,false},
//				new Object[]{"inv_sold",false ,false ,false,false,false},
//				new Object[]{"inv_sold_cust_limit",false,false,false,false,false},
//				new Object[]{"inv_sold_platform_monthly",false,false,false,false,false},
//				new Object[]{"inv_sold_repo_monthly",false,false,false,false,false},
//				new Object[]{"oms_controller_role",true ,false ,false,true,false},
//				new Object[]{"oms_div_role",true,false,false,true,true},
//				new Object[]{"oms_op_log",false ,false ,false,false,false},
//				new Object[]{"oms_role",false ,false ,false,false,true},
//				new Object[]{"oms_dept",false ,false ,false,false,true},
//				new Object[]{"oms_req_param_role_val",false ,false ,false,false,true},
//				new Object[]{"oms_usr",false,false,false,false,false},
//				new Object[]{"oms_usr_role",true,false,false,true,false},
//				new Object[]{"ord_invoice",true ,false ,false,false,false},
//				new Object[]{"ord_item",true,false,false,false,false},
//				new Object[]{"ord_master",true,false,false,false,false},
//				new Object[]{"ord_op_log",true,false,false,false,false},
//				new Object[]{"ord_vendor",true,false,false,false,false},
//				new Object[]{"ord_vou",true ,false ,false,false,false},
//				new Object[]{"rma_op_log",true,false,false,false,false},
//				new Object[]{"rma_ord",true ,false ,false,false,false},
//				new Object[]{"rma_pic",true ,false ,false,false,false},
//				new Object[]{"rma_subtotal",true,false,false,false,false},
//				new Object[]{"opt_pos",false,false,false,false,false},
//				new Object[]{"opt_sku_category",false,false,false,true,false},
//				new Object[]{"platform_sku",true,false,false,false,false},
//				new Object[]{"platform_sku_chk",true,false,false,false,false},
//				new Object[]{"platform_sku_chk_log",false ,false ,false,false,false},
//				new Object[]{"platform_sku_quota",true,false,false,false,false},
//				new Object[]{"platform_sku_quota_log",false ,false ,false,false,false},
//				new Object[]{"repo_price",true,false,false,true,false},
//				new Object[]{"repo_price_chk",true,false,false,false,false},
//                new Object[]{"repo_price_chk_log",false ,false ,false,false,false},
//				new Object[]{"repo_sku",true,false,false,false,false},
//				new Object[]{"repo_sku_chk",true,false,false,false,false},
//                new Object[]{"repo_sku_quota",true,false,false,false,false},
//                new Object[]{"repo_sku_quota_log",true,false,false,false,false},
//				new Object[]{"shlf_sku",true,false,false,true,false},
//				new Object[]{"shlf_info",false,false,false,false,false},
//				new Object[]{"sku_grp_category_chk",true,false,false,false,false},
//				new Object[]{"sku_grp_desc_chk",true,false,false,false,false},
//				new Object[]{"sku_grp_info_chk",true,false,false,false,false},
//				new Object[]{"sku_info_chk",true,false,false,false,false},
//				new Object[]{"sku_pic_chk",true ,false ,false,false,false},
//				new Object[]{"sku_prop_chk",true,false,false,false,false},
//				new Object[]{"sku_chk_diff",false ,false ,false,false,false},
//				new Object[]{"sku_draft",true ,false ,false,false,false},
//				new Object[]{"sku_grp_category",true,false,false,false,false},
//				new Object[]{"sku_grp_desc",true,false,false,false,false},
//				new Object[]{"sku_grp_info",true,false,false,false,false},
//				new Object[]{"sku_grp_tag",true ,false ,false,false,false},
				//new Object[]{"sku_info",true,false,false,false,false},
//				new Object[]{"sku_pic",true ,false ,false,true,false},
//				new Object[]{"sku_platform_black_list",true ,false ,false,false,true},
//				new Object[]{"sku_platform_black_list_chk",true ,false ,false,false,false},
//				new Object[]{"sku_prop",true,false,false,false,false},
//				new Object[]{"sp_controller_role",true,false,false,true,false},
//				new Object[]{"sp_div_role",true ,false ,false,true,false},
//				new Object[]{"sp_op_log",false,false,false,false,false},
//				new Object[]{"sp_role",true ,false ,false,false,true},
//				new Object[]{"sp_req_param_role_val",false ,false ,false,false,true},
//				new Object[]{"sp_usr",false ,false ,false,false,false},
//				new Object[]{"sp_usr_role",true ,false ,false,true,false},
//				new Object[]{"stl_batch_date", false,false,false,false,false},
//				new Object[]{"stl_cmb", false,false,false,false,false},
//				new Object[]{"stl_except_patron", false,false,false,false,false},
//				new Object[]{"stl_except_self", false,false,false,false,false},
//				new Object[]{"stl_master", true,false,false,false,false},
//				new Object[]{"stl_pay_dtl", false,false,false,false,false},
//				new Object[]{"stl_pay_dtl_subject", false,false,false,false,false},
// 				new Object[]{"stl_monthly_bill_result", false,false,false,false,false},

//				new Object[]{"vou_code_cust",true,false,false,true,false},
//				new Object[]{"vou_code",false,true,false,false,false},
//				new Object[]{"vou_info",false ,false ,false,true,false},
//                new Object[]{"vou_op_log",false ,false ,false,false,false},
//                new Object[]{"vou_op_fail",true ,false ,false,false,false},
//				new Object[]{"vou_limit_date",true,false,false,false,false},
//				new Object[]{"vou_limit_pay_way",true ,false ,false,false,false},
//				new Object[]{"vou_limit_platform_chnl_biz",true ,false ,false,false,false},
//				new Object[]{"vou_limit_sku_grp",true ,false ,false,false,false},
//				new Object[]{"vou_limit_vendor",true,false,false,false,false},
//				new Object[]{"vndr_attachment",true ,false ,false,false,false},
//				new Object[]{"vndr_info",false,false,false,false,false},
//				new Object[]{"vndr_rma_warehouse",false ,false ,false,false,false},
//                new Object[]{"vndr_attachment_chk",true ,false ,false,false,false},
//                new Object[]{"vndr_info_chk",false,false,false,false,false},
//                new Object[]{"vndr_rma_warehouse_chk",false ,false ,false,false,false},
//				new Object[]{"lck_function",false,false,true,false,false},
//                new Object[]{"lck_platform_resale",false,false,true,false,false},
//                new Object[]{"lck_platform_retail",false,false,true,false,false},
//                new Object[]{"lck_sku",false,false,true,false,false},
//                new Object[]{"lck_vndr",false ,false ,true,false,false},
//                new Object[]{"lck_vou",false,false,true,false,false},
//                new Object[]{"log_upload_oms",false,false,false,false,false},
//                new Object[]{"log_upload_sp",false,false,false,false,false},
//                new Object[]{"refund_info",false,false,false,false,false},
//                new Object[]{"data_vendor",true,false,false,false,false},
//                new Object[]{"data_vendor_instant",true,false,false,false,false},
//                new Object[]{"data_oms",true,false,false,false,false},
//                new Object[]{"data_oms_instant",true,false,false,false,false},
				null}) {
			if (null == table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache) {
				continue;
			}

			String table = ((String) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[0]).toLowerCase();
			Boolean needDelete = (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[1];
			Boolean needInsertIgnore = (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[2];
			Boolean needSelectForUpdate = (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[3];
			Boolean needUnion = (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[4];
			Boolean isRedisCache = table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache.length < 5 ? false : (Boolean) table_needDelete_needInsertIgnore_needSelectForUpdate_needUnion_isRedisCache[5];
			Triple<String, Boolean, Map<String, Pair<String, String>>> idClassSimpleName_isAutoIncrId_column_type_comment = queryColumnType(table);
			if (null != idClassSimpleName_isAutoIncrId_column_type_comment) {
//				Dto dto = resolveFieldClass(table, idClassSimpleName_isAutoIncrId_column_type_comment);
//				po(dto);
//				mapper(dto, table, needDelete, needInsertIgnore, needSelectForUpdate, needUnion);
//				if (isRedisCache) {
//					redisCacheDao(dto);
//				} else {
//					dao(dto, needDelete, needInsertIgnore, needSelectForUpdate, needUnion);
//				}
			}
		}

	}
}
