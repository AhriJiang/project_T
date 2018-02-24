package com.juntao.commons.vo.out;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.juntao.commons.consts.ResponseConsts;

import java.io.Serializable;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by major on 2016/11/21.
 */
public class ResponseVo<T> implements Serializable {
    private static final long serialVersionUID = 1990023423405231338L;

    private static final String PLACE_HOLDER = "${}";
    public static final BiFunction<Pair<String, String>, List<String>, Pair<String, String>> fillStringFunc = (oriPair, fillerList) -> {
        String message = fillerList.stream()
                .reduce(oriPair.getRight(),(msg,filler) -> StringUtils.replaceOnce(msg, PLACE_HOLDER, filler));
        return Pair.of(oriPair.getLeft(), message);
    };

    public static final ResponseVo SUCCESS = new ResponseVo<Object>(ResponseConsts.SUCCESS, null);
    public static final ResponseVo FAIL = new ResponseVo<Object>(ResponseConsts.FAIL, null);

    private String resCode;
    private String resMsg;
    private T result;
    private Object byproduct;

    public ResponseVo() {
    }

    public ResponseVo(ResponseVo responseVo, T result) {
        this.resCode = responseVo.getResCode();
        this.resMsg = responseVo.getResMsg();
        this.result = result;
        this.byproduct = null;
    }

    public ResponseVo(ResponseVo responseVo, T result, Object byproduct) {
        this.resCode = responseVo.getResCode();
        this.resMsg = responseVo.getResMsg();
        this.result = result;
        this.byproduct = byproduct;
    }

    public ResponseVo(Pair<String, String> resCode_resMsg, T result) {
        this.resCode = resCode_resMsg.getLeft();
        this.resMsg = resCode_resMsg.getRight();
        this.result = result;
        this.byproduct = null;
    }

    public ResponseVo(Pair<String, String> resCode_resMsg, T result, Object byproduct) {
        this.resCode = resCode_resMsg.getLeft();
        this.resMsg = resCode_resMsg.getRight();
        this.result = result;
        this.byproduct = byproduct;
    }

    public String getResCode() {
        return resCode;
    }

    public void setResCode(String resCode) {
        this.resCode = resCode;
    }

    public String getResMsg() {
        return resMsg;
    }

    public void setResMsg(String resMsg) {
        this.resMsg = resMsg;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public Object getByproduct() {
        return byproduct;
    }

    public void setByproduct(Object byproduct) {
        this.byproduct = byproduct;
    }

    public boolean isSuccess() {
        return ResponseConsts.SUCCESS.getLeft().equals(this.resCode);
    }
}
