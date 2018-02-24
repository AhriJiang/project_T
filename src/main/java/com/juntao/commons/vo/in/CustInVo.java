package com.juntao.commons.vo.in;

import java.io.Serializable;

import com.juntao.commons.consts.ToStringObject;

/**
 * Created by major on 2016/12/21.
 */
public class CustInVo extends ToStringObject implements Serializable {
    private static final long serialVersionUID = 5205415840919707926L;

    private String platformCode;
    private String chnlCode;
    private Long custNo;
    private String custName;

    public String getPlatformCode() {
        return platformCode;
    }

    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode;
    }

    public String getChnlCode() {
        return chnlCode;
    }

    public void setChnlCode(String chnlCode) {
        this.chnlCode = chnlCode;
    }

    public Long getCustNo() {
        return custNo;
    }

    public void setCustNo(Long custNo) {
        this.custNo = custNo;
    }

    public String getCustName() {
        return custName;
    }

    public void setCustName(String custName) {
        this.custName = custName;
    }

    @Override
    public String toString() {
        return "CustInVo{" +
                "platformCode='" + platformCode + '\'' +
                ", chnlCode='" + chnlCode + '\'' +
                ", custNo=" + custNo +
                ", custName='" + custName + '\'' +
                '}';
    }
}
