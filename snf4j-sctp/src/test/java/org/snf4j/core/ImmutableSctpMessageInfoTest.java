package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.Test;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;

public class ImmutableSctpMessageInfoTest {
	
	void assertMessageInfo(MessageInfo expected, ImmutableSctpMessageInfo value) {
		assertEquals(expected.address(), value.address());
		assertEquals(expected.association(), value.association());
		assertEquals(expected.streamNumber(), value.streamNumber());
		assertEquals(expected.payloadProtocolID(), value.payloadProtocolID());
		assertEquals(expected.isComplete(), value.isComplete());
		assertEquals(expected.isUnordered(), value.isUnordered());
		assertEquals(expected.timeToLive(), value.timeToLive());
		assertEquals(0, value.bytes());
	}
	
	@Test
	public void testCreate() {
		MessageInfo m = MessageInfo.createOutgoing(null, 1);
		MessageInfo m2;
		SocketAddress a = new InetSocketAddress(0);
		
		ImmutableSctpMessageInfo sm = ImmutableSctpMessageInfo.create(1);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);

		m.payloadProtocolID(33);
		sm = ImmutableSctpMessageInfo.create(1, 33);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		m.unordered(true);
		sm = ImmutableSctpMessageInfo.create(1, 33, true);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		m = MessageInfo.createOutgoing(a, 2);
		sm = ImmutableSctpMessageInfo.create(a, 2);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);

		m.payloadProtocolID(56);
		sm = ImmutableSctpMessageInfo.create(a, 2, 56);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		m.unordered(true);		
		sm = ImmutableSctpMessageInfo.create(a, 2, 56, true);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		m.timeToLive(3333);
		sm = ImmutableSctpMessageInfo.create(m);
		assertMessageInfo(m, sm);
		m2 = sm.unwrap();
		assertFalse(m2 == m);
		assertMessageInfo(m2, sm);
		
		
		m = MessageInfo.createOutgoing(new TestAssociation(10), a, 77)
				.payloadProtocolID(34)
				.unordered(true)
				.timeToLive(5555)
				.complete(false);
		sm = ImmutableSctpMessageInfo.create(m);
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
		
		ImmutableSctpMessageInfo sm = ImmutableSctpMessageInfo.wrap(m);
		assertMessageInfo(m, sm);
		assertTrue(m == sm.unwrap());
		
	}
	
	void assertUnsupported(ImmutableSctpMessageInfo msInfo) {
		int i;
		boolean b;
		long l;
		
		i = msInfo.streamNumber();
		try {
			msInfo.streamNumber(i+1);
			fail();
		}
		catch (UnsupportedOperationException e) {
		}
		assertEquals(i, msInfo.streamNumber());
		
		i = msInfo.payloadProtocolID();
		try {
			msInfo.payloadProtocolID(i+1);
			fail();
		}
		catch (UnsupportedOperationException e) {
		}
		assertEquals(i, msInfo.payloadProtocolID());
		
		b = msInfo.isComplete();
		try {
			msInfo.complete(!b);
			fail();
		}
		catch (UnsupportedOperationException e) {
		}
		assertEquals(b, msInfo.isComplete());

		b = msInfo.isUnordered();
		try {
			msInfo.unordered(!b);
			fail();
		}
		catch (UnsupportedOperationException e) {
		}
		assertEquals(b, msInfo.isUnordered());

		l = msInfo.timeToLive();
		try {
			msInfo.timeToLive(l);
			fail();
		}
		catch (UnsupportedOperationException e) {
		}
		assertEquals(l, msInfo.timeToLive());
			
	}
	
	@Test
	public void testUnsupportedMethods() {
		MessageInfo m = MessageInfo.createOutgoing(null, 77);
		
		ImmutableSctpMessageInfo sm = ImmutableSctpMessageInfo.wrap(m);
		assertUnsupported(sm);
		
		sm = ImmutableSctpMessageInfo.create(1);
		assertUnsupported(sm);
	}
	
	static class TestAssociation extends Association {
		protected TestAssociation(int id) {
			super(id, 0, 100);
		}
		
	}
}
