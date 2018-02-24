package com.juntao.commons.util;

import net.iharder.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.juntao.commons.consts.SConsts;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Created by xiajunjie on 2016/12/13.
 */
public class RSAUtil {
    private static final Logger log = LoggerFactory.getLogger(RSAUtil.class);

    /**
     * 加密算法RSA
     */
    private static final String KEY_ALGORITHM = "RSA";
    /**
     * 获取公钥的key
     */
    private static final String PUBLIC_KEY = "RSAPublicKey";
    /**
     * 获取私钥的key
     */
    private static final String PRIVATE_KEY = "RSAPrivateKey";
    /**
     * RSA加密最大明文长度
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;
    /**
     * RSA解密最大密文长度
     */
    private static final int MAX_DECRYPT_BLOCK = 128;
    /**
     * 签名算法
     */
    private static final String SIGN_ALGORITHMS = "SHA256WithRSA";

    public static final KeyFactory keyFactory = ((Supplier<KeyFactory>) () -> {
        try {
            return KeyFactory.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error("加密签名发生错误!!!");
            System.exit(-1);
            return null;
        }
    }).get();


    /**
     * 随机生成密钥对
     */
    public static void genKeyPair() throws Exception {
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        // 初始化密钥对生成器，密钥大小为96-1024位
        keyPairGen.initialize(1024, new SecureRandom());
        // 生成一个密钥对，保存在keyPair中
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // 得到公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        // 得到私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 得到公钥字符串
        String publicKeyStr = Base64.encodeBytes(publicKey.getEncoded());
        // 得到私钥字符串
        String privateKeyStr = Base64.encodeBytes(privateKey.getEncoded());
        // 将密钥存入Properties文件
        Properties props = new Properties();
        FileOutputStream fos = new FileOutputStream("keystore.properties");
        props.setProperty(PUBLIC_KEY, publicKeyStr);
        props.setProperty(PRIVATE_KEY, privateKeyStr);
        props.store(fos, "Save Key");
        fos.close();
    }

