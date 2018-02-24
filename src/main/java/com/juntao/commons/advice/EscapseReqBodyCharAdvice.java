package com.juntao.commons.advice;

import com.juntao.commons.consts.ToStringObject;
import com.juntao.commons.util.SEscapseCharUtil;
import com.juntao.commons.util.SProcessPojoStringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/**
 * Created by major on 2017/6/30.
 */
//@ControllerAdvice
public class EscapseReqBodyCharAdvice extends RequestBodyAdviceAdapter {
	private static final Logger log = LoggerFactory.getLogger(EscapseReqBodyCharAdvice.class);

	@Override
	public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
		return true;
	}

	@Override
	public Object afterBodyRead(Object o, HttpInputMessage httpInputMessage, MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
		if (null == o) {
			return o;
		}

		try {
			SProcessPojoStringUtil.processPojo(o, fieldClass -> ToStringObject.class.isAssignableFrom(fieldClass),
					(field, strVal) -> true, (field, strVal) -> SEscapseCharUtil.convertHalf2Full(strVal));
		} catch (Exception e) {
			log.error("convertHalf2Full error!! requestBody pojo= " + o, e);
		}

		return o;
	}
}
