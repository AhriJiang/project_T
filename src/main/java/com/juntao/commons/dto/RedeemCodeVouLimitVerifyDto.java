package com.juntao.commons.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import com.juntao.commons.consts.ToStringObject;

/**
 * Created by major on 2016/12/22.
 */
public class RedeemCodeVouLimitVerifyDto extends ToStringObject implements Serializable {
    private static final long serialVersionUID = -7361090559040329737L;

    private String redeemCode;
    //    private Long skuNo;
    private BigDecimal subAmtRetail;
    private Integer subPointRetail;
    private String platformCode;
    private String chnlCode;
    private Long bizModNo;
    private Long vendorNo;
    private Long skuGrpNo;

    public String getRedeemCode() {
        return redeemCode;
    }

    public void setRedeemCode(String redeemCode) {
        this.redeemCode = redeemCode;
    }

//    public Long getSkuNo() {
//        return skuNo;
//    }
//
//    public void setSkuNo(Long skuNo) {
//        this.skuNo = skuNo;
//    }


    public BigDecimal getSubAmtRetail() {
        return subAmtRetail;
    }

    public void setSubAmtRetail(BigDecimal subAmtRetail) {
        this.subAmtRetail = subAmtRetail;
    }

    public Integer getSubPointRetail() {
        return subPointRetail;
    }

    public void setSubPointRetail(Integer subPointRetail) {
        this.subPointRetail = subPointRetail;
    }

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

    public Long getBizModNo() {
        return bizModNo;
    }

    public void setBizModNo(Long bizModNo) {
        this.bizModNo = bizModNo;
    }

    public Long getVendorNo() {
        return vendorNo;
    }

    public void setVendorNo(Long vendorNo) {
        this.vendorNo = vendorNo;
    }

    public Long getSkuGrpNo() {
        return skuGrpNo;
    }

    public void setSkuGrpNo(Long skuGrpNo) {
        this.skuGrpNo = skuGrpNo;
    }
}
