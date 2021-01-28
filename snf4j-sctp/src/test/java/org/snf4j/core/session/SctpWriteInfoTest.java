package org.snf4j.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.Test;
import org.snf4j.core.session.SctpWriteInfo;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;

public class SctpWriteInfoTest {

	void assertMessageInfo(MessageInfo expected, SctpWriteInfo value) {
		assertEquals(expected.address(), value.getAddress());
		assertEquals(expected.association(), value.getAssociation());
		assertEquals(expected.streamNumber(), value.getStreamNumber());
		assertEquals(expected.payloadProtocolID(), value.getProtocolID());
		assertEquals(expected.isComplete(), value.isComplete());
		assertEquals(expected.isUnordered(), value.isUnordered());
		assertEquals(expected.timeToLive(), value.getTimeToLive());
	}
	
	@Test
	public void testCreate() {
		MessageInfo m = MessageInfo.createOutgoing(null, 1);
		MessageInfo m2;
		SocketAddress a = new InetSocketAddress(0);
		
		SctpWriteInfo sm = SctpWriteInfo.create(1);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);

		m.payloadProtocolID(33);
		sm = SctpWriteInfo.create(1, 33);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		m.unordered(true);
		sm = SctpWriteInfo.create(1, 33, true);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		m = MessageInfo.createOutgoing(a, 2);
		sm = SctpWriteInfo.create(a, 2);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);

		m.payloadProtocolID(56);
		sm = SctpWriteInfo.create(a, 2, 56);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		m.unordered(true);		
		sm = SctpWriteInfo.create(a, 2, 56, true);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		m.timeToLive(3333);
		sm = SctpWriteInfo.create(m);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		
		m = MessageInfo.createOutgoing(new TestAssociation(10), a, 77)
				.payloadProtocolID(34)
				.unordered(true)
				.timeToLive(5555)
				.complete(false);
		sm = SctpWriteInfo.create(m);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
	}
	
	@Test
	public void testWrap() {
		SocketAddress a = new InetSocketAddress(0);
		MessageInfo m = MessageInfo.createOutgoing(new TestAssociation(10), a, 77)
				.payloadProtocolID(34)
				.unordered(true)
				.timeToLive(5555)
				.complete(false);
		
		SctpWriteInfo sm = SctpWriteInfo.wrap(m);
		assertMessageInfo(m, sm);
		assertTrue(m == sm.unwrap());
		
	}
	
	static class TestAssociation extends Association {
		protected TestAssociation(int id) {
			super(id, 0, 100);
		}
		
	}
}
