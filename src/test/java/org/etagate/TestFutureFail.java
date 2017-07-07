/**
 * 
 */
package org.etagate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年4月19日
 */
@RunWith(VertxUnitRunner.class)
public class TestFutureFail {

	@Rule
	public RunTestOnContext rule = new RunTestOnContext();

	@Test
	public void test(TestContext context) throws InterruptedException {
//		Async async = context.async();
		Vertx vertx = rule.vertx();

		WebClient client = WebClient.create(vertx);
		HttpRequest<Buffer> req = client.get(41414, "172.21.9.21", "/metrics");
		req.putHeader("aa","ccc");
		 
		req.timeout(50000);
		req.send(ar -> {
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
//		async.awaitSuccess();
	}

}
