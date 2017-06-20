/**
 * 
 */
package org.etagate.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.etagate.app.AppObject.Node;

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
		
		int idx = last.getAndIncrement();
		
		if(idx>=size){
			last.set(1);
			return node.get(0);
		}else
			return node.get(idx);
	}

	@Override
	public void addNode(Node node) {
		this.node.add(node);	
		this.size = this.node.size();
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
