package utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import burp.BurpExtender;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import com.bit4woo.utilbox.burp.HelperPlus;
import com.bit4woo.utilbox.utils.DomainUtils;
import com.bit4woo.utilbox.utils.IPAddressUtils;

public class NetworkUtils {

	private static final Logger log = LogManager.getLogger(NetworkUtils.class);

	// 默认最大跳转次数
	public static final int DEFAULT_MAX_REDIRECTS = 5;

	//Just do request（默认跟踪跳转，最多 DEFAULT_MAX_REDIRECTS 次）
	public static IHttpRequestResponse doRequest(URL url, String cookie) {
		return doRequest(url, cookie, true, DEFAULT_MAX_REDIRECTS);
	}

	public static IHttpRequestResponse doRequest(String url, String cookie) {
		return doRequest(url, cookie, true, DEFAULT_MAX_REDIRECTS);
	}

	/**
	 * 发起GET请求，并可选地跟踪HTTP跳转。
	 *
	 * <h3>为什么要有"上一次有效结果"</h3>
	 * <p>
	 * Burp 的 {@code makeHttpRequest} 在目标无法解析时会<b>抛 RuntimeException</b>
	 * （典型：{@code UnknownHostException: mb-api-gateway}），而不是返回 null。
	 * 原来的实现没有任何捕获，异常会一路冒到采集线程（{@code thread.Producer.run}）：
	 * 该条记录丢失，严重时整条采集线程直接死掉，队列里后面的域名全部不再处理。
	 * </p>
	 * <p>
	 * 更常见的触发场景是<b>跳转</b>：目标站点返回 302，Location 指向一个只在目标内网
	 * 存在的名字（例如 {@code http://mb-api-gateway/}）。这类名字在公网无法解析，
	 * 于是"一次无害的跳转"把整条记录变成了异常。
	 * </p>
	 * <p>
	 * 因此现在的语义是：<b>始终返回"最近一次真正拿到报文"的响应</b>。
	 * 后续某一跳失败（抛异常 / 空响应 / 主机不可请求 / 跳转成环），就停在这里，
	 * 把前面那一跳的有效结果返回给调用方 —— 那仍然是这个 URL 的真实信息
	 * （比如它确实 302 到了某个地方），比 null 或异常有用得多。
	 * </p>
	 *
	 * @param url            目标URL
	 * @param cookie         Cookie（可为空；若以"cookie:"开头会自动去掉前缀）
	 * @param followRedirect 是否跟踪跳转（301/302/303/307/308）
	 * @param maxRedirects   最大跳转次数；&lt;=0 时等同不跟踪跳转
	 * @return 最近一次有效响应；一次都没成功则返回 null（不会抛异常）
	 */
	public static IHttpRequestResponse doRequest(URL url, String cookie, boolean followRedirect, int maxRedirects) {
		if (url == null) {
			return null;
		}

		IExtensionHelpers helpers = BurpExtender.getCallbacks().getHelpers();
		HelperPlus getter = new HelperPlus(helpers);

		URL currentUrl = url;
		int redirectCount = 0;

		// 最近一次真正拿到报文的响应；一旦后续跳转失败就把它返回
		IHttpRequestResponse lastValidResponse = null;
		// 已请求过的 URL，用于识别 A→B→A 这类跳转环，避免同一主机被反复请求
		Set<String> visited = new HashSet<String>();
		visited.add(currentUrl.toString());

		while (true) {
			byte[] byteRequest = helpers.buildHttpRequest(currentUrl);//GET

			if (StringUtils.isNotEmpty(cookie)) {
				String c = cookie;
				if (c.toLowerCase().startsWith("cookie:")) {
					c = c.substring("cookie:".length());
				}
				byteRequest = getter.addOrUpdateHeader(true, byteRequest, "Cookie", c);
			}

			int port = currentUrl.getPort();
			if (port == -1) {
				port = currentUrl.getDefaultPort();
			}
			IHttpService service = helpers.buildHttpService(currentUrl.getHost(), port, currentUrl.getProtocol());

			IHttpRequestResponse response;
			try {
				response = BurpExtender.getCallbacks().makeHttpRequest(service, byteRequest);
			} catch (RuntimeException e) {
				// 解析失败/连接失败/被 Burp 内部拒绝等都在这里：不再向上抛，回退到上一次有效结果。
				// 只打一行日志（不带堆栈）：一次采集可能命中成百上千个这种跳转，堆栈会把输出刷爆。
				log.warn("doRequest failed at " + currentUrl + " cause=" + rootCauseMessage(e)
						+ (lastValidResponse != null ? " -> fallback to previous valid response" : " -> no valid response"));
				break;
			}

			// 没有拿到报文（null / 空 body）：算作无效结果，回退到上一次
			byte[] responseBytes = response == null ? null : response.getResponse();
			if (responseBytes == null || responseBytes.length == 0) {
				if (lastValidResponse != null) {
					log.debug("doRequest got empty response at " + currentUrl + " -> fallback to previous valid response");
				}
				break;
			}

			lastValidResponse = response;

			if (!followRedirect) {
				break;
			}

			short statusCode = getter.getStatusCode(response);
			if (!isRedirect(statusCode)) {
				break;
			}

			// 已达跳转上限，返回当前这次（仍是3xx跳转）的响应
			if (redirectCount >= maxRedirects) {
				break;
			}

			String location = HelperPlus.getHeaderValueOf(false, responseBytes, "Location");
			if (StringUtils.isEmpty(location)) {
				// 3xx但没有Location头，无法继续跳转
				break;
			}

			URL nextUrl;
			try {
				// 支持绝对地址、相对地址、协议相对(//host)等Location格式
				nextUrl = new URL(currentUrl, location.trim());
			} catch (MalformedURLException e) {
				// Location非法，无法继续跳转
				break;
			}

			// 跳转目标不可请求就不要发了：这类目标必然抛 UnknownHostException，
			// 只是白白浪费一次请求并污染日志（典型值：mb-api-gateway 这类内网单标签主机名）
			if (!isRequestableHost(nextUrl.getHost())) {
				log.warn("doRequest stop following redirect: not a requestable host [" + nextUrl.getHost()
						+ "] from " + currentUrl);
				break;
			}

			// 跳转成环（A→B→A）时不再重复请求同一地址
			if (!visited.add(nextUrl.toString())) {
				log.warn("doRequest stop following redirect: loop detected at " + nextUrl);
				break;
			}

			currentUrl = nextUrl;
			redirectCount++;
		}

		return lastValidResponse;
	}

