package com.juntao.commons.util;

import com.juntao.commons.annotation.NeedStarMask;
import com.juntao.commons.consts.SConsts;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Created by major on 2017/6/30.
 */
public final class StarMaskUtil {
	private static final BigDecimal THREE = new BigDecimal(3);

	private static final Predicate<Class<?>> shouldPojoClassProcessedFunc = fieldValueClass ->
			fieldValueClass.isAnnotationPresent(NeedStarMask.class);

	private static final BiPredicate<Field, String> shouldFieldStrValProcessedFunc = (field, strVal) ->
			field.isAnnotationPresent(NeedStarMask.class);

	private static final BiFunction<Field, String, String> processStrFunc = (field, strVal) -> {
		switch (field.getAnnotation(NeedStarMask.class).value()) {
			case COMMON_STRING:
				return starMaskCommonString(strVal);
			case NAME:
				return starMaskName(strVal);
			case CELLPHONE:
				return starMaskCellphone(strVal);
			case BANK_CARD:
				return starMaskBankCard(strVal);
			case ID_CARD:
				return starMaskIdCard(strVal);
			case EMAIL:
				return starMaskEmail(strVal);
			case TEL:
				return starMaskTel(strVal);
			default:
				return starMaskCommonString(strVal);
		}
	};

	public static final boolean starMaskVo(Object vo) throws Exception {
		if (null == vo) {
			return false;
		}

		SProcessPojoStringUtil.processPojo(vo, shouldPojoClassProcessedFunc, shouldFieldStrValProcessedFunc, processStrFunc);

		return true;
	}

	public static final boolean starMaskListVo(List<?> voList) throws Exception {
		if (CollectionUtils.isEmpty(voList)) {
			return false;
		}

		SProcessPojoStringUtil.processPojoCollection(voList, shouldPojoClassProcessedFunc,
				shouldFieldStrValProcessedFunc, processStrFunc);

		return true;
	}

	/**
	 * 地址，单位等字符串信息加星号
	 * 屏蔽规则为： 每4个汉字展示前1位，末3位屏蔽；少于4个汉字的只展示第1位，其他屏蔽。（数字字母均视同汉字处理）
	 *
	 * @param commonString
	 * @return
	 */
	public static final String starMaskCommonString(String commonString) {
		if (StringUtils.isBlank(commonString)) {
			return commonString;
		}

		String s = StringUtils.strip(commonString);
		List<Character> unmaskedCharList = new ArrayList<>();
		for (int i = 0; i < s.length(); i += 4) {
			unmaskedCharList.add(s.charAt(i));
		}
		String maskResult = StringUtils.join(unmaskedCharList, StringUtils.repeat(SConsts.STAR, 3));
		if (maskResult.length() == s.length()) {
			return maskResult;
		} else {
			return StringUtils.rightPad(maskResult, s.length(), SConsts.STAR);
		}
	}

	/**
	 * 姓名加星号
	 * 屏蔽规则为：
	 * 含中文姓名：每4个汉字展示前1位，末3位屏蔽；少于4个汉字的只展示第1位， 其他屏蔽。（数字母均视同汉处理）
	 * 例子：张 **
	 * 全英文姓名：如果中间是有空格的，屏蔽第一个空格后面所字母；如果中间没有空格，末3分之2( 四舍五入)部分屏蔽掉。
	 * 例子： Mark Elliot Zuckerberg 屏蔽后为 Mark ****** ********** ；
	 * Mark 屏蔽后为 M***；
	 *
	 * @param name
	 * @return
	 */
	public static final String starMaskName(String name) {
		if (StringUtils.isBlank(name)) {
			return name;
		}

		String s = StringUtils.strip(name);
		final int firstIndexOfSpace = StringUtils.indexOf(s, StringUtils.SPACE);
		final int firstIndexOfFullSpace = StringUtils.indexOf(s, SConsts.FULL_SPACE);
		if (0 < firstIndexOfSpace || 0 < firstIndexOfFullSpace) {
			final int minIndex = IntStream.of(firstIndexOfSpace, firstIndexOfFullSpace).filter(i -> 0 < i).min().orElse(0);
			StringBuilder sb = new StringBuilder(StringUtils.substring(s, 0, minIndex));
			for (int i = minIndex; i < s.length(); i++) {
				String c = String.valueOf(s.charAt(i));
				if (StringUtils.SPACE.equals(c) || SConsts.FULL_SPACE.equals(c)) {
					sb.append(c);
				} else {
					sb.append(SConsts.STAR);
				}
			}
			return sb.toString();
		} else {
			return starMaskCommonString(name);
		}
	}

	/**
	 * 手机号码加星号
	 * 屏蔽规则为：11位手机号码，展示为: 号码前3位 + ***** + 号码末3位
	 * 例子： 186*****258
	 *
	 * @param cellPhone
	 * @return
	 */
	public static final String starMaskCellphone(String cellPhone) {
		if (StringUtils.isBlank(cellPhone)) {
			return cellPhone;
		}

		String s = StringUtils.strip(cellPhone);
		if (6 >= s.length()) {
			return s;
		}

		return StringUtils.substring(s, 0, 3)
				+ StringUtils.join(IntStream.range(0, s.length() - 6).boxed().map(i -> SConsts.STAR).toArray())
				+ StringUtils.substring(s, s.length() - 3);
	}

