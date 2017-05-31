/**
 * 
 */
package org.etagate.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.etagate.app.AppObject.Node;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年5月27日
 */
public class WeightNodeStrategy implements NodeStragegy {
	
	private List<Node> node =new ArrayList<>();
	
	private int totalWeight=0;
	
	@Override
	public Node getNode(){
		int sum =0 ;
		int rand=(int)(1+Math.random()*totalWeight);
		for(Node n : node){
			sum = sum+n.weight;
			if(rand<sum)
				return n;
		}
		return node.get(0);
	}

	@Override
	public void addNode(Node node) {
		this.node.add(node);	
		this.totalWeight = this.totalWeight+node.weight;
	}

	@Override
	public void delNode(Node node) {
		this.node.remove(node);
		this.totalWeight=this.totalWeight-node.weight;
	}

	@Override
	public List<Node> nodes() {
		return Collections.unmodifiableList(this.node);
	}


	
	
}
