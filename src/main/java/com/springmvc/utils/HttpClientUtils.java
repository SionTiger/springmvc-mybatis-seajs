package com.springmvc.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.Cookie;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.google.api.client.http.HttpHeaders;

/**
 * HttpClient Util
 * 
 */
public class HttpClientUtils {

	private static Logger logger = Logger.getLogger(HttpClientUtils.class);

	private HttpClientUtils() {

	}

	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final Integer SUCCESS_CODE = 200;
	public static final String DEFAULT_GET = "GET";

	/**
	 * 发送HTTP_PUT请求
	 * 
	 * @param reqURL
	 * @param jsonBody
	 * @param decodeCharset
	 * @return
	 */
	public static Map<String, String> sendJsonPutRequest(String reqURL, String jsonBody, String decodeCharset,
			String cookie) {
		long responseLength = 0; // 响应长度
		String responseContent = null; // 响应内容
		HttpClient httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例
		HttpPut httpPut = new HttpPut(reqURL);
		httpPut.addHeader("Accept", "application/json");
		httpPut.setHeader(HTTP.CONTENT_TYPE, "application/json");
		if (cookie != null) {
			httpPut.setHeader("Cookie", cookie);
		}
		Integer statusCode = 200;
		Map<String, String> map = new HashMap<String, String>();
		try {
			// 添加json格式的请求数据
			if (StringUtils.isNotBlank(jsonBody)) {
				httpPut.setEntity(new StringEntity(jsonBody));
			}
			HttpResponse response = httpClient.execute(httpPut); // 执行PUT请求
			HttpEntity entity = response.getEntity(); // 获取响应实体
			statusCode = response.getStatusLine().getStatusCode();
			if (null != entity) {
				responseLength = entity.getContentLength();
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity); // Consume response content
			}

			Header[] headers = response.getAllHeaders();
			if (headers != null) {
				for (Header header : headers) {
					if (header.getName().toLowerCase().contains("cookie")) {
						cookie += header.getValue();
						// break;
					}
				}
			}
			statusCode = response.getStatusLine().getStatusCode();

			logger.debug("请求地址: " + httpPut.getURI());
			logger.debug("响应状态: " + response.getStatusLine());
			logger.debug("响应长度: " + responseLength);
			logger.debug("响应内容: " + responseContent);
		} catch (ClientProtocolException e) {
			logger.error("该异常通常是协议错误导致,比如构造HttpGet对象时传入的协议不对(将'http'写成'htp')或者服务器端返回的内容不符合HTTP协议要求等,堆栈信息如下", e);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error("该异常通常是网络原因引起的,如HTTP服务器未启动等,堆栈信息如下", e);
		} finally {
			httpClient.getConnectionManager().shutdown(); // 关闭连接,释放资源
		}

