package com.juntao.commons.annotation;

/**
 * Created by major on 2017/6/30.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NeedStarMask {

	enum StarMaskType {COMMON_STRING, NAME, CELLPHONE, BANK_CARD, ID_CARD, EMAIL, TEL}

	StarMaskType value() default StarMaskType.COMMON_STRING;
}
