/**
 * 
 */
package org.etagate.app.node;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.etagate.app.App;
import org.etagate.auth.GateUser;
import org.etagate.conf.Globe;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
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
 * @author 瞿建军       Email: troopson@163.com
 * 2017年6月21日
 */
public class Node {
	
	public static final Logger log = LoggerFactory.getLogger(Node.class);
	
	public final String host;
	public final int port;
	public final int weight;

	public final App app;
	
	private int failtimes=0;
	
	public Node(App app,String host, int port, int weight){
		this.host=host;
		this.port=port;
		this.weight=weight;
		this.app = app;
	}
	
	
	private void markFail(Throwable t){
		if(t instanceof java.util.concurrent.TimeoutException)
			System.out.println("mark timeout error");
		else
			System.out.println("mark fail:"+t.getClass());
		log.error(t);
		this.failtimes ++;
	}
	
	public int getFailTimes(){
		return this.failtimes;
	}
	
	
	@Override
	public String toString(){
		return host+":"+port;
	}
	

	@Override
	public boolean equals(Object n){
		if(n==null || !(n instanceof Node))
			return false;
		Node t =(Node)n;
		return t.host.equals(host)&& t.port == port;
	}
	
	//==================================================
	public void get(WebClient http,String uri, JsonObject param, Handler<AsyncResult<HttpResponse<Buffer>>> h) {

		HttpRequest<Buffer> req = http.get(this.port,this.host, uri);
		param.forEach(entry -> {
			req.addQueryParam(entry.getKey(), "" + entry.getValue());
		});

		req.timeout(this.app.timeout).send(this.wrap(h));
	}
	
	public void getJson(WebClient http,String uri, JsonObject param, Handler<AsyncResult<JsonObject>> h) {
		
		this.get(http, uri, param, ar -> {
			if(ar.succeeded()){
				JsonObject u = ar.result().bodyAsJsonObject();
				h.handle(Future.succeededFuture(u));
			}else{
				h.handle(Future.failedFuture(ar.cause()));
			}			
		});

	}
	
	
	public Future<HttpResponse<Buffer>> dispatchRequest(WebClient http,RoutingContext rc,HttpServerRequest clientRequest,String uri){
		HttpMethod method = clientRequest.method();
		
		HttpRequest<Buffer> appRequest = http.request(method, uri).ssl(false).timeout(app.timeout)
				.port(this.port).host(this.host);
		
		
		MultiMap cheads = clientRequest.headers();		
		MultiMap heads = appRequest.headers();
		heads.addAll(cheads);
		heads.remove("Host");
		heads.add("Host", this.host + ":" + this.port);
		
		heads.remove(Globe.GATE_PRINCIPAL);
//		System.out.println("==============="+user.principal().encode());
		GateUser user = (GateUser)rc.user();
		if (user != null)
			try {
				heads.add(Globe.GATE_PRINCIPAL, new String(user.principal().encode().getBytes("UTF-8"),"ISO8859-1"));
			} catch (UnsupportedEncodingException e) {}
		
		
		Set<FileUpload> up = rc.fileUploads();
		if(up!=null && !up.isEmpty()){
			Set<String> upfiles = new HashSet<>();
			up.forEach(f->{
				upfiles.add(this.fileToJson(f).encode());
			});
			appRequest.addQueryParam("_upload_files_", Json.encode(upfiles));
		}
		
		Future<HttpResponse<Buffer>> fu = Future.future();
		
		MultiMap attribute = clientRequest.formAttributes();		
		if(attribute!=null && method.equals(HttpMethod.POST)){
			
			MultiMap query = MultiMap.caseInsensitiveMultiMap();
			query.addAll(attribute);
			
			appRequest.sendForm(query, this.wrap(fu.completer()));
			
		}else{
			appRequest.sendBuffer(rc.getBody(),this.wrap(fu.completer()));
			
		}
		
		log.info("request:" + app.name + "  method:" + method + "   http://" + this.host + ":"
				+ this.port + uri);
		
		return fu;
		
	}
	
	private Handler<AsyncResult<HttpResponse<Buffer>>> wrap(Handler<AsyncResult<HttpResponse<Buffer>>> h){
		return res->{
			if(!res.succeeded())
				Node.this.markFail(res.cause());
			 h.handle(res);
		};			
	}
	
	private JsonObject fileToJson(FileUpload fu){
		JsonObject j =new JsonObject();
		j.put("name",fu.name());
		j.put("fileName",fu.fileName());
		j.put("uploadedFileName",fu.uploadedFileName());
		j.put("size",fu.size());
		return j;
	}
	
	
}