		map.put("resMsg", responseContent);
		map.put("statusCode", String.valueOf(statusCode));
		map.put("Cookie", cookie);
		return map;
	}

	/**
	 * 发送HTTP_DELETE请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @param requestURL
	 *            请求地址(含参数)
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	public static Map<String, String> sendJsonDeleteRequest(String reqURL, String jsonBody, String decodeCharset,
			String cookie) {
		long responseLength = 0; // 响应长度
		String responseContent = null; // 响应内容
		HttpClient httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例
		HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(reqURL);
		httpDelete.addHeader("Accept", HttpClientUtils.CONTENT_TYPE_JSON);

		Map<String, String> map = new HashMap<String, String>();
		Integer statusCode = 200;
		if (StringUtils.isNotBlank(cookie)) {
			httpDelete.setHeader("Cookie", cookie);
		}
		try {
			if (StringUtils.isNotBlank(jsonBody)) {
				httpDelete.setHeader(HTTP.CONTENT_TYPE, HttpClientUtils.CONTENT_TYPE_JSON);
				httpDelete.setEntity(new StringEntity(jsonBody));
			}
			HttpResponse response = httpClient.execute(httpDelete); // 执行GET请求
			HttpEntity entity = response.getEntity(); // 获取响应实体

			Header[] headers = response.getAllHeaders();
			if (headers != null) {
				for (Header header : headers) {
					if (header.getName().toLowerCase().contains("cookie")) {
						cookie += header.getValue();
						// break;
					}
				}
			}
			statusCode = response.getStatusLine().getStatusCode();
			if (null != entity) {
				responseLength = entity.getContentLength();
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity); // Consume response content
			}
			logger.debug("请求地址: " + httpDelete.getURI());
			logger.debug("响应状态: " + response.getStatusLine());
			logger.debug("响应长度: " + responseLength);
			logger.debug("响应内容: " + responseContent);
		} catch (ClientProtocolException e) {
			logger.error("该异常通常是协议错误导致,比如构造HttpGet对象时传入的协议不对(将'http'写成'htp')或者服务器端返回的内容不符合HTTP协议要求等,堆栈信息如下", e);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error("该异常通常是网络原因引起的,如HTTP服务器未启动等,堆栈信息如下", e);
		} finally {
			httpClient.getConnectionManager().shutdown(); // 关闭连接,释放资源
		}
		map.put("statusCode", String.valueOf(statusCode));
		map.put("resMsg", responseContent);
		map.put("Cookie", cookie);
		return map;
	}

	/**
	 * 发送HTTP_GET请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @param requestURL
	 *            请求地址(含参数)
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	/**
	 * @param reqURL
	 * @param decodeCharset
	 * @return
	 */
	public static String sendGetRequest(String reqURL, String decodeCharset) {
		long responseLength = 0; // 响应长度
		String responseContent = null; // 响应内容
		HttpClient httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例
		HttpGet httpGet = new HttpGet(reqURL); // 创建org.apache.http.client.methods.HttpGet
		// httpGet.setHeader("accessAppKey","Ml9kYXRhX29uZV8xMjM=");
		// Ml9kYXRhX29uZV8xMjM=
		// Ml9kYXRhX29uZV8xM=
		try {
			HttpResponse response = httpClient.execute(httpGet); // 执行GET请求
			HttpEntity entity = response.getEntity(); // 获取响应实体
			if (null != entity) {
				responseLength = entity.getContentLength();
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity); // Consume response content
			}
			logger.debug("请求地址: " + httpGet.getURI());
			logger.debug("响应状态: " + response.getStatusLine());
			logger.debug("响应长度: " + responseLength);
			logger.debug("响应内容: " + responseContent);
		} catch (ClientProtocolException e) {
			logger.error("该异常通常是协议错误导致,比如构造HttpGet对象时传入的协议不对(将'http'写成'htp')或者服务器端返回的内容不符合HTTP协议要求等,堆栈信息如下", e);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error("该异常通常是网络原因引起的,如HTTP服务器未启动等,堆栈信息如下", e);
		} finally {
			httpClient.getConnectionManager().shutdown(); // 关闭连接,释放资源
		}
		return responseContent;
	}

	/**
	 * 发送HTTP_GET请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @param requestURL
	 *            请求地址(含参数)
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	public static Map<String, String> sendJsonGetRequest(String reqURL, String decodeCharset) {
		long responseLength = 0; // 响应长度
		String responseContent = null; // 响应内容
		HttpClient httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例
		HttpGet httpGet = new HttpGet(reqURL); // 创建org.apache.http.client.methods.HttpGet
		httpGet.addHeader("Accept", "application/json");

		Integer statusCode = 200;
		Map<String, String> map = new HashMap<String, String>();
		try {
			HttpResponse response = httpClient.execute(httpGet); // 执行GET请求
			HttpEntity entity = response.getEntity(); // 获取响应实体
			statusCode = response.getStatusLine().getStatusCode();
			if (null != entity) {
				responseLength = entity.getContentLength();
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity); // Consume response content
			}
			logger.debug("请求地址: " + httpGet.getURI());
			logger.debug("响应状态: " + response.getStatusLine());
			logger.debug("响应长度: " + responseLength);
			logger.debug("响应内容: " + responseContent);
		} catch (ClientProtocolException e) {
			logger.error("该异常通常是协议错误导致,比如构造HttpGet对象时传入的协议不对(将'http'写成'htp')或者服务器端返回的内容不符合HTTP协议要求等,堆栈信息如下", e);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error("该异常通常是网络原因引起的,如HTTP服务器未启动等,堆栈信息如下", e);
		} finally {
			httpClient.getConnectionManager().shutdown(); // 关闭连接,释放资源
		}

		map.put("resMsg", responseContent);
		map.put("statusCode", String.valueOf(statusCode));
		return map;
	}

	/**
	 * Send the Get request with cookie
	 * 
	 * @param reqURL
	 * @param decodeCharset
	 * @param contentType
	 * @param cookie
	 * @return
	 */
	public static Map<String, String> sendJsonGetRequest(String reqURL, String decodeCharset, String contentType) {
		long responseLength = 0; // 响应长度
		String responseContent = null; // 响应内容
		HttpClient httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例
		HttpGet httpGet = new HttpGet(reqURL); // 创建org.apache.http.client.methods.HttpGet
		httpGet.addHeader("Accept", "application/json");
		if (StringUtils.isBlank(contentType)) {
			// TODO
		} else {
			httpGet.setHeader(HTTP.DEFAULT_CONTENT_CHARSET, "UTF-8");// 设置默认的请求内容编码是UTF-8编码
		}

		Integer statusCode = 200;
		Map<String, String> map = new HashMap<String, String>();
		try {
			HttpResponse response = httpClient.execute(httpGet); // 执行GET请求
			HttpEntity entity = response.getEntity(); // 获取响应实体
			statusCode = response.getStatusLine().getStatusCode();
			if (null != entity) {
				responseLength = entity.getContentLength();
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity); // Consume response content
			} else {
				logger.info(responseLength);
			}
		} catch (ClientProtocolException e) {
			logger.error("该异常通常是协议错误导致,比如构造HttpGet对象时传入的协议不对(将'http'写成'htp')或者服务器端返回的内容不符合HTTP协议要求等,堆栈信息如下", e);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error("该异常通常是网络原因引起的,如HTTP服务器未启动等,堆栈信息如下", e);
		} finally {
			httpClient.getConnectionManager().shutdown(); // 关闭连接,释放资源
		}

		map.put("resMsg", responseContent);
		map.put("statusCode", String.valueOf(statusCode));
		return map;
	}

	/**
	 * 发送HTTP_POST请求
	 * 
	 * @see 该方法为
	 *      <code>sendPostRequest(String,String,boolean,String,String)</code>
	 *      的简化方法
	 * @see 该方法在对请求数据的编码和响应数据的解码时,所采用的字符集均为UTF-8
	 * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>中的[中文][|][
	 *      ]等特殊字符进行<code>URLEncoder.encode(string,"UTF-8")</code>
	 * @param isEncoder
	 *            用于指明请求数据是否需要UTF-8编码,true为需要
	 */
	public static String sendPostRequest(String reqURL, String sendData, boolean isEncoder) {
		return sendPostRequest(reqURL, sendData, isEncoder, null, null);
	}

	/**
	 * 发送HTTP_POST请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>中的[中文][|][
	 *      ]等特殊字符进行<code>URLEncoder.encode(string,encodeCharset)</code>
	 * @param reqURL
	 *            请求地址
	 * @param sendData
	 *            请求参数,若有多个参数则应拼接成param11=value11?m22=value22?m33=value33的形式后,
	 *            传入该参数中
	 * @param isEncoder
	 *            请求数据是否需要encodeCharset编码,true为需要
	 * @param encodeCharset
	 *            编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	public static String sendPostRequest(String reqURL, String sendData, boolean isEncoder, String encodeCharset,
			String decodeCharset) {
		String responseContent = null;
		HttpClient httpClient = null;
		httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(reqURL);
		httpPost.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");
		try {
			if (isEncoder) {
				List<NameValuePair> formParams = new ArrayList<NameValuePair>();
				for (String str : sendData.split("&")) {
					formParams.add(new BasicNameValuePair(str.substring(0, str.indexOf("=")),
							str.substring(str.indexOf("=") + 1)));
				}
				httpPost.setEntity(new StringEntity(
						URLEncodedUtils.format(formParams, encodeCharset == null ? "UTF-8" : encodeCharset)));
			} else {
				httpPost.setEntity(new StringEntity(sendData));
			}

			HttpResponse response = httpClient.execute(httpPost);
			// Header[] headers = response.getAllHeaders();
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity);
			}
		} catch (Exception e) {
			logger.error("与[" + reqURL + "]通信过程中发生异常,堆栈信息如下", e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return responseContent;
	}

	/**
	 * 发送HTTP_POST请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>中的[中文][|][
	 *      ]等特殊字符进行<code>URLEncoder.encode(string,encodeCharset)</code>
	 * @param reqURL
	 *            请求地址
	 * @param sendData
	 *            请求参数,若有多个参数则应拼接成param11=value11?m22=value22?m33=value33的形式后,
	 *            传入该参数中
	 * @param isEncoder
	 *            请求数据是否需要encodeCharset编码,true为需要
	 * @param encodeCharset
	 *            编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	public static Map<String, String> sendPostRequestContainCookies(String reqURL, String sendData, boolean isEncoder,
			String encodeCharset, String decodeCharset) {
		String responseContent = null;
		HttpClient httpClient = null;
		httpClient = new DefaultHttpClient();
		Map<String, String> map = new HashMap<String, String>();
		HttpPost httpPost = new HttpPost(reqURL);
		httpPost.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");
		String cookie = StringUtils.EMPTY;
		int statusCode = 200;
		try {
			if (isEncoder) {
				List<NameValuePair> formParams = new ArrayList<NameValuePair>();
				for (String str : sendData.split("&")) {
					formParams.add(new BasicNameValuePair(str.substring(0, str.indexOf("=")),
							str.substring(str.indexOf("=") + 1)));
				}
				httpPost.setEntity(new StringEntity(
						URLEncodedUtils.format(formParams, encodeCharset == null ? "UTF-8" : encodeCharset)));
			} else {
				httpPost.setEntity(new StringEntity(sendData));
			}

			HttpResponse response = httpClient.execute(httpPost);
			statusCode = response.getStatusLine().getStatusCode();
			// Set-Cookie:newark=C6912BBDD6A7A9C9C64A9596C87EBE00; Path=/;
			// HttpOnly
			Header[] headers = response.getAllHeaders();
			for (Header header : headers) {
				if (header.getName().toLowerCase().contains("cookie")) {
					cookie += header.getValue();
					// break;
				}
			}
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity);
			}

		} catch (Exception e) {
			logger.error("与[" + reqURL + "]通信过程中发生异常,堆栈信息如下", e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		map.put("resMsg", responseContent);
		map.put("Cookie", cookie);
		map.put("statusCode", String.valueOf(statusCode));
		return map;
	}

	/**
	 * 发送HTTP_POST请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @see 当<code>isEncoder=true</code>时,其会自动对<code>sendData</code>中的[中文][|][
	 *      ]等特殊字符进行<code>URLEncoder.encode(string,encodeCharset)</code>
	 * @param reqURL
	 *            请求地址
	 * @param sendData
	 *            请求参数,若有多个参数则应拼接成param11=value11?m22=value22?m33=value33的形式后,
	 *            传入该参数中
	 * @param isEncoder
	 *            请求数据是否需要encodeCharset编码,true为需要
	 * @param encodeCharset
	 *            编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	public static Map<String, String> sendJsonPostRequest(String reqURL, String sendData, boolean isEncoder,
			String encodeCharset, String decodeCharset, String contentType, String cookie) {
		String responseContent = null;
		HttpClient httpClient = null;
		httpClient = new DefaultHttpClient();
		Map<String, String> map = new HashMap<String, String>();
		Integer statusCode = 200;
		HttpPost httpPost = new HttpPost(reqURL);
		httpPost.setHeader(HTTP.CONTENT_TYPE, "application/json");// 设置内容类型是json
		if (StringUtils.isBlank(contentType)) {

		} else {
			httpPost.setHeader(HTTP.DEFAULT_CONTENT_CHARSET, "UTF-8");// 设置默认的请求内容编码是UTF-8编码
		}

		if (StringUtils.isNotBlank(cookie)) {
			httpPost.addHeader("Cookie", cookie);
		}

		httpPost.addHeader("Accept", "application/json");// 设置接受类型是json类型
		try {
			if (isEncoder) {
				List<NameValuePair> formParams = new ArrayList<NameValuePair>();
				for (String str : sendData.split("&")) {
					formParams.add(new BasicNameValuePair(str.substring(0, str.indexOf("=")),
							str.substring(str.indexOf("=") + 1)));
				}
				httpPost.setEntity(new StringEntity(
						URLEncodedUtils.format(formParams, encodeCharset == null ? "UTF-8" : encodeCharset)));
			} else {
				httpPost.setEntity(new StringEntity(sendData));
			}
			HttpResponse response = httpClient.execute(httpPost);
			statusCode = response.getStatusLine().getStatusCode();
			Header[] headers = response.getAllHeaders();
			for (Header header : headers) {
				if (header.getName().toLowerCase().contains("cookie")) {
					cookie += header.getValue();
					// break;
				}
			}
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity);
			}
		} catch (Exception e) {
			logger.error("与[" + reqURL + "]通信过程中发生异常,堆栈信息如下", e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}

		map.put("statusCode", String.valueOf(statusCode));
		map.put("Cookie", cookie);
		map.put("resMsg", responseContent);
		return map;
	}

	/**
	 * 发送HTTP_POST请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
	 *      <code>URLEncoder.encode(string,encodeCharset)</code>
	 * @param reqURL
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param encodeCharset
	 *            编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	public static String sendPostRequest(String reqURL, Map<String, String> params, String encodeCharset,
			String decodeCharset) {
		String responseContent = null;
		HttpClient httpClient = new DefaultHttpClient();

		HttpPost httpPost = new HttpPost(reqURL);
		List<NameValuePair> formParams = new ArrayList<NameValuePair>(); // 创建参数队列
		for (Map.Entry<String, String> entry : params.entrySet()) {
			formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(formParams, encodeCharset == null ? "UTF-8" : encodeCharset));

			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity);
			}
		} catch (Exception e) {
			logger.error("与[" + reqURL + "]通信过程中发生异常,堆栈信息如下", e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return responseContent;
	}

	/**
	 * 发送HTTPS_POST请求
	 * 
	 * @see 该方法为
	 *      <code>sendPostSSLRequest(String,Map<String,String>,String,String)</code>
	 *      方法的简化方法
	 * @see 该方法在对请求数据的编码和响应数据的解码时,所采用的字符集均为UTF-8
	 * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
	 *      <code>URLEncoder.encode(string,"UTF-8")</code>
	 */
	public static String sendPostSSLRequest(String reqURL, Map<String, String> params) {
		return sendPostSSLRequest(reqURL, params, null, null);
	}

	/**
	 * 发送HTTPS_POST请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
	 *      <code>URLEncoder.encode(string,encodeCharset)</code>
	 * @param reqURL
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param encodeCharset
	 *            编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	public static String sendPostSSLRequest(String reqURL, Map<String, String> params, String encodeCharset,
			String decodeCharset) {
		String responseContent = "";
		HttpClient httpClient = new DefaultHttpClient();
		X509TrustManager xtm = new WechatX509TrustManager();
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] { xtm }, null);
			SSLSocketFactory socketFactory = new SSLSocketFactory(ctx);
			httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, socketFactory));

			HttpPost httpPost = new HttpPost(reqURL);
			List<NameValuePair> formParams = new ArrayList<NameValuePair>();
			for (Map.Entry<String, String> entry : params.entrySet()) {
				formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
			httpPost.setEntity(new UrlEncodedFormEntity(formParams, encodeCharset == null ? "UTF-8" : encodeCharset));

			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity);
			}
		} catch (Exception e) {
			logger.error("与[" + reqURL + "]通信过程中发生异常,堆栈信息为", e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return responseContent;
	}

	/**
	 * 发送HTTPS_POST,类型为JSON的请求
	 * 
	 * @see 该方法为<code>sendPostSSLRequest(String,String,String,String)</code>
	 *      方法的简化方法
	 * @see 该方法在对请求数据的编码和响应数据的解码时,所采用的字符集均为UTF-8
	 * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
	 *      <code>URLEncoder.encode(string,"UTF-8")</code>
	 */
	public static String sendPostSSLRequest(String reqURL, String params) {
		return sendPostSSLRequest(reqURL, params, null, null);
	}

	/**
	 * 发送HTTPS_POST，类型为JSON的请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @see 该方法会自动对<code>params</code>中的[中文][|][ ]等特殊字符进行
	 *      <code>URLEncoder.encode(string,encodeCharset)</code>
	 * @param reqURL
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param encodeCharset
	 *            编码字符集,编码请求数据时用之,其为null时默认采用UTF-8解码
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	public static String sendPostSSLRequest(String reqURL, String params, String encodeCharset, String decodeCharset) {
		String responseContent = "";
		HttpClient httpClient = new DefaultHttpClient();
		X509TrustManager xtm = new WechatX509TrustManager();
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] { xtm }, null);
			SSLSocketFactory socketFactory = new SSLSocketFactory(ctx);
			httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, socketFactory));

			HttpPost httpPost = new HttpPost(reqURL);
			httpPost.setEntity(new StringEntity(params, encodeCharset == null ? "UTF-8" : encodeCharset));
			httpPost.addHeader("content-type", "application/json");
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity);
			}
		} catch (Exception e) {
			logger.error("与[" + reqURL + "]通信过程中发生异常,堆栈信息为", e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return responseContent;
	}

	/**
	 * 发送HTTPS_GET请求
	 * 
	 * @see 该方法会自动关闭连接,释放资源
	 * @param requestURL
	 *            请求地址(含参数)
	 * @param decodeCharset
	 *            解码字符集,解析响应数据时用之,其为null时默认采用UTF-8解码
	 * @return 远程主机响应正文
	 */
	public static String sendGetSSLRequest(String reqURL, String decodeCharset) {
		long responseLength = 0; // 响应长度
		String responseContent = null; // 响应内容
		HttpClient httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例
		X509TrustManager xtm = new WechatX509TrustManager();
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] { xtm }, null);
			SSLSocketFactory socketFactory = new SSLSocketFactory(ctx);
			httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, socketFactory));

			HttpGet httpGet = new HttpGet(reqURL); // 创建org.apache.http.client.methods.HttpGet
			HttpResponse response = httpClient.execute(httpGet); // 执行GET请求
			HttpEntity entity = response.getEntity(); // 获取响应实体
			if (null != entity) {
				responseLength = entity.getContentLength();
				responseContent = EntityUtils.toString(entity, decodeCharset == null ? "UTF-8" : decodeCharset);
				EntityUtils.consume(entity); // Consume response content
			}
			logger.debug("请求地址: " + httpGet.getURI());
			logger.debug("响应状态: " + response.getStatusLine());
			logger.debug("响应长度: " + responseLength);
			logger.debug("响应内容: " + responseContent);
		} catch (Exception e) {
			logger.error("与[" + reqURL + "]通信过程中发生异常,堆栈信息为", e);
		} finally {
			httpClient.getConnectionManager().shutdown(); // 关闭连接,释放资源
		}
		return responseContent;
	}

	/**
	 * 发送HTTP_POST请求
	 * 
	 * @see 若发送的<code>params</code>中含有中文,记得按照双方约定的字符集将中文
	 *      <code>URLEncoder.encode(string,encodeCharset)</code>
	 * @see 本方法默认的连接超时时间为30秒,默认的读取超时时间为30秒
	 * @param reqURL
	 *            请求地址
	 * @param params
	 *            发送到远程主机的正文数据,其数据类型为<code>java.util.Map<String, String></code>
	 * @return 远程主机响应正文`HTTP状态码,如<code>"SUCCESS`200"</code><br>
	 *         若通信过程中发生异常则返回"Failed`HTTP状态码",如<code>"Failed`500"</code>
	 */
	public static String sendPostRequestByJava(String reqURL, Map<String, String> params) {
		StringBuilder sendData = new StringBuilder();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			sendData.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
		}
		if (sendData.length() > 0) {
			sendData.setLength(sendData.length() - 1); // 删除最后一个&符号
		}
		return sendPostRequestByJava(reqURL, sendData.toString());
	}

	/**
	 * 发送HTTP_POST请求
	 * 
	 * @see 若发送的<code>sendData</code>中含有中文,记得按照双方约定的字符集将中文
	 *      <code>URLEncoder.encode(string,encodeCharset)</code>
	 * @see 本方法默认的连接超时时间为30秒,默认的读取超时时间为30秒
	 * @param reqURL
	 *            请求地址
	 * @param sendData
	 *            发送到远程主机的正文数据
	 * @return 远程主机响应正文`HTTP状态码,如<code>"SUCCESS`200"</code><br>
	 *         若通信过程中发生异常则返回"Failed`HTTP状态码",如<code>"Failed`500"</code>
	 */
	public static String sendPostRequestByJava(String reqURL, String sendData) {
		HttpURLConnection httpURLConnection = null;
		OutputStream out = null; // 写
		InputStream in = null; // 读
		int httpStatusCode = 0; // 远程主机响应的HTTP状态码
		try {
			URL sendUrl = new URL(reqURL);
			httpURLConnection = (HttpURLConnection) sendUrl.openConnection();
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setDoOutput(true); // 指示应用程序要将数据写入URL连接,其值默认为false
			httpURLConnection.setUseCaches(false);
			httpURLConnection.setConnectTimeout(30000); // 30秒连接超时
			httpURLConnection.setReadTimeout(30000); // 30秒读取超时

			out = httpURLConnection.getOutputStream();
			out.write(sendData.toString().getBytes());

			// 清空缓冲区,发送数据
			out.flush();

			// 获取HTTP状态码
			httpStatusCode = httpURLConnection.getResponseCode();

			// 该方法只能获取到[HTTP/1.0 200 OK]中的[OK]
			// 若对方响应的正文放在了返回报文的最后一行,则该方法获取不到正文,而只能获取到[OK],稍显遗憾
			// respData = httpURLConnection.getResponseMessage();

			// //处理返回结果
			// BufferedReader br = new BufferedReader(new
			// InputStreamReader(httpURLConnection.getInputStream()));
			// String row = null;
			// String respData = "";
			// if((row=br.readLine()) != null){
			// //readLine()方法在读到换行[\n]或回车[\r]时,即认为该行已终止
			// respData = row; //HTTP协议POST方式的最后一行数据为正文数据
			// }
			// br.close();

			in = httpURLConnection.getInputStream();
			byte[] byteDatas = new byte[in.available()];
			in.read(byteDatas);
			return new String(byteDatas) + "`" + httpStatusCode;
		} catch (Exception e) {
			logger.error(e.getMessage());
			return "Failed`" + httpStatusCode;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
					logger.error("关闭输出流时发生异常,堆栈信息如下", e);
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					logger.error("关闭输入流时发生异常,堆栈信息如下", e);
				}
			}
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
				httpURLConnection = null;
			}
		}
	}

	public static boolean RequestSuccess(String statusCode) {
		if (StringUtils.equals(statusCode, String.valueOf(HttpClientUtils.SUCCESS_CODE))) {
			return true;
		} else {
			return false;
		}
	}

	public static void main(String[] args) {

		final String kerberosUrl = "http://10.100.16.114:10080/user/sys?request=add";
		final String requestboydString = "{\"username\":\"systest1\",\"group\":\"hive\"}";
		Map<String, String> map = HttpClientUtils.sendJsonPostRequest(kerberosUrl, requestboydString, false,
				HttpClientUtils.DEFAULT_CHARSET, HttpClientUtils.DEFAULT_CHARSET, null, StringUtils.EMPTY);
		String responseBodyString = map.get("resMsg");
		System.out.println(responseBodyString);

	}
}

/**
 * HttpDeleteWithBody
 * @description It's used to send the request who's type is delete with body
 * @author Administrator
 *
 */
@NotThreadSafe
class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
	public static final String METHOD_NAME = "DELETE";

	public String getMethod() {
		return METHOD_NAME;
	}

	public HttpDeleteWithBody(final String uri) {
		super();
		setURI(URI.create(uri));
	}

	public HttpDeleteWithBody(final URI uri) {
		super();
		setURI(uri);
	}

	public HttpDeleteWithBody() {
		super();
	}
}
