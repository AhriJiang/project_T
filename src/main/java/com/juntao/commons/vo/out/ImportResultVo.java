package com.juntao.commons.vo.out;

import java.io.Serializable;
import java.util.List;

import com.juntao.commons.consts.ToStringObject;

/**
 * Created by huangchenggang on 2017/3/9.
 */
public class ImportResultVo<T> extends ToStringObject implements Serializable {

    private static final long serialVersionUID = 4695735894687416372L;

    private Long succQty;

    private Long totalQty;

    private Long failureQty;

    private String opState;

    private SearchResultVo importLog;

    private List<String> titles;

    private List<T> errItems;

    private ImportResultVo() {
    }
    @Deprecated
    public ImportResultVo(Long totalQty, Long succQty, Long failureQty, List<T> errItems) {
        this.succQty = succQty;
        this.totalQty = totalQty;
        this.failureQty = failureQty;
        this.errItems = errItems;
    }

    public ImportResultVo(Long totalQty, Long succQty, Long failureQty, List<T> errItems, List<String> titles, SearchResultVo importLog,String opState) {
        this.succQty = succQty;
        this.totalQty = totalQty;
        this.failureQty = failureQty;
        this.importLog = importLog;
        this.titles = titles;
        this.errItems = errItems;
        this.opState = opState;
    }

    public String getOpState() {
        return opState;
    }

    public void setOpState(String opState) {
        this.opState = opState;
    }

    public Long getSuccQty() {
        return succQty;
    }

    public void setSuccQty(Long succQty) {
        this.succQty = succQty;
    }

    public Long getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Long totalQty) {
        this.totalQty = totalQty;
    }

    public Long getFailureQty() {
        return failureQty;
    }

    public void setFailureQty(Long failureQty) {
        this.failureQty = failureQty;
    }

    public SearchResultVo getImportLog() {
        return importLog;
    }

    public void setImportLog(SearchResultVo importLog) {
        this.importLog = importLog;
    }

    public List<String> getTitles() {
        return titles;
    }

    public void setTitles(List<String> titles) {
        this.titles = titles;
    }

    public List<T> getErrItems() {
        return errItems;
    }

    public void setErrItems(List<T> errItems) {
        this.errItems = errItems;
    }
}
