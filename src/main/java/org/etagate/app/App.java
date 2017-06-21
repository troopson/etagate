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

import io.vertx.core.http.HttpServerRequest;

/**
 * @author 瞿建军       Email: troopson@163.com
 * 2017年5月16日
 */
public class App {
	
	
	
	public String name;
	public boolean cut_appName=true;
	public long timeout=5000;
	
	private boolean dev=false;

	private Set<String> routePath=null;
	
	private NodeStragegy nodeStrategy  = null;

	private DevModeSupport devmode=null;

	public App(String name){
		this(name,null);
	}
	
	public App(String name,NodeStragegy s){
		this.name=name;
		if(s==null)
			this.nodeStrategy = new RoundNodeStrategy();
		else
			this.nodeStrategy = s;
	}
		
	public void addNode(String host, int port, int weight){
		if(S.isBlank(host))
			return;
		this.nodeStrategy.addNode(this.createNode(host, port, weight));
	}
	
	public Node createNode(String host, int port, int weight){
		return new Node(this,host,port,weight);
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
	
	
	public Node getNode(HttpServerRequest clientRequest){	
		if(clientRequest!=null && this.dev && devmode!=null){
			Node n = devmode.getDevNode(this, clientRequest);
			if(n!=null)
				return n;
		}
		return nodeStrategy.getNode(Optional.ofNullable(clientRequest));
	}
	
	

	public String toString(){
		return name+"  "+nodeStrategy.nodes()+"  cut:"+this.cut_appName+"   timeout:"+this.timeout;
	}
	
	
	
}
