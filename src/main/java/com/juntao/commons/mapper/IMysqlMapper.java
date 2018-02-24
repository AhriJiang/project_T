package com.juntao.commons.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.juntao.commons.po.IPo;

public interface IMysqlMapper<S extends Serializable, T extends IPo<S>> {
	String _ID = "id";
	String _ID_LIST = "idList";

	boolean _ASC = true;
	boolean _DESC = false;

	String _SELECT_COLUMN_LIST = "selectColumnList";

	String _UPDATE_SET_MAP = "updateSetMap";

	String _ORDER_BY_MAP = "orderByMap";

	String _START = "start";
	String _OFFSET = "offset";

	int insert(T t); // 如果插入了某一行则返回1， 如果这一行没有插入则返回0

	int insertList(List<T> tList); // 总共有size行，如果插入成功了n行， 同时(size - n)行没有插入成功，则返回n

	int save(T t); // 如果更新了某一行则返回2， 如果没有更新任何一行则返回1  （如果t同时覆盖了多行的主键或唯一键，则更新会失败，返回1）

	int saveList(List<T> tList);  // 总共有size行，如果更新了n行， 同时(size - n)行没有更新；则返回2n + 1*(size - n)， 等于n + size

	int update(T t);

	T select(S id);

	List<T> selectList(Map<String, Object> args);

	List<S> selectIdList(Map<String, Object> args);

	int count(Map<String, Object> args);
}
