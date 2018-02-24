package com.juntao.commons.spring.view;

import com.juntao.commons.annotation.ExcelColumn;
import com.juntao.commons.consts.SConsts;
import com.juntao.commons.function.UsefulFunctions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.view.document.AbstractXlsxStreamingView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by major on 2017/1/11.
 */
public class ExcelView extends AbstractXlsxStreamingView {
    private static final Logger log = LoggerFactory.getLogger(ExcelView.class);

    public static final class ExportExcelDto {
        private String fileName;
        private List voList;

        private ExportExcelDto() {
        }

        public ExportExcelDto(String fileName, List voList) {
            this.fileName = fileName;
            this.voList = voList;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public List getVoList() {
            return voList;
        }

        public void setVoList(List voList) {
            this.voList = voList;
        }
    }

    public static final String EXPORT_EXCEL_DATA_KEY = "EXPORT_EXCEL_DATA_KEY";

    @Override
    protected void buildExcelDocument(Map<String, Object> map, Workbook workbook, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        ExportExcelDto exportExcelDto = (ExportExcelDto) map.values().stream()
                .filter(UsefulFunctions.notNull).filter(val -> ExportExcelDto.class == val.getClass()).findAny().orElse(null);
        if (null == exportExcelDto) {
            return;
        }

        String fileName = exportExcelDto.getFileName();
        List voList = exportExcelDto.getVoList();
        if (StringUtils.isBlank(fileName) || CollectionUtils.isEmpty(voList)) {
            return;
        }

        List<Pair<Field, ExcelColumn>> pairList = Stream.of(voList.get(0).getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ExcelColumn.class))
                .peek(field -> field.setAccessible(true))
                .map(field -> Pair.of(field, field.getAnnotation(ExcelColumn.class)))
                .collect(Collectors.toList());

        final int columnQty = pairList.size();
        List<String[]> lineList = new ArrayList<>();
        lineList.add(pairList.stream().map(Pair::getRight).map(ExcelColumn::header).toArray(String[]::new));
        for (Object vo : voList) {
            String[] line = new String[columnQty];
            for (int j = 0; j < columnQty; j++) {
                Pair<Field, ExcelColumn> pair = pairList.get(j);
                Object fieldValue = pair.getLeft().get(vo);
                String fieldValueStr = Optional.ofNullable(fieldValue).map(Object::toString).orElse(StringUtils.EMPTY);
                if (null != fieldValue) {
                    if (Date.class.isAssignableFrom(fieldValue.getClass())) {
                        fieldValueStr = Optional.ofNullable((Date) fieldValue)
                                .map(date -> DateFormatUtils.format(date, pair.getRight().dateFormat())).orElse(StringUtils.EMPTY);
                    }
                }

                line[j] = fieldValueStr;
            }
            lineList.add(line);
        }

        Font thFond = workbook.createFont();
        thFond.setBold(true);

        CellStyle thStyle = workbook.createCellStyle();
        thStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.index);
        thStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        thStyle.setFont(thFond);

        SXSSFSheet sheet = ((SXSSFWorkbook) workbook).createSheet();
        for (int i = 0; i < lineList.size(); i++) {
            String[] line = lineList.get(i);

            SXSSFRow row = sheet.createRow(i);
            for (int j = 0; j < columnQty; j++) {
                SXSSFCell cell = row.createCell(j);
                if (0 == i) {
                    cell.setCellStyle(thStyle);
                }

                cell.setCellValue(line[j]);
            }
        }

        httpServletResponse.setCharacterEncoding(SConsts.UTF_8);
        // 若想下载时自动填好文件名，则需要设置响应头的"Content-disposition"
        httpServletResponse.setHeader("Content-disposition", "attachment;filename="
                + new String(fileName.getBytes(), SConsts.ISO_8859_1)
                + DateFormatUtils.format(new Date(), SConsts.HYPHEN + SConsts.yyyyMMdd + SConsts.HYPHEN + SConsts.HHmmss)
                + ".xlsx");
    }
}