	/**
	 * 银行卡号加星号
	 * 屏蔽规则为：只展示前2位、末2位，中间均屏蔽。
	 *
	 * @param bankCard
	 * @return
	 */
	public static final String starMaskBankCard(String bankCard) {
		if (StringUtils.isBlank(bankCard)) {
			return bankCard;
		}

		String s = StringUtils.strip(bankCard);
		if (4 >= s.length()) {
			return s;
		}

		return StringUtils.substring(s, 0, 2)
				+ StringUtils.join(IntStream.range(0, s.length() - 4).boxed().map(i -> SConsts.STAR).toArray())
				+ StringUtils.substring(s, s.length() - 2);
	}

	/**
	 * 身份证号加星号
	 * 屏蔽规则为：
	 * 号码长度为15位的：号码第7位到12位和最后1位屏蔽，其他不屏蔽
	 * 号码长度为18位的：号码第7位到14位和最后2位屏蔽，其他不屏蔽
	 * 例子 : 510281******30* :
	 * 例子 : 510281********30**
	 *
	 * @param idCard
	 * @return
	 */
	public static final String starMaskIdCard(String idCard) {
		if (StringUtils.isBlank(idCard)) {
			return idCard;
		}

		String s = StringUtils.strip(idCard);
		int middleStarLength = 0;
		String tailStarStr = null;
		if (15 == s.length()) {
			middleStarLength = 6;
			tailStarStr = SConsts.STAR;
		} else if (18 == s.length()) {
			middleStarLength = 8;
			tailStarStr = StringUtils.repeat(SConsts.STAR, 2);
		} else {
			return s;
		}

		return StringUtils.substring(s, 0, 6)
				+ StringUtils.join(IntStream.range(0, middleStarLength).boxed().map(i -> SConsts.STAR).toArray())
				+ StringUtils.substring(s, 6 + middleStarLength, 8 + middleStarLength) + tailStarStr;
	}

	/**
	 * 电子邮箱加星号
	 * 屏蔽规则为：Email地址‘@’前面字符的末3分之2(四舍五入)部分屏蔽掉。
	 * 例子：ag****@gmail.com
	 *
	 * @param email
	 * @return
	 */
	public static final String starMaskEmail(String email) {
		if (StringUtils.isBlank(email)) {
			return email;
		}

		String s = StringUtils.strip(email);
		int indexOfAt = StringUtils.indexOf(s, SConsts.AT);
		if (0 >= indexOfAt) {
			return s;
		}

		int unmaskHeadLength = (indexOfAt + 1) / 3;
		return StringUtils.substring(s, 0, unmaskHeadLength)
				+ StringUtils.join(IntStream.range(0, indexOfAt - unmaskHeadLength).boxed().map(i -> SConsts.STAR).toArray())
				+ StringUtils.substring(s, indexOfAt);
	}

	/**
	 * 固定电话加星号
	 * 屏蔽规则为：区号不屏蔽；电话号码屏蔽末4位；分机号码屏蔽末2位。
	 * 例子：0571 -8700**** 8700**** 8700****-00** 00**00**
	 *
	 * @param tel
	 * @return
	 */
	public static final String starMaskTel(String tel) {
		if (StringUtils.isBlank(tel)) {
			return tel;
		}

		String s = StringUtils.strip(tel);
		if (4 >= s.length()) {
			return s;
		}

		String twoStarStr = StringUtils.repeat(SConsts.STAR, 2);
		String fourStarStr = StringUtils.repeat(SConsts.STAR, 4);

		int hyphenQty = StringUtils.countMatches(s, SConsts.HYPHEN);
		if (0 == hyphenQty) {
			return StringUtils.substring(s, 0, s.length() - 4) + fourStarStr;
		} else {
			final int firstIndexOfHyphen = StringUtils.indexOf(s, SConsts.HYPHEN);
			if (1 == hyphenQty) {
				if (4 < firstIndexOfHyphen) {
					final String left = StringUtils.substring(s, 0, firstIndexOfHyphen - 4) + fourStarStr;
					if (firstIndexOfHyphen + 2 > s.length()) {
						return left + StringUtils.substring(s, firstIndexOfHyphen);
					} else {
						return left + StringUtils.substring(s, firstIndexOfHyphen, s.length() - 2) + twoStarStr;
					}
				} else {
					return StringUtils.substring(s, 0, s.length() - 4) + fourStarStr;
				}
			} else if (2 == hyphenQty) {
				final int lastIndexOfHyphen = StringUtils.lastIndexOf(s, SConsts.HYPHEN);
				String right = null;
				if (lastIndexOfHyphen + 2 < s.length()) {
					right = StringUtils.substring(s, lastIndexOfHyphen, s.length() - 2) + twoStarStr;
				} else {
					right = StringUtils.substring(s, lastIndexOfHyphen);
				}

				if (4 >= lastIndexOfHyphen - firstIndexOfHyphen) {
					return StringUtils.substring(s, 0, lastIndexOfHyphen) + right;
				} else {
					return StringUtils.substring(s, 0, lastIndexOfHyphen - 4) + StringUtils.repeat(SConsts.STAR, 4) + right;
				}
			} else {
				return s;
			}
		}
	}
}
