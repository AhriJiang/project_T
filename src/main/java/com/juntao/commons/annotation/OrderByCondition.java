package com.juntao.commons.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.juntao.commons.mapper.IMysqlMapper;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrderByCondition {

    Class<? extends IMysqlMapper> mapper();
}
