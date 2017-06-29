/**
 * 
 */
package org.etagate.request;

import org.etagate.app.App;
import org.etagate.app.node.Node;
import org.etagate.helper.S;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年4月12日
 */
public class RequestHandler implements Handler<RoutingContext> {

	public static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	
	private final App appObj;
	private final WebClient client;

	public RequestHandler(WebClient client, App appinfo) {
		this.appObj = appinfo;
		this.client = client;
	}

	@Override
	public void handle(RoutingContext rc) {

		HttpServerRequest clientRequest = rc.request();
		
		String uri = clientRequest.uri();
		
		uri = appObj.offsetUrl(uri);

		Node node = appObj.getNode(clientRequest);
		
		node.dispatchRequest(this.client, rc, clientRequest, uri)
		    .setHandler(ar->{
		    	HttpServerResponse clientResponse = rc.response();
		    	if (ar.succeeded()) {					
					this.handle(clientRequest, clientResponse, ar.result());					
				} else {
					clientResponse.setStatusCode(500);
					ar.cause().printStackTrace();					
				}		    	
		    	clientResponse.end();				
		    });

	}
	
	
	public static String HTTP_SCHEMAL_HOST_REG = "http[s]+://[^/]*/";

	public void handle(HttpServerRequest clientRequest, HttpServerResponse clientResponse,
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
				clientResponse.putHeader("Location", v.replaceAll(HTTP_SCHEMAL_HOST_REG, schemal + "://" + host + "/"));
			} else
				clientResponse.putHeader(k, v);
		});

		clientResponse.putHeader("Content-Length",""+appResponse.body().length());
		
//		String s =appResponse.bodyAsString();
//		System.out.println(s);
		clientResponse.write(appResponse.body());
	}

}
