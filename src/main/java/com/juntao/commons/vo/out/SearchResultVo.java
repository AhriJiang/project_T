package com.juntao.commons.vo.out;

import java.io.Serializable;
import java.util.List;

import com.juntao.commons.consts.ToStringObject;

/**
 * Created by major on 2017/1/10.
 */
public class SearchResultVo<T> extends ToStringObject implements Serializable {
    private static final long serialVersionUID = 8587776686079874702L;

    private Integer totalQty;
    private List<T> itemList;

    private SearchResultVo() {
    }

    public SearchResultVo(Integer totalQty, List<T> itemList) {
        this.totalQty = totalQty;
        this.itemList = itemList;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public List<T> getItemList() {
        return itemList;
    }

    public void setItemList(List<T> itemList) {
        this.itemList = itemList;
    }
}
