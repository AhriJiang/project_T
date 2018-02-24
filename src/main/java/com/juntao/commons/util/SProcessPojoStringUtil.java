package com.juntao.commons.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Created by major on 2017/7/7.
 */
public final class SProcessPojoStringUtil {

	public static final boolean processPojo(Object vo, Predicate<Class<?>> shouldPojoClassProcessedFunc,
											BiPredicate<Field, String> shouldFieldStrValProcessedFunc,
											BiFunction<Field, String, String> processStrFunc) throws Exception {
		if (null == vo) {
			return false;
		}

		for (Field field : vo.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			Object fieldValue = field.get(vo);
			if (null == fieldValue) {
				continue;
			}

			Class fieldValueClass = fieldValue.getClass();

			if (String.class.isAssignableFrom(fieldValueClass)) {
				String fieldValueStr = (String) fieldValue;
				if (StringUtils.isBlank(fieldValueStr)) {
					continue;
				}

				if (shouldFieldStrValProcessedFunc.test(field, fieldValueStr)) {
					field.set(vo, processStrFunc.apply(field, fieldValueStr));
				}
			} else if (Collection.class.isAssignableFrom(fieldValueClass)) {
				processPojoCollection((Collection) fieldValue, shouldPojoClassProcessedFunc, shouldFieldStrValProcessedFunc,
						processStrFunc);
			} else if (Map.class.isAssignableFrom(fieldValueClass)) {
				processPojoCollection(((Map) fieldValue).values(), shouldPojoClassProcessedFunc, shouldFieldStrValProcessedFunc, processStrFunc);
			} else {
				if (shouldPojoClassProcessedFunc.test(fieldValueClass)) {
					processPojo(fieldValue, shouldPojoClassProcessedFunc, shouldFieldStrValProcessedFunc, processStrFunc);
				}
			}
		}

		return true;
	}

	public static final boolean processPojoCollection(Collection<?> voList, Predicate<Class<?>> shouldPojoClassProcessedFunc,
													  BiPredicate<Field, String> shouldFieldStrValProcessedFunc,
													  BiFunction<Field, String, String> processStrFunc) throws Exception {
		if (CollectionUtils.isEmpty(voList)) {
			return false;
		}

		for (Object vo : voList) {
			if (null == vo) {
				continue;
			}

			Class<?> voClass = vo.getClass();
			if (shouldPojoClassProcessedFunc.test(voClass)) {
				processPojo(vo, shouldPojoClassProcessedFunc, shouldFieldStrValProcessedFunc, processStrFunc);
			} else {
				if (Collection.class.isAssignableFrom(voClass)) {
					processPojoCollection((Collection<?>) vo, shouldPojoClassProcessedFunc, shouldFieldStrValProcessedFunc, processStrFunc);
				} else if (Map.class.isAssignableFrom(voClass)) {
					processPojoCollection(((Map) vo).values(), shouldPojoClassProcessedFunc, shouldFieldStrValProcessedFunc, processStrFunc);
				}
			}
		}

		return true;
	}
}
