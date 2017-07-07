/**
 * 
 */
package org.etagate.request;

import java.util.concurrent.TimeoutException;

import org.etagate.app.App;
import org.etagate.helper.HttpStatus;
import org.etagate.helper.S;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年4月12日
 */
public class RequestHandler implements Handler<RoutingContext> {

	public static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	
	private final App appObj;

	public RequestHandler(App appinfo) {
		this.appObj = appinfo;
	}

	@Override
	public void handle(RoutingContext rc) {

		HttpServerRequest clientRequest = rc.request();
		
		String uri = clientRequest.uri();
		
		uri = appObj.offsetUrl(uri);
		
		Future<HttpResponse<Buffer>> fu = appObj.takeRequest(rc, clientRequest, uri);
		fu.setHandler(ar->{
		    	HttpServerResponse clientResponse = rc.response();
		    	if (ar.succeeded()) {					
					this.handle(clientRequest, clientResponse, ar.result());					
				} else {
					
					if(ar.cause() instanceof TimeoutException)
						clientResponse.setStatusCode(HttpStatus.Request_Timeout);
					else
						clientResponse.setStatusCode(HttpStatus.Service_Unavailable);
					
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

		clientResponse.setChunked(true);
//		clientResponse.putHeader("Content-Length",""+appResponse.body().length());
		
//		String s =appResponse.bodyAsString();
//		System.out.println(s);
		clientResponse.write(appResponse.bodyAsBuffer());
		
	}

}
