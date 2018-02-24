package com.juntao.commons.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.juntao.commons.consts.SConsts;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SKryoSerializeUtil {
	private static final Logger log = LoggerFactory.getLogger(SKryoSerializeUtil.class);

	public static final String serialize(Object o) {
		if (null == o) {
			return null;
		}

		try {
			try (Output output = new Output(65536)) {
				new Kryo().writeObject(output, o);
				return new String(output.toBytes(), SConsts.ISO_8859_1);
			}
		} catch (Exception e) {
			log.error("serialize error!!", e);
			return null;
		}
	}

	public static final <T> T unserialize(String s, Class<T> clazz) {
		if (StringUtils.isBlank(s) || null == clazz) {
			return null;
		}

		try {
			try (Input input = new Input(s.getBytes(SConsts.ISO_8859_1))) {
				return new Kryo().readObject(input, clazz);
			}
		} catch (Exception e) {
			log.error("unserialize error!!", e);
			return null;
		}
	}

	public static final <T> T copy(T t) {
		return new Kryo().copy(t);
	}

	public static final <T> T copyShallow(T t) {
		return new Kryo().copyShallow(t);
	}
}
