package com.juntao.commons.dao;

import java.io.Serializable;

import com.juntao.commons.po.IPo;

/**
 * Created by major on 2017/7/1.
 */
public interface IPoClassAware<T extends IPo<? extends Serializable>> {
	Class<T> getPoClass();
}
