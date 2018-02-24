package com.juntao.commons.consts;

import com.juntao.commons.annotation.NeedStarMask;
import com.juntao.commons.util.StarMaskUtil;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public abstract class ToStringObject {

	@Override
	public String toString() {
		if (null != this && this.getClass().isAnnotationPresent(NeedStarMask.class)) {
			try {
				StarMaskUtil.starMaskVo(this);
			} catch (Exception e) {
			}
		}

		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
