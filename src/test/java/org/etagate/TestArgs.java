package org.etagate;

import org.etagate.helper.Args;
import org.junit.Test;


public class TestArgs {

	@Test
	public void test() {
		String[] s = new String[]{"--class","com.jrzn.aggevent.AggeventMain","--master","local"};
		Args a =new Args(s);
		System.out.println(a.toString());
		System.out.println(a.get("master"));
	}
	


}
