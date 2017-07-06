/**
 * 
 */
package org.etagate.app.node;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
		
	private AtomicInteger failTimes=new AtomicInteger(0);
	private boolean gtMaxFail=false;
		
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
	
	//如果有断路器，那么返回断路器的状态，
	//如果没有，按照最大失败次数的设置，超过最大失败次数，返回false
	//如果没有设置最大失败次数，那么始终返回true
	public boolean isActive(){
		if(this.breaker==null){			
			return !this.gtMaxFail;
		}else if(this.breaker.state()==CircuitBreakerState.OPEN)
			return false;
		return true;
	}
	
	public CircuitBreakerState status(){
		if(this.breaker==null)
			return null;
		return breaker.state();
	}
	
	
	private void markFail(Throwable t){
		
		int failed = failTimes.incrementAndGet();
		int maxFail = app.getMaxfail();
		if(maxFail>0 && failed>maxFail)
			gtMaxFail=true;
		
		
		log.error(t);
		
	}
	
	
	
	
	@Override
	public String toString(){
		return host+":"+port+"("+this.weight+")  failed times:"+failTimes.get();
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
				
		this.sendWithBreaker(f->req.timeout(this.app.timeout).send(f), this.wrap(HttpMethod.GET,uri, h));
				
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
//		heads.remove("Host");
//		heads.add("Host", this.host + ":" + this.port);
		
		heads.remove(Globe.GATE_PRINCIPAL);
//		System.out.println("==============="+user.principal().encode());
		GateUser user = (GateUser)rc.user();
		if (user != null)
			heads.add(Globe.GATE_PRINCIPAL, user.principalString());
		
		if(method == HttpMethod.POST){
			Set<FileUpload> up = rc.fileUploads();
			if(up!=null && !up.isEmpty()){		
				Set<String> upfiles = new HashSet<>();
				up.forEach(f->{
					upfiles.add(this.fileToJson(f).encode());
				});
				appRequest.addQueryParam("_upload_files_", Json.encode(upfiles));	
			}
		}
		
		Future<HttpResponse<Buffer>> fu = Future.future();
		
		Handler<AsyncResult<HttpResponse<Buffer>>> h = this.wrap(method,uri, fu.completer());
		
		MultiMap attribute = clientRequest.formAttributes();		
		if(attribute!=null && method.equals(HttpMethod.POST)){
			
			MultiMap query = MultiMap.caseInsensitiveMultiMap();
			query.addAll(attribute);
			
			
			this.sendWithBreaker(f->appRequest.sendForm(query, f), h);
			
		}else{
			Buffer body = rc.getBody();
			if(body.length()<=0)
				this.sendWithBreaker(f->appRequest.send(f), h);
			else
			    this.sendWithBreaker(f->appRequest.sendBuffer(body, f), h);
			
		}		
		
		return fu;
		
	}
	

	private void sendWithBreaker(Handler<Handler<AsyncResult<HttpResponse<Buffer>>>> dorequest,Handler<AsyncResult<HttpResponse<Buffer>>> handler){

		if(this.breaker!=null){
			breaker.<HttpResponse<Buffer>>execute(f->{
				dorequest.handle(f.completer());
			}).setHandler(handler);	
		}else{
			dorequest.handle(handler);
		}
	}
	
	private Handler<AsyncResult<HttpResponse<Buffer>>> wrap(HttpMethod method,String uri,Handler<AsyncResult<HttpResponse<Buffer>>> h){
		return res->{			
			
			if(res.succeeded()){
				
				h.handle(Future.succeededFuture(res.result()));
				if(log.isInfoEnabled())
					log.info(method + " " + app.name +  " http://" + this.host + ":"
						+ this.port + uri);
			}else{
				Throwable t = res.cause();
				Node.this.markFail(t);
				h.handle(Future.failedFuture(t));
				if(log.isInfoEnabled()){
					if(t instanceof java.util.concurrent.TimeoutException)
						log.info("timeout "+this.toString());
					else
						log.info("fail:"+t.getClass()+" "+this.toString());		
				}
			}
			
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
