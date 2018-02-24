package com.juntao.commons.dao;

import com.juntao.commons.consts.SConsts;
import com.juntao.commons.function.UsefulFunctions;
import com.juntao.commons.po.IPo;
import com.juntao.commons.util.MySqlParamMapBuilder;
import com.juntao.commons.util.SCollectionUtil;
import com.juntao.commons.util.SJedis2Util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 所有全表记录，按照某一个字段或几个字段的组合为K，group起来以后，把某个字段V toString后放到集合里，  k,v放到redis的set类型缓存的表。可以继承这个dao。
 * <p>
 * 泛型   S ：po的主键字段的class类型        T：po自己的class类型        K：group的key的类型
 */
public abstract class ARedisGroupCacheDao<S extends Serializable, T extends IPo<S>, K extends Serializable>
		implements IDao<S, T>, IDeletableDao<S, T>, IUnionDao<S, T> {

	public static final int ZSET_CACHE_SENCONDS = 604800;

	@Autowired
	private Pool<Jedis> jedisPool4Cache;

	protected abstract Function<K, String> getK2CacheKeyFunc(); // k转化成string，作为redis的key使用

	protected abstract Function<K, MySqlParamMapBuilder> getSelectKVByKBuilderFunc(); // 获取  selectColumn是v； where条件是k的sql

	protected abstract Function<Map.Entry<K, Set<String>>, MySqlParamMapBuilder> getSelectIdByKVSetBuilderFunc(); // 获取  selectColumn是id； where条件是k，v的sql

	protected abstract Function<T, K> getPo2KFunc(); // 获取某个po的k

	protected abstract Function<T, String> getPo2ValFunc(); // 获取某个po的v

	protected abstract List<T> newPoList(String creator, Date created, K k, Set<String> valSet); // new出一堆po出来

	private final String cacheKeyPrefix = getPoClass().getSimpleName().toLowerCase() + SConsts.UNDER_SCORE;

	@PostConstruct
	public final void init() {
		clearAllCache();
	}

	private Map<K, Set<String>> groupList2Cache(List<T> tList) {
		if (CollectionUtils.isEmpty(tList)) {
			return Collections.emptyMap();
		}

		return tList.stream().filter(UsefulFunctions.notNull).map(t -> Pair.of(getPo2KFunc().apply(t), getPo2ValFunc().apply(t)))
				.filter(UsefulFunctions.notNull).filter(pair -> null != pair.getLeft() && StringUtils.isNotBlank(pair.getRight()))
				.distinct().collect(Collectors.groupingBy(Pair::getLeft,
						Collectors.mapping(Pair::getRight, Collectors.toSet())));
	}

	private void addCache(Map<K, Set<String>> key_valSets) {
		if (CollectionUtils.isEmpty(key_valSets)) {
			return;
		}

		Map<String, String[]> cacheKey_valsMap = key_valSets.entrySet().stream().collect(Collectors.toMap(
//				entry -> cacheKeyPrefix + getK2CacheKeyFunc().apply(entry.getKey()),
				key_valSet -> getCacheKey(key_valSet.getKey()),
				key_valSet -> key_valSet.getValue().stream().toArray(String[]::new)));

		SJedis2Util.pipelinedBatchFunc(
				(pipeline, cacheKey_vals) -> pipeline.sadd(cacheKey_vals.getKey(), Optional.ofNullable(cacheKey_vals.getValue()).orElse(null)),
//				(pipeline, _cacheKey_vals) -> pipeline.sadd(_cacheKey_vals.getKey(), Optional.ofNullable(_cacheKey_vals.getValue()).map(Object::toString).orElse(null)),
				jedisPool4Cache, cacheKey_valsMap);
		SJedis2Util.sadd(jedisPool4Cache, cacheKeyPrefix, cacheKey_valsMap.keySet().stream().toArray(String[]::new));

		cacheKey_valsMap.keySet().forEach(loginNameCacheKey -> {
			SJedis2Util.expire(jedisPool4Cache, loginNameCacheKey, ZSET_CACHE_SENCONDS);
		});

		SJedis2Util.expire(jedisPool4Cache, cacheKeyPrefix, ZSET_CACHE_SENCONDS);

	}

	public final void clearAllCache() {
		Set<String> cacheKeySet = SJedis2Util.smembers(jedisPool4Cache, cacheKeyPrefix);
		if (!CollectionUtils.isEmpty(cacheKeySet) && !cacheKeySet.stream().allMatch(UsefulFunctions.isBlankString)) {
			SJedis2Util.del(jedisPool4Cache, cacheKeySet.stream().filter(UsefulFunctions.notBlankString).toArray(String[]::new));
		}
		SJedis2Util.del(jedisPool4Cache, cacheKeyPrefix);
	}

	public final void buildAllCache() {
		Map<K, Set<String>> k_valSet = selectList(MySqlParamMapBuilder.create().limit(0, Integer.MAX_VALUE).build()).stream()
				.map(t -> Pair.of(getPo2KFunc().apply(t), getPo2ValFunc().apply(t)))
				.filter(pair -> null != pair.getLeft() && StringUtils.isNotBlank(pair.getRight()))
				.collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, Collectors.toSet())));

		clearAllCache();

		if (!CollectionUtils.isEmpty(k_valSet)) {
			addCache(k_valSet);
		}
	}

