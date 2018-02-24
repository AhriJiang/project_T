package com.juntao.commons.util;

import com.juntao.commons.consts.SConsts;
import com.juntao.commons.function.UsefulFunctions;
import com.juntao.commons.mapper.IMysqlMapper;
import com.juntao.commons.po.IPo;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MySqlParamMapBuilder {
	private static final Logger log = LoggerFactory.getLogger(MySqlParamMapBuilder.class);

	private static final int _MAX_OFFSET = 10000;

	private static final String UNION_LIST = "_unionlist";

	public static final String WHERE_OPERATOR_EQ = "_eq";
	public static final String WHERE_OPERATOR_NE = "_ne";
	public static final String WHERE_OPERATOR_LIKE = "_like";
	public static final String WHERE_OPERATOR_GT = "_gt";
	public static final String WHERE_OPERATOR_GTE = "_gte";
	public static final String WHERE_OPERATOR_LT = "_lt";
	public static final String WHERE_OPERATOR_LTE = "_lte";
	public static final String WHERE_OPERATOR_IN = "_in";
	public static final String WHERE_OPERATOR_IS_NULL = "_isNull";
	public static final String WHERE_OPERATOR_IS_NOT_NULL = "_isNotNull";

	private static final String[] NEED_ENCRYPT_PARAM_MAP_KEYS = new String[]
			{IMysqlMapper._UPDATE_SET_MAP, WHERE_OPERATOR_EQ, WHERE_OPERATOR_NE, WHERE_OPERATOR_IN};

	private int start;
	private int offset;
	private LinkedHashSet<String> selectColumnSet;
	private List<Object[]> updateSetList;
	private Map<String, List<Object[]>> whereMap = new HashMap<>();
	private List<Object[]> orderByList;
	private List<MySqlParamMapBuilder> unionList;

	private static final Map<String, Object> args4SelectAll = new Supplier<Map<String, Object>>() {
		@Override
		public Map<String, Object> get() {
			Map<String, Object> result = new HashMap();
			result.put(IMysqlMapper._START, 0);
			result.put(IMysqlMapper._OFFSET, _MAX_OFFSET);
			return result;
		}
	}.get();

	public static final Map<String, Object> createSelectAll() {
		return args4SelectAll;
	}

	public static final MySqlParamMapBuilder create() {
		return new MySqlParamMapBuilder();
	}

	private MySqlParamMapBuilder() {
	}

	public final MySqlParamMapBuilder limit(int start, int offset) {
		this.start = (0 > start ? 0 : start);
		this.offset = (0 >= offset ? _MAX_OFFSET : offset);
		return this;
	}

	public final MySqlParamMapBuilder selectColumns(String... columns) {
		if (null == columns || Stream.of(columns).allMatch(StringUtils::isBlank)) {
			return this;
		}

		if (null == selectColumnSet) {
			selectColumnSet = new LinkedHashSet<>();
		}
		selectColumnSet.addAll(Stream.of(columns).filter(UsefulFunctions.notBlankString).collect(Collectors.toList()));
		return this;
	}

	public final MySqlParamMapBuilder updateSet(String column, String value) {
		return this.updateSetImpl(column, value);
	}

	public final MySqlParamMapBuilder updateSet(String column, Number value) {
		return this.updateSetImpl(column, value);
	}

	public final MySqlParamMapBuilder updateSet(String column, Date value) {
		return this.updateSetImpl(column, value);
	}

	private final MySqlParamMapBuilder updateSetImpl(String column, Serializable value) {
		if (null == updateSetList) {
			updateSetList = new ArrayList<>();
		}
		updateSetList.add(new Object[]{column, value});
		return this;
	}

	public final MySqlParamMapBuilder addWhereAnd(String whereOpt, String column, Object value) {
		whereMap.computeIfAbsent(whereOpt, x -> new ArrayList<>()).add(new Object[]{column, value});
		return this;
	}

	public final MySqlParamMapBuilder andEq(String column, String value) {
		return this.andEqImpl(column, value);
	}

	public final MySqlParamMapBuilder andEq(String column, Number value) {
		return this.andEqImpl(column, value);
	}

	public final MySqlParamMapBuilder andEq(String column, Date value) {
		return this.andEqImpl(column, value);
	}

	private final MySqlParamMapBuilder andEqImpl(String column, Serializable value) {
		return addWhereAnd(WHERE_OPERATOR_EQ, column, value);
	}

	public final MySqlParamMapBuilder andNe(String column, String value) {
		return this.andNeImpl(column, value);
	}

	public final MySqlParamMapBuilder andNe(String column, Number value) {
		return this.andNeImpl(column, value);
	}

	public final MySqlParamMapBuilder andNe(String column, Date value) {
		return this.andNeImpl(column, value);
	}

	private final MySqlParamMapBuilder andNeImpl(String column, Serializable value) {
		return addWhereAnd(WHERE_OPERATOR_NE, column, value);
	}

	public final MySqlParamMapBuilder andLike(String column, String value) {
		return addWhereAnd(WHERE_OPERATOR_LIKE, column, value);
	}

	public final MySqlParamMapBuilder andGt(String column, String value) {
		return this.andGtImpl(column, value);
	}

	public final MySqlParamMapBuilder andGt(String column, Number value) {
		return this.andGtImpl(column, value);
	}

	public final MySqlParamMapBuilder andGt(String column, Date value) {
		return this.andGtImpl(column, value);
	}

	private final MySqlParamMapBuilder andGtImpl(String column, Serializable value) {
		return addWhereAnd(WHERE_OPERATOR_GT, column, value);
	}

	public final MySqlParamMapBuilder andGte(String column, String value) {
		return this.andGteImpl(column, value);
	}

	public final MySqlParamMapBuilder andGte(String column, Number value) {
		return this.andGteImpl(column, value);
	}

	public final MySqlParamMapBuilder andGte(String column, Date value) {
		return this.andGteImpl(column, value);
	}

	private final MySqlParamMapBuilder andGteImpl(String column, Serializable value) {
		return addWhereAnd(WHERE_OPERATOR_GTE, column, value);
	}

	public final MySqlParamMapBuilder andLt(String column, String value) {
		return this.andLtImpl(column, value);
	}

	public final MySqlParamMapBuilder andLt(String column, Number value) {
		return this.andLtImpl(column, value);
	}

	public final MySqlParamMapBuilder andLt(String column, Date value) {
		return this.andLtImpl(column, value);
	}

	private final MySqlParamMapBuilder andLtImpl(String column, Serializable value) {
		return addWhereAnd(WHERE_OPERATOR_LT, column, value);
	}

	public final MySqlParamMapBuilder andLte(String column, String value) {
		return this.andLteImpl(column, value);
	}

	public final MySqlParamMapBuilder andLte(String column, Number value) {
		return this.andLteImpl(column, value);
	}

	public final MySqlParamMapBuilder andLte(String column, Date value) {
		return this.andLteImpl(column, value);
	}

	private final MySqlParamMapBuilder andLteImpl(String column, Serializable value) {
		return addWhereAnd(WHERE_OPERATOR_LTE, column, value);
	}

	public final MySqlParamMapBuilder andIn(String column, Collection<?> value) {
		return addWhereAnd(WHERE_OPERATOR_IN, column, value);
	}

	public final MySqlParamMapBuilder andIsNull(String column) {
		return addWhereAnd(WHERE_OPERATOR_IS_NULL, column, StringUtils.EMPTY);
	}

	public final MySqlParamMapBuilder andIsNotNull(String column) {
		return addWhereAnd(WHERE_OPERATOR_IS_NOT_NULL, column, StringUtils.EMPTY);
	}

	public final MySqlParamMapBuilder orderBy(String column, boolean isAsc) {
		if (null == orderByList) {
			orderByList = new ArrayList<>();
		}
		orderByList.add(new Object[]{column, isAsc ? "ASC" : "DESC"});
		return this;
	}

	public final MySqlParamMapBuilder union(MySqlParamMapBuilder union) {
		if (null == unionList) {
			unionList = new ArrayList<>();
		}
		unionList.add(union);
		return this;
	}

	public final boolean isWhereEmpty() {
		return CollectionUtils.isEmpty(whereMap);
	}

	public final Map<String, Object> build() {
		Map<String, Object> args = new HashMap<String, Object>();

		args.put(IMysqlMapper._START, 0 > start ? 0 : start);
		args.put(IMysqlMapper._OFFSET, 0 >= offset ? _MAX_OFFSET : offset);

		if (null != selectColumnSet) {
			args.put(IMysqlMapper._SELECT_COLUMN_LIST, selectColumnSet.stream().collect(Collectors.toList()));
		}

		if (null != updateSetList) {
			args.put(IMysqlMapper._UPDATE_SET_MAP, updateSetList.stream().collect(Collectors.toMap(
					array -> (String) array[0], array -> array[1], (oldValue, newValue) -> newValue, HashMap::new)));
		}

		if (!CollectionUtils.isEmpty(whereMap)) {
			whereMap.entrySet().stream().forEach(entry -> {
				args.put(entry.getKey(), entry.getValue().stream().collect(Collectors.toMap(
						array -> (String) array[0], array -> array[1], (oldValue, newValue) -> newValue, HashMap::new)));
			});
		}

		if (null != unionList) {
			args.put(UNION_LIST, unionList.stream().map(MySqlParamMapBuilder::build).collect(Collectors.toList()));
		}

		if (null != orderByList) {
			args.put(IMysqlMapper._ORDER_BY_MAP, orderByList.stream().collect(Collectors.toMap(
					array -> (String) array[0], array -> array[1], (oldValue, newValue) -> newValue, LinkedHashMap::new)));
		}

		return args;
	}

	public static final <T extends IPo> Map<String, Object> encryptParamMap(Map<String, Object> args, Class<T> poClass) {
		if (CollectionUtils.isEmpty(args) || null == poClass) {
			return args;
		}

		Set<String> needEncryptColumnNameSet = SEncryptPoUtil.needEncryptFieldSet(poClass)
				.stream().filter(UsefulFunctions.notNull).map(Field::getName).filter(UsefulFunctions.notBlankString)
				.map(MySqlParamMapBuilder::convertFieldName2ColumnName).collect(Collectors.toSet());
		if (CollectionUtils.isEmpty(needEncryptColumnNameSet)) {
			return args;
		}

		Map<String, Object> resultArgs = new HashMap<>();
		resultArgs.putAll(args);

		for (String key : NEED_ENCRYPT_PARAM_MAP_KEYS) {
			if (!args.containsKey(key)) {
				continue;
			}

			Map<String, Object> columnNameValue = (Map<String, Object>) args.get(key);
			if (CollectionUtils.isEmpty(columnNameValue)) {
				resultArgs.put(key, columnNameValue);
			} else {
				resultArgs.put(key, encryptColumnNameValueMap(columnNameValue, needEncryptColumnNameSet));
			}
		}

		if (!args.containsKey(UNION_LIST)) {
			return resultArgs;
		}

		List<Map<String, Object>> unionMapList = (List<Map<String, Object>>) args.get(UNION_LIST);
		if (CollectionUtils.isEmpty(unionMapList)) {
			resultArgs.put(UNION_LIST, unionMapList);
			return resultArgs;
		}

		List<Map<String, Object>> resultUnionMapList = new ArrayList<>();
		for (Map<String, Object> unionMap : unionMapList) {
			if (CollectionUtils.isEmpty(unionMap)) {
				resultUnionMapList.add(unionMap);
			} else {
				resultUnionMapList.add(encryptParamMap(unionMap, poClass));
			}
		}
		resultArgs.put(UNION_LIST, resultUnionMapList);

		return resultArgs;
	}

	private static final Map<String, Object> encryptColumnNameValueMap(Map<String, Object> columnName_value,
																	   Set<String> needEncryptColumnNameSet) {
		if (CollectionUtils.isEmpty(columnName_value) || CollectionUtils.isEmpty(needEncryptColumnNameSet)) {
			return columnName_value;
		}

		Map<String, Object> resultColumnNameValueMap = new HashMap<>();
		resultColumnNameValueMap.putAll(columnName_value);

		for (String columnName : needEncryptColumnNameSet) {
			Object value = resultColumnNameValueMap.get(columnName);
			if (null == value) {
				continue;
			}

			Class valueClass = value.getClass();

			if (String.class.equals(valueClass)) {
				String valueStr = (String) value;
				if (StringUtils.isBlank(valueStr)) {
					continue;
				}

				try {
					resultColumnNameValueMap.put(columnName, SEncryptPoUtil.encryptFieldValueStrByPrivateKey(valueStr));
				} catch (Exception e) {
					log.error("encryptFieldValueStrByPrivateKey error!  value= " + valueStr, e);
				}
			} else if (Collection.class.isAssignableFrom(valueClass)) {
				Collection<?> collection = ((Collection<?>) value);
				if (CollectionUtils.isEmpty(collection) || collection.stream().allMatch(UsefulFunctions.isNull)) {
					continue;
				}

				Set<Object> encryptedValSet = new HashSet<>();
				for (Object o : collection) {
					if (null == o || !String.class.equals(o.getClass())) {
						encryptedValSet.add(o);
						continue;
					}

					String s = (String) o;
					if (StringUtils.isBlank(s)) {
						encryptedValSet.add(o);
						continue;
					}

					try {
						encryptedValSet.add(SEncryptPoUtil.encryptFieldValueStrByPrivateKey(s));
					} catch (Exception e) {
						log.error("encryptFieldValueStrByPrivateKey error!  value= " + s, e);
						encryptedValSet.add(o);
					}
				}
				resultColumnNameValueMap.put(columnName, encryptedValSet);
			}
		}

		return resultColumnNameValueMap;
	}

	private static final String convertFieldName2ColumnName(String fieldName) {
		if (StringUtils.isBlank(fieldName)) {
			return fieldName;
		}

		StringBuilder sb = new StringBuilder();
		for (char c : StringUtils.strip(fieldName).toCharArray()) {
			if (Character.isUpperCase(c)) {
				sb.append(SConsts.UNDER_SCORE).append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
