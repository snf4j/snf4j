package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Arrays;

import org.junit.Test;
import org.snf4j.core.allocator.TestAllocator;

import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SendFailedNotification;
import com.sun.nio.sctp.ShutdownNotification;

public class SctpChannelContextTest extends SctpTest {
	
	@Test
	public void testRegisterPendingConnection() throws Exception {
		assumeSupported();
		startClientServer();

		c.session.write(nopb("1"), info(1,2));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)[1p2]|", s.getTrace());

		c.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}

	@Test
	public void testRegisterConnectedChannel() throws Exception {
		assumeSupported();
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.start();
		
		SctpChannel sc = SctpChannel.open();
		sc.connect(new InetSocketAddress(InetAddress.getByName(c.ip), c.port));
		c.start(sc);
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);	
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		assertEquals("SCR|SOP|RDY|", s.getTrace());
		
		c.session.write(nopb("1"), info(1,2));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)[1p2]|", s.getTrace());
		
		c.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}
	
	@Test
	public void testRegisterOpenChannel() throws Exception {
		assumeSupported();
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.start();
		
		SctpChannel sc = SctpChannel.open();
		c.start(sc);
		waitFor(50);
		assertEquals("", c.getTrace());
		assertEquals("", s.getTrace());
			
		sc.connect(new InetSocketAddress(InetAddress.getByName(c.ip), c.port));
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);	
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		assertEquals("SCR|SOP|RDY|", s.getTrace());
		
		c.session.write(nopb("1"), info(1,2));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)[1p2]|", s.getTrace());
		
		c.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}
	
	@Test
	public void testRegisterClosedChannel() throws Exception {
		assumeSupported();
		c = new SctpClient(PORT);
		SctpChannel sc = SctpChannel.open();
		c.start(sc);
		waitFor(50);
		assertEquals("", c.getTrace());
		
		SelectionKey k = sc.keyFor(c.loop.selector);
		SctpChannelContext ctx = (SctpChannelContext) k.attachment();
		sc.close();
		
		c.loop.execute(new Runnable() {

			@Override
			public void run() {
				try {
					ctx.completeRegistration(c.loop, k, sc);
				} catch (Exception e) {
				}
			}			
		});
		
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", c.getTrace());
		c.stop(TIMEOUT);
		
		c = new SctpClient(PORT);
		SctpChannel sc2 = SctpChannel.open();
		c.start(sc2);
		waitFor(50);
		assertEquals("", c.getTrace());
		
		SelectionKey k2 = sc2.keyFor(c.loop.selector);
		SctpChannelContext ctx2 = (SctpChannelContext) k2.attachment();
		TestSctpChannel tsc = new TestSctpChannel();
		tsc.remoteAddressesException = new ClosedChannelException();
		sc2.close();

		c.loop.execute(new Runnable() {

			@Override
			public void run() {
				try {
					ctx2.completeRegistration(c.loop, k2, tsc);
				} catch (Exception e) {
				}
			}			
		});
		
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", c.getTrace());
		c.stop(TIMEOUT);
		
	}	

	@Test
	public void testWriteSpinCount() throws Exception {
		assumeSupported();
		startClientServer();
		
		byte[] payload = new byte[1000];
		Arrays.fill(payload, (byte)'1');
		byte[] data = nopb(new String(payload));
		
		SctpSession session = c.session;
		ImmutableSctpMessageInfo msgInfo = info(0);
		session.suspendWrite();
		for (int i=0; i<32; ++i) {
			session.writenf(data, msgInfo);
		}
		session.resumeWrite();
		waitFor(500);
		assertEquals("DS|DS|", c.getTrace());
		assertEquals(32, countRDNOP(s.getTrace(), payload));
		c.stop(TIMEOUT);
		
		c = new SctpClient(PORT);
		c.maxWriteSpinCount = 1;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		session = c.session;
		session.suspendWrite();
		for (int i=0; i<32; ++i) {
			session.write(data, msgInfo);
		}
		session.resumeWrite();
		waitFor(500);
		assertEquals("DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|", c.getTrace());
		assertEquals(32, countRDNOP(s.getTrace(), payload));
		c.stop(TIMEOUT);
		
		c = new SctpClient(PORT);
		c.maxWriteSpinCount = 16;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		s.waitForSessionReady(TIMEOUT);
		s.getTrace();
		session = c.session;
		session.suspendWrite();
		session.write(nopb("1234"), msgInfo);
		TestSelectionKey key = new TestSelectionKey(new TestSctpChannel());
		SctpChannelContext ctx = (SctpChannelContext) session.channel.keyFor(c.loop.selector).attachment();
		Method m = SctpChannelContext.class.getDeclaredMethod("handleWriting", SelectorLoop.class, SctpSession.class, SelectionKey.class, int.class);
		m.setAccessible(true);
		assertEquals(new Integer(0), m.invoke(ctx, c.loop, session, key, 1));
		session.resumeWrite();
		waitFor(100);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1234)|", s.getTrace());
		
		c.closeInEvent = EventType.DATA_SENT;
		c.closeType = StoppingType.DIRTY;
		session.suspendWrite();
		for (int i=0; i<15; ++i) {
			session.write(data, msgInfo);
		}
		session.resumeWrite();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getTrace());
		waitFor(500);
		assertEquals(15, countRDNOP(s.getTrace(), payload));
		c.stop(TIMEOUT);
		
		c = new SctpClient(PORT);
		c.maxWriteSpinCount = 16;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		s.waitForSessionReady(TIMEOUT);
		s.getTrace();
		session = c.session;
		session.suspendWrite();
		session.writenf(nopb("1234"), msgInfo);
		c.writeInEvent = EventType.DATA_SENT;
		c.packetToWriteInEvent = nop("5678");
		session.resumeWrite();
		waitFor(100);
		assertEquals("DS|DS|", c.getTrace());
		assertEquals("DR|NOP(1234)|DR|NOP(5678)|", s.getTrace());
	}
	
	@Test
	public void testHandleWithNotSelectedKey() throws Exception {
		assumeSupported();
		startClientServer();
	
		SctpSession session = c.session;
		SelectionKey k = getKey(c);
		SctpChannelContext ctx = (SctpChannelContext) k.attachment();
		TestSelectionKey tsk = new TestSelectionKey(session.channel);
		
		ctx.handle(c.loop, tsk);
		waitFor(50);
		assertEquals("", c.getTrace());
		assertEquals("", s.getTrace());
		
		session.writenf(nopb("1"), info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)|", s.getTrace());
	}
	
	@Test
	public void testHandleReadingException() throws Exception {
		assumeSupported();
		startClientServer();
		
		SctpSession session = c.session;
		session.writenf(nopb("44"), info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(44)|", s.getTrace());

		SctpChannelContext ctx = (SctpChannelContext) getKey(c).attachment();
		TestSctpChannel sc = new TestSctpChannel();
		TestSelectionKey k = new TestSelectionKey(sc);
		
		sc.receiveException = new IOException();
		
		c.loop.execute(new Runnable() {

			@Override
			public void run() {
				Method m;
				try {
					m = SctpChannelContext.class.getDeclaredMethod("handleReading", 
							SelectorLoop.class, 
							SctpSession.class, 
							SelectionKey.class);
					m.setAccessible(true);
					m.invoke(ctx, c.loop, session, k);
				} catch (Exception e) {
				}
			}
		});
		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}

	@Test
	public void testHandleReadingZeroBytes() throws Exception {
		assumeSupported();
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.start();
		c.allocator = new TestAllocator(false, true);
		c.optimizeCopying = true;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		setAllocator(c.allocator);
		
		assertAllocator(1,1,0);
		SctpChannelContext ctx = (SctpChannelContext) getKey(c).attachment();
		TestSctpChannel sc = new TestSctpChannel();
		TestSelectionKey k = new TestSelectionKey(sc);
		Method m = SctpChannelContext.class.getDeclaredMethod("handleReading", 
				SelectorLoop.class, 
				SctpSession.class, 
				SelectionKey.class);
		
		sc.msgInfo = MessageInfo.createOutgoing(null, 0);
		m.setAccessible(true);
		m.invoke(ctx, c.loop, c.session, k);
		assertAllocator(2,2,0);
		
	}
	
	@Test
	public void testHandleWritingException() throws Exception {
		assumeSupported();
		startClientServer();
		
		SctpSession session = c.session;
		session.writenf(nopb("44"), info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(44)|", s.getTrace());
		
		SctpChannelContext ctx = (SctpChannelContext) getKey(c).attachment();
		TestSctpChannel sc = new TestSctpChannel();
		TestSelectionKey k = new TestSelectionKey(sc);
		
		sc.sendException = new IOException();
		session.suspendWrite();
		session.writenf(nopb("1"), info(0));
		
		c.loop.execute(new Runnable() {

			@Override
			public void run() {
				Method m;
				try {
					m = SctpChannelContext.class.getDeclaredMethod("handleWriting", 
							SelectorLoop.class, 
							SctpSession.class, 
							SelectionKey.class,
							int.class);
					m.setAccessible(true);
					Integer i = (Integer) m.invoke(ctx, c.loop, session, k, 10);
					c.trace("Spin=" + i);
				} catch (Exception e) {
				}
			}
		});
		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|Spin=0|SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}

	@Test
	public void testHandleWritingBufferRelease() throws Exception {
		assumeSupported();
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.start();
		c.allocator = new TestAllocator(false, true);
		c.start();
		waitForReady(TIMEOUT);
		setAllocator(c.allocator);
		SctpSession session = c.session;
		
		assertAllocator(1,0,1);
		session.writenf(nopb("12345"), info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(12345)|", s.getTrace());
		assertAllocator(2,1,1);
		c.stop(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		clearTraces();
		
		c = new SctpClient(PORT);
		c.allocator = new TestAllocator(false, true);
		c.optimizeCopying = true;
		c.start();
		waitForReady(TIMEOUT);
		setAllocator(c.allocator);
		session = c.session;		
		assertAllocator(1,1,0);
		session.writenf(nopb("12345"), info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(12345)|", s.getTrace());
		assertAllocator(1,1,0);
	}
	
	void assertStats(SctpSession session, long read, long written) {
		assertEquals(read, session.getReadBytes());
		assertEquals(written, session.getWrittenBytes());
	}
	
	@Test
	public void testStatistics() throws Exception {
		assumeSupported();
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		c.traceDataLength = true;
		s.traceDataLength = true;
		s.start();
		c.start();
		waitForReady(TIMEOUT);

		assertStats(c.session, 0, 0);
		assertStats(s.session, 0, 0);
		c.session.writenf(nopb(""), info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(3)|", c.getTrace());
		assertEquals("DR(3)|NOP()|", s.getTrace());
		assertStats(c.session, 0, 3);
		assertStats(s.session, 3, 0);
		
		s.session.suspendWrite();
		s.session.writenf(nopb("1234"), info(0));
		s.session.writenf(nopb("56"), info(0));
		s.session.writenf(nopb("789"), info(0));
		s.session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(500);
		assertEquals("DS(18)|", s.getTrace());
		assertEquals("DR(7)|NOP(1234)|DR(5)|NOP(56)|DR(6)|NOP(789)|", c.getTrace());
		assertStats(c.session, 18, 3);
		assertStats(s.session, 3, 18);
		c.stop(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		clearTraces();
		
		c = new SctpClient(PORT);
		c.throughputCalculationInterval = 500;
		c.start();
		waitForReady(TIMEOUT);
		
		assertTrue(0.0 == c.session.getReadBytesThroughput());
		assertTrue(0.0 == c.session.getWrittenBytesThroughput());
		c.session.writenf(nopb("123456789"), info(0));
		c.waitForDataSent(TIMEOUT);
		waitFor(600);
		c.session.writenf(nopb("123456789"), info(0));
		c.waitForDataSent(TIMEOUT);
		assertTrue(0.0 == c.session.getReadBytesThroughput());
		assertTrue(0.0 < c.session.getWrittenBytesThroughput());
		
		s.session.writenf(nopb("123456789"), info(0));
		c.waitForDataRead(TIMEOUT);
		waitFor(500);
		s.session.writenf(nopb("123456789"), info(0));
		c.waitForDataRead(TIMEOUT);
		assertTrue(0.0 < c.session.getReadBytesThroughput());
	}
	
	void assertToString(String expected, String value) {
		assertEquals(TestSctpChannel.class.getName() + expected, value);
	}
	
	@Test
	public void testToString() {
		ChannelContext<?> ctx = new SctpChannelContext(null);
		TestSctpChannel sc = new TestSctpChannel();
		
		InetSocketAddress a1 = address(2001);
		InetSocketAddress a2 = address(2002);
		InetSocketAddress a3 = address(2003);
		InetSocketAddress a4 = address(2004);
		
		assertNull(ctx.toString((SelectableChannel)null));
		assertToString("[not-connected local=not-bound]", ctx.toString(sc));
		sc.connectionPending = true;
		assertToString("[connection-pending local=not-bound]", ctx.toString(sc));		
		sc.localAddresses.add(a1);
		assertToString("[connection-pending local="+a1+"]", ctx.toString(sc));		
		sc.localAddresses.add(a2);
		assertToString("[connection-pending local="+a1+","+a2+"]", ctx.toString(sc));
		sc.remoteAddresses.add(a3);
		assertToString("[connected local="+a1+","+a2+" remote="+a3+"]", ctx.toString(sc));
		sc.remoteAddresses.add(a4);
		assertToString("[connected local="+a1+","+a2+" remote="+a3+","+a4+"]", ctx.toString(sc));
		sc.remoteAddressesException = new IOException();
		assertToString("[local="+a1+","+a2+" remote=unknown]", ctx.toString(sc));
		sc.remoteAddressesException = null;
		sc.localAddressesException = new IOException();
		assertToString("[connected local=unknown remote="+a3+","+a4+"]", ctx.toString(sc));
		sc.remoteAddressesException = new IOException();
		assertToString("[local=unknown remote=unknown]", ctx.toString(sc));
	}
	
	@Test
	public void testWrap() {
		TestSctpHandler handler = new TestSctpHandler();
		SctpSession session = new SctpSession(handler);
		
		SctpChannelContext ctx = new SctpChannelContext(session);
		ChannelContext<SctpSession> ctx2  = ctx.wrap(session);
		assertFalse(ctx2 == ctx);
		assertTrue(ctx2.getSession() == session);
		assertTrue(ctx2.getClass() == SctpChannelContext.class);
	}
	
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
