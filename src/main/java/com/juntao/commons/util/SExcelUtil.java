package com.juntao.commons.util;

import com.juntao.commons.consts.SConsts;
import com.juntao.commons.vo.out.ResponseVo;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

@Component
public class SExcelUtil {

    public static final String EXCEL_TYPE_XLSX = "504B0304";// excel 2007

    public static final String EXCEL_TYPE_XLS = "D0CF11E0";// excel 2003

    public static final String[] EXCEL_File_SUFFIX = {"xls", "xlsx"};

    public static final String getCellValue(HSSFCell cell) {
        if (null == cell) {
            return StringUtils.EMPTY;
        }

        cell.setCellType(Cell.CELL_TYPE_STRING);
        return StringUtils.stripToEmpty(cell.getStringCellValue());
    }

    public static final String getCellValue(XSSFCell cell) {
        if (null == cell) {
            return StringUtils.EMPTY;
        }

        cell.setCellType(Cell.CELL_TYPE_STRING);
        return StringUtils.stripToEmpty(cell.getStringCellValue());
    }


    public static final String getCellValue(Cell cell) {
        if (null == cell) {
            return StringUtils.EMPTY;
        }

        cell.setCellType(Cell.CELL_TYPE_STRING);
        return StringUtils.stripToEmpty(cell.getStringCellValue());
    }

    public static final Workbook getWorkBook(MultipartFile file) throws IOException {

        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(SConsts.DOT) + 1);
        if (Stream.of(EXCEL_File_SUFFIX).noneMatch(s -> s.equals(suffix))) {
            return null;
        }
        String value;
        try (InputStream input = file.getInputStream()) {
            byte[] b = new byte[4];
            input.read(b, 0, b.length);
            value = Hex.encodeHexString(b);
        }
        try (InputStream input = file.getInputStream()) {
            if (EXCEL_TYPE_XLSX.equalsIgnoreCase(value)) {
                return new XSSFWorkbook(input);
            } else if (EXCEL_TYPE_XLS.equalsIgnoreCase(value)) {
                return new HSSFWorkbook(input);
            }
            // not an Excel file,return null;
            return null;
        }
    }

    public static final void downloadErrorReturn(ResponseVo<? extends Object> responseVo, HttpServletResponse response) {
        try {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            response.getOutputStream().write(
                    //                    ("<script language='javascript'>window.opener.$.alert('" + responseVo.getResMsg() + "');window.close();</script>").getBytes(Consts.UTF_8));
                    ("<script language='javascript'>window.alert('" + responseVo.getResMsg() + "');window.close();</script>").getBytes(Consts.UTF_8));

            response.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
