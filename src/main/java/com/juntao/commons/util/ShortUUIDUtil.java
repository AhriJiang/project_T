package com.juntao.commons.util;

import java.util.UUID;

/**
 * Created by major on 2017/7/4.
 */
public class ShortUUIDUtil {
	private final static char[] DIGITS64 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_".toCharArray();

	public static String[] chars = new String[] { "a", "b", "c", "d", "e", "f",
			"g", "h", "i", "j", "k", "m", "n", "p", "q", "r", "s",
			"t", "u", "v", "w", "x", "y", "z", "2", "3", "4", "5",
			"6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
			"J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V",
			"W", "X", "Y", "Z" };

	public static String next() {
		UUID u = UUID.randomUUID();
		return toIDString(u.getMostSignificantBits()) + toIDString(u.getLeastSignificantBits());
	}

	private static String toIDString(long l) {
		char[] buf = "00000000000".toCharArray(); // 限定11位长度
		int length = 11;
		long least = 63L; // 0x0000003FL
		do {
			buf[--length] = DIGITS64[(int) (l & least)]; // l & least取低6位
			/* 无符号的移位只有右移，没有左移
             * 使用“>>>”进行移位
             * 为什么没有无符号的左移呢，知道原理的说一下哈
             */
			l >>>= 6;
		} while (l != 0);
		return new String(buf);
	}

	public static String next8BitCode(){
		while(true){
			String result = generateShort8BitUuid();
			if(result.matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[0-9a-zA-Z]{6,16}$")){
				return result;
			}
		}
	}

	public static String generateShort8BitUuid() {
		StringBuffer shortBuffer = new StringBuffer();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		for (int i = 0; i < 8; i++) {
			String str = uuid.substring(i * 4, i * 4 + 4);
			int x = Integer.parseInt(str, 16);
			shortBuffer.append(chars[x % 0x39]);
		}
		return shortBuffer.toString();

	}
}
