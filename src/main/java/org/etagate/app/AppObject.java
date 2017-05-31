/**
 * 
 */
package org.etagate.app;

import java.util.HashSet;
import java.util.Set;

import org.etagate.helper.S;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年5月16日
 */
public class AppObject {
	
	public String name;
	public boolean cut_appName=true;
	public long timeout=5000;
	
	private Set<String> routePath=null;
	
	private NodeStragegy nodeStrategy  = null;


	public AppObject(String name){
		this(name,null);
	}
	
	public AppObject(String name,NodeStragegy s){
		this.name=name;
		if(s==null)
			this.nodeStrategy = new RoundNodeStrategy();
		else
			this.nodeStrategy = s;
	}
	
	public void addNode(String host, int port, int weight){
		if(S.isBlank(host))
			return;
		
		this.nodeStrategy.addNode(new Node(host,port,weight));
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
	
	
	
	public Node getNode(){
		return nodeStrategy.getNode();
	}
	
	

	public String toString(){
		return name+"  "+nodeStrategy.nodes()+"  cut:"+this.cut_appName+"   timeout:"+this.timeout;
	}
	
	
	public class Node {
		
		public final String host;
		public final int port;
		public final int weight;
		
		
		
		public Node(String host, int port, int weight){
			this.host=host;
			this.port=port;
			this.weight=weight;
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
		
	}
}
