package com.juntao.commons.dao;

import com.juntao.commons.consts.SConsts;
import com.juntao.commons.function.UsefulFunctions;
import com.juntao.commons.po.IPo;
import com.juntao.commons.util.MySqlParamMapBuilder;
import com.juntao.commons.util.SJedis2Util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * 所有可以全表放到redis里缓存的表，可以继承这个dao
 * 泛型   S ：po的主键字段的class类型        T：po自己的class类型        V：放到id cache里面的value的class类型
 *
 */
public abstract class ARedisCacheDao<S extends Serializable, T extends IPo<S>, V extends Serializable>
		implements IDao<S, T> {

	@Autowired
	private Pool<Jedis> jedisPool4Cache;

	private final Map<String, Function<T, String>> idxHashKey_fieldGroupClassifierFunc = new HashMap<>(); // 存放所有的索引key，以及各自对应的field的生成方法。（所有的索引hash的value都是逗号分隔的id）

	protected abstract List<T> getAllAvailableList(); // 查库表得到所有状态正常的po list

	protected abstract String getIdColumn();

	protected abstract Function<String, S> getStr2IdFunc();

	protected abstract Function<T, V> getPo2IdCacheValueFunc(); // 将po转变成放到id cache的string value（所有的id cache的key都是getIdCacheKey(), field都是id，value就是这个函数的返回值）

	protected abstract Function<V, String> getIdCacheValue2StringFunc();

	protected abstract Function<String, V> getIdCacheString2ValueFunc(); // 将id cache的value反解析出来，转变成泛型V的class类型的对象

	protected abstract void initIdxCacheMap(final Map<String, Function<T, String>> redisHashKey_fieldGroupClassifierFunc); // 添加索引缓存的key，和对应的field的生成方法

	private final String getIdCacheKey() {
		return getPoClass().getSimpleName().toLowerCase();
	}

	private final void refreshOneCache(String key, Map<String, String> field_value) { // 刷新单个hash cache
		if (StringUtils.isBlank(key) || CollectionUtils.isEmpty(field_value)) {
			return;
		}

		/*
		 * Set<String> oldFieldSet = SJedis2Util.hkeys(key); if (!CollectionUtils.isEmpty(oldFieldSet)) { oldFieldSet.removeAll(field_value.keySet()); } if
		 * (!CollectionUtils.isEmpty(oldFieldSet)) { SJedis2Util.hdel(key, oldFieldSet); }
		 */

		SJedis2Util.del(jedisPool4Cache, key);
		SJedis2Util.hmset(jedisPool4Cache, key, field_value);
	}

	@PostConstruct
	public final void init() {
		initIdxCacheMap(idxHashKey_fieldGroupClassifierFunc);

		refreshAllCaches();
	}

	public final void refreshSingleIdCache(T t) {
		SJedis2Util.hset(jedisPool4Cache, getIdCacheKey(), id2StringFunc.apply(t.getId()),
				getIdCacheValue2StringFunc().apply(getPo2IdCacheValueFunc().apply(t)));
	}

	public final void refreshAllCaches() { // 刷新所有hash cache
		List<T> allPoList = getAllAvailableList();
		if (CollectionUtils.isEmpty(allPoList)) {
			return;
		}

		refreshOneCache(getIdCacheKey(),
				allPoList.stream().collect(Collectors.toMap(
						t -> id2StringFunc.apply(t.getId()),
						t -> getIdCacheValue2StringFunc().apply(getPo2IdCacheValueFunc().apply(t)))));

		idxHashKey_fieldGroupClassifierFunc.entrySet().forEach(
				entry -> refreshOneCache(entry.getKey(),
						allPoList.stream().collect(Collectors.groupingBy(entry.getValue(),
								Collectors.mapping(t -> id2StringFunc.apply(t.getId()), Collectors.toList())))
								.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> StringUtils.join(e.getValue(), SConsts.COMMA)))));
	}

	public final List<V> getAll() { // 取得全部id cache缓存
		Map<String, String> allIdCacheMap = SJedis2Util.hgetAll(jedisPool4Cache, getIdCacheKey());
		if (CollectionUtils.isEmpty(allIdCacheMap)) {
			return IDao.super.selectList(MySqlParamMapBuilder.createSelectAll())
					.stream().map(getPo2IdCacheValueFunc()::apply).collect(Collectors.toList());
		} else {
			return allIdCacheMap.values().stream().map(getIdCacheString2ValueFunc()::apply).collect(Collectors.toList());
		}
	}

	public final V get(S id) { // 取得单条id cache缓存
		if (null == id) {
			return null;
		}

		String cache = SJedis2Util.hget(jedisPool4Cache, getIdCacheKey(), id2StringFunc.apply(id));
		if (StringUtils.isBlank(cache)) {
			T t = IDao.super.select(id);
			return null == t ? null : getPo2IdCacheValueFunc().apply(t);
		} else {
			return getIdCacheString2ValueFunc().apply(cache);
		}
	}

	public final Map<S, V> mGet(Set<S> idSet) { // 批量取得id cache缓存
		if (CollectionUtils.isEmpty(idSet)) {
			return null;
		}

		Set<String> idStrSet = idSet.stream().filter(UsefulFunctions.notNull).map(id2StringFunc::apply).collect(Collectors.toSet()); // 去掉空字符串后trim一下
		if (CollectionUtils.isEmpty(idSet)) {
			return null;
		}

		Map<String, String> id_cache = SJedis2Util.hmget(jedisPool4Cache, getIdCacheKey(), idStrSet);
		if (CollectionUtils.isEmpty(id_cache)) {
			List<T> tList = IDao.super.selectList(MySqlParamMapBuilder.create().andIn(getIdColumn(), idSet).build());
			return tList.stream().collect(Collectors.toMap(T::getId, getPo2IdCacheValueFunc()::apply));
		} else {
			return id_cache.entrySet().stream().collect(Collectors.toMap(
					entry -> getStr2IdFunc().apply(entry.getKey()), entry -> getIdCacheString2ValueFunc().apply(entry.getValue())));
		}
	}

	public final List<S> getIdListByIdx(String key, String field) { // 根据index缓存，先取得逗号分隔的id字符串，然后再批量取得id cache缓存
		if (StringUtils.isBlank(key) || StringUtils.isBlank(field)) {
			return null;
		}

		String idCommaStr = SJedis2Util.hget(jedisPool4Cache, key, field); // id逗号分隔
		if (StringUtils.isBlank(idCommaStr)) {
			return null;
		}

		return Arrays.asList(idCommaStr.split(SConsts.COMMA)).stream().map(getStr2IdFunc()::apply).collect(Collectors.toList());
	}

	public final List<V> getListByIdx(String key, String field) { // 根据index缓存，先取得逗号分隔的id字符串，然后再批量取得id cache缓存
		if (StringUtils.isBlank(key) || StringUtils.isBlank(field)) {
			return null;
		}

		List<S> idList = getIdListByIdx(key, field); // id逗号分隔
		if (CollectionUtils.isEmpty(idList)) {
			return null;
		}

		Map<S, V> id_v = mGet(new HashSet<>(idList));
		if (CollectionUtils.isEmpty(id_v)) {
			return null;
		}

		return new ArrayList<>(id_v.values());
	}

	@Override
	public int insert(T t) {
		int i = IDao.super.insert(t);
		if (!CollectionUtils.isEmpty(idxHashKey_fieldGroupClassifierFunc)) {
			refreshAllCaches();
		} else {
			refreshSingleIdCache(t);
		}
		return i;
	}

	@Override
	public int insertList(List<T> tList) {
		int i = IDao.super.insertList(tList);
		refreshAllCaches();
		return i;
	}

	@Override
	public int save(T t) {
		int i = IDao.super.save(t);
		if (!CollectionUtils.isEmpty(idxHashKey_fieldGroupClassifierFunc)) {
			refreshAllCaches();
		} else {
			refreshSingleIdCache(t);
		}
		return i;
	}

	@Override
	public int saveList(List<T> tList) {
		int i = IDao.super.saveList(tList);
		refreshAllCaches();
		return i;
	}

	@Override
	public int update(T t) {
		int i = IDao.super.update(t);
		if (!CollectionUtils.isEmpty(idxHashKey_fieldGroupClassifierFunc)) {
			refreshAllCaches();
		} else {
			refreshSingleIdCache(t);
		}
		return i;
	}
}
