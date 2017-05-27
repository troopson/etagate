/**
 * 
 */
package org.etagate.app;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.etagate.app.AppObject.Node;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年5月27日
 */
public class RoundNodeStrategy implements NodeStragegy {

	private AtomicInteger last=new AtomicInteger(0);
	
	public Node getNode(List<Node> node){
		int size = node.size();
		if(size==1)
			return node.get(0);
		
		if(last.compareAndSet(size, 0))
			return node.get(0);
		else
			return node.get(last.incrementAndGet());
	}
	
}
