package com.juntao.commons.spring.view;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.document.AbstractXlsxStreamingView;

import com.juntao.commons.consts.SConsts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by major on 2017/1/11.
 */
public class CustomExcelView extends AbstractXlsxStreamingView {
	private static final Logger log = LoggerFactory.getLogger(CustomExcelView.class);

	private String fileName;
	private Supplier<SXSSFWorkbook> workbookSupplier;

	private CustomExcelView() {
	}

	public CustomExcelView(String fileName, Supplier<SXSSFWorkbook> workbookSupplier) {
		this.fileName = fileName;
		this.workbookSupplier = workbookSupplier;
	}

	@Override
	protected SXSSFWorkbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return workbookSupplier.get();
	}

	@Override
	protected void buildExcelDocument(Map<String, Object> map, Workbook workbook, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
		httpServletResponse.setCharacterEncoding(SConsts.UTF_8);
		// 若想下载时自动填好文件名，则需要设置响应头的"Content-disposition"
		httpServletResponse.setHeader("Content-disposition", "attachment;filename="
				+ new String(fileName.getBytes(), SConsts.ISO_8859_1)
				+ DateFormatUtils.format(new Date(), SConsts.HYPHEN + SConsts.yyyyMMdd + SConsts.HYPHEN + SConsts.HHmmss)
				+ ".xlsx");
	}
}
