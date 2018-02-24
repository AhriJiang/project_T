package com.juntao.commons.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by major on 2017/7/6.
 */
public final class SEscapseCharUtil {
	public static String convertHalf2Full(String src) {
		return StringUtils.replaceEach(src,
				new String[]{"<", ">", "'", "\""},
				new String[]{"＜", "＞", "＇", "＂"});
	}

	public static void main(String[] args) {
		System.out.println(convertHalf2Full("<script>function aa(){;}</script>"));
	}
}
