package com.juntao.commons.spring.binder;

import java.beans.PropertyEditorSupport;

/**
 * Created by major on 2016/9/26.
 */
public class FilterSpecialCharactersStringEditor extends PropertyEditorSupport {

    public void setAsText(String text) {

        if (text == null || (text = text.trim()).length() == 0) {
            return;
        }
        try {
            //去除html标签
            String str = text.replaceAll("<[a-zA-Z]+[1-9]?[^><]*>", "")
                    .replaceAll("</[a-zA-Z]+[1-9]?>", "");
            setValue(str);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
