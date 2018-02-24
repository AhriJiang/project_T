package com.juntao.commons.util;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.juntao.commons.function.UsefulFunctions;

/**
 * Created by major on 2016/12/28.
 */
public class SNumberUtil {
    public static final BigDecimal min(BigDecimal b1, BigDecimal b2) {
        Set<BigDecimal> set = Stream.of(b1, b2).filter(UsefulFunctions.notNull).collect(Collectors.toSet());
        if (set.size() == 0) {
            return null;
        }
        if (set.size() == 1) {
            return set.iterator().next();
        }

        if (b1.compareTo(b2) <= 0) {
            return b1;
        } else {
            return b2;
        }
    }

    public static final BigDecimal max(BigDecimal b1, BigDecimal b2) {
        Set<BigDecimal> set = Stream.of(b1, b2).filter(UsefulFunctions.notNull).collect(Collectors.toSet());
        if (set.size() == 0) {
            return null;
        }
        if (set.size() == 1) {
            return set.iterator().next();
        }

        if (b1.compareTo(b2) >= 0) {
            return b1;
        } else {
            return b2;
        }
    }
}
