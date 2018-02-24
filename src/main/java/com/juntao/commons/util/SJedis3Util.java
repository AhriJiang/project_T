package com.juntao.commons.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SJedis3Util {
    public static final Logger log = LoggerFactory.getLogger(SJedis3Util.class);

    public static final boolean exists(JedisCluster jedisCluster, String key) {
        if (null == jedisCluster || StringUtils.isBlank(key)) {
            return false;
        }

        return jedisCluster.exists(StringUtils.stripToEmpty(key));
    }

    public static final Long expire(JedisCluster jedisCluster, String key, int seconds) {
        if (null == jedisCluster || StringUtils.isBlank(key)) {
            return null;
        }

        return jedisCluster.expire(StringUtils.stripToEmpty(key), seconds);
    }

    public static final Long ttl(JedisCluster jedisCluster, String key) {
        if (null == jedisCluster || StringUtils.isBlank(key)) {
            return null;
        }

        return jedisCluster.ttl(StringUtils.stripToEmpty(key));
    }

    public static final String get(JedisCluster jedisCluster, String key) {
        if (null == jedisCluster || StringUtils.isBlank(key)) {
            return null;
        }

        return jedisCluster.get(StringUtils.stripToEmpty(key));
    }

/*  cluster不支持
    public static final Map<String, String> mget(JedisCluster jedisCluster, String... keys) {
        if (null == jedisCluster) {
            return null;
        }

        List<String> distinctKeyList = new ArrayList<>(
                Stream.of(keys).filter(key -> StringUtils.isNotBlank(key)).map(key -> StringUtils.stripToEmpty(key)).collect(Collectors.toSet()));
        if (CollectionUtils.isEmpty(distinctKeyList)) {
            return null;
        }

        String[] distinctKeys = distinctKeyList.toArray(new String[distinctKeyList.size()]);

        List<String> valueList = jedisCluster.mget(distinctKeys);
        return distinctKeyList.stream().map(key -> Pair.of(key, valueList.get(distinctKeyList.indexOf(key)))).filter(pair -> null != pair.getRight())
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }*/

    public static final String set(JedisCluster jedisCluster, String key, String value) {
        if (null == jedisCluster || StringUtils.isBlank(key) || null == value) {
            return null;
        }

        return jedisCluster.set(StringUtils.stripToEmpty(key), value);
    }

/*  cluster不支持
    public static final String mset(JedisCluster jedisCluster, Map<String, String> key_value) {
        if (null == jedisCluster || CollectionUtils.isEmpty(key_value)) {
            return null;
        }

        List<String> list = key_value.entrySet().stream().filter(entry -> StringUtils.isNotBlank(entry.getKey()) && null != entry.getValue())
                .flatMap(entry -> Stream.of(StringUtils.stripToEmpty(entry.getKey()), entry.getValue())).collect(Collectors.toList());

        return jedisCluster.mset(list.toArray(new String[list.size()]));
    }*/

    public static final Long del(JedisCluster jedisCluster, String... keys) {
        if (null == jedisCluster) {
            return null;
        }

        Set<String> distinctKeySet = Stream.of(keys).filter(key -> StringUtils.isNotBlank(key)).map(key -> StringUtils.stripToEmpty(key))
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(distinctKeySet)) {
            return null;
        }

        String[] distinctKeys = distinctKeySet.toArray(new String[distinctKeySet.size()]);

        return jedisCluster.del(distinctKeys);
    }

    public static final String hget(JedisCluster jedisCluster, String key, String field) {
        if (null == jedisCluster || StringUtils.isBlank(key) || StringUtils.isBlank(field)) {
            return null;
        }

        return jedisCluster.hget(StringUtils.stripToEmpty(key), StringUtils.stripToEmpty(field));
    }

    public static final Map<String, String> hgetAll(JedisCluster jedisCluster, String key) {
        if (null == jedisCluster || StringUtils.isBlank(key)) {
            return null;
        }

        return jedisCluster.hgetAll(key);
    }

    /**
     * @param key
     * @param fieldSet key下面的若干field
     * @return
     */
    public static final Map<String, String> hmget(JedisCluster jedisCluster, String key, Set<String> fieldSet) {
        if (null == jedisCluster || StringUtils.isBlank(key) || CollectionUtils.isEmpty(fieldSet)) {
            return null;
        }

        List<String> distinctFieldList = new ArrayList<>(
                fieldSet.stream().filter(field -> StringUtils.isNotBlank(field)).map(field -> StringUtils.stripToEmpty(field)).collect(Collectors.toSet()));
        if (CollectionUtils.isEmpty(distinctFieldList)) {
            return null;
        }

        String[] distinctFields = distinctFieldList.toArray(new String[distinctFieldList.size()]);

        List<String> valueList = jedisCluster.hmget(key, distinctFields);
        return distinctFieldList.stream().map(field -> Pair.of(field, valueList.get(distinctFieldList.indexOf(field))))
                .filter(pair -> null != pair.getRight()).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    public static final Long hset(JedisCluster jedisCluster, String key, String field, String value) {
        if (null == jedisCluster || StringUtils.isBlank(key) || StringUtils.isBlank(field) || null == value) {
            return null;
        }

        return jedisCluster.hset(StringUtils.stripToEmpty(key), StringUtils.stripToEmpty(field), value);
    }

    public static final String hmset(JedisCluster jedisCluster, String key, Map<String, String> field_value) {
        if (null == jedisCluster || StringUtils.isBlank(key)) {
            return null;
        }

        Map<String, String> cleanedMap = field_value.entrySet().stream().filter(entry -> StringUtils.isNotBlank(entry.getKey()) && null != entry.getValue())
                .collect(Collectors.toMap(entry -> StringUtils.stripToEmpty(entry.getKey()), Map.Entry::getValue, (oldValue, newValue) -> newValue));

        return jedisCluster.hmset(key, cleanedMap);
    }

    public static final Long hdel(JedisCluster jedisCluster, String key, Set<String> fieldSet) {
        if (null == jedisCluster || StringUtils.isBlank(key) || CollectionUtils.isEmpty(fieldSet)) {
            return null;
        }

        List<String> distinctFieldList = new ArrayList<>(
                fieldSet.stream().filter(field -> StringUtils.isNotBlank(field)).map(field -> StringUtils.stripToEmpty(field)).collect(Collectors.toSet()));
        if (CollectionUtils.isEmpty(distinctFieldList)) {
            return null;
        }

        String[] distinctFields = distinctFieldList.toArray(new String[distinctFieldList.size()]);

        return jedisCluster.hdel(key, distinctFields);
    }

    public static final Set<String> hkeys(JedisCluster jedisCluster, String key) {
        if (null == jedisCluster || StringUtils.isBlank(key)) {
            return null;
        }

        return jedisCluster.hkeys(StringUtils.stripToEmpty(key));
    }

    public static final Long hincrBy(JedisCluster jedisCluster, String key, String field, long value) {
        if (null == jedisCluster || StringUtils.isBlank(key)) {
            return null;
        }

        return jedisCluster.hincrBy(key, field, value);
    }

    public static final String getSet(JedisCluster jedisCluster, String key, String value) {
        if (null == jedisCluster || StringUtils.isBlank(key)) {
            return null;
        }

        return jedisCluster.getSet(key, value);
    }

    public static final Long zadd(JedisCluster jedisCluster, String key, double score, String member) {
        if (null == jedisCluster || StringUtils.isBlank(key) || StringUtils.isBlank(member)) {
            return null;
        }

        return jedisCluster.zadd(StringUtils.stripToEmpty(key), score, StringUtils.stripToEmpty(member));
    }


    public static final Long zrank(JedisCluster jedisCluster, String key, String member) {
        if (null == jedisCluster || StringUtils.isBlank(key) || StringUtils.isBlank(member)) {
            return null;
        }

        return jedisCluster.zrank(StringUtils.stripToEmpty(key), StringUtils.stripToEmpty(member));
    }
}
