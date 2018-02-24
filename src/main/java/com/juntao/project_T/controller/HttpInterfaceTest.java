package com.juntao.project_T.controller;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.juntao.project_T.dao.HttptestCasesMapper;
import com.juntao.project_T.entity.HttptestCases;
import com.juntao.project_T.requestVo.InsertTestCase;

@RestController
public class HttpInterfaceTest {
	
	@Autowired
	private HttptestCasesMapper httptestcase;

	@RequestMapping(value="/httpTestcase/list",method=RequestMethod.GET)
	public  HttptestCases getTestCaseList(String caseNo){
		//httpTestcase?caseNp={caseNo} 这种方式的去参数方式是甚么?多个参数查找又是怎么写的?
		return httptestcase.selectByPrimaryKey(Long.parseLong(caseNo));
		
	}
	
	@RequestMapping(value="/httpTestcase/detail",method=RequestMethod.GET)
	public  HttptestCases getTestCaseDetail(String caseNo){
		//httpTestcase?caseNp={caseNo}&caseName={caseName}
		//这种多个参数的情况的GET入参,(String caseName,String caseName)写多个入参几个, 或者写一个入参VO对象,将参数放入VO对象
		//不要在PO中改动get方法, 重新写一个outVo接收po的数据
		return httptestcase.selectByPrimaryKey(Long.parseLong(caseNo));
	}
	
	@RequestMapping(value="/httpTestcase/create",method=RequestMethod.POST)
	public String saveTestCase(@RequestBody InsertTestCase InsertTestCase) throws IllegalAccessException, InvocationTargetException{
		
		HttptestCases po=new HttptestCases();
		BeanUtils.copyProperties(po,InsertTestCase);
		httptestcase.insert(po);
		
		return "操作成功";
	}
}
