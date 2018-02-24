package com.juntao.commons.dao;

import com.juntao.commons.mapper.IMysqlDeletableMapper;
import com.juntao.commons.po.IPo;
import com.juntao.commons.util.MySqlParamMapBuilder;
import com.juntao.commons.util.SEncryptPoUtil;

import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Map;

public interface IDeletableDao<S extends Serializable, T extends IPo<S>> extends IPoClassAware<T> {

	IMysqlDeletableMapper<S, T> getDeletableMapper();

	default int delete(S id) {
		if (null == id) {
			return -1;
		}

		return getDeletableMapper().delete(id);
	}

	default int deleteList(Map<String, Object> args) {
		if (CollectionUtils.isEmpty(args)) {
			return -1;
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			args = MySqlParamMapBuilder.encryptParamMap(args, getPoClass());
		}

		return getDeletableMapper().deleteList(args);
	}

	default int updateList(Map<String, Object> args) {
		if (CollectionUtils.isEmpty(args)) {
			return -1;
		}

		if (SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			args = MySqlParamMapBuilder.encryptParamMap(args, getPoClass());
		}

		return getDeletableMapper().updateList(args);
	}
}
