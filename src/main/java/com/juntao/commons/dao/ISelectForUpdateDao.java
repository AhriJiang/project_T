package com.juntao.commons.dao;

import com.juntao.commons.mapper.IMysqlSelectForUpdateMapper;
import com.juntao.commons.po.IPo;
import com.juntao.commons.util.SEncryptPoUtil;

import java.io.Serializable;

public interface ISelectForUpdateDao<S extends Serializable, T extends IPo<S>> extends IPoClassAware<T> {

	IMysqlSelectForUpdateMapper<S, T> getSelectForUpdateMapper();

	default T selectForUpdate(S id) {
		if (null == id) {
			return null;
		}

		T t = getSelectForUpdateMapper().selectForUpdate(id);
		if (null != t && SEncryptPoUtil.isNeedEncrpty(getPoClass())) {
			SEncryptPoUtil.decrypt(t);
		}

		return t;
	}
}
