package com.juntao.commons.dao;

import java.io.Serializable;

import com.juntao.commons.po.IPo;

public abstract class ADao<S extends Serializable, T extends IPo<S>> implements IDao<S, T> {
}
