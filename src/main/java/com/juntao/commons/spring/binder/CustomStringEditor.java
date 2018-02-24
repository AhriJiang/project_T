package com.juntao.commons.spring.binder;

import org.apache.commons.lang3.StringUtils;

import com.juntao.commons.util.SEscapseCharUtil;

import java.beans.PropertyEditorSupport;

/**
 * Created by major on 2016/9/26.
 */
public class CustomStringEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        if (StringUtils.isNotBlank(text)) {
            this.setValue(SEscapseCharUtil.convertHalf2Full(StringUtils.stripToEmpty(text)));
        } else {
            this.setValue(null);
        }
    }
}
