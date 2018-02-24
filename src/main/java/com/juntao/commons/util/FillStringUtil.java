package com.juntao.commons.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by psw on 2017/2/16.
 */
public class FillStringUtil {

    private static final String PLACE_HOLDER = "${}";
    private static final String NEW_LINE = "、";

    public static final BiFunction<Pair<String, String>, List<String>, String> fillStringFunc = (oriPair, fillerList) -> {
        String message = fillerList.stream()
                .reduce(oriPair.getRight(), (msg, filler) -> StringUtils.replaceOnce(msg, PLACE_HOLDER, filler));
        return message + NEW_LINE;
    };


}
