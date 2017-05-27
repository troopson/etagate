/**
 * 
 */
package org.etagate;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * @author 瞿建军 Email: jianjun.qu@istuary.com 2017年4月19日
 */

public class TestRequestMain {

	public static void main(String[] args){
		Vertx vertx = Vertx.vertx();

		WebClient client = WebClient.create(vertx);
		client.get(41414, "172.21.9.21", "/metrics").send(ar -> {
			if (ar.succeeded()) {

				HttpResponse<Buffer> response = ar.result();
				System.out.println(response.body());
				System.out.println("Received response with status code" + response.statusCode());
			} else {
				System.out.println("Something went wrong " + ar.cause().getMessage());
			}
//			async.complete();

		});

	
		System.out.println("here1");
		
		
	}

}
