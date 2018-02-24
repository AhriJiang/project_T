package com.juntao.commons.security;

import org.apache.commons.lang3.StringUtils;

import com.juntao.commons.util.RSAUtil;
import com.juntao.commons.util.SJacksonUtil;
import com.juntao.commons.util.SnowflakeSequenceGenerator;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by xiajunjie on 2016/12/13.
 */
public class SecurityService {

    /**
     * 对请求进行签名
     *
     * @param params    待签名参数
     * @param privateKey  私钥
     * @return 签名后请求
     */
    public static String sign(Map<String, String> params, String privateKey) throws Exception {
        if (params == null || params.size() == 0) {
            throw new IllegalArgumentException("待签名参数不能为空!");
        }
        if (StringUtils.isBlank(privateKey)) {
            throw new IllegalArgumentException("私钥不能为空!");
        }

        Long random = new SnowflakeSequenceGenerator(Short.MAX_VALUE).nextId();
        params.put("random", random.toString());
        String url = getUrlParamsByMap(params, true, false);

        String sign;
        try {
            sign = RSAUtil.sign(url, privateKey);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeySpecException("密钥格式不对!" + e.getMessage());
        }
        params.put("sign", sign);

        return getUrlParamsByMap(params, true, true);
    }

    /**
     * 对响应进行验签
     *
     * @param response   响应
     * @param publicKey 公钥
     * @return 是否成功
     */
    public static boolean verify (String response, String publicKey) throws Exception {
        if (StringUtils.isBlank(response)) {
            throw new IllegalArgumentException("响应不能为空!");
        }
        if (StringUtils.isBlank(publicKey)) {
            throw new IllegalArgumentException("公钥不能为空!");
        }

        Map<String, String> params = SJacksonUtil.extractMap(response, String.class, String.class);
        String sign = params.get("sign");
        params.remove("sign");
        try {
            return RSAUtil.verify(getUrlParamsByMap(params, true, false), sign, publicKey);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeySpecException("密钥格式不对!" + e.getMessage());
        } catch (SignatureException e) {
            throw new SignatureException("签名格式不对" + e.getMessage());
        }
    }

    /**
     * 将map参数转为Url请求
     *
     * @param map map参数
     * @param isSort 是否排序
     * @param isUrlEncode 是否UrlEncode
     * @return
     */
    public static String getUrlParamsByMap(Map<String, String> map, boolean isSort, boolean isUrlEncode)
            throws UnsupportedEncodingException {
        Map<String, String> tempMap;
        if(isSort) {
            tempMap = new TreeMap<String, String>();
            tempMap.putAll(map);
        } else {
            tempMap = map;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : tempMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (StringUtils.isNotBlank(value)) {
                if (isUrlEncode) {
                    value = URLEncoder.encode(value, "UTF-8");
                }
                sb.append(key).append("=").append(value).append("&");
            }
        }

        String urlString = sb.toString();
        if (urlString.length() > 1) {
            urlString = urlString.substring(0, urlString.length() - 1);
        }
        return urlString;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("--------------生成密钥对---------------");
        RSAUtil.genKeyPair();
        String publicKeyStr = RSAUtil.getPublicKey();
        String privateKeyStr = RSAUtil.getPrivateKey();
        System.out.println("公钥: \n\r" + publicKeyStr);
        System.out.println("私钥： \n\r" + privateKeyStr);
        System.out.println();

        String source = "加密测试加密测试加密测试加密测试加密测试加密测试加密测试加密测试加密测试";
        System.out.println("加密前文字：\r\n" + source);
        String encodedData = RSAUtil.encryptByPublicKey(publicKeyStr, source);
        System.out.println("加密后文字：\r\n" + encodedData);
        String decodedData = RSAUtil.decryptByPrivateKey(privateKeyStr, encodedData);
        System.out.println("解密后文字: \r\n" + decodedData);
        System.out.println();

        Map<String, String> map = new HashMap<>();
        map.put("aa", "111");
        map.put("bb", "222");
        System.out.println("签名原串：\r\n" + getUrlParamsByMap(map, true, false));
        String sign = sign(map, privateKeyStr);
        System.out.println("签名串：\r\n" + sign);
        System.out.println();

        String mapString = SJacksonUtil.compress(map);
        System.out.println("验签结果：\r\n" + verify(mapString, publicKeyStr));
    }
}
