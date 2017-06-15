/**
 * 
 */
package org.etagate.request;

import org.etagate.helper.S;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年4月12日
 */
public class AppResponse {

	public static final Logger log = LoggerFactory.getLogger(AppResponse.class);

	public static String HTTP_SCHEMAL_HOST_REG = "http[s]+://[^/]*/";

	public static void handle(HttpServerRequest clientRequest, HttpServerResponse clientResponse,
			HttpResponse<Buffer> appResponse) {

		int statusCode = appResponse.statusCode();
		clientResponse.setStatusCode(statusCode);
		clientResponse.setStatusMessage(appResponse.statusMessage());

		MultiMap appHeaders = appResponse.headers();
		appHeaders.forEach(entry -> {
			String k = entry.getKey();
			String v = entry.getValue();
			// System.out.println("app Response:["+k+"]="+v);
			if ("Location".equalsIgnoreCase(k) && S.isNotBlank(v)) {
				String schemal = clientRequest.scheme();
				String host = clientRequest.host();
				log.debug(v + "===>" + v.replaceAll(HTTP_SCHEMAL_HOST_REG, schemal + "://" + host + "/"));
				clientResponse.putHeader("Location", v.replaceAll(HTTP_SCHEMAL_HOST_REG, schemal + "://" + host + "/"));
			} else
				clientResponse.putHeader(k, v);
		});

		clientResponse.putHeader("Content-Length",""+appResponse.body().length());
		
		String s =appResponse.bodyAsString();
		System.out.println(s);
		clientResponse.write(appResponse.body());
		clientResponse.end();
	}

}
