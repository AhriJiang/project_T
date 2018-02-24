package com.juntao.commons.vo.out;

import java.util.List;

/**
 * Created by huangchenggang on 2017/5/19.
 */
public class SearchResultWithStatisticsVo<S,T> extends SearchResultVo{
    private static final long serialVersionUID = -369174152801008817L;

    private List<S> statisticsList;

    public SearchResultWithStatisticsVo(Integer totalQty, List<T> itemList, List<S> statisticsList) {
        super(totalQty, itemList);
        this.statisticsList = statisticsList;
    }

    public List<S> getStatisticsList() {
        return statisticsList;
    }

    public void setStatisticsList(List<S> statisticsList) {
        this.statisticsList = statisticsList;
    }
}
