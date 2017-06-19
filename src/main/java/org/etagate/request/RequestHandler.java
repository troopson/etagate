/**
 * 
 */
package org.etagate.request;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.etagate.app.AppObject;
import org.etagate.auth.GateUser;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年4月12日
 */
public class RequestHandler implements Handler<RoutingContext> {

	public static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	public static final String GATE_PRINCIPAL = "gate_principal";
	
	private final AppObject appObj;
	private final WebClient client;

	public RequestHandler(WebClient client, AppObject appinfo) {
		this.appObj = appinfo;
		this.client = client;
	}

	@Override
	public void handle(RoutingContext rc) {

		HttpServerRequest clientRequest = rc.request();
		
		this.sendRequest(rc, clientRequest);

	}

	public void sendRequest(RoutingContext clientRc, HttpServerRequest clientRequest) {

		HttpMethod method = clientRequest.method();
		String uri = clientRequest.uri();
		
		uri = appObj.offsetUrl(uri);

		AppObject.Node node = appObj.getNode();
		
		log.info("request:" + appObj.name + "  method:" + method + "   http://" + node.host + ":"
				+ node.port + uri);

		HttpRequest<Buffer> appRequest = this.client.request(method, uri).ssl(false).timeout(appObj.timeout)
				.port(node.port).host(node.host);

		this.buildHeads(clientRc, node,clientRequest, appRequest);
		
		this.buildPredefineParam(clientRc, appRequest);
		
//		System.out.println(clientRc.getBody());
		
		MultiMap mm = clientRequest.formAttributes();
		if(mm!=null && method.equals(HttpMethod.POST)){
			
			MultiMap query = MultiMap.caseInsensitiveMultiMap();
			query.addAll(mm);
			
			appRequest.sendForm(query, new AppRequestHandler(clientRc,clientRequest));
			
		}else{
			appRequest.sendBuffer(clientRc.getBody(), new AppRequestHandler(clientRc,clientRequest));
			
		}

	}

	private void buildPredefineParam(RoutingContext clientRc, HttpRequest<Buffer> appRequest) {
			
		Set<FileUpload> up = clientRc.fileUploads();
		if(up!=null && !up.isEmpty()){
			Set<String> upfiles = new HashSet<>();
			up.forEach(f->{
				upfiles.add(this.toJson(f).encode());
			});
			appRequest.addQueryParam("_upload_files_", Json.encode(upfiles));
//			System.out.println(Json.encode(upfiles));
		}
	}

	private void buildHeads(RoutingContext clientRc, AppObject.Node node, HttpServerRequest clientRequest, HttpRequest<Buffer> appRequest) {
		MultiMap heads = appRequest.headers();
		heads.addAll(clientRequest.headers());
		heads.remove("Host");
		heads.add("Host", node.host + ":" + node.port);
		
		heads.remove(GATE_PRINCIPAL);
		GateUser user = (GateUser) clientRc.user();
//		System.out.println("==============="+user.principal().encode());
		if (user != null)
			try {
				heads.add(GATE_PRINCIPAL, new String(user.principal().encode().getBytes("UTF-8"),"ISO8859-1"));
			} catch (UnsupportedEncodingException e) {}
			
		
	}
	
	private JsonObject toJson(FileUpload fu){
		JsonObject j =new JsonObject();
		j.put("name",fu.name());
		j.put("fileName",fu.fileName());
		j.put("uploadedFileName",fu.uploadedFileName());
		j.put("size",fu.size());
		return j;
	}
	
	private class AppRequestHandler implements Handler<AsyncResult<HttpResponse<Buffer>>> {

		private RoutingContext clientRc;
		private HttpServerRequest clientRequest; 
		
		public AppRequestHandler(RoutingContext clientRc, HttpServerRequest clientRequest){
			this.clientRc = clientRc;
			this.clientRequest = clientRequest;
		}
		
		@Override
		public void handle(AsyncResult<HttpResponse<Buffer>> ar) {
			
			if (ar.succeeded()) {

				HttpServerResponse clientResponse = clientRc.response();
				HttpResponse<Buffer> appResponse = ar.result();
				AppResponse.handle(clientRequest, clientResponse, appResponse);

				if (!clientResponse.ended())
					clientResponse.end();
				
			} else {
				clientRc.fail(500);
				ar.cause().printStackTrace();
			}
			
		}
		
	}

}
