package com.juntao.commons.spring.binder;

import java.beans.PropertyEditorSupport;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by major on 2016/9/26.
 */
public class MillisDateEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.isBlank(text)) {
            super.setValue(null);
        } else {
            super.setValue(new Date(Long.valueOf(StringUtils.stripToEmpty(text))));
        }
    }
}
