/**
 * 
 */
package org.etagate.app.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.http.HttpServerRequest;

/**
 * @author 瞿建军       Email: troopson@163.com
 * 2017年5月27日
 */
public class RoundNodeStrategy implements NodeStragegy {

	private AtomicInteger last=new AtomicInteger(0);
	
	private List<Node> node =new ArrayList<>();
	
	private int size = 1;
	
	@Override
	public Node getNode(Optional<HttpServerRequest> clientRequest){
		if(size==1)
			return node.get(0);
		
		int times = 0;
		while(times<size){
			int idx = last.getAndIncrement();	
			Node n = null;
			if(idx>=size){
				last.set(1);
				n = node.get(0);
			}else
				n = node.get(idx);
			
			if(n.canTake())
				return n;	
			else
				times++;
		}
		
		return null;
		
	}

		

	@Override
	public void addNode(Node node) {
		this.node.add(node);	
		this.size = this.node.size();
	}

	public int size(){
		return this.size;
	}
	
	@Override
	public void delNode(Node node) {
		this.node.remove(node);
		this.size = this.node.size();
	}

	@Override
	public List<Node> nodes() {
		return Collections.unmodifiableList(this.node);
	}


	
	
}
