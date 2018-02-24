package com.juntao.commons.util;

import com.juntao.commons.annotation.NeedEncryptInDB;
import com.juntao.commons.dao.IInsertIgnoreDao;
import com.juntao.commons.function.UsefulFunctions;
import com.juntao.commons.po.IPo;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by major on 2017/7/3.
 */
public class SEncryptPoUtil {
	private static final Logger log = LoggerFactory.getLogger(IInsertIgnoreDao.class);

	private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("encryptKey");
	private static final String privateKeyStr = resourceBundle.getString("privateKey");
	private static final String publicKeyStr = resourceBundle.getString("publicKey");

	public static final <T extends IPo> boolean isNeedEncrpty(Class<T> poClass) {
		return !CollectionUtils.isEmpty(needEncryptFieldSet(poClass));
	}

	public static final <T extends IPo> Set<Field> needEncryptFieldSet(Class<T> poClass) {
		if (null == poClass) {
			return Collections.emptySet();
		}

		return Stream.of(poClass.getDeclaredFields()).filter(field -> field.isAnnotationPresent(NeedEncryptInDB.class))
				.filter(field -> String.class.equals(field.getType())).collect(Collectors.toSet());
	}

	public static final <T extends IPo> void encrypt(T t) {
		if (null == t) {
			return;
		}

		encrypt(Arrays.asList(t));
	}

	public static final <T extends IPo> void encrypt(List<T> tList) {
		convertFieldValue(tList, false);
	}

	private static final <T extends IPo> void convertFieldValue(List<T> tList, boolean isDecrypt) {
		if (CollectionUtils.isEmpty(tList)) {
			return;
		}

		List<T> tmpList = tList.stream().filter(UsefulFunctions.notNull).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(tmpList)) {
			return;
		}

		for (Field field : needEncryptFieldSet(tmpList.get(0).getClass())) {
			field.setAccessible(true);

			for (T t : tmpList) {
				try {
					String fieldValueStr = (String) field.get(t);
					if (StringUtils.isBlank(fieldValueStr)) {
						continue;
					}

					if (isDecrypt) {
						field.set(t, decryptFieldValueStrByPublicKey(fieldValueStr));
					} else {
						field.set(t, encryptFieldValueStrByPrivateKey(fieldValueStr));
					}
				} catch (Exception e) {
					log.error("reflect or encrypt error!!  field= " + field.getName() + ",  po=" + t, e);
				}
			}
		}
	}

	public static final String encryptFieldValueStrByPrivateKey(String fieldValueStr) throws Exception {
		return RSAUtil.encryptByPrivateKey(privateKeyStr, fieldValueStr);
	}

	public static final String decryptFieldValueStrByPublicKey(String fieldValueStr) throws Exception {
		return RSAUtil.decryptByPublicKey(publicKeyStr, fieldValueStr);
	}

	public static final <T extends IPo> void decrypt(T t) {
		if (null == t) {
			return;
		}

		decrypt(Arrays.asList(t));
	}

	public static final <T extends IPo> void decrypt(List<T> tList) {
		convertFieldValue(tList, true);
	}
}
