package org.etagate;

import org.junit.Test;

import io.vertx.core.buffer.Buffer;

public class BufferTest {

	@Test
	public void test() {
		Buffer buff = Buffer.buffer();
		System.out.println(buff.length());
		
	}

}
