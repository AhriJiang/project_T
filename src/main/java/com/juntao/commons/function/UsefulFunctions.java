package com.juntao.commons.function;

import com.juntao.commons.po.IPo;
import com.juntao.commons.util.SCollectionUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by major on 2016/8/23.
 */
public class UsefulFunctions {
    private static final Logger log = LoggerFactory.getLogger(UsefulFunctions.class);

    public static final UnaryOperator<Predicate<Object>> not = predicate -> predicate.negate();
    public static final Predicate<Object> isNull = object -> null == object;
    public static final Predicate<String> isBlankString = string -> StringUtils.isBlank(string);
    public static final Predicate<Collection> isEmptyCollection = collection -> CollectionUtils.isEmpty(collection);
    public static final Predicate<Map> isEmptyMap = map -> CollectionUtils.isEmpty(map);
    public static final Predicate<Object> notNull = object -> null != object;
    public static final Predicate<String> notBlankString = string -> StringUtils.isNotBlank(string);
    public static final Predicate<Collection> notEmptyCollection = collection -> !CollectionUtils.isEmpty(collection);
    public static final Predicate<Map> notEmptyMap = map -> !CollectionUtils.isEmpty(map);

    public static final <O, P, R> Function<O, R> apply(BiFunction<O, P, R> classFunction, P param) {
        return obj -> classFunction.apply(obj, param);
    }

    public static final <O, P> Consumer<O> accept(BiConsumer<O, P> classConsumer, P param) {
        return obj -> classConsumer.accept(obj, param);
    }

    public static final <O, P> Predicate<O> test(BiPredicate<O, P> classPredicateFunction, P param) {
        return obj -> classPredicateFunction.test(obj, param);
    }

    public static final <T, U> Predicate<T> mapResultContainedBy(Function<T, U> mapFunc, Collection<U> uCollection) {
        return t -> uCollection.contains(mapFunc.apply(t));
    }

    public static final <T, U> Predicate<T> mapResultNotContainedBy(Function<T, U> mapFunc, Collection<U> uCollection) {
        return mapResultContainedBy(mapFunc, uCollection).negate();
    }

    public static final <T> Function<T, T> consumeAndReturn(Consumer<T> tConsumer) {
        return t -> {
            tConsumer.accept(t);
            return t;
        };
    }

    public static final <O, P> Function<O, O> setter(BiConsumer<O, P> classConsumer, P param) {
        return consumeAndReturn(accept(classConsumer, param));
    }

    public static final <T> BinaryOperator<Set<T>> setMerger() {
        return (s1, s2) -> Stream.of(s1, s2).flatMap(Set::stream).collect(Collectors.toSet());
    }

    public static final <K, V> BinaryOperator<Map<K, V>> mapMerger(BinaryOperator<V> valueMerger) {
        return (m1, m2) -> Stream.of(m1, m2).map(Map::entrySet).flatMap(Set::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, valueMerger, HashMap::new));
    }

    public static class MapValueMerger {
        public static final <T> BinaryOperator<T> replaceOldWithNew() {
            return (oldValue, newValue) -> newValue;
        }

        public static final <T> BinaryOperator<T> retainOldDiscardNew() {
            return (oldValue, newValue) -> oldValue;
        }
    }

    public static final <T, R> Function<T, R> tryCatchFunction(ThrowsExceptionFunction<T, R> throwsExceptionFunction, BiFunction<Exception, T, R> exceptionBiFunction) {
        return t -> {
            try {
                return throwsExceptionFunction.apply(t);
            } catch (Exception e) {
                return Optional.ofNullable(exceptionBiFunction).orElse((_e, _t) -> {
                    log.error("", _e);
                    return null;
                }).apply(e, t);
            }
        };
    }

    public static final <T> Supplier<T> tryCatchSupplier(ThrowsExceptionSupplier<T> throwsExceptionSupplier, Function<Exception, T> exceptionFunction) {
        return () -> {
            try {
                return throwsExceptionSupplier.get();
            } catch (Exception e) {
                return Optional.ofNullable(exceptionFunction).orElse(_e -> {
                    log.error("", _e);
                    return null;
                }).apply(e);
            }
        };
    }

    public static final <T> Consumer<T> tryCatchConsumer(ThrowsExceptionConsumer<T> throwsExceptionConsumer, BiConsumer<Exception, T> exceptionBiConsumer) {
        return t -> {
            try {
                throwsExceptionConsumer.accept(t);
            } catch (Exception e) {
                Optional.ofNullable(exceptionBiConsumer).orElse((_e, _i) -> log.error("", _e)).accept(e, t);
            }
        };
    }


    public static void main(String[] args) {
//        ThrowsExceptionConsumer<Integer> throwsExceptionConsumer = t -> {
//            throw new Exception();
//        };
//        Stream.of(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO).min(BigDecimal::compareTo).ifPresent(System.out::println);
//        Stream.of(StringUtils.splitByWholeSeparatorPreserveAllTokens(",4,,,,,,,", ",,")).forEach(System.out::println);

//        new File("/home/appsvr/file/images/1.txt").renameTo(new File("/tmp/2.txt"));


        Map<String, Integer> tm1 = SCollectionUtil.toMap(
                "a", 1,
                "b", 2,
                "c", 3
        );
        Map<String, Integer> tm100 = SCollectionUtil.toMap(
                "a", 101,
                "b", 102,
                "c", 103
        );

        Stream.of("ab", "xy").map(apply(String::charAt, 0)).forEach(System.out::println);
        Stream.of(tm1, tm100).forEach(accept(Map::putAll, new HashMap<String, Integer>()));
        Stream.of(tm1, tm100).filter(test(Map::containsKey, "a")).forEach(System.out::println);

        PojoBean b = Optional.of(new PojoBean())
                .map(setter(PojoBean::setL, new ArrayList<>()))
                .map(setter(PojoBean::setPo, null)).get();

    }

    static class PojoBean {
        private List<String> l;
        private IPo po;

        public List<String> getL() {
            return l;
        }

        public void setL(List<String> l) {
            this.l = l;
        }

        public IPo getPo() {
            return po;
        }

        public void setPo(IPo po) {
            this.po = po;
        }
    }
}
