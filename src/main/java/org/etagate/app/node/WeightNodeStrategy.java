/**
 * 
 */
package org.etagate.app.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.vertx.core.http.HttpServerRequest;

/**
 * @author 瞿建军       Email: troopson@163.com
 * 2017年5月27日
 */
public class WeightNodeStrategy implements NodeStragegy {
	
	private List<Node> node =new ArrayList<>();
	
	
	@Override
	public Node getNode(Optional<HttpServerRequest> clientRequest){
		int sum =0 ;
		int total=0;
		List<Node> canNode = new ArrayList<>();
		for(Node n: node){
			if(n.canTake()){
				canNode.add(n);
				total=total+n.weight;
			}
		}
		if(canNode.isEmpty())
			return null;
		
		int rand=(int)(1+Math.random()*total);
//		System.out.println(rand+"  "+total);
		for(Node n : canNode){			
			sum = sum+n.weight;		
			if(rand<=sum)
				return n;
		}
		return null;
	}
	
	
	
	public int size(){
		return this.node.size();
	}

	@Override
	public void addNode(Node node) {
		this.node.add(node);	
		this.sortNode();
	}

	@Override
	public void delNode(Node node) {
		this.node.remove(node);
		this.sortNode();
	}

	@Override
	public List<Node> nodes() {
		return Collections.unmodifiableList(this.node);
	}
	
	private void sortNode(){
		
		this.node.sort((o1,o2)-> { return o1.weight>o2.weight?1:-1; });
		
//		System.out.println(this.node.toString());
	}

	
}
