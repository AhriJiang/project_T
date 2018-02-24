package com.juntao.commons.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.juntao.commons.po.IPo;

public interface IMysqlUnionMapper<S extends Serializable, T extends IPo<S>> {

	List<T> selectListUnion(Map<String, Object> args);

	List<S> selectIdListUnion(Map<String, Object> args);

	int countUnion(Map<String, Object> args);
}