	/**
	 * 字符串形式的重载，额外处理URL解析异常。
	 */
	public static IHttpRequestResponse doRequest(String url, String cookie, boolean followRedirect, int maxRedirects) {
		try {
			URL Url = new URL(url);
			return doRequest(Url, cookie, followRedirect, maxRedirects);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 判断一个主机名是否值得发起请求。
	 * <p>
	 * 只用于<b>跳转目标</b>的预检，不用来过滤用户给定的初始目标：
	 * 内网测试场景下初始目标本来就是 {@code http://gitlab} 这种单标签主机名，必须允许。
	 * 但"从公网站点跳到一个单标签主机名"意味着跳进了对方内网，我们的网络环境根本解析不了，
	 * 请求必然失败，所以直接停止跟踪、保留上一跳的有效响应。
	 * </p>
	 *
	 * @param host URL.getHost() 的返回值
	 * @return true 表示可以发起请求
	 */
	public static boolean isRequestableHost(String host) {
		if (StringUtils.isEmpty(host)) {
			return false;
		}
		String h = host.trim();
		// IPv6 字面量：URL.getHost() 形如 [::1]
		if (h.startsWith("[") && h.endsWith("]")) {
			return true;
		}
		if (IPAddressUtils.isValidIPv4NoPort(h)) {
			return true;
		}
		// 单标签主机名（mb-api-gateway）：公网不可解析，几乎必然是内网名或拼写错误
		if (h.indexOf('.') < 0) {
			return false;
		}
		// 下划线不是合法域名字符，DNS 一定解析失败（常见于内部服务名）
		if (h.indexOf('_') >= 0) {
			return false;
		}
		// 去掉结尾的点（FQDN 写法 example.com.）再交给工具库校验
		if (h.endsWith(".")) {
			h = h.substring(0, h.length() - 1);
		}
		try {
			return DomainUtils.isValidDomainNoPort(h);
		} catch (Exception e) {
			// 校验工具本身异常时按"可请求"处理：宁可多试一次，也不要误杀正常目标
			return true;
		}
	}

	/**
	 * 判断状态码是否为需要跟踪的跳转（301/302/303/307/308）。
	 * 300(多选)、304(未修改)等不在此列。
	 */
	private static boolean isRedirect(int statusCode) {
		return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
	}

	/**
	 * 取异常链最内层的信息，用于日志。
	 * Burp 抛出来的通常是 {@code RuntimeException: java.lang.RuntimeException: java.net.UnknownHostException: xxx}
	 * 这种多层包装，全部打印没有价值，只保留最内层的类型和消息。
	 */
	private static String rootCauseMessage(Throwable e) {
		Throwable cause = e;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		String msg = cause.getMessage();
		return cause.getClass().getSimpleName() + (StringUtils.isEmpty(msg) ? "" : ": " + msg);
	}

	public static void main(String[] args) {
	}
}
