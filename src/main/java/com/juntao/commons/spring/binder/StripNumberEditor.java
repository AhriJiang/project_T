package com.juntao.commons.spring.binder;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.CustomNumberEditor;

/**
 * Created by major on 2016/9/26.
 */
public class StripNumberEditor extends CustomNumberEditor {

    public StripNumberEditor(Class<? extends Number> numberClass) {
        super(numberClass, true);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        super.setAsText(StringUtils.stripToEmpty(text));
    }
}
