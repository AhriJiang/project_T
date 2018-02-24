package com.juntao.commons.annotation;

import java.lang.annotation.*;

import com.juntao.commons.mapper.IMysqlMapper;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SearchCondition {

    Class<? extends IMysqlMapper> mapper();

    String column();

    String operator();
}