    /**
     * 密钥加密(公/私钥)
     *
     * @param key        公钥/私钥
     * @param sourceData 明文数据
     * @return 密文
     * @throws Exception
     */
    public static String encrypt(Key key, String sourceData) throws Exception {
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] data = sourceData.getBytes(SConsts.UTF_8);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        int i = 0;
        byte[] cache;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return Base64.encodeBytes(encryptedData);
    }

    /**
     * 密钥解密(公/私钥)
     *
     * @param key           公钥/私钥
     * @param encryptedData 密文数据
     * @return 明文
     * @throws Exception
     */
    public static String decrypt(Key key, String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] data = Base64.decode(encryptedData);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        int i = 0;
        byte[] cache;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return new String(decryptedData, SConsts.UTF_8);
    }

    /**
     * 通过公钥字符串加载公钥
     *
     * @param publicKeyStr 公钥数据字符串
     * @return
     * @throws Exception
     */
    public static RSAPublicKey getPublicKeyByStr(String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    /**
     * 通过私钥字符串加载私钥
     *
     * @param privateKeyStr 私钥数据字符串
     * @return
     * @throws Exception
     */
    public static RSAPrivateKey getPrivateKeyByStr(String privateKeyStr) throws Exception {
        byte[] keyBytes = Base64.decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    /**
     * 获取公钥
     *
     * @return 公钥
     * @throws Exception
     */
    public static String getPublicKey() throws Exception {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream("keystore.properties");
        props.load(in);
        return props.getProperty(PUBLIC_KEY);
    }

    /**
     * 获取私钥
     *
     * @return 私钥
     * @throws Exception
     */
    public static String getPrivateKey() throws Exception {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream("keystore.properties");
        props.load(in);
        return props.getProperty(PRIVATE_KEY);
    }

    /**
     * 用私钥生成数字签名
     *
     * @param content    签名数据
     * @param privateKey 私钥
     * @return 数字签名
     */
    public static String sign(String content, String privateKey) throws Exception {
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKey));
        PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);

        Signature signature = Signature.getInstance(SIGN_ALGORITHMS);
        signature.initSign(priKey);
        signature.update(content.getBytes(SConsts.UTF_8));

        return Base64.encodeBytes(signature.sign());
    }

    /**
     * 校验数字签名
     *
     * @param content   签名数据
     * @param sign      数字签名
     * @param publicKey 公钥
     * @return 是否成功
     */
    public static boolean verify(String content, String sign, String publicKey) throws Exception {
        byte[] encodedKey = Base64.decode(publicKey);
        PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

        Signature signature = Signature.getInstance(SIGN_ALGORITHMS);
        signature.initVerify(pubKey);
        signature.update(content.getBytes(SConsts.UTF_8));

        return signature.verify(Base64.decode(sign));
    }

    /**
     * 一网通校验数字签名
     *
     * @param strToSign   签名数据
     * @param strSign      数字签名
     * @param publicKey 公钥
     * @return 是否成功
     */
    public static boolean isValidSignature(String strToSign, String strSign, String publicKey) throws Exception {

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.decode(publicKey);
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

            Signature signature = Signature.getInstance("SHA1WithRSA");

            signature.initVerify(pubKey);
            signature.update(strToSign.getBytes(SConsts.UTF_8));

            boolean bverify = signature.verify( Base64.decode(strSign) );

            return bverify;

    }

    /**
     * 公钥加密
     * @param publicKeyStr 公钥
     * @param source 明文
     * @return 密文
     */
    public static String encryptByPublicKey(String publicKeyStr, String source) throws Exception {
        if (StringUtils.isBlank(publicKeyStr)) {
            throw new IllegalArgumentException("公钥不能为空!");
        }
        if (StringUtils.isBlank(source)) {
            throw new IllegalArgumentException("报文不能为空!");
        }
        RSAPublicKey publicKey = getPublicKeyByStr(publicKeyStr);
        return RSAUtil.encrypt(publicKey, source);
    }

    /**
     * 私钥解密
     * @param privateKeyStr 私钥
     * @param source 密文
     * @return 明文
     */
    public static String decryptByPrivateKey(String privateKeyStr, String source) throws Exception {
        if (StringUtils.isBlank(privateKeyStr)) {
            throw new IllegalArgumentException("私钥不能为空!");
        }
        if (StringUtils.isBlank(source)) {
            throw new IllegalArgumentException("密文不能为空!");
        }
        RSAPrivateKey privateKey = getPrivateKeyByStr(privateKeyStr);
        return RSAUtil.decrypt(privateKey, source);
    }

    /**
     * 私钥加密
     * @param privateKeyStr 私钥
     * @param source 明文
     * @return 密文
     */
    public static String encryptByPrivateKey(String privateKeyStr, String source) throws Exception {
        if (StringUtils.isBlank(privateKeyStr)) {
            throw new IllegalArgumentException("私钥不能为空!");
        }
        if (StringUtils.isBlank(source)) {
            throw new IllegalArgumentException("报文不能为空!");
        }
        RSAPrivateKey privateKey = getPrivateKeyByStr(privateKeyStr);
        return RSAUtil.encrypt(privateKey, source);
    }

    /**
     * 公钥解密
     * @param publicKeyStr 公钥
     * @param source 密文
     * @return 明文
     */
    public static String decryptByPublicKey(String publicKeyStr, String source) throws Exception {
        if (StringUtils.isBlank(publicKeyStr)) {
            throw new IllegalArgumentException("公钥不能为空!");
        }
        if (StringUtils.isBlank(source)) {
            throw new IllegalArgumentException("密文不能为空!");
        }
        RSAPublicKey publicKey = getPublicKeyByStr(publicKeyStr);
        return RSAUtil.decrypt(publicKey, source);
    }



    public static void main(String[] args) throws Exception {
        System.out.println("--------------生成密钥对---------------");
        RSAUtil.genKeyPair();
        String publicKeyStr = RSAUtil.getPublicKey();
        String privateKeyStr = RSAUtil.getPrivateKey();
        RSAPublicKey publicKey = RSAUtil.getPublicKeyByStr(publicKeyStr);
        RSAPrivateKey privateKey = RSAUtil.getPrivateKeyByStr(privateKeyStr);
        System.out.println("公钥: \n\r" + publicKeyStr);
        System.out.println("私钥： \n\r" + privateKeyStr);
        System.out.println();

        System.out.println("--------------公钥加密私钥解密-------------------");
        String source = "这是一行测试公钥加密私钥解密的无意义文字";
        System.out.println("加密前文字：\r\n" + source);
        String encodedData = RSAUtil.encrypt(publicKey, source);
        System.out.println("加密后文字：\r\n" + encodedData);
        String decodedData = RSAUtil.decrypt(privateKey, encodedData);
        System.out.println("解密后文字: \r\n" + decodedData);
        System.out.println();

        System.out.println("--------------私钥加密公钥解密-------------------");
        source = "这是一行测试私钥加密公钥解密的无意义文字";
        System.out.println("原文字：\r\n" + source);
        encodedData = RSAUtil.encrypt(privateKey, source);
        System.out.println("加密后：\r\n" + encodedData);
        decodedData = RSAUtil.decrypt(publicKey, encodedData);
        System.out.println("解密后: \r\n" + decodedData);
        System.out.println();

        System.out.println("---------------私钥签名------------------");
        String content = "这是一行用于测试签名的原始数据";
        System.out.println("签名原串：\r\n" + content);
        String sign = RSAUtil.sign(content, privateKeyStr);
        System.out.println("签名串：\r\n" + sign);
        System.out.println();

        System.out.println("---------------公钥校验签名------------------");
        System.out.println("签名原串：\r\n" + content);
        System.out.println("签名串：\r\n" + sign);
        System.out.println("验签结果：\r\n" + RSAUtil.verify(content, sign, publicKeyStr));
        System.out.println();
    }
}
