package org.snf4j.core.timer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DefaultRetransmissionModelTest {
	
	@Test
	public void testAll() {
		DefaultRetransmissionModel m = new DefaultRetransmissionModel(1, 8);
		
		assertEquals(1, m.current());
		assertEquals(1, m.next());
		assertEquals(2, m.current());
		assertEquals(2, m.next());
		assertEquals(4, m.current());
		assertEquals(4, m.next());
		assertEquals(8, m.current());
		assertEquals(8, m.next());
		assertEquals(8, m.current());
		assertEquals(8, m.next());
		m.reset();
		assertEquals(1, m.current());
		assertEquals(1, m.next());
		assertEquals(2, m.current());
		assertEquals(2, m.next());
		
		m = new DefaultRetransmissionModel(1, 3);
		assertEquals(1, m.next());
		assertEquals(2, m.next());
		assertEquals(3, m.next());
		assertEquals(3, m.current());
		assertEquals(3, m.next());
		assertEquals(3, m.current());
		
		m = new DefaultRetransmissionModel();
		assertEquals(1000, m.current());
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
