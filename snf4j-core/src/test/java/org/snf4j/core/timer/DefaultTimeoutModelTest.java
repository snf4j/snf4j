package org.snf4j.core.timer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DefaultTimeoutModelTest {
	
	@Test
	public void testAll() {
		DefaultTimeoutModel m = new DefaultTimeoutModel(1, 8);
		
		assertEquals(1, m.next());
		assertEquals(2, m.next());
		assertEquals(4, m.next());
		assertEquals(8, m.next());
		assertEquals(8, m.next());
		m.reset();
		assertEquals(1, m.next());
		assertEquals(2, m.next());
		
		m = new DefaultTimeoutModel(1, 3);
		assertEquals(1, m.next());
		assertEquals(2, m.next());
		assertEquals(3, m.next());
		assertEquals(3, m.next());
		
		m = new DefaultTimeoutModel();
		assertEquals(1000, m.next());
		assertEquals(2000, m.next());
		assertEquals(4000, m.next());
		assertEquals(8000, m.next());
		assertEquals(16000, m.next());
		assertEquals(32000, m.next());
		assertEquals(60000, m.next());
		assertEquals(60000, m.next());
		
	}
}
