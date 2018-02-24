package com.juntao.commons.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SCollectionUtil {

    private static final Logger log = LoggerFactory.getLogger(SCollectionUtil.class);

    /**
     * 把一个list分成若干个batchSize大小的小list，最后一个list的size可能小于batchSize如果不能整除的时候
     *
     * @param list
     * @param batchSize
     * @return
     */
    public static final <T> List<List<T>> sliceListByBatchSize(final List<T> list, int batchSize) {
        List<List<T>> result = new ArrayList<List<T>>();
        if (CollectionUtils.isEmpty(list) || 0 >= batchSize) {
            return result;
        }

        final int n = (list.size() + batchSize - 1) / batchSize;
        for (int i = 0; i < n; i++) {
            result.add(list.subList(i * batchSize,
                    Math.min((1 + i) * batchSize, list.size())));
        }

        return result;
    }

    /**
     * 把两个list笛卡尔积
     *
     * @param aList
     * @param bList
     * @return
     */
    public static final <A, B> List<Pair<A, B>> matrix(List<A> aList, List<B> bList) {
        List<Pair<A, B>> result = new ArrayList<>();
        for (A a : aList) {
            for (B b : bList) {
                result.add(Pair.of(a, b));
            }
        }

        return result;
    }

    /**
     * 把一堆对象变成map，第0,2,4,6,8。。。为key， 第1,3,5,7,9。。。为对应的value
     *
     * @param mapSupplier
     * @param kv
     * @return
     */
    public static final <M extends Map<K, V>, K, V> M toMap(Supplier<Map> mapSupplier, Object... kv) {
        if (ArrayUtils.isEmpty(kv) || ArrayUtils.contains(kv, null) || 0 != kv.length % 2) {
            return (M) Collections.emptyMap();
        }

        return (M) IntStream.range(0, kv.length).boxed().filter(i -> 0 == i % 2)
                .collect(Collectors.toMap(i -> kv[i], i -> kv[i + 1], (oldValue, newValue) -> newValue, mapSupplier));
    }

    /**
     * 把一堆对象变成map，第0,2,4,6,8。。。为key， 第1,3,5,7,9。。。为对应的value
     *
     * @param kv
     * @return
     */
    public static final <K, V> Map<K, V> toMap(Object... kv) {
        return toMap(HashMap::new, kv);
    }
}
