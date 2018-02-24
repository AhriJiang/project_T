package com.juntao.commons.spring.interceptor;

import com.juntao.commons.function.UsefulFunctions;
import com.juntao.commons.spring.view.ExcelView;
import com.juntao.commons.util.StarMaskUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * Created by AZ6247 on 2016/12/20.
 */
public class StartMaskExcelDtoInterceptor extends HandlerInterceptorAdapter {
	private static final Logger log = LoggerFactory.getLogger(StartMaskExcelDtoInterceptor.class);

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		if (null == modelAndView) {
			return;
		}

		StarMaskUtil.starMaskListVo(modelAndView.getModel().values().stream()
				.filter(obj -> ExcelView.ExportExcelDto.class.isAssignableFrom(obj.getClass()))
				.map(obj -> ((ExcelView.ExportExcelDto) obj).getVoList())
				.filter(UsefulFunctions.notEmptyCollection).findAny()
				.orElse(Collections.emptyList()));
	}
}
