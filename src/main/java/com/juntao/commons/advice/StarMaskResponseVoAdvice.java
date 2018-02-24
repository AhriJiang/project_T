package com.juntao.commons.advice;

import com.juntao.commons.util.StarMaskUtil;
import com.juntao.commons.vo.out.ResponseVo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Created by major on 2017/6/30.
 */
//@ControllerAdvice
public class StarMaskResponseVoAdvice implements ResponseBodyAdvice<Object> {
	private static final Logger log = LoggerFactory.getLogger(StarMaskResponseVoAdvice.class);

	@Override
	public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType,
								  Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest,
								  ServerHttpResponse serverHttpResponse) {
		if (null == o || !ResponseVo.class.isAssignableFrom(o.getClass())) {
			return o;
		}

		try {
			StarMaskUtil.starMaskVo(((ResponseVo) o).getResult());
		} catch (Exception e) {
			log.error("startMaskVo error!! ResponseVo= " + o, e);
		}

		return o;
	}
}
