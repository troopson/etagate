package org.etagate;

import org.etagate.app.App;
import org.etagate.app.node.Node;
import org.etagate.app.node.RoundNodeStrategy;
import org.etagate.app.node.WeightNodeStrategy;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class TestNodeSelectStrategy {

	
	@Test
	public void rand(){
		for(int i=0;i<10 ; i++){
			int rand=(int)(1+Math.random()*22);
			System.out.println(rand);
		}
	}
	
	@Test
	public void testGetNode() {
		Vertx vertx = Vertx.vertx();
		WebClient http = WebClient.create(vertx);
		App a = new App(vertx,http,"test");
		WeightNodeStrategy w = new WeightNodeStrategy();
		w.addNode(a.createDevNode("10.10.10.1",80,1));
		w.addNode(a.createDevNode("10.10.10.2",80,2));
		w.addNode(a.createDevNode("10.10.10.3",80,3));
		w.addNode(a.createDevNode("10.10.10.4",80,4));
		w.addNode(a.createDevNode("10.10.10.5",80,5));
		w.addNode(a.createDevNode("10.10.10.6",80,6));
		w.addNode(a.createDevNode("10.10.10.7",80,7));
		w.addNode(a.createDevNode("10.10.10.8",80,8));
		w.addNode(a.createDevNode("10.10.10.9",80,9));
		w.addNode(a.createDevNode("10.10.10.10",80,10));
		
		for(int i=0; i<100 ;i++){
//			Node n = w.getNode(null);
//			System.out.println(n);
			System.out.println(w.getNode(null).toString());
		}
		
		
	}
	
	
	@Test
	public void testRoundStrategy() {
		Vertx vertx = Vertx.vertx();
		WebClient http = WebClient.create(vertx);
		App a = new App(vertx,http,"test");
		RoundNodeStrategy w = new RoundNodeStrategy();
		Node n1 = a.createDevNode("10.10.10.1",80,1);
		Node n2 = a.createDevNode("10.10.10.1",80,1);
		Node n3 = a.createDevNode("10.10.10.1",80,1);
		Node n4 = a.createDevNode("10.10.10.1",80,1);
		Node n5 = a.createDevNode("10.10.10.1",80,1);
		w.addNode(n1);
		w.addNode(n2);
		w.addNode(n3);
		w.addNode(n4);
		w.addNode(n5);
		
		
		for(int i=0; i<100 ;i++){
			System.out.println(w.getNode(null).toString());
		}
		
		
	}

}
