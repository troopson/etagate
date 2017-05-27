/**
 * 
 */
package org.etagate;

import io.vertx.ext.unit.TestOptions;
import io.vertx.ext.unit.TestSuite;
import io.vertx.ext.unit.report.ReportOptions;

/**
 * @author 瞿建军       Email: jianjun.qu@istuary.com
 * 2017年5月16日
 */
public class VertxTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TestSuite suite = TestSuite.create("the_test_suite");
		suite.test("my_test_case", context -> {
		  String s = "value";
		  context.assertEquals("value", s);
		});
		suite.run(new TestOptions().addReporter(new ReportOptions().setTo("console")));

	}

}
