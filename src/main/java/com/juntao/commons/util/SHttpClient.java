package com.juntao.commons.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class SHttpClient implements Cloneable {
	private static final Logger log = LoggerFactory.getLogger(SHttpClient.class);

	private static final int HTTP_STATUS_EXCEED_LIMIT = 429;

	private static final HttpHost proxy = new HttpHost("172.20.30.240", 3128, "http");

	// private static final Header[] defaultHeaders = new Header[] {
	// new BasicHeader(
	// "User-Agent",
	// "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0"),
	// new BasicHeader("Accept-Language", " zh-cn,en-us;q=0.8,zh-tw;q=0.5,en;q=0.3"),
	// new BasicHeader("Accept-Charset", "utf-8, GBK, GB2312;q=0.7,*;q=0.7") };

	private static final Header[] defaultHeaders = new Header[]{new BasicHeader(
			"User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0")};

	private static final AtomicLong counter = new AtomicLong(0);
	private static final List<Inet4Address> localPrivateInet4AddressList = SIpUtil.getAllLocalPrivateInet4AddressList();

	private static final Pattern charsetPattern = Pattern.compile("<(meta|\\?\\s?xml).*?(charset=|encoding=)\"?([\\w-]+)", Pattern.CASE_INSENSITIVE);

	private static final PoolingHttpClientConnectionManager poolingClientConnectionManager = new PoolingHttpClientConnectionManager();

	static {
		poolingClientConnectionManager.setMaxTotal(200);
		poolingClientConnectionManager.setDefaultMaxPerRoute(200);
	}

	private static final Builder requestConfigBuilder = RequestConfig.custom().setConnectionRequestTimeout(30000).setConnectTimeout(30000).setSocketTimeout(
			30000);

	private static final HttpClientBuilder closeableHttpClientBuilder = HttpClients
			.custom().setDefaultCookieStore(new BasicCookieStore()).addInterceptorLast(new HttpResponseInterceptor() {

				public void process(final HttpResponse response, final HttpContext context) {
					Header contentEncodingheader = response.getEntity().getContentEncoding();
					if (contentEncodingheader != null) {
						HeaderElement[] codecs = contentEncodingheader.getElements();
						for (int i = 0; i < codecs.length; i++) {
							if (codecs[i].getName().equalsIgnoreCase("gzip")) {
								response.setEntity(new HttpEntityWrapper(response.getEntity()) {

									@Override
									public InputStream getContent() throws IOException, IllegalStateException {
										return new GZIPInputStream(wrappedEntity.getContent());
									}

									@Override
									public long getContentLength() {
										// length of ungzipped content is not
										// known
										return -1;
									}
								});
							}
						}
					}
				}
			}).setConnectionManager(poolingClientConnectionManager).setConnectionReuseStrategy(new ConnectionReuseStrategy() {
				@Override
				public boolean keepAlive(HttpResponse lastHttpResponse, HttpContext httpContext) {
					return false;
				}
			}).setRetryHandler(new HttpRequestRetryHandler() {
				public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
					if (executionCount >= 2) {
						// Do not retry if over max retry count
						return false;
					}
					if (exception instanceof NoHttpResponseException) {
						// Retry if the server dropped connection on us
						return true;
					}
					if (exception instanceof SSLHandshakeException) {
						// Do not retry on SSL handshake exception
						return false;
					}

					HttpRequest request = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
					boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
					if (idempotent) {
						// Retry if the request is considered idempotent
						return true;
					}
					return false;
				}
			});

	private String ip;
	private CloseableHttpClient closeableHttpClient;

	public SHttpClient() {
		this(null, false);
	}

	public SHttpClient(boolean useProxy) {
		this(null, useProxy);
	}

	public SHttpClient(String ip, boolean useProxy) {
		Inet4Address inet4Address = null;

		if (StringUtils.isBlank(ip)) {
			inet4Address = localPrivateInet4AddressList.stream().findFirst().orElse(null);
		}

		if (null != inet4Address) {
			init(inet4Address, useProxy);
		} else {
			setLocalIp(ip, useProxy);
		}
	}

	public void setLocalIp(String ip, boolean useProxy) {
		if (StringUtils.isBlank(ip)) {
			return;
		}

		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			log.error("UnknownHostException ip= " + ip, e);
			return;
		}

		init(inetAddress, useProxy);
	}

	private void init(InetAddress inetAddress, boolean useProxy) {
		if (useProxy) {
			requestConfigBuilder.setProxy(proxy);
		}

		if (null == inetAddress) {
			return;
		}

		closeableHttpClient = closeableHttpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.setLocalAddress(inetAddress).build()).build();

		if (null != closeableHttpClient) {
			this.ip = inetAddress.getHostAddress();
		}
	}

	public static final String clean(String url) {
		return url.replaceAll("\\|", "%7C");// 目前只发现此字符会导致异常
	}

	public String getIp() {
		return ip;
	}

	public String get(String url) {
		return get(url, null);
	}

	public String get(String url, String referUrl) {
		Triple<String, String, Charset> realUrl_content_charset = getRealUrlAndContentAndCharset(url, referUrl);
		if (null != realUrl_content_charset) {
			return realUrl_content_charset.getMiddle();
		}
		return null;
	}

	public String get(String url, Header[] headers, String referUrl) {
		Triple<String, String, Charset> realUrl_content_charset = getRealUrlAndContentAndCharset(url, headers, referUrl);
		if (null != realUrl_content_charset) {
			return realUrl_content_charset.getMiddle();
		}
		return null;
	}

	public Pair<String, Charset> getContentAndCharset(String url) {
		return getContentAndCharset(url, null);
	}

	public Pair<String, Charset> getContentAndCharset(String url, String referUrl) {
		Triple<String, String, Charset> realUrl_content_charset = getRealUrlAndContentAndCharset(url, referUrl);
		if (null != realUrl_content_charset) {
			return Pair.of(realUrl_content_charset.getMiddle(), realUrl_content_charset.getRight());
		}
		return null;
	}

	public Pair<String, Charset> getContentAndCharset(String url, Header[] headers, String referUrl) {
		Triple<String, String, Charset> realUrl_content_charset = getRealUrlAndContentAndCharset(url, headers, referUrl);
		if (null != realUrl_content_charset) {
			return Pair.of(realUrl_content_charset.getMiddle(), realUrl_content_charset.getRight());
		}
		return null;
	}

	public Triple<String, String, Charset> getRealUrlAndContentAndCharset(String url, String referUrl) {
		return getRealUrlAndContentAndCharset(url, null, referUrl);
	}

	public Triple<String, String, Charset> getRealUrlAndContentAndCharset(String url, Header[] headers, String referUrl) {
		HttpGet httpGet = null;
		try {
			httpGet = new HttpGet(clean(url));
		} catch (Exception e) {
			log.error("illegal url for httpGet,  url= " + url, e);
			return Triple.of(url, null, null);
		}

		if (ArrayUtils.isEmpty(headers)) {
			httpGet.setHeaders(defaultHeaders);
			httpGet.addHeader("Accept-Encoding", "gzip, deflate");
		} else {
			for (Header header : headers) {
				httpGet.addHeader(header);
			}
		}
		if (StringUtils.isNotBlank(referUrl)) {
			httpGet.addHeader("Referer", clean(referUrl));
		}

		HttpClientContext httpClientContext = HttpClientContext.create();
		CloseableHttpResponse closeableHttpResponse = requestExecutor(httpGet, httpClientContext);
		if (null == closeableHttpResponse) {
			return Triple.of(url, null, null);
		}

		String realUrl = url;
		// List<URI> redirectLocations =
		// httpClientContext.getRedirectLocations();
		Object httpRequestObj = httpClientContext.getAttribute(HttpCoreContext.HTTP_REQUEST);
		if (null != httpRequestObj) {
			if (httpRequestObj instanceof HttpRequestWrapper) {
				realUrl = ((HttpRequestWrapper) httpRequestObj).getOriginal().getRequestLine().getUri();
			} else if (httpRequestObj instanceof BasicHttpRequest) {
				realUrl = ((BasicHttpRequest) httpRequestObj).getRequestLine().getUri();
				if (StringUtils.contains(realUrl, ":")) {
					realUrl = StringUtils.left(realUrl, realUrl.indexOf(":"));
				}
			}
			log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% end http execute!! realUrl= " + realUrl);
		} else {
			log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% end http execute!! url unchanged! url= " + url);
		}

		Pair<String, Charset> response_charset = getResponseAndCharset(closeableHttpResponse.getEntity());
		if (null == response_charset) {
			return Triple.of(realUrl, null, null);
		}
		return Triple.of(realUrl, response_charset.getLeft(), response_charset.getRight());
	}

	public Triple<Header[], String, Charset> post(String url, Map<String, String> paramsMap, String charset) {
		try {
			return post(
					url, new UrlEncodedFormEntity(paramsMap.entrySet().stream().map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(
							Collectors.toList()), charset));
		} catch (UnsupportedEncodingException e) {
			log.error("new UrlEncodedFormEntity error!! paramsMap= " + paramsMap, e);
			return null;
		}
	}

	public Triple<Header[], String, Charset> post(String url, HttpEntity httpEntity) {
		return postOrPut(new HttpPost(clean(url)), httpEntity, null, null);
	}

	public Triple<Header[], String, Charset> put(String url, HttpEntity httpEntity) {
		return postOrPut(new HttpPut(clean(url)), httpEntity, null, null);
	}

	public String delete(String url) {
		HttpDelete httpDelete = null;
		try {
			httpDelete = new HttpDelete(clean(url));
		} catch (Exception e) {
			log.error("illegal url for httpDelete,  url= " + url, e);
			return null;
		}

		HttpClientContext httpClientContext = HttpClientContext.create();
		CloseableHttpResponse closeableHttpResponse = requestExecutor(httpDelete, httpClientContext);
		if (null == closeableHttpResponse) {
			return null;
		}

		Pair<String, Charset> response_charset = getResponseAndCharset(closeableHttpResponse.getEntity());
		if (null == response_charset) {
			return null;
		}

		return response_charset.getLeft();
	}

	public Triple<Header[], String, Charset> postOrPut(HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase,
													   HttpEntity httpEntity, Header[] headers, String referUrl) {
		httpEntityEnclosingRequestBase.setEntity(httpEntity);

		if (null != headers && 1 <= headers.length) {
			httpEntityEnclosingRequestBase.setHeaders(headers);
		} else {
			httpEntityEnclosingRequestBase.setHeaders(defaultHeaders);
		}
		if (StringUtils.isNotBlank(referUrl)) {
			httpEntityEnclosingRequestBase.addHeader("Referer", clean(referUrl));
		}

		HttpClientContext httpClientContext = HttpClientContext.create();
		CloseableHttpResponse closeableHttpResponse = requestExecutor(httpEntityEnclosingRequestBase, httpClientContext);
		if (null == closeableHttpResponse) {
			return Triple.of(null, null, null);
		}

		Header[] allHeaders = closeableHttpResponse.getAllHeaders();
		Pair<String, Charset> response_charset = getResponseAndCharset(closeableHttpResponse.getEntity());
		if (null == response_charset) {
			return Triple.of(allHeaders, null, null);
		}
		return Triple.of(allHeaders, response_charset.getLeft(), response_charset.getRight());
	}

	//带cookie
	public Triple<Header[], String, Charset> postOrPut(HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase,
													   HttpEntity httpEntity, Header[] headers, String referUrl, Pair<String, String> cookie) {
		httpEntityEnclosingRequestBase.setEntity(httpEntity);

		if (null != headers && 1 <= headers.length) {
			httpEntityEnclosingRequestBase.setHeaders(headers);
		} else {
			httpEntityEnclosingRequestBase.setHeaders(defaultHeaders);
		}
		if (StringUtils.isNotBlank(referUrl)) {
			httpEntityEnclosingRequestBase.addHeader("Referer", clean(referUrl));
		}
		if (cookie != null) {
			httpEntityEnclosingRequestBase.addHeader(cookie.getLeft(), clean(cookie.getRight()));
		}

		HttpClientContext httpClientContext = HttpClientContext.create();
		CloseableHttpResponse closeableHttpResponse = requestExecutor(httpEntityEnclosingRequestBase, httpClientContext);
		if (null == closeableHttpResponse) {
			return null;
		}


		System.out.println(closeableHttpResponse.getEntity());

		Header[] allHeaders = closeableHttpResponse.getAllHeaders();
		Pair<String, Charset> response_charset = getResponseAndCharset(closeableHttpResponse.getEntity());
		if (null == response_charset) {
			return null;
		}
		return Triple.of(allHeaders, response_charset.getLeft(), response_charset.getRight());
	}

	private CloseableHttpResponse requestExecutor(HttpRequestBase method, HttpClientContext context) {
		String actualIp = ip;
		if (StringUtils.isBlank(ip)) {
			Inet4Address Inet4Address = localPrivateInet4AddressList.get(
					Long.valueOf(counter.getAndIncrement() % localPrivateInet4AddressList.size()).intValue());
			closeableHttpClient = closeableHttpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.setLocalAddress(Inet4Address).build()).build();
			actualIp = Inet4Address.getHostAddress();
		}

		log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% start http execute!! url= " + method.getURI() + ",  ip= " + actualIp);

		try {
			CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(method, context);
			int httpStatusCode = closeableHttpResponse.getStatusLine().getStatusCode();
			if (HttpStatus.SC_OK != httpStatusCode) {
				log.info("abnormal requestExecutor response!! http status code= " + httpStatusCode);
				method.abort();
				poolingClientConnectionManager.closeIdleConnections(0, TimeUnit.SECONDS);
				if (HTTP_STATUS_EXCEED_LIMIT == httpStatusCode) {
					Thread.sleep(30000);
				}
			} else {
				method.completed();
			}

			return closeableHttpResponse;
		} catch (Exception e) {
			// 出现异常要release掉connection不然会导致很多CLOSE_WAIT状态的连接。
			method.abort();
			poolingClientConnectionManager.closeIdleConnections(0, TimeUnit.SECONDS);
			log.error("closeableHttpClient execute error!!  url= " + method.getURI() + ",   ip= " + ip, e);
			log.error("", e);
			return null;
		}
	}

	private Pair<String, Charset> getResponseAndCharset(HttpEntity httpEntity) {
		if (null == httpEntity) {
			return null;
		}

		byte[] bytes = null;
		Charset charset = null;
		ContentType contentType = null;
		try {
			bytes = EntityUtils.toByteArray(httpEntity);
			contentType = ContentType.get(httpEntity);
		} catch (Exception e) {
			log.error("EntityUtils.toByteArray error or ContentType.getOrDefault error!!", e);
			return null;
		}

		if (null != contentType) {
			charset = contentType.getCharset();
		}

		String content = "";
		if (null == charset) {
			content = new String(bytes);

			Matcher m = charsetPattern.matcher(content);
			if (m.find() && 3 == m.groupCount()) {
				charset = Charset.forName(m.group(3));
			} else {
				charset = Consts.UTF_8;
			}
		}

		EntityUtils.consumeQuietly(httpEntity);

		return Pair.of(new String(bytes, charset), charset);
	}

	@Override
	public SHttpClient clone() throws CloneNotSupportedException {
		return (SHttpClient) super.clone();
	}

	public static void main1(String[] args) {
		SHttpClient myHttpUtil = new SHttpClient();
		String t = myHttpUtil.get("http://weixin.sogou.com/weixin?type=2&query=文化人", new Header[]{new BasicHeader("Cookie", "SUV=;SNUID=")}, null);

		// myHttpUtil.get("")''
		//
		// myHttpUtil.getRealUrlAndContentAndCharset(url, referUrl);

		System.out.println(t);
	}

	public static void main(String[] args) {
//		String t = new MyHttpUtil().get(
//				"http://pb.sogou.com/pv.gif?uigs_productid=webapp&uigs_uuid=1442553883345173&uigs_version=v2.0&uigs_refer=&uigs_cookie=SUID%3DF3F61C71260C930A0000000055FB9FFB&uuid=f35810f2-0759-436f-acd2-ea11ae9ac2e0&query=qq&weixintype=2&exp_status=-1&noresult=0&type=weixin_search_pc&xy=1291,406&uigs_t=1442553883347000");
//		// new MyHttpUtil().get("http://dyapi.inews.qq.com/getSubWebMediaNews?chlid=2749&page=0&count=100&callback=createHtml",
//		// "http://dy.qq.com/list-user.htm?chlid=2749");
//
//		System.out.println(t);

		StringEntity stringEntity = new StringEntity(
//				"{\"activityId\":\"activityId\",  \"productNoList\":[\"1213\",\"23231\"],  \"activityName\":\"dfweqgga\",  \"activityGroupId\":\"wr3fasfasf\", \"activityGroupName\":\"dsfewgaaf\"}",

				"{\"a\":\"1\"}", ContentType.APPLICATION_JSON);
		new SHttpClient().postOrPut(new HttpPost(clean("http://172.16.0.26/oms/activity/mbank/product/check")), stringEntity,
				null, null);

	}
}
