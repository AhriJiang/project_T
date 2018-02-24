package com.juntao.commons.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.juntao.commons.consts.SConsts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SObjectStreamSerializeUtil {
	private static final Logger log = LoggerFactory.getLogger(SObjectStreamSerializeUtil.class);

	public static final String serialize(Object o) {
		if (null == o) {
			return null;
		}

		try {
			try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);) {

				objectOutputStream.writeObject(o);
				String result = byteArrayOutputStream.toString(SConsts.ISO_8859_1);// 必须是ISO-8859-1
				// result = java.net.URLEncoder.encode(result, "UTF-8");// 编码后字符串不是乱码（不编也不影响功能）

				return result;
			}
		} catch (Exception e) {
			log.error("serialize error!!", e);
			return null;
		}
	}

	public static final Object unserialize(String s) {
		if (StringUtils.isBlank(s)) {
			return null;
		}

		try {
			// s = java.net.URLDecoder.decode(s, "UTF-8"); // 编码后字符串不是乱码（不编也不影响功能）
			try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(s.getBytes(SConsts.ISO_8859_1)));) {
				return objectInputStream.readObject();
			}
		} catch (Exception e) {
			log.error("unserialize error!!", e);
			return null;
		}
	}

	public static final <T> T unserialize(String s, Class<T> clazz) {
		if (StringUtils.isBlank(s) || null == clazz) {
			return null;
		}

		return (T) unserialize(s);
	}
}
