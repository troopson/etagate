package org.etagate.app;

import org.etagate.app.node.Node;
import org.etagate.app.node.RoundNodeStrategy;
import org.etagate.app.node.WeightNodeStrategy;
import org.junit.Assert;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class TestNodeStrategy {

	@Test
	public void testRoundNodeStrategy() {
		Vertx vertx = Vertx.vertx();
		WebClient http = WebClient.create(vertx);
		App a =new App(http,"test");		
		RoundNodeStrategy r = new RoundNodeStrategy();
		
		r.addNode(new Node(a,"1.1.1.1", 80, 0));
		r.addNode(new DownNode(a,"1.1.1.2", 80, 0));
		r.addNode(new Node(a,"1.1.1.3", 80, 0));
		
		System.out.println("==========RoundNodeStrategy =================");
		for(int i=0;i<1000;i++){
			Node n = r.getNode(null);
			Assert.assertTrue(n.canTake());
			System.out.println(n.host);
		}
		
		
	}
	
	@Test
	public void testWeightNodeStrategy() {
		Vertx vertx = Vertx.vertx();
		WebClient http = WebClient.create(vertx);
		App a =new App(http,"test");		
		WeightNodeStrategy r = new WeightNodeStrategy();
		
		r.addNode(new Node(a,"1.1.1.1", 80, 1));
		r.addNode(new DownNode(a,"1.1.1.2", 80, 2));
		r.addNode(new Node(a,"1.1.1.3", 80, 1));
		r.addNode(new Node(a,"1.1.1.4", 80, 3));

		System.out.println("==========WeightNodeStrategy =================");
		for(int i=0;i<1000;i++){
			Node n = r.getNode(null);
			Assert.assertTrue(n.canTake());
			System.out.println(n.host);
		}
	}
	
	class DownNode extends Node{

		public DownNode(App app, String host, int port, int weight) {
			super(app, host, port, weight);
		}
		
		@Override
		public boolean canTake(){
			return false;
		}
		
	}

}
