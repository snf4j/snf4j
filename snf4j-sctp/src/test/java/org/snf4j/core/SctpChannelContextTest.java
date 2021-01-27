package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.junit.Test;

import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SendFailedNotification;
import com.sun.nio.sctp.ShutdownNotification;

public class SctpChannelContextTest {
	
	@Test
	public void testNotificationHandler() throws Exception {
		Field f = SctpChannelContext.class.getDeclaredField("HANDLER");
		TestSctpHandler handler = new TestSctpHandler();
		SctpSession session = new SctpSession(handler);
		
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		AbstractNotificationHandler<SctpSession> h = (AbstractNotificationHandler<SctpSession>) f.get(null);
		h.handleNotification(new TestPeerAddressChangeNotification(), session);
		assertEquals("N|TestPeerAddressChangeNotification|PEER_ADDRESS_CHANGE|", handler.getTrace());
		assertFalse(session.markedShutdown());
		h.handleNotification(new TestAssociationChangeNotification(AssocChangeEvent.COMM_UP), session);
		assertEquals("N|TestAssociationChangeNotification|ASSOCIATION_CHANGE|", handler.getTrace());
		assertFalse(session.markedShutdown());
		h.handleNotification(new TestAssociationChangeNotification(AssocChangeEvent.SHUTDOWN), session);
		assertEquals("N|TestAssociationChangeNotification|ASSOCIATION_CHANGE|", handler.getTrace());
		assertTrue(session.markedShutdown());
		h.handleNotification(new TestSendFailedNotification(), session);
		assertEquals("N|TestSendFailedNotification|SEND_FAILED|", handler.getTrace());
		h.handleNotification(new TestShutdownNotification(), session);
		assertEquals("N|TestShutdownNotification|SHUTDOWN|", handler.getTrace());
		h.handleNotification(new TestNotification(), session);
		assertEquals("N|TestNotification|GENERIC|", handler.getTrace());
		
	}
	
	static class TestNotification implements Notification {

		@Override
		public Association association() {
			return null;
		}
		
	}
	
	static class TestShutdownNotification extends ShutdownNotification {

		@Override
		public Association association() {
			return null;
		}
		
	}
	
	static class TestSendFailedNotification extends SendFailedNotification {

		@Override
		public Association association() {
			return null;
		}

		@Override
		public SocketAddress address() {
			return null;
		}

		@Override
		public ByteBuffer buffer() {
			return null;
		}

		@Override
		public int errorCode() {
			return 0;
		}

		@Override
		public int streamNumber() {
			return 0;
		}
		
	}
	
	static class TestAssociationChangeNotification extends AssociationChangeNotification {
		
		AssocChangeEvent event;
		
		TestAssociationChangeNotification(AssocChangeEvent event) {
			this.event = event;
		}
		
		@Override
		public Association association() {
			return null;
		}

		@Override
		public AssocChangeEvent event() {
			return event;
		}
		
	}
	
	static class TestPeerAddressChangeNotification extends PeerAddressChangeNotification {
		@Override
		public SocketAddress address() {
			return null;
		}

		@Override
		public Association association() {
			return null;
		}

		@Override
		public AddressChangeEvent event() {
			return null;
		}		
	}
}
