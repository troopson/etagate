/**
 * 
 */
package org.etagate.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	private List<Node> node =new ArrayList<>();

	public AppObject(String name){
		this.name=name;
	}
	
	public void addNode(String host, int port){
		if(S.isBlank(host))
			return;
		
		node.add(new Node(host,port));
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
		return this.node.get(0);
	}
	
	

	public String toString(){
		return name+"  "+node.toString()+"  cut:"+this.cut_appName+"   timeout:"+this.timeout;
	}
	
	
	public class Node {
		
		public final String host;
		public final int port;
		
		public Node(String host, int port){
			this.host=host;
			this.port=port;
		}
	}
}
