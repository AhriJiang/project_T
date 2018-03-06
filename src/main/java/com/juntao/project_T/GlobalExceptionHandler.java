package com.juntao.project_T;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.juntao.commons.consts.ResponseConsts;
import com.juntao.commons.vo.out.ResponseVo;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * 处理所有接口数据验证异常
	 * 
	 * @param e
	 * @return
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseBody
	ResponseVo<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

		LOGGER.error(e.getMessage(), e);

		ResponseVo<?> response = new ResponseVo();
		response.setResMsg(e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
		return response;
	}

}
