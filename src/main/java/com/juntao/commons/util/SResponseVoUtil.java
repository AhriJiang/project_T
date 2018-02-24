package com.juntao.commons.util;

import com.juntao.commons.consts.SConsts;
import com.juntao.commons.vo.out.ResponseVo;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by major on 2017/6/30.
 */
public class SResponseVoUtil {

	public static final void reponseReturn(HttpServletResponse response, ResponseVo responseVo) throws Exception {
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding(SConsts.UTF_8);
		response.getWriter().write(SJacksonUtil.compress(responseVo));
	}
}
