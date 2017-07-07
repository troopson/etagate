/**
 * 
 */
package org.etagate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Future;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * @author 瞿建军 Email: troopson@163.com 2017年4月19日
 */
@RunWith(VertxUnitRunner.class)
public class TestRequest {

	@Rule
	public RunTestOnContext rule = new RunTestOnContext();

	@Test
	public void test(TestContext context) throws InterruptedException {
		Future<String> f =Future.future();
		f.fail("aaa");
		f.setHandler(res->{
			
			if(res.failed())
				System.out.println("failed...");
			else
				System.out.println("ok....");
		});
//		async.awaitSuccess();
		
		System.out.println("end");
	}

}
