package com.juntao.commons.dao;

import com.juntao.commons.function.UsefulFunctions;
import com.juntao.commons.mapper.IMysqlMapper;
import com.juntao.commons.po.IPo;
import com.juntao.commons.util.MySqlParamMapBuilder;
import com.juntao.commons.util.SCollectionUtil;
import com.juntao.commons.util.SEncryptPoUtil;
import com.juntao.commons.util.SKryoSerializeUtil;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface IDao<S extends Serializable, T extends IPo<S>> extends IPoClassAware<T> {

	int DEFAULT_SLICE_BATCH_SIZE = 1000;

	Function<Object, String> id2StringFunc = id -> StringUtils.stripToEmpty(String.valueOf(ObjectUtils.defaultIfNull(id, StringUtils.EMPTY)));

	IMysqlMapper<S, T> getMapper();

	default int insert(T t) {
		if (null == t) {
			return -1;
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			t = SKryoSerializeUtil.copy(t);
			SEncryptPoUtil.encrypt(t);
		}

		return getMapper().insert(t);
	}

	default int insertList(List<T> tList) {
		return insertList(tList, IDao.DEFAULT_SLICE_BATCH_SIZE);
	}

	default int insertList(List<T> tList, int sliceBatchSize) {
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
			i += getMapper().insertList(slicedTList);
		}
		return i;
	}

	default int save(T t) {
		if (null == t) {
			return -1;
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			t = SKryoSerializeUtil.copy(t);
			SEncryptPoUtil.encrypt(t);
		}

		return getMapper().save(t);
	}

	default int saveList(List<T> tList) {
		return saveList(tList, IDao.DEFAULT_SLICE_BATCH_SIZE);
	}

	default int saveList(List<T> tList, int sliceBatchSize) {
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
			i += getMapper().saveList(slicedTList);
		}
		return i;
	}

	default int update(T t) {
		if (null == t || StringUtils.isBlank(id2StringFunc.apply(t.getId()))) {
			return -1;
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			t = SKryoSerializeUtil.copy(t);
			SEncryptPoUtil.encrypt(t);
		}

		return getMapper().update(t);
	}

	default T select(S id) {
		if (null == id) {
			return null;
		}

		T t = getMapper().select(id);
		if (null != t && SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			SEncryptPoUtil.decrypt(t);
		}

		return t;
	}

	default List<T> selectList(Map<String, Object> args) {
		if (CollectionUtils.isEmpty(args)) {
			return Collections.emptyList();
		}

		boolean isNeedEncrypt = SEncryptPoUtil.isNeedEncrpty(getPoClass());

		if (isNeedEncrypt) {
			args = MySqlParamMapBuilder.encryptParamMap(args, getPoClass());
		}

		List<T> tList = getMapper().selectList(args);
		if (isNeedEncrypt && !CollectionUtils.isEmpty(tList)) {
			SEncryptPoUtil.decrypt(tList);
		}

		return tList;
	}

	default List<S> selectIdList(Map<String, Object> args) {
		if (CollectionUtils.isEmpty(args)) {
			return Collections.emptyList();
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			args = MySqlParamMapBuilder.encryptParamMap(args, getPoClass());
		}

		return getMapper().selectIdList(args);
	}

	default int count(Map<String, Object> args) {
		if (CollectionUtils.isEmpty(args)) {
			return -1;
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			args = MySqlParamMapBuilder.encryptParamMap(args, getPoClass());
		}

		return getMapper().count(args);
	}
}
