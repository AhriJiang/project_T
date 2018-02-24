package com.juntao.commons.mapper;

import java.io.Serializable;
import java.util.Map;

import com.juntao.commons.po.IPo;

public interface IMysqlDeletableMapper<S extends Serializable, T extends IPo<S>> {

	// @WriteDataSource
	int delete(S id);

	// @WriteDataSource
	int deleteList(Map<String, Object> args);

	// @WriteDataSource
	int updateList(Map<String, Object> args);
}
