package com.juntao.commons.util;

import org.apache.commons.lang3.StringUtils;

import com.juntao.commons.function.UsefulFunctions;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SIpUtil {
	public static final List<Inet4Address> getAllLocalInet4AddressList() {
		List<Inet4Address> result = new ArrayList<Inet4Address>();

		Enumeration<NetworkInterface> allNetInterfaces = null;
		try {
			allNetInterfaces = NetworkInterface.getNetworkInterfaces();
		} catch (java.net.SocketException e) {
			e.printStackTrace();
		}
		InetAddress inetAddress = null;
		while (allNetInterfaces.hasMoreElements()) {
			NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
			Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
			while (addresses.hasMoreElements()) {
				inetAddress = addresses.nextElement();
				if (inetAddress != null && inetAddress instanceof Inet4Address && !"127.0.0.1".equals(StringUtils.stripToEmpty(inetAddress.getHostAddress()))) {
					result.add((Inet4Address) inetAddress);
				}
			}
		}

		return result;
	}

	public static final List<Inet4Address> getAllLocalPrivateInet4AddressList() {
		return getAllLocalInet4AddressList().stream().filter(inet4Address -> isPrivateIPv4(inet4Address.getHostAddress())).collect(Collectors.toList());
	}

	private static final boolean isPrivateIPv4(String ipv4) {
		if (ipv4.startsWith("192.168.") || ipv4.startsWith("10.") || ipv4.startsWith("99.")) {
			return true;
		}

		if (ipv4.startsWith("172.")) {
			final int seg2 = Integer.valueOf(StringUtils.split(ipv4, '.')[1]);
			if (16 <= seg2 && 31 >= seg2) {
				return true;
			}
		}

		return false;
	}

	public static String getRequestIp(HttpServletRequest request) {
		return	Stream.of("X-REAL-IP", "X-FORWARDED-FOR", "PROXY-CLIENT-IP", "WL-PROXY-CLIENT-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR")
						.map(request::getHeader).filter(UsefulFunctions.notBlankString)
						.filter(UsefulFunctions.test(String::equalsIgnoreCase, "unknown").negate())
						.findFirst().orElse(request.getRemoteAddr());
	}

	public static void main(String[] args) {
		getAllLocalPrivateInet4AddressList().stream().map(Inet4Address::getHostAddress).forEach(System.out::println);
	}
}
