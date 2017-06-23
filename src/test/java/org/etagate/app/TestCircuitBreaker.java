package org.etagate.app;

import org.etagate.app.node.Node;
import org.junit.Assert;
import org.junit.Test;

import io.vertx.circuitbreaker.CircuitBreakerState;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class TestCircuitBreaker {

	
	public static void main(String[] args) throws InterruptedException {
		Vertx vertx = Vertx.vertx();
		WebClient http = WebClient.create(vertx);
		App a =new App("test");		
		
		
		
		Node n = new Node(a,"localhost", 8080, 1);
		n.addCircuitBreaker(vertx, 1000, 2, 30*1000);
		
		Handler<AsyncResult<HttpResponse<Buffer>>> h = r->{
			if(r.succeeded())
				System.out.println("OK "+n.status());
			else
				System.out.println("Fail "+n.status());
				
		};
		
		n.get(http, "/test", null, h);
		n.get(http, "/test", null, h);
		n.get(http, "/test", null, h);

		for(int i=0;i<20;i++){
			Thread.sleep(10* 1000);
			n.get(http, "/test", null, h);
			n.get(http, "/test", null, h);
			n.get(http, "/test", null, h);
		}

	}

}
