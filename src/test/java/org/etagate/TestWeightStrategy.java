package org.etagate;

import org.etagate.app.AppObject;
import org.etagate.app.WeightNodeStrategy;
import org.junit.Test;

public class TestWeightStrategy {

	
	@Test
	public void rand(){
		for(int i=0;i<10 ; i++){
			int rand=(int)(1+Math.random()*22);
			System.out.println(rand);
		}
	}
	
	@Test
	public void testGetNode() {
		AppObject a = new AppObject("test");
		WeightNodeStrategy w = new WeightNodeStrategy();
		w.addNode(a.new Node("10.10.10.1",80,1));
		w.addNode(a.new Node("10.10.10.2",80,2));
		w.addNode(a.new Node("10.10.10.3",80,3));
		w.addNode(a.new Node("10.10.10.4",80,4));
		w.addNode(a.new Node("10.10.10.5",80,5));
		w.addNode(a.new Node("10.10.10.6",80,6));
		w.addNode(a.new Node("10.10.10.7",80,7));
		w.addNode(a.new Node("10.10.10.8",80,8));
		w.addNode(a.new Node("10.10.10.9",80,9));
		w.addNode(a.new Node("10.10.10.10",80,10));
		
		for(int i=0; i<100 ;i++)
		   System.out.println(w.getNode(null).toString());
		
		
	}

}
