package com.juntao.commons.dto;

import java.io.Serializable;

import com.juntao.commons.consts.ToStringObject;

/**
 * Created by HouKun on 2017/2/4.
 */
public class DiffDto extends ToStringObject implements Serializable {
    private static final long serialVersionUID = 6477239649499881615L;

    private String fieldName;

    private Object oldValue;

    private Object newValue;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }


    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }
}
