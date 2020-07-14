package org.snf4j.core.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SessionExceptionTest {
	
	@Test
	public void testConstructor() {
		SessionException e = new SessionException();
		assertNull(e.getMessage());
		assertNull(e.getCause());
		
		e = new SessionException("Test1");
		assertEquals("Test1", e.getMessage());
		assertNull(e.getCause());
		
		Exception c = new Exception("Test2");
		e = new SessionException("Test1", c);
		assertEquals("Test1", e.getMessage());
		assertTrue(c == e.getCause());
	
		e = new SessionException(c);
		assertEquals("java.lang.Exception: Test2", e.getMessage());
		assertTrue(c == e.getCause());
		
	}
}
