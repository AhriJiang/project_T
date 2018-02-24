package com.juntao.commons.dao;

import com.juntao.commons.mapper.IMysqlMapper;
import com.juntao.commons.po.IPo;
import com.juntao.commons.util.SKryoSerializeUtil;

import java.io.Serializable;
import java.util.function.Function;

/*
 * 所有可以全表放到redis里缓存的表，可以继承这个service
 * 泛型   S ：po的主键字段的class类型            T：po自己的class类型
 *
 */
public abstract class ARedisCachePoDao<S extends Serializable, T extends IPo<S>>
		extends ARedisCacheDao<S, T, T> {

	@Override
	protected Function<T, T> getPo2IdCacheValueFunc() {
		return t -> t;
	}

	@Override
	protected Function<T, String> getIdCacheValue2StringFunc() {
		return SKryoSerializeUtil::serialize;
	}

	@Override
	protected String getIdColumn() {
		return IMysqlMapper._ID;
	}

	@Override
	protected Function<String, T> getIdCacheString2ValueFunc() {
		return t -> SKryoSerializeUtil.unserialize(t, getPoClass());
	}
}
