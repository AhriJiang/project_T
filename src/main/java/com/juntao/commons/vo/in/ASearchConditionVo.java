package com.juntao.commons.vo.in;

import com.juntao.commons.annotation.SearchCondition;
import com.juntao.commons.annotation.SearchConditions;
import com.juntao.commons.consts.SConsts;
import com.juntao.commons.consts.ToStringObject;
import com.juntao.commons.function.UsefulFunctions;
import com.juntao.commons.mapper.IMysqlMapper;
import com.juntao.commons.util.MySqlParamMapBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Min;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ASearchConditionVo extends ToStringObject {
	private static final Logger log = LoggerFactory.getLogger(ASearchConditionVo.class);

	public static interface IValidate {}

	private static final Set<Integer> legalPageSizeSet = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 50, 100, 200).collect(Collectors.toSet());

	public static final int MAX_PAGE_SIZE = 100;

	public static final int DEFAULT_MAX_EXCEL_LINE_QTY = 10000;

	abstract public <M extends IMysqlMapper> Pair<Class<M>, String> getDefaultOrderByColumn();

	protected String getIdColumn4OrderBy() {
		return IMysqlMapper._ID;
	}

	@Min(value = 1, message = "页码数无效", groups = IValidate.class)
	private Integer pageNum = 1;

	@Min(value = 1, message = "每页条数无效", groups = IValidate.class)
	private Integer pageSize = 10;

	private boolean exportExcelFlag = false;

	public Integer getPageNum() {
		return pageNum;
	}

	public void setPageNum(Integer pageNum) {
		this.pageNum = pageNum;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = legalPageSizeSet.contains(pageSize) ? pageSize : -1;
	}

	public boolean isExportExcelFlag() {
		return exportExcelFlag;
	}

	public void init4ExportExcel() {
		this.pageNum = 1;
		this.pageSize = DEFAULT_MAX_EXCEL_LINE_QTY;
		exportExcelFlag = true;
	}

	private static final void addAllSuperVoClass(Class<?> clazz, Set<Class> set) {
		if (clazz.equals(ASearchConditionVo.class)) {
			return;
		}

		Class<?> superClass = clazz.getSuperclass();
		set.add(superClass);
		addAllSuperVoClass(superClass, set);
	}

	public final <M extends IMysqlMapper> MySqlParamMapBuilder createMySqlParamMapBuilder(Class<M> mapperClass) {
		Set<Class> classSet = new HashSet<>();
		classSet.add(getClass());
		addAllSuperVoClass(getClass(), classSet);

		final MySqlParamMapBuilder mySqlParamMapBuilder = MySqlParamMapBuilder.create()
//                .orderBy(getDefaultOrderByColumn(), IMysqlMapper._DESC)
				.limit((pageNum - 1) * pageSize, pageSize);
		if (getDefaultOrderByColumn().getLeft() == mapperClass) {
			mySqlParamMapBuilder.orderBy(getDefaultOrderByColumn().getRight(), IMysqlMapper._DESC)
								.orderBy(getIdColumn4OrderBy(), IMysqlMapper._DESC);
		}
//
//        //TODO order by
//        Method orderbyMethod = getOrderbyMethod();
//        if (orderbyMethod != null) {
//            OrderByCondition orderByConAnno =  orderbyMethod.getAnnotation(OrderByCondition.class);
//            OrderByConditions orderByConsAnno = orderbyMethod.getAnnotation(OrderByConditions.class);
//
//            if(orderByConAnno == null && orderByConsAnno == null){
//                mySqlParamMapBuilder.orderBy(getDefaultOrderByColumn(), IMysqlMapper._DESC);
//            }else {
//                Stream<OrderByCondition> orderByConStream = Stream.concat(Stream.of(orderByConAnno),
//                        Optional.ofNullable(orderByConsAnno).map(OrderByConditions::value).map(Stream::of).orElse(Stream.empty()));
//
//                OrderByCondition orderByCon = orderByConStream.filter(UsefulFunctions.notNull).
//                        filter(anno -> mapperClass == anno.mapper()).findAny().orElse(null);
//                if (orderByCon != null) {
//                    mySqlParamMapBuilder.orderBy(getDefaultOrderByColumn(), IMysqlMapper._DESC);
//                }
//            }
//        }

		classSet.stream().flatMap(clazz -> Stream.of(clazz.getDeclaredFields()))
				.filter(field -> !"pageNum".equals(field.getName()) && !"pageSize".equals(field.getName()))
				.filter(field -> !modifierContains(field.getModifiers(), Modifier.PUBLIC)
						&& !modifierContains(field.getModifiers(), Modifier.STATIC)
						&& !modifierContains(field.getModifiers(), Modifier.FINAL))
				.forEach(field -> mySqlParamMapBuilderAddWhere(mapperClass, field, this, mySqlParamMapBuilder));

		return mySqlParamMapBuilder;
	}

	private final <M extends IMysqlMapper> void mySqlParamMapBuilderAddWhere(Class<M> mapperClass, Field field, Object obj,
																			 MySqlParamMapBuilder mySqlParamMapBuilder) {
		try {
			if (!field.isAnnotationPresent(SearchCondition.class) && !field.isAnnotationPresent(SearchConditions.class)) {
				return;
			}

			SearchCondition searchCondition = Stream.concat(
					Optional.ofNullable(field.getAnnotation(SearchCondition.class)).map(Stream::of).orElse(Stream.empty()),
					Optional.ofNullable(field.getAnnotation(SearchConditions.class)).map(SearchConditions::value).map(Stream::of).orElse(Stream.empty()))
					.filter(UsefulFunctions.notNull).filter(anno -> mapperClass == anno.mapper()).findAny().orElse(null);
			if (null == searchCondition) {
				return;
			}

			field.setAccessible(true);
			Object fieldValue = field.get(obj);
			if (null == fieldValue) {
				return;
			}

			Class fieldValueClass = fieldValue.getClass();
			String whereOperator = searchCondition.operator();
			if (MySqlParamMapBuilder.WHERE_OPERATOR_IN.equals(whereOperator)) {
				if (String.class.isAssignableFrom(fieldValueClass)) {
					fieldValue = Stream.of(StringUtils.split((String) fieldValue, SConsts.COMMA)).map(s -> StringUtils.stripToEmpty(s))
							.collect(Collectors.toSet());
				} else if (Collection.class.isAssignableFrom(fieldValueClass)) {
					fieldValue = ((Collection) fieldValue).stream().filter(item -> null != item).collect(Collectors.toSet());
				} else {
					return;
				}

				if (Collection.class.isAssignableFrom(fieldValue.getClass()) && CollectionUtils.isEmpty(((Collection) fieldValue))) {
					return;
				}
			} else if (MySqlParamMapBuilder.WHERE_OPERATOR_LIKE.equals(whereOperator)) {
				if (String.class.isAssignableFrom(fieldValueClass) && StringUtils.isNotBlank((String) fieldValue)) {
					fieldValue = SConsts.PERCENT + StringUtils.stripToEmpty((String) fieldValue) + SConsts.PERCENT;
				} else {
					return;
				}
			} else if (MySqlParamMapBuilder.WHERE_OPERATOR_EQ.equals(whereOperator) || MySqlParamMapBuilder.WHERE_OPERATOR_NE.equals(whereOperator)) {
				if (String.class.isAssignableFrom(fieldValueClass)) {
					String fieldValueStr = (String) fieldValue;
					if (StringUtils.isBlank(fieldValueStr)) {
						return;
					}
					fieldValue = StringUtils.stripToEmpty(fieldValueStr);
				}
			}
			mySqlParamMapBuilder.addWhereAnd(whereOperator, searchCondition.column(), fieldValue);
			return;
		} catch (Exception e) {
			log.error("mySqlParamMapBuilder add searchConditionVo field error!! field= " + field.getName() + ", searchConditionVo= " + this, e);
			return;
		}
	}

	private static final boolean modifierContains(int compoundModifiers, int targetModifier) {
		return 0 < targetModifier && (compoundModifiers > targetModifier) && ((compoundModifiers - targetModifier) == (compoundModifiers ^ targetModifier));
	}

//    private final Method getOrderbyMethod() {
//        try {
//            return this.getClass().getDeclaredMethod(ORDER_BY_METHOD_NAME, null);
//        } catch (NoSuchMethodException e) {
//            //ignore
//        }
//
//        return null;
//    }
}
