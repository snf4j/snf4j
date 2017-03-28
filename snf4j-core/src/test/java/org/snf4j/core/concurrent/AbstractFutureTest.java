package org.snf4j.core.concurrent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.snf4j.core.TestSession;

public class AbstractFutureTest {
	
	@Test
	public void testToString() {
		TestSession session = new TestSession("Session-1");
		SessionFutures futures = new SessionFutures(session);
		Exception cause = new Exception("Ex1");
		
		assertEquals("CancelledFuture[canceled]", new SessionFutures(null).getCancelledFuture().toString());
		assertEquals("Session-1-CancelledFuture[canceled]", futures.getCancelledFuture().toString());
		assertEquals("Session-1-SuccessfulFuture[successful]", futures.getSuccessfulFuture().toString());
		assertEquals("Session-1-FailedFuture[failed:"+cause+"]", futures.getFailedFuture(cause).toString());
		assertEquals("Session-1-EventFuture[incomplete,event=CREATED]", futures.getCreateFuture().toString());
		assertEquals("Session-1-WriteFuture[incomplete,bytes=100]", futures.getWriteFuture(100).toString());
	}
}
