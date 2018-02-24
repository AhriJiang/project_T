package com.juntao.commons.dao;

import com.juntao.commons.mapper.IMysqlUnionMapper;
import com.juntao.commons.po.IPo;
import com.juntao.commons.util.MySqlParamMapBuilder;
import com.juntao.commons.util.SEncryptPoUtil;

import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface IUnionDao<S extends Serializable, T extends IPo<S>> extends IPoClassAware<T> {

	IMysqlUnionMapper<S, T> getUnionMapper();

	default List<T> selectListUnion(Map<String, Object> args) {
		if (CollectionUtils.isEmpty(args)) {
			return Collections.emptyList();
		}

		boolean isNeedEncrypt = SEncryptPoUtil.isNeedEncrpty(getPoClass());

		if (isNeedEncrypt) {
			args = MySqlParamMapBuilder.encryptParamMap(args, getPoClass());
		}

		List<T> tList = getUnionMapper().selectListUnion(args);
		if (isNeedEncrypt && !CollectionUtils.isEmpty(tList)) {
			SEncryptPoUtil.decrypt(tList);
		}

		return tList;
	}

	default List<S> selectIdListUnion(Map<String, Object> args) {
		if (CollectionUtils.isEmpty(args)) {
			return Collections.emptyList();
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			args = MySqlParamMapBuilder.encryptParamMap(args, getPoClass());
		}

		return getUnionMapper().selectIdListUnion(args);
	}

	default int countUnion(Map<String, Object> args) {
		if (CollectionUtils.isEmpty(args)) {
			return -1;
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			args = MySqlParamMapBuilder.encryptParamMap(args, getPoClass());
		}

		return getUnionMapper().countUnion(args);
	}
}
