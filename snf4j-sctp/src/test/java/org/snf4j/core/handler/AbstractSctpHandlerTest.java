package org.snf4j.core.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.SctpSession;
import org.snf4j.core.TestSctpHandler;
import org.snf4j.core.TestSession;
import org.snf4j.core.session.DefaultSctpSessionConfig;

import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;

public class AbstractSctpHandlerTest {

	int count;
	
	Object msg;
	
	MessageInfo msgInfo;
	
	@Test
	public void testAll() {
		TestAbstractSctpHandler h = new TestAbstractSctpHandler();
		SctpSession s = new SctpSession(new TestSctpHandler()); 
		
		assertNull(h.getName());
		assertNull(h.getSession());
		h.setSession(s);
		assertTrue(s == h.getSession());
		assertTrue(DefaultSctpSessionConfig.class == h.getConfig().getClass());
		assertEquals(0, count);
		assertTrue(HandlerResult.CONTINUE == h.notification(null, null));
		h.read("");
		
		byte[] mb = new byte[10];
		MessageInfo mi = MessageInfo.createOutgoing(null, 1);
		
		h.read(mb, mi);
		assertEquals(1, count);
		assertTrue(msg == mb);
		assertTrue(msgInfo == mi);
		
		ByteBuffer mbb = ByteBuffer.allocate(10);
		mi = MessageInfo.createOutgoing(null, 1);
		h.read(mbb, mi);
		assertEquals(2, count);
		assertTrue(msg == mbb);
		assertTrue(msgInfo == mi);
		
		h = new TestAbstractSctpHandler("TTest");
		assertEquals("TTest", h.getName());
		assertTrue(DefaultSctpSessionConfig.class == h.getConfig().getClass());
		try {
			h.setSession(new TestSession());
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		
	}
	
	class TestAbstractSctpHandler extends AbstractSctpHandler {
		
		TestAbstractSctpHandler() {
			super();
		}
		
		TestAbstractSctpHandler(String name) {
			super(name);
		}

		@Override
		public void read(Object msg, MessageInfo msgInfo) {
			AbstractSctpHandlerTest.this.msg = msg;
			AbstractSctpHandlerTest.this.msgInfo = msgInfo;
			++count;
		}

	}
}
