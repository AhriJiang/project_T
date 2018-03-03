package com.juntao.project_T.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.juntao.commons.consts.ResponseConsts;
import com.juntao.commons.vo.out.ResponseVo;
import com.juntao.project_T.dao.HttptestCasesMapper;
import com.juntao.project_T.entity.HttptestCases;
import com.juntao.project_T.vo.in.InsertTestCase;
import com.juntao.project_T.vo.out.HttptestCasesVoOut;

@RestController
public class HttpInterfaceTest {

	@Autowired
	private HttptestCasesMapper httptestcase;

	@RequestMapping(value = "/httpTestcase/list", method = RequestMethod.GET)
	public ResponseVo<List<HttptestCasesVoOut>> getTestCaseList()
			throws IllegalAccessException, InvocationTargetException {

		// 拿到cookie 然后去redis找这个角色信息,找到这个人的team, 暂时写死为1
		Long teamid = (long) 1;

		List<HttptestCases> testCasesList = httptestcase.selectCasesList(teamid);
		List<HttptestCasesVoOut> testCasesListOutVo = new ArrayList<HttptestCasesVoOut>();

		for (int i = 0; i < testCasesList.size(); i++) {
			HttptestCasesVoOut testCaseDetailOut = new HttptestCasesVoOut();
			BeanUtils.copyProperties(testCaseDetailOut, testCasesList.get(i));
			testCasesListOutVo.add(testCaseDetailOut);
		}

		ResponseVo<List<HttptestCasesVoOut>> Response = new ResponseVo<List<HttptestCasesVoOut>>(ResponseConsts.SUCCESS,
				testCasesListOutVo);
		return Response;

	}

	@RequestMapping(value = "/httpTestcase/detail", method = RequestMethod.GET)
	public ResponseVo<HttptestCasesVoOut> getTestCaseDetail(
			@NotBlank(message = "caseNo不可为空") @NotNull(message = "caseNo不可为null") @NotEmpty(message = "caseNo不可为empty") String caseNo)
			throws IllegalAccessException, InvocationTargetException {
		// httpTestcase?caseNp={caseNo}&caseName={caseName}
		// 这种多个参数的情况的GET入参,(String caseName,String caseName)写多个入参几个,
		// 或者写一个入参VO对象,将参数放入VO对象

		ResponseVo Response = new ResponseVo<>();

		if (caseNo.equals(null)) {
			return Response = Response = new ResponseVo(ResponseConsts.REQUEST_PARAM_ERROR, null);
		}

		HttptestCases testCaseDetail = httptestcase.selectByPrimaryKey(Long.parseLong(caseNo));
		HttptestCasesVoOut testCaseDetailOut = new HttptestCasesVoOut();

		// 不要在PO中改动get方法, 重新写一个outVo接收po的数据

		try {
			BeanUtils.copyProperties(testCaseDetailOut, testCaseDetail);
		} catch (Exception e) {
			return Response = new ResponseVo(ResponseConsts.CASENO_NOT_EXIST, null);
		}

		if (!testCaseDetailOut.getId().equals(null)) {
			Response = new ResponseVo<HttptestCasesVoOut>(ResponseConsts.SUCCESS, testCaseDetailOut);
		}
		return Response;
	}

	@RequestMapping(value = "/httpTestcase/create", method = RequestMethod.POST)
	public String saveTestCase(@RequestBody InsertTestCase InsertTestCase)
			throws IllegalAccessException, InvocationTargetException {

		HttptestCases po = new HttptestCases();
		BeanUtils.copyProperties(po, InsertTestCase);
		httptestcase.insert(po);

		return "操作成功";
	}
}
