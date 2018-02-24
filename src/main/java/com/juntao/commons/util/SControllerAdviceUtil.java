package com.juntao.commons.util;

import com.juntao.commons.annotation.ExcelExport;
import com.juntao.commons.consts.ResponseConsts;
import com.juntao.commons.spring.binder.CustomStringEditor;
import com.juntao.commons.spring.binder.MillisDateEditor;
import com.juntao.commons.spring.binder.StripNumberEditor;
import com.juntao.commons.vo.out.ResponseVo;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by major on 2017/3/1.
 */
public class SControllerAdviceUtil {

/*	private static final Function<Throwable, ResponseVo<Object>> funcMethodArgumentNotValidException = throwable ->
			new ResponseVo<Object>(ResponseConsts.FAIL, null,
					((MethodArgumentNotValidException) throwable).getBindingResult().getFieldError().getDefaultMessage());

	private static final Function<Throwable, ResponseVo<Object>> funcBindException = throwable ->
			new ResponseVo<Object>(ResponseConsts.FAIL, null,
					((BindException) throwable).getBindingResult().getFieldError().getDefaultMessage());

	private static final Function<Throwable, ResponseVo<Object>> funcConstraintViolationException = throwable ->
			new ResponseVo<Object>(ResponseConsts.FAIL, null,
					((ConstraintViolationException) throwable).getConstraintViolations().iterator().next().getMessage());*/

	private static final Function<Throwable, ResponseVo<Object>> funcMethodArgumentNotValidException = throwable ->
			new ResponseVo<Object>(Pair.of(ResponseConsts.FAIL.getLeft(),
					((MethodArgumentNotValidException) throwable).getBindingResult().getFieldError().getDefaultMessage()), null);

	private static final Function<Throwable, ResponseVo<Object>> funcBindException = throwable ->
			new ResponseVo<Object>(Pair.of(ResponseConsts.FAIL.getLeft(),
					((BindException) throwable).getBindingResult().getFieldError().getDefaultMessage()), null);

	private static final Function<Throwable, ResponseVo<Object>> funcConstraintViolationException = throwable ->
			new ResponseVo<Object>(Pair.of(ResponseConsts.FAIL.getLeft(),
					((ConstraintViolationException) throwable).getConstraintViolations().iterator().next().getMessage()), null);

	private static final Map<Class<? extends Throwable>, Function<Throwable, ResponseVo<Object>>> exceptionFuncMap = SCollectionUtil.toMap(
			MethodArgumentNotValidException.class, funcMethodArgumentNotValidException,
			BindException.class, funcBindException,
			ConstraintViolationException.class, funcConstraintViolationException
	);

	public static final Object exceptionHandler(HandlerMethod handlerMethod, HttpServletResponse response, Throwable throwable,
												Map<Class<? extends Throwable>, Function<Throwable, ResponseVo<Object>>> specificExceptionFuncMap) {
		Optional<ResponseVo<Object>> responseVoOptional = Optional.ofNullable(
				Optional.ofNullable(specificExceptionFuncMap).map(x -> {
					x.putAll(exceptionFuncMap);
					return x;
				}).orElse(exceptionFuncMap)
						.get(throwable.getClass())).map(func -> func.apply(throwable));

		ExcelExport anno = handlerMethod.getMethod().getAnnotation(ExcelExport.class);
		if (null != anno) {
			SExcelUtil.downloadErrorReturn(
					responseVoOptional.orElse(new ResponseVo<Object>(
							ResponseVo.fillStringFunc.apply(ResponseConsts.REQUEST_PARAM_ERROR, Arrays.asList(anno.msg())), null)),
					response);
			return null;
		} else {
			return responseVoOptional.orElse(new ResponseVo(ResponseConsts.FAIL, null, throwable.getMessage()));
		}
	}

	public static final void initBinder(WebDataBinder webDataBinder) {
		initBinder(webDataBinder, new CustomStringEditor());
	}

	public static final void initBinder(WebDataBinder webDataBinder, PropertyEditorSupport stringEditor) {
		Optional.ofNullable(stringEditor).ifPresent(x -> webDataBinder.registerCustomEditor(String.class, x));
		webDataBinder.registerCustomEditor(Date.class, new MillisDateEditor());
		Stream.of(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, BigInteger.class, BigDecimal.class)
				.forEach(numberClass -> webDataBinder.registerCustomEditor(numberClass, new StripNumberEditor(numberClass)));
	}
}
