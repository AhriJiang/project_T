package com.juntao.commons.util;

import java.util.stream.Stream;

import com.juntao.commons.function.UsefulFunctions;

/**
 * Created by major on 2017/7/20.
 */
public final class SEqualUtil {

	public static final <O> boolean equals(O o1, O o2) {
		if (o1 == o2) {
			if (null == o1) {
				throw new NullPointerException();
			} else {
				return true;
			}
		}

		if (1 == Stream.of(o1, o2).filter(UsefulFunctions.isNull).count()) {
			return false;
		}

		return o2.equals(o1);
	}
}