//	protected int removeList(List<T> tList) {
//		if (CollectionUtils.isEmpty(tList) || tList.stream().allMatch(UsefulFunctions.isNull)) {
//			return -1;
//		}
//
//		Map<K, Set<String>> k_valSet = tList.stream().map(t -> Pair.of(getPo2KFunc().apply(t), getPo2ValFunc().apply(t)))
//				.filter(UsefulFunctions.notNull).filter(pair -> null != pair.getLeft() && StringUtils.isNotBlank(pair.getRight()))
//				.distinct().collect(Collectors.groupingBy(Pair::getLeft,
//						Collectors.mapping(Pair::getRight, Collectors.toSet())));
//		if (!CollectionUtils.isEmpty(k_valSet)) {
//			MySqlParamMapBuilder unionBuilder = MySqlParamMapBuilder.create().limit(0, Integer.MAX_VALUE);
//			k_valSet.entrySet().stream().map(entry -> getSelectIdByKVSetBuilderFunc().apply(entry).limit(0, Integer.MAX_VALUE))
//					.forEach(unionBuilder::union);
//			List<S> idList = selectIdListUnion(unionBuilder.build());
//			int i = -1;
//			if (!CollectionUtils.isEmpty(idList)) {
//				i = CDeletableDaoProxy.deleteList(MySqlParamMapBuilder.create().andIn(IMysqlMapper._ID, idList).build());
//			}
//
//			if (0 < i) {
//				SJedis2Util.pipelinedBatchFunc(
//						(pipeline, cacheKey_vals) -> pipeline.srem(cacheKey_vals.getKey(), cacheKey_vals.getValue()),
//						jedisPool4Cache, k_valSet.entrySet().stream().collect(Collectors.toMap(
//								entry -> cacheKeyPrefix + getK2CacheKeyFunc().apply(entry.getKey()),
//								entry -> entry.getValue().stream().toArray(String[]::new)
//						)));
//			}
//		}
//
//		return -1;
//	}

	protected Map<K, Set<String>> lookup(Collection<K> keys) {
		if (CollectionUtils.isEmpty(keys)) {
			return Collections.emptyMap();
		}
		Set<K> notNullOriginalKeys = keys.stream().filter(UsefulFunctions.notNull).collect(Collectors.toSet());
		if (CollectionUtils.isEmpty(notNullOriginalKeys)) {
			return Collections.emptyMap();
		}

//		Map<String, K> cacheKey_k = noNullkeys.stream().collect(Collectors.toMap(k -> cacheKeyPrefix + getK2CacheKeyFunc().apply(k), Function.identity()));
		Map<String, K> key_originalKeyMap = notNullOriginalKeys.stream().collect(Collectors.toMap(key -> getCacheKey(key), Function.identity()));

		Map<K, Set<String>> result = new HashMap<>();

		Map<String, Set<String>> key_ZSetVals = SJedis2Util.pipelinedBatchFunc(
				(pipeline, key_originalKey) -> pipeline.smembers(key_originalKey.getKey()),
				jedisPool4Cache, key_originalKeyMap)
				.entrySet().stream().filter(entry -> StringUtils.isNotBlank(entry.getKey()))
				.filter(entry -> !CollectionUtils.isEmpty(entry.getValue()))
				.filter(entry -> !entry.getValue().stream().allMatch(UsefulFunctions.isBlankString))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		if (!CollectionUtils.isEmpty(key_ZSetVals)) {
			key_ZSetVals.entrySet().forEach(key_ZSetVal ->
					result.put(key_originalKeyMap.get(key_ZSetVal.getKey()), key_ZSetVal.getValue())
			);

			key_originalKeyMap.keySet().removeAll(key_ZSetVals.keySet());
		}

		if (!CollectionUtils.isEmpty(key_originalKeyMap)) {
			MySqlParamMapBuilder unionBuilder = MySqlParamMapBuilder.create().limit(0, Integer.MAX_VALUE);
			key_originalKeyMap.values().stream().map(originalKey -> getSelectKVByKBuilderFunc().apply(originalKey).limit(0, Integer.MAX_VALUE))
					.forEach(unionBuilder::union);
			Map<K, Set<String>> originalKey_valSets = selectListUnion(unionBuilder.build())
					.stream().collect(Collectors.groupingBy(getPo2KFunc()::apply,
							Collectors.mapping(getPo2ValFunc()::apply, Collectors.toSet())));
			if (!CollectionUtils.isEmpty(originalKey_valSets)) {
				result.putAll(originalKey_valSets);

				addCache(originalKey_valSets);
			}
		}

		return result;
	}

	private String getCacheKey(K key) {
		return String.format("%s%s", cacheKeyPrefix, getK2CacheKeyFunc().apply(key));
	}

	protected void resetOneGroup(String creator, Date created, K k, Set<String> valSet) {
		if (StringUtils.isBlank(creator) || null == created || null == k
				|| CollectionUtils.isEmpty(valSet) || valSet.stream().allMatch(UsefulFunctions.isBlankString)) {
			return;
		}

		Set<String> valSetInDB = selectList(getSelectKVByKBuilderFunc().apply(k).limit(0, Integer.MAX_VALUE).build())
				.stream().map(getPo2ValFunc()::apply).collect(Collectors.toSet());

		Set<String> insertValSet = valSet.stream().filter(UsefulFunctions.notNull)
				.filter(val -> !valSetInDB.contains(val)).collect(Collectors.toSet());
		Set<String> deleteValSet = valSetInDB.stream().filter(UsefulFunctions.notNull)
				.filter(valInDB -> !valSet.contains(valInDB)).collect(Collectors.toSet());

		if (!CollectionUtils.isEmpty(deleteValSet)) {
			deleteList(getSelectIdByKVSetBuilderFunc().apply(Pair.of(k, deleteValSet)).build());
			SJedis2Util.srem(jedisPool4Cache, cacheKeyPrefix + getK2CacheKeyFunc().apply(k),
					deleteValSet.stream().toArray(String[]::new));
		}

		if (!CollectionUtils.isEmpty(insertValSet)) {
			insertList(newPoList(creator, created, k, insertValSet));
			addCache(SCollectionUtil.toMap(k, insertValSet));
		}
	}

	@Override
	public int insert(T t) {
		if (null == t) {
			return -1;
		}

		return insertList(Arrays.asList(t));
	}

	@Override
	public int insertList(List<T> tList) {
		if (CollectionUtils.isEmpty(tList) || tList.stream().allMatch(UsefulFunctions.isNull)) {
			return -1;
		}

		Map<K, Set<String>> k_valSet = groupList2Cache(tList);
		if (!CollectionUtils.isEmpty(k_valSet)) {
			int i = IDao.super.insertList(tList);
			addCache(k_valSet);
			return i;
		} else {
			return 0;
		}
	}

	@Override
	public int save(T t) {
		if (null == t) {
			return -1;
		}

		return saveList(Arrays.asList(t));
	}

	@Override
	public int saveList(List<T> tList) {
		if (CollectionUtils.isEmpty(tList) || tList.stream().allMatch(UsefulFunctions.isNull)) {
			return -1;
		}

		Map<K, Set<String>> k_valSet = groupList2Cache(tList);
		if (!CollectionUtils.isEmpty(k_valSet)) {
			int i = IDao.super.saveList(tList);
			addCache(k_valSet);
			return i;
		} else {
			return 0;
		}
	}

	@Override
	public int delete(S id) {
		if (null == id) {
			return -1;
		}

		T tInDB = select(id);
		if (null == tInDB) {
			return -1;
		}

		SJedis2Util.srem(jedisPool4Cache, cacheKeyPrefix + getK2CacheKeyFunc().apply(getPo2KFunc().apply(tInDB)),
				getPo2ValFunc().apply(tInDB));

		return IDeletableDao.super.delete(id);
	}

	@Override
	public int deleteList(Map<String, Object> paramMap) {
		if (CollectionUtils.isEmpty(paramMap)) {
			return -1;
		}

		int result = IDeletableDao.super.deleteList(paramMap);
		if (0 < result) {
			clearAllCache();
		}
		return result;
	}

	@Override
	public int update(T t) {
		if (null == t || StringUtils.isBlank(id2StringFunc.apply(t.getId()))) {
			return -1;
		}

		T tInDB = select(t.getId());
		if (null == tInDB) {
			return -1;
		}

		int i = IDao.super.update(t);
		if (0 < i) {
			SJedis2Util.srem(jedisPool4Cache, cacheKeyPrefix + getK2CacheKeyFunc().apply(getPo2KFunc().apply(tInDB)),
					getPo2ValFunc().apply(tInDB));

			String newCacheKey = cacheKeyPrefix + getK2CacheKeyFunc().apply(getPo2KFunc().apply(t));
			SJedis2Util.sadd(jedisPool4Cache, newCacheKey, getPo2ValFunc().apply(t));
			SJedis2Util.sadd(jedisPool4Cache, cacheKeyPrefix, newCacheKey);
		}

		return i;
	}

	@Override
	public int updateList(Map<String, Object> paramMap) {
		if (CollectionUtils.isEmpty(paramMap)) {
			return -1;
		}

		int i = IDeletableDao.super.updateList(paramMap);
		if (0 < i) {
			clearAllCache();
		}
		return i;
	}
}
