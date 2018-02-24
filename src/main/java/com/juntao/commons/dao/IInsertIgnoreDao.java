package com.juntao.commons.dao;

import com.juntao.commons.function.UsefulFunctions;
import com.juntao.commons.mapper.IMysqlInsertIgnoreMapper;
import com.juntao.commons.po.IPo;
import com.juntao.commons.util.SCollectionUtil;
import com.juntao.commons.util.SEncryptPoUtil;
import com.juntao.commons.util.SKryoSerializeUtil;

import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public interface IInsertIgnoreDao<S extends Serializable, T extends IPo<S>> extends IPoClassAware<T> {
	IMysqlInsertIgnoreMapper<S, T> getIMysqlInsertIgnoreMapper();

	default int insertIgnore(T t) {
		if (null == t) {
			return -1;
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			t = SKryoSerializeUtil.copy(t);
			SEncryptPoUtil.encrypt(t);
		}

		return getIMysqlInsertIgnoreMapper().insertIgnore(t);
	}

	default int insertIgnoreList(List<T> tList) {
		return insertIgnoreList(tList, IDao.DEFAULT_SLICE_BATCH_SIZE);
	}

	default int insertIgnoreList(List<T> tList, int sliceBatchSize) {
		if (CollectionUtils.isEmpty(tList)) {
			return -1;
		}

		tList = tList.stream().filter(UsefulFunctions.notNull).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(tList)) {
			return -1;
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			tList = tList.stream().map(SKryoSerializeUtil::copy).collect(Collectors.toList());
			SEncryptPoUtil.encrypt(tList);
		}

		int i = 0;
		for (List<T> slicedTList : SCollectionUtil.sliceListByBatchSize(tList, sliceBatchSize)) {
			i += getIMysqlInsertIgnoreMapper().insertIgnoreList(slicedTList);
		}
		return i;
	}
}
