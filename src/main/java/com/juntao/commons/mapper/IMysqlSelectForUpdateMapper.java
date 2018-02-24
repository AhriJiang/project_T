package com.juntao.commons.mapper;

import java.io.Serializable;

import com.juntao.commons.po.IPo;

public interface IMysqlSelectForUpdateMapper<S extends Serializable, T extends IPo<S>> {

	// @WriteDataSource
	T selectForUpdate(S id);
}
