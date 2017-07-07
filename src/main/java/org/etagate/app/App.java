/**
 * 
 */
package org.etagate.app;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.etagate.app.node.Node;
import org.etagate.app.node.NodeStragegy;
import org.etagate.app.node.RoundNodeStrategy;
import org.etagate.helper.S;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * @author 瞿建军       Email: troopson@163.com
 * 2017年5月16日
 */
public class App {
	

	public final WebClient webclient;
	public String name;
	public boolean cut_appName=true;
	public long timeout=1000;
	
	private boolean dev=false;

	private Set<String> routePath=null;
	
	private NodeStragegy nodeStrategy  = null;

	private DevModeSupport devmode=null;
	
	private int maxfail=-1; 
	private long circuit_reset=-1;

	public App(WebClient webclient,String name){
		this(webclient,name,null);
	}
	
	public App(WebClient webclient,String name,NodeStragegy s){
		this.name=name;
		this.webclient=webclient;
		if(s==null)
			this.nodeStrategy = new RoundNodeStrategy();
		else
			this.nodeStrategy = s;
	}
		
	public void addNode(Vertx vertx,String host, int port, int weight){
		if(S.isBlank(host))
			return;
		Node node = new Node(this,host,port,weight);
		if(vertx!=null && !this.dev && this.timeout>0 && this.maxfail>0 && this.circuit_reset>0)
			node.addCircuitBreaker(vertx,timeout, this.maxfail, this.circuit_reset);
		
		this.nodeStrategy.addNode(node);
	}
	
	public Node createDevNode(String host, int port, int weight){
		return new Node(this,host,port,weight,true);
	}
	
	public void addRoutePath(String path){
		if(S.isBlank(path))
			return;
		if(this.routePath==null)
			this.routePath=new HashSet<>();
		this.routePath.add(path);
	}
	
	public Set<String> getRoutePath(){
		return this.routePath;
	}
	
	public void setTimeout(long timeout){
		this.timeout=timeout;
	}
	
	public void setCutAppName(boolean iscut){
		this.cut_appName=iscut;
	}
	
	
	public void setDevmode(DevModeSupport devmode) {
		this.devmode = devmode;
		this.dev = true;
	}

	public String offsetUrl(String uri){
		if (this.cut_appName) {
			int i = uri.indexOf("/", 1);
			if(i==-1)
				return uri;
			String firstPath = uri.substring(1, i);
			if (this.name.equals(firstPath))
				return uri.substring(i);
		}
		return uri;
	}
	
	
	private Node getNode(HttpServerRequest clientRequest){	
		if(clientRequest!=null && this.dev && devmode!=null){
			Node n = devmode.getDevNode(this, clientRequest);
			if(n!=null)
				return n;
		}
		return  nodeStrategy.getNode(Optional.ofNullable(clientRequest));
				
	}
	
	
	public Future<HttpResponse<Buffer>> doGet(String uri,JsonObject param){
		
		Node node = this.getNode(null);
		
		Future<HttpResponse<Buffer>> fu = Future.future();
		if(node == null){			
			fu.fail(new java.util.concurrent.TimeoutException());
			return fu;
		}
		
		node.get(uri, param, fu.completer());
		
		return fu;
		
	}
	
	public Future<JsonObject> doGetJson(String uri,JsonObject param){
		Future<HttpResponse<Buffer>> fu = this.doGet(uri, param);
		Future<JsonObject> re= Future.future();
		fu.setHandler(ar -> {
				if(ar.succeeded()){
					JsonObject u = ar.result().bodyAsJsonObject();
					re.complete(u);
				}else{
					re.fail(ar.cause());
				}	
			});
		return re;
	}
	
	
	public Future<HttpResponse<Buffer>> takeRequest(RoutingContext rc, HttpServerRequest clientRequest,String uri){
		Node node = this.getNode(clientRequest);
		
		Future<HttpResponse<Buffer>> fu = Future.future();
		if(node == null){			
			fu.fail(new java.util.concurrent.TimeoutException());
			return fu;
		}
			
		node.dispatchRequest(rc, clientRequest, uri)
		    .setHandler(fu.completer());
		
		return fu;
	}


	public String toString(){
		return name+"  "+nodeStrategy.nodes()+"  cut:"+this.cut_appName+"   timeout:"+this.timeout;
	}

	public int getMaxfail() {
		return maxfail;
	}

	public void setCircuitMaxfail(int circuit_maxfail) {
		this.maxfail = circuit_maxfail;
	}

	public long getCircuitReset() {
		return circuit_reset;
	}

	public void setCircuitReset(long circuit_reset) {
		this.circuit_reset = circuit_reset;
	}
	
	
	
}
