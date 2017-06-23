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
	
	private int totalWeight=0;
	
	@Override
	public Node getNode(Optional<HttpServerRequest> clientRequest){
		int sum =0 ;
		int rand=(int)(1+Math.random()*totalWeight);
		for(Node n : node){			
			sum = sum+n.weight;		
			if(n.isActive() && rand<sum)
				return n;
		}
		return node.get(0);
	}
	
	
	
	public int size(){
		return this.node.size();
	}

	@Override
	public void addNode(Node node) {
		this.node.add(node);	
		this.totalWeight = this.totalWeight+node.weight;
		this.sortNode();
	}

	@Override
	public void delNode(Node node) {
		this.node.remove(node);
		this.totalWeight=this.totalWeight-node.weight;
		this.sortNode();
	}

	@Override
	public List<Node> nodes() {
		return Collections.unmodifiableList(this.node);
	}
	
	private void sortNode(){
		
		this.node.sort((o1,o2)-> { return o1.weight>o2.weight?1:-1; });
		
		System.out.println(this.node.toString());
	}

	
}
