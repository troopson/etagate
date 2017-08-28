/**
 * 
 */
package org.etagate.app.node;

import java.util.HashSet;
import java.util.Set;

import org.etagate.app.App;
import org.etagate.auth.GateUser;
import org.etagate.conf.Globe;
import org.etagate.helper.S;

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
		
	private int continueFailTimes=0;
	private boolean gtMaxFail=false;
		
//	private boolean paused=false;
//	private int taskInProcessing=0;
	
		
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
	public boolean canTake(){
		if(this.breaker==null){		
			if(this.gtMaxFail)
				return false;
//			if(this.paused && taskInProcessing>0)
//				return false;
			
			return true;
		}else if(this.breaker.state()==CircuitBreakerState.OPEN)
			return false;
		return true;
	}
	
	
	public CircuitBreakerState status(){
		if(this.breaker==null)
			return null;
		return breaker.state();
	}
	
	public JsonObject toJsonObject(){
		JsonObject json = new JsonObject();
		json.put("host", this.host);
		json.put("port", this.port);
		return json;
	}
	
	@Override
	public String toString(){
		return host+":"+port+"("+this.weight+")"
//				+"  paused:"+this.paused
				+"  failed times:"+continueFailTimes
//				+"  tasks queue:"+taskInProcessing
				+"  gtMaxFail:"+this.gtMaxFail;
	}
	

	@Override
	public boolean equals(Object n){
		if(n==null || !(n instanceof Node))
			return false;
		Node t =(Node)n;
		return t.host.equals(host)&& t.port == port;
	}
	
	
	
	//==================================================
	public void get(String uri, JsonObject param, Handler<AsyncResult<HttpResponse<Buffer>>> h) {

		HttpRequest<Buffer> req = app.webclient.get(this.port,this.host, uri)
				.timeout(app.timeout);
		if(param!=null){
			param.forEach(entry -> {
				req.addQueryParam(entry.getKey(), "" + entry.getValue());
			});
		}		
		
		Handler<AsyncResult<HttpResponse<Buffer>>> callback =res->{
			if(res.succeeded()){				
				continueFailTimes=0;		
				log.info("{}, {} http://{}:{}{}",app.name,HttpMethod.GET,this.host,this.port,uri);			
				h.handle(Future.succeededFuture(res.result()));			
			}else{
				Throwable t = res.cause();
				this.onException(t, uri);
				h.handle(Future.failedFuture(t));				
			}		
		};
		
//		taskInProcessing=taskInProcessing+1;
		if(this.breaker!=null){
			breaker.<HttpResponse<Buffer>>execute(f->{
				req.send(f);
			}).setHandler(callback);	
		}else{			
			req.send(callback);
		}
				
	}	
	
	public Future<HttpResponse<Buffer>> dispatchRequest(RoutingContext rc,HttpServerRequest clientRequest,String uri){
		HttpMethod method = clientRequest.method();
		HttpRequest<Buffer> appRequest = app.webclient.request(method, uri).ssl(false)
				.timeout(app.timeout)
				.port(this.port).host(this.host);
				
		
		MultiMap cheads = clientRequest.headers();		
		MultiMap heads = appRequest.headers();
		heads.addAll(cheads);
//		heads.remove("Host");
//		heads.add("Host", this.host + ":" + this.port);
		
		String depdstr = app.getDependAppAddr(clientRequest);
		if(S.isNotBlank(depdstr))
			heads.add(Globe.APP_DEPEND, depdstr);
		
//		System.out.println("==================="+depdstr);
		heads.remove(Globe.GATE_PRINCIPAL);
//		System.out.println("==============="+user.principal().encode());
		GateUser user = (GateUser)rc.user();
		if (user != null)
			heads.add(Globe.GATE_PRINCIPAL, user.encodePrincipal());
		String gateAddress = app.getInside_address();
		if(gateAddress!=null)
			heads.add(Globe.GATE_ADDRESS, gateAddress);
		
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
		Handler<AsyncResult<HttpResponse<Buffer>>> h = res->{
			if(res.succeeded()){				
				continueFailTimes=0;		
				log.info("{}, {} http://{}:{}{}",app.name,method,this.host,this.port,uri);			
				fu.complete(res.result());			
			}else{
				Throwable t = res.cause();
				this.onException(t, uri);
				fu.fail(t);				
			}		
		};
		
		MultiMap attribute = clientRequest.formAttributes();		
		if(attribute!=null && method.equals(HttpMethod.POST)){
			
			MultiMap query = MultiMap.caseInsensitiveMultiMap();
			query.addAll(attribute);
			
			if(this.breaker!=null){
				breaker.<HttpResponse<Buffer>>execute(f->{
					appRequest.sendForm(query, f);
				}).setHandler(h);	
			}else{			
				appRequest.send(h);
			}
			
			
		}else{
			Buffer body = rc.getBody();
			if(body.length()<=0){
				if(this.breaker!=null){
					breaker.<HttpResponse<Buffer>>execute(f->{
						appRequest.send(f);
					}).setHandler(h);	
				}else{			
					appRequest.send(h);
				}
				
			}else{
				if(this.breaker!=null){
					breaker.<HttpResponse<Buffer>>execute(f->{
						appRequest.sendBuffer(body, f);
					}).setHandler(h);	
				}else{			
					appRequest.send(h);
				}
			}
			
		}		
		
		return fu;
		
	}
	
	public String hostport(){
		return this.host+":"+this.port;
	}
	
	private void onException(Throwable t,  String uri){
		if( t instanceof io.vertx.core.http.ConnectionPoolTooBusyException){
//			this.paused = true;
			log.warn("Too Busy Exception. {}  url: {} " ,this.toString(), uri);
		}else{						
			if( t instanceof java.util.concurrent.TimeoutException  ){
				log.warn("Timeout: {} url: {}", this.toString() , uri);						
			}else{
				continueFailTimes = continueFailTimes+1;
				int maxFail = app.getMaxfail();
				if(maxFail>0 && continueFailTimes > maxFail)
					gtMaxFail=true;						
				log.error("Connect Error: {}  url: {}",t, this.toString() , uri);
				t.printStackTrace();
			}	
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
