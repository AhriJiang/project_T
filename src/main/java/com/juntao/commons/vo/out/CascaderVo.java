package com.juntao.commons.vo.out;

import java.io.Serializable;
import java.util.List;

import com.juntao.commons.consts.ToStringObject;

/**
 * Created by psw on 2017/3/6.
 * 页面级联下拉框用VO
 */
public class CascaderVo<T extends CascaderVo> extends ToStringObject implements Serializable {
	private static final long serialVersionUID = 2652448931399344608L;

	private String value;
	private String label;
	private List<T> children;

	public CascaderVo() {
	}

	public CascaderVo(String value, String label, List<T> children) {
		this.value = value;
		this.label = label;
		this.children = children;
	}

	public List<T> getChildren() {
		return children;
	}

	public void setChildren(List<T> children) {
		this.children = children;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
