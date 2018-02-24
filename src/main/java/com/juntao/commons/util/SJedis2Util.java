package com.juntao.commons.util;

import com.juntao.commons.consts.SConsts;
import com.juntao.commons.function.UsefulFunctions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.util.Pool;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SJedis2Util {
	public static final Logger log = LoggerFactory.getLogger(SJedis2Util.class);

	private static final LinkedHashMap emptyMap = new LinkedHashMap();

	public static final Long SETNX_SUCCESS = 1L;
	public static final Long SETNX_FAIL = 0L;

	public static final <T, P> LinkedHashMap<String, T> pipelinedBatchFunc(BiFunction<Pipeline, Map.Entry<String, P>, Response<T>> singleFunc,
																		   Pool<Jedis> pool, Map<String, P> key_params) {
		if (null == singleFunc || null == pool || CollectionUtils.isEmpty(key_params)) {
			return emptyMap;
		}

		try (Jedis jedis = pool.getResource()) {
			Pipeline pipeline = jedis.pipelined();
			Map<String, Response<T>> key_pinplineResponse = key_params.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, entry -> singleFunc.apply(pipeline, entry)));
			pipeline.sync();
			return key_pinplineResponse.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
					entry -> entry.getValue().get(), UsefulFunctions.MapValueMerger.replaceOldWithNew(), LinkedHashMap::new));
		}
	}

	public static final boolean exists(Pool<Jedis> pool, String key) { // 建议直接使用setex或psetex
		if (null == pool || StringUtils.isBlank(key)) {
			return false;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.exists(StringUtils.stripToEmpty(key));
		}
	}

	@Deprecated
	public static final Long expire(Pool<Jedis> pool, String key, int seconds) {
		if (null == pool || StringUtils.isBlank(key) || 0 > seconds) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.expire(StringUtils.stripToEmpty(key), seconds);
		}
	}

	public static final Long pexpire(Pool<Jedis> pool, String key, long milliSeconds) {
		if (null == pool || StringUtils.isBlank(key) || 0 > milliSeconds) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.pexpire(StringUtils.stripToEmpty(key), milliSeconds);
		}
	}

	public static final Long ttl(Pool<Jedis> pool, String key) {
		if (null == pool || StringUtils.isBlank(key)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.ttl(StringUtils.stripToEmpty(key));
		}
	}

	public static final Long incr(Pool<Jedis> pool, String key) {
		if (null == pool || StringUtils.isBlank(key)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.incr(StringUtils.stripToEmpty(key));
		}
	}

	public static final String get(Pool<Jedis> pool, String key) {
		if (null == pool || StringUtils.isBlank(key)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.get(StringUtils.stripToEmpty(key));
		}
	}

	public static final LinkedHashMap<String, String> mget(Pool<Jedis> pool, String... keys) {
		if (null == pool || null == keys) {
			return emptyMap;
		}

		List<String> distinctKeyList = new ArrayList<>(
				Stream.of(keys).filter(UsefulFunctions.notBlankString).map(StringUtils::stripToEmpty).collect(Collectors.toSet()));
		if (CollectionUtils.isEmpty(distinctKeyList)) {
			return emptyMap;
		}

		String[] distinctKeys = distinctKeyList.toArray(new String[distinctKeyList.size()]);
		try (Jedis jedis = pool.getResource()) {
			List<String> valueList = jedis.mget(distinctKeys);
			return distinctKeyList.stream().map(key -> Pair.of(key, valueList.get(distinctKeyList.indexOf(key))))
					.filter(pair -> null != pair.getRight()).collect(Collectors.toMap(Pair::getLeft, Pair::getRight,
							UsefulFunctions.MapValueMerger.replaceOldWithNew(), LinkedHashMap::new));
		}
	}

	public static final String set(Pool<Jedis> pool, String key, String value) {
		if (null == pool || StringUtils.isBlank(key) || null == value) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.set(StringUtils.stripToEmpty(key), value);
		}
	}

	public static final Long setnx(Pool<Jedis> pool, String key, String value) {
		if (null == pool || StringUtils.isBlank(key) || null == value) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.setnx(StringUtils.stripToEmpty(key), value);
		}
	}

	public static final String setex(Pool<Jedis> pool, String key, int expireSeconds, String value) {
		if (null == pool || StringUtils.isBlank(key) || 0 > expireSeconds || null == value) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.setex(StringUtils.stripToEmpty(key), expireSeconds, value);
		}
	}

	public static final String psetex(Pool<Jedis> pool, String key, long expireMilliSeconds, String value) {
		if (null == pool || StringUtils.isBlank(key) || 0 > expireMilliSeconds || null == value) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.psetex(StringUtils.stripToEmpty(key), expireMilliSeconds, value);
		}
	}

	public static final String mset(Pool<Jedis> pool, Map<String, String> key_value) {
		if (null == pool || CollectionUtils.isEmpty(key_value)) {
			return null;
		}

		List<String> list = key_value.entrySet().stream().filter(entry -> StringUtils.isNotBlank(entry.getKey()) && null != entry.getValue())
				.flatMap(entry -> Stream.of(StringUtils.stripToEmpty(entry.getKey()), entry.getValue())).collect(Collectors.toList());
		try (Jedis jedis = pool.getResource()) {
			return jedis.mset(list.toArray(new String[list.size()]));
		}
	}

	public static final Long del(Pool<Jedis> pool, String... keys) {
		if (null == pool || null == keys) {
			return null;
		}

		Set<String> distinctKeySet = Stream.of(keys).filter(UsefulFunctions.notBlankString).map(StringUtils::stripToEmpty)
				.collect(Collectors.toSet());
		if (CollectionUtils.isEmpty(distinctKeySet)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.del(distinctKeySet.stream().toArray(String[]::new));
		}
	}

	public static final String hget(Pool<Jedis> pool, String key, String field) {
		if (null == pool || StringUtils.isBlank(key) || StringUtils.isBlank(field)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.hget(StringUtils.stripToEmpty(key), StringUtils.stripToEmpty(field));
		}
	}

	public static final Map<String, String> hgetAll(Pool<Jedis> pool, String key) {
		if (null == pool || StringUtils.isBlank(key)) {
			return Collections.emptyMap();
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.hgetAll(key);
		}
	}

	/**
	 * @param key
	 * @param fieldSet key下面的若干field
	 * @return
	 */
	public static final LinkedHashMap<String, String> hmget(Pool<Jedis> pool, String key, Set<String> fieldSet) {
		if (null == pool || StringUtils.isBlank(key) || CollectionUtils.isEmpty(fieldSet)) {
			return emptyMap;
		}

		List<String> distinctFieldList = new ArrayList<>(
				fieldSet.stream().filter(field -> StringUtils.isNotBlank(field)).map(field -> StringUtils.stripToEmpty(field)).collect(Collectors.toSet()));
		if (CollectionUtils.isEmpty(distinctFieldList)) {
			return emptyMap;
		}

		String[] distinctFields = distinctFieldList.toArray(new String[distinctFieldList.size()]);
		try (Jedis jedis = pool.getResource()) {
			List<String> valueList = jedis.hmget(key, distinctFields);
			return distinctFieldList.stream().map(field -> Pair.of(field, valueList.get(distinctFieldList.indexOf(field))))
					.filter(pair -> null != pair.getRight()).collect(Collectors.toMap(Pair::getLeft, Pair::getRight,
							UsefulFunctions.MapValueMerger.replaceOldWithNew(), LinkedHashMap::new));
		}
	}

	public static final Long hset(Pool<Jedis> pool, String key, String field, String value) {
		if (null == pool || StringUtils.isBlank(key) || StringUtils.isBlank(field) || null == value) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.hset(StringUtils.stripToEmpty(key), StringUtils.stripToEmpty(field), value);
		}
	}

	public static final String hmset(Pool<Jedis> pool, String key, Map<String, String> field_value) {
		if (null == pool || StringUtils.isBlank(key)) {
			return null;
		}

		Map<String, String> cleanedMap = field_value.entrySet().stream().filter(entry -> StringUtils.isNotBlank(entry.getKey()) && null != entry.getValue())
				.collect(Collectors.toMap(entry -> StringUtils.stripToEmpty(entry.getKey()), Map.Entry::getValue, (oldValue, newValue) -> newValue));
		try (Jedis jedis = pool.getResource()) {
			return jedis.hmset(key, cleanedMap);
		}
	}

	public static final Long hdel(Pool<Jedis> pool, String key, Set<String> fieldSet) {
		if (null == pool || StringUtils.isBlank(key) || CollectionUtils.isEmpty(fieldSet)) {
			return null;
		}

		List<String> distinctFieldList = new ArrayList<>(
				fieldSet.stream().filter(field -> StringUtils.isNotBlank(field)).map(field -> StringUtils.stripToEmpty(field)).collect(Collectors.toSet()));
		if (CollectionUtils.isEmpty(distinctFieldList)) {
			return null;
		}

		String[] distinctFields = distinctFieldList.toArray(new String[distinctFieldList.size()]);
		try (Jedis jedis = pool.getResource()) {
			return jedis.hdel(key, distinctFields);
		}
	}

	public static final Set<String> keys(Pool<Jedis> pool, String pattern) {
		if (null == pool || StringUtils.isBlank(pattern)) {
			return Collections.emptySet();
		}

		pattern = StringUtils.stripToEmpty(pattern);
		try (Jedis jedis = pool.getResource()) {
			return jedis.keys(pattern.endsWith(SConsts.STAR) ? pattern : pattern + SConsts.STAR);
		}
	}

	public static final Set<String> hkeys(Pool<Jedis> pool, String key) {
		if (null == pool || StringUtils.isBlank(key)) {
			return Collections.emptySet();
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.hkeys(StringUtils.stripToEmpty(key));
		}
	}

	public static final Long hincrBy(Pool<Jedis> pool, String key, String field, long value) {
		if (null == pool || StringUtils.isBlank(key)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.hincrBy(key, field, value);
		}
	}

	public static final String getSet(Pool<Jedis> pool, String key, String value) {
		if (null == pool || StringUtils.isBlank(key)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.getSet(key, value);
		}
	}

	public static final Long zadd(Pool<Jedis> pool, String key, double score, String member) {
		if (null == pool || StringUtils.isBlank(key) || StringUtils.isBlank(member)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.zadd(StringUtils.stripToEmpty(key), score, StringUtils.stripToEmpty(member));
		}
	}

	public static final Long zrank(Pool<Jedis> pool, String key, String member) {
		if (null == pool || StringUtils.isBlank(key) || StringUtils.isBlank(member)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.zrank(StringUtils.stripToEmpty(key), StringUtils.stripToEmpty(member));
		}
	}

	public static final Long rpush(Pool<Jedis> pool, String key, String... values) {
		if (null == pool || StringUtils.isBlank(key)) {
			return null;
		}

		LinkedHashSet<String> distinctValueSet = Stream.of(values).filter(value -> StringUtils.isNotBlank(value))
				.map(value -> StringUtils.stripToEmpty(value)).collect(Collectors.toCollection(LinkedHashSet::new));
		if (CollectionUtils.isEmpty(distinctValueSet)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.rpush(StringUtils.stripToEmpty(key), distinctValueSet.stream().toArray(String[]::new));
		}
	}

	public static final List<String> lrange(Pool<Jedis> pool, String key, long start, long stop) {
		if (null == pool || StringUtils.isBlank(key)) {
			return Collections.emptyList();
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.lrange(StringUtils.stripToEmpty(key), start, stop);
		}
	}

	public static final Long sadd(Pool<Jedis> pool, String key, String... members) {
		if (null == pool || StringUtils.isBlank(key)) {
			return null;
		}

		Set<String> distinctMemberSet = Stream.of(members).filter(member -> StringUtils.isNotBlank(member))
				.map(member -> StringUtils.stripToEmpty(member)).collect(Collectors.toSet());
		if (CollectionUtils.isEmpty(distinctMemberSet)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.sadd(StringUtils.stripToEmpty(key), distinctMemberSet.stream().toArray(String[]::new));
		}
	}

	public static final Set<String> smembers(Pool<Jedis> pool, String key) {
		if (null == pool || StringUtils.isBlank(key)) {
			return Collections.emptySet();
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.smembers(StringUtils.stripToEmpty(key));
		}
	}

	public static final boolean sismember(Pool<Jedis> pool, String key, String member) {
		if (null == pool || StringUtils.isBlank(key) || null == member) {
			return false;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.sismember(StringUtils.stripToEmpty(key), member);
		}
	}

	public static final Long srem(Pool<Jedis> pool, String key, String... members) {
		if (null == pool || StringUtils.isBlank(key)) {
			return null;
		}

		Set<String> distinctMemberSet = Stream.of(members).filter(member -> StringUtils.isNotBlank(member))
				.map(member -> StringUtils.stripToEmpty(member)).collect(Collectors.toSet());
		if (CollectionUtils.isEmpty(distinctMemberSet)) {
			return null;
		}

		try (Jedis jedis = pool.getResource()) {
			return jedis.srem(StringUtils.stripToEmpty(key), distinctMemberSet.stream().toArray(String[]::new));
		}
	}
}
