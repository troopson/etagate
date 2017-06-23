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

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.CircuitBreakerState;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
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
	
	private CircuitBreaker breaker;
	
	public final String host;
	public final int port;
	public final int weight;
	
	public final boolean isDev;

	public final App app;
		
	public Node(App app,String host, int port, int weight){
		this(app,host, port, weight,false);
	}
	
	public Node(App app,String host, int port, int weight,boolean isDev){
		this.host=host;
		this.port=port;
		this.weight=weight;
		this.app = app;
		this.isDev=isDev;
		
	}
	
	public void addCircuitBreaker(Vertx vertx, long timeout, int maxfail, long resettimeout){
		if(this.isDev)
			throw new IllegalArgumentException("Can't set circuitbreaker for dev node.");
		
		breaker = CircuitBreaker.create(app.name+"-"+host+":"+port, vertx,
				    new CircuitBreakerOptions()
				        .setMaxFailures(maxfail) // number of failure before opening the circuit
				        .setTimeout(timeout) // consider a failure if the operation does not succeed in time
				        .setFallbackOnFailure(false) // do we call the fallback on failure
				        .setResetTimeout(resettimeout) // time spent in open state before attempting to re-try
				);
				
	}
	
	public boolean isActive(){
		if(this.breaker==null)
			return true;
		else if(this.breaker.state()==CircuitBreakerState.OPEN)
			return false;
		return true;
	}
	
	public CircuitBreakerState status(){
		if(this.breaker==null)
			return null;
		return breaker.state();
	}
	
	
	private void markFail(Throwable t){
		if(t instanceof java.util.concurrent.TimeoutException)
			System.out.println("mark timeout error");
		else
			System.out.println("mark fail:"+t.getClass());
		log.error(t);
	}
	
	
	
	
	@Override
	public String toString(){
		return host+":"+port+" "+this.weight;
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
		if(param!=null){
			param.forEach(entry -> {
				req.addQueryParam(entry.getKey(), "" + entry.getValue());
			});
		}
		
		this.sendWithBreaker(f->req.timeout(this.app.timeout).send(f), h);
		
//		if(this.breaker!=null){
//			breaker.<HttpResponse<Buffer>>execute(f->{
//				req.send(f.completer());
//			}).setHandler(this.wrap(h));	
//		}else{
//			req.timeout(this.app.timeout).send(this.wrap(h));
//		}	
		
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
			
//			if(this.breaker!=null){
//				breaker.<HttpResponse<Buffer>>execute(f->{
//					appRequest.sendForm(query, f.completer());
//				}).setHandler(this.wrap(fu.completer()));	
//			}else{
//				appRequest.sendForm(query, this.wrap(fu.completer()));
//			}
			
			this.sendWithBreaker(f->appRequest.sendForm(query, f), fu.completer());
			
		}else{
						
//			if(this.breaker!=null){
//				breaker.<HttpResponse<Buffer>>execute(f->{
//					appRequest.sendBuffer(rc.getBody(), f.completer());
//				}).setHandler(this.wrap(fu.completer()));	
//			}else{
//				appRequest.sendBuffer(rc.getBody(),this.wrap(fu.completer()));
//			}
			
			this.sendWithBreaker(f->appRequest.sendBuffer(rc.getBody(), f), fu.completer());
			
		}
		
		if(this.breaker!=null)
			System.out.println("circuit breaker:"+this.status().name());
		
		log.info("request:" + app.name + "  method:" + method + "   http://" + this.host + ":"
				+ this.port + uri);
		
		return fu;
		
	}
	
	private Handler<AsyncResult<HttpResponse<Buffer>>> wrap(Handler<AsyncResult<HttpResponse<Buffer>>> h){
		return res->{
			if(!res.succeeded()){
				
				Node.this.markFail(res.cause());
			}
			h.handle(res);
		};			
	}
	
	private void sendWithBreaker(Handler<Handler<AsyncResult<HttpResponse<Buffer>>>> dorequest,Handler<AsyncResult<HttpResponse<Buffer>>> handler){

		if(this.breaker!=null){
			breaker.<HttpResponse<Buffer>>execute(f->{
				dorequest.handle(f.completer());
				//appRequest.sendBuffer(rc.getBody(), this.wrap(f.completer()));
			}).setHandler(this.wrap(handler));	
		}else{
			dorequest.handle(this.wrap(handler));
			//appRequest.sendBuffer(rc.getBody(),this.wrap(fu.completer()));
		}
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
