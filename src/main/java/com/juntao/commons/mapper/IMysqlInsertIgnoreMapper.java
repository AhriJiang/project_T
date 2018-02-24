package com.juntao.commons.mapper;

import java.io.Serializable;
import java.util.List;

import com.juntao.commons.po.IPo;

public interface IMysqlInsertIgnoreMapper<S extends Serializable, T extends IPo<S>> {

	// @WriteDataSource
	int insertIgnore(T t);

	// @WriteDataSource
	int insertIgnoreList(List<T> tList);
}
