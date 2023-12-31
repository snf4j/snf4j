/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * -----------------------------------------------------------------------------
 */
package org.snf4j.tls;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.snf4j.core.engine.HandshakeStatus.NEED_WRAP;
import static org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP;
import static org.snf4j.core.engine.HandshakeStatus.NEED_TASK;
import org.junit.Test;
import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.engine.Status;
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.tls.alert.BadCertificateAlert;
import org.snf4j.tls.alert.BadRecordMacAlert;
import org.snf4j.tls.alert.CertificateRequiredAlert;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.alert.HandshakeFailureAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.NoApplicationProtocolAlert;
import org.snf4j.tls.alert.RecordOverflowAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.engine.CertificateCriteria;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.engine.EngineStateListener;
import org.snf4j.tls.engine.EngineTest;
import org.snf4j.tls.engine.FlightController;
import org.snf4j.tls.engine.HandshakeAggregator;
import org.snf4j.tls.engine.HandshakeController;
import org.snf4j.tls.engine.HandshakeEngine;
import org.snf4j.tls.engine.IHandshakeEngine;
import org.snf4j.tls.engine.TestHandshakeHandler;
import org.snf4j.tls.engine.TicketInfo;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.record.Cryptor;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.Record;
import org.snf4j.tls.session.ISession;

public class TLSEngineTest extends EngineTest {

	ByteBuffer in, out;
	
	TLSEngine cli, srv;
	
	private static final String PEER_HOST = "snf4j.org";
	
	private static final int PEER_PORT = 100;
	
	@Override
	public void before() throws Exception {
		super.before();
		in = ByteBuffer.allocate(100000);
		out = ByteBuffer.allocate(100000);
	}
	
	static IHandshakeEngine handshaker(TLSEngine e) throws Exception {
		Field f = TLSEngine.class.getDeclaredField("handshaker");
		
		f.setAccessible(true);
		return (IHandshakeEngine) f.get(e);
	}
	
	static HandshakeAggregator aggregator(TLSEngine e) throws Exception {
		Field f = TLSEngine.class.getDeclaredField("aggregator");
		
		f.setAccessible(true);
		return (HandshakeAggregator) f.get(e);
	}
	
	static void handshakeStatus(TLSEngine e, HandshakeStatus status) throws Exception {
		Field f = TLSEngine.class.getDeclaredField("status");
		
		f.setAccessible(true);
		f.set(e, status);
	}
	
	void clear() {
		in.clear().flip();
		out.clear();
	}

	void clear(byte[] data) {
		in.clear();
		in.put(data).flip();
		out.clear();
	}
	
	void flip() {
		ByteBuffer tmp = in;
		in = out;
		out = tmp;
		in.flip();
		out.clear();
	}

	byte[] out() {
		ByteBuffer dup = out.duplicate();
		dup.flip();
		byte[] data = new byte[dup.remaining()];
		dup.get(data);
		return data;
	}
	
	void assertInOut(int inLen, int outLen) {
		assertEquals(inLen, in.remaining());
		assertEquals(outLen, out.position());
	}
	
	void assertClosing(TLSEngine e, boolean in, boolean out) {
		String expected = "inClosed="+in+",outClosed="+out;
		assertEquals(expected, "inClosed="+e.isInboundDone()+",outClosed="+e.isOutboundDone());
	}
	
	void assertClosed(TLSEngine e) throws Exception {
		clear(new byte[100]);
		assertInOut(100,0);
		IEngineResult r = e.wrap(in, out);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, r.getHandshakeStatus());
		assertSame(Status.CLOSED, r.getStatus());
		assertEquals(0, r.bytesConsumed());
		assertEquals(0, r.bytesProduced());
		assertInOut(100,0);

		r = e.unwrap(in, out);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, r.getHandshakeStatus());
		assertSame(Status.CLOSED, r.getStatus());
		assertEquals(0, r.bytesConsumed());
		assertEquals(0, r.bytesProduced());
		assertInOut(100,0);
		e.cleanup();
		e.closeOutbound();
	}
	
	void assertClosed(TLSEngine cli, TLSEngine srv) throws Exception {
		assertClosed(cli);
		assertClosed(srv);
	}
	
	void assertAppData(TLSEngine cli, TLSEngine srv, int... sizes) throws Exception {
		for (int size: sizes) {
			byte[] data = random(size);
			clear(data);
			cli.wrap(in, out);
			flip();
			srv.unwrap(in, out);
			out.flip();
			byte[] data2 = new byte[out.remaining()];
			out.get(data2);
			assertArrayEquals(data, data2);
			
			clear(data);
			srv.wrap(in, out);
			flip();
			cli.unwrap(in, out);
			out.flip();
			data2 = new byte[out.remaining()];
			out.get(data2);
			assertArrayEquals(data, data2);
		}
	}
	
	void prepareTickets(long... maxSizes) throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.init();
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.init();
		srv.beginHandshake();
		
		handler.ticketInfos = new TicketInfo[maxSizes.length];
		for (int i=0; i<maxSizes.length; ++i) {
			long maxSize = maxSizes[i];
			handler.ticketInfos[i] = maxSize == -1 ? new TicketInfo() : new TicketInfo(maxSize);
		}
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		flip();
		fc.fly(srv, in, out);
		flip();
		fc.fly(cli, in, out);
		flip();
		fc.fly(srv, in, out);
		flip();
		fc.fly(cli, in, out);
		assertInOut(0,0);
	}

	void prepareEngines() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.init();
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.init();
		srv.beginHandshake();		
	}
	
	void prepareConnection() throws Exception {
		prepareEngines();
		
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		flip();
		fc.fly(srv, in, out);
		flip();
		fc.fly(cli, in, out);
		flip();
		fc.fly(srv, in, out);
		flip();
		fc.fly(cli, in, out);
		assertInOut(0,0);
	}
	
	static Encryptor[] encryptors(TLSEngine e) throws Exception {
		Field f = TLSEngine.class.getDeclaredField("listener");
		f.setAccessible(true);
		EngineStateListener listener = (EngineStateListener) f.get(e);
		f = EngineStateListener.class.getDeclaredField("encryptors");
		f.setAccessible(true);
		return (Encryptor[]) f.get(listener);
	}
	
	@Test
	public void testEarlyData() throws Exception {
		prepareTickets(111);
		
		byte[] data = random(111);
		handler.earlyData.add(data);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|Ued(111)|OK:uu|", fc.trace());
		assertArrayEquals(data, fc.earlyData);
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
		
		handler.earlyData.add(split(data, 50)[0]);
		handler.earlyData.add(split(data, 50)[1]);
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|Ued(111)|OK:uu|", fc.trace());
		assertArrayEquals(data, fc.earlyData);
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);	
		assertAppData(cli,srv,100,1000);
	}

	@Test
	public void testEarlyDataWithUnexpectedMessage() throws Exception {
		prepareTickets(100000);
		
		byte[] data = random(18110);
		handler.earlyData.add(data);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		cli.wrap(in, out);
		cli.wrap(in, out);
		out.put(bytes(21,3,3,0,2,1,1));
		cli.wrap(in, out);
		
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
	}
	
	@Test
	public void testEarlyDataInCompatibilityMode() throws Exception {
		prepareTickets(111);
		
		byte[] data = random(111);
		handler.earlyData.add(data);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.compatibilityMode(true)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|U|OK:uu|Ued(111)|OK:uu|", fc.trace());
		assertArrayEquals(data, fc.earlyData);
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
		
		handler.earlyData.add(split(data, 50)[0]);
		handler.earlyData.add(split(data, 50)[1]);
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|Ued(111)|OK:uu|", fc.trace());
		assertArrayEquals(data, fc.earlyData);
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);	
		assertAppData(cli,srv,100,1000);
	}
	
	@Test
	public void testEarlyDataWithoutEarlyDataTicket() throws Exception {
		prepareTickets(-1);
		
		byte[] data = random(111);
		handler.earlyData.add(data);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		assertNull(fc.earlyData);
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
	}
	
	@Test
	public void testEarlyDataWithoutSrvTicket() throws Exception {
		prepareTickets(1000);
		
		byte[] data = random(111);
		handler.earlyData.add(data);
		handler.padding = 100;
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				new TestHandshakeHandler());
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|U|OK:uu|", fc.trace());
		assertNull(fc.earlyData);
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
	}

	@Test
	public void testEarlyDataWithHRR() throws Exception {
		prepareTickets(1000);
		
		byte[] data = random(111);
		handler.earlyData.add(data);
		handler.padding = 100;
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.namedGroups(NamedGroup.FFDHE3072)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:uu|U|OK:uu|", fc.trace());
		assertNull(fc.earlyData);
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
	}
	
	@Test
	public void testEarlyDataWithoutCliTicket() throws Exception {
		byte[] data = random(111);
		handler.earlyData.add(data);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				new TestHandshakeHandler());
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		assertNull(fc.earlyData);
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
	}
	
	@Test
	public void testEarlyDataTooBig() throws Exception {
		prepareTickets(110);
		
		byte[] data = random(111);
		handler.earlyData.add(data);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Early data is too big", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
	}

	@Test
	public void testEarlyDataTooBigInChunks() throws Exception {
		prepareTickets(110);
		
		byte[] data = random(111);
		handler.earlyData.add(split(data, 50)[0]);
		handler.earlyData.add(split(data, 50)[1]);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Early data is too big", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
	}

	@Test
	public void testEarlyDataRejected() throws Exception {
		prepareTickets(110);
		
		byte[] data = random(111);
		handler.earlyData.add(data);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}

		handler.earlyData.add(data);
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|U|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
	}

	@Test
	public void testEarlyDataRejectedAndTooBig() throws Exception {
		prepareTickets(110);
		
		byte[] data = random(111);
		handler.earlyData.add(data);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}

		data = random(1000);
		handler.earlyData.add(data);
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
	}

	@Test
	public void testEarlyDataThatIsUnexpected() throws Exception {
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();
		
		clear();
		out.put(bytes(23,3,3,0,10,1,2,3,4,5,6,7,8,9,10));
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
	}
	
	@Test
	public void testHandshakeWithNoTask() throws Exception {
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
	}
	
	@Test
	public void testHandshakeWithAllTasks() throws Exception {
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:tt|T|w|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:tt|T|t|T|w|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:tt|T|u|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
	}

	@Test
	public void testHandshakeWithTask() throws Exception {
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:tt|T|w|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:tt|T|u|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
	}
	
	@Test
	public void testHandshakeWithFragmentation() throws Exception {
		prepareEngines();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		fc.trace();
		handler.certificateSelector.certNames = new String[100];
		Arrays.fill(handler.certificateSelector.certNames, "rsasha256");
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:ww|W|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:uu|U|OK:uu|U|OK:uu|U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
	}
	
	@Test
	public void testAppDataNoPadding() throws Exception {
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		flip();
		fc.fly(srv, in, out);
		flip();
		fc.fly(cli, in, out);
		flip();
		fc.fly(srv, in, out);
		flip();
		fc.fly(cli, in, out);
		assertInOut(0,0);
		assertAppData(cli,srv,1,100,1000,16384);
		
		byte[] data = random(16385);
		clear(data);
		cli.wrap(in, out);
		assertEquals(16384,in.position());
		assertEquals(16384+1+5+16,out.position());
		cli.wrap(in, out);
		assertEquals(16385,in.position());
		assertEquals(16384+1+5+16+1+1+5+16,out.position());
		flip();
		srv.unwrap(in, out);
		assertEquals(16384,out.position());
		srv.unwrap(in, out);
		assertEquals(16385,out.position());
		assertEquals(0, in.remaining());
		out.flip();
		byte[] data2 = new byte[out.remaining()];
		out.get(data2);
		assertArrayEquals(data, data2);
	}
	
	@Test
	public void testAppDataPadding() throws Exception {
		handler.padding = 16384;
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		flip();
		fc.fly(srv, in, out);
		flip();
		fc.fly(cli, in, out);
		flip();
		fc.fly(srv, in, out);
		flip();
		fc.fly(cli, in, out);
		assertInOut(0,0);
		assertAppData(cli,srv,1,100,1000,16384);

		int[] paddings = new int[] {16384, 100, 15384};
		int[] sizes = new int[] {1, 10, 1000};
		
		for (int i=0; i<paddings.length; ++i) {
			int size = sizes[i];
			int padding = paddings[i];
			byte[] data, data2;
			
			data = random(size);
			handler.padding = padding;
			clear(data);
			cli.wrap(in, out);
			assertEquals(size,in.position());
			assertEquals(Math.min(16384, size+padding)+1+5+16,out.position());
			flip();
			srv.unwrap(in, out);
			assertEquals(size,out.position());
			out.flip();
			data2 = new byte[out.remaining()];
			out.get(data2);
			assertArrayEquals(data, data2);
		}
	}

	@Test
	public void testClosing() throws Exception {
		prepareConnection();

		assertClosing(cli,false,false);
		cli.closeOutbound();
		assertClosing(cli,false,false);
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertClosing(cli,false,true);
		assertEquals("W|C:uu|", fc.trace());
		flip();
		assertClosing(srv,false,false);
		fc.fly(srv, in, out);
		assertClosing(srv,true,true);
		assertEquals("U|C:ww|W|C:nhnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertClosing(cli,true,true);
		assertEquals("U|C:nhnh|NH|", fc.trace());
		assertInOut(0,0);
		assertClosed(cli,srv);
	}

	@Test
	public void testClosingAlertDuringFragmentation() throws Exception {
		prepareEngines();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		fc.trace();
		handler.certificateSelector.certNames = new String[100];
		Arrays.fill(handler.certificateSelector.certNames, "rsasha256");
		flip();
		srv.unwrap(in, out);
		clear();
		srv.wrap(in, out);
		srv.wrap(in, out);
		srv.closeOutbound();
		
		int i=0;
		for (; i<16384+1+16+5; ++i) {
			out.limit(out.position()+i);
			assertSame(Status.BUFFER_OVERFLOW, srv.wrap(in, out).getStatus());
		}
		out.limit(out.position()+i);
		assertSame(Status.OK, srv.wrap(in, out).getStatus());
		out.limit(out.capacity());
		
		fc.fly(srv, in, out);
		assertEquals("W|OK:ww|W|OK:ww|W|C:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:uu|U|OK:uu|U|OK:uu|U|OK:uu|U|C:ww|W|C:nhnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|C:nhnh|NH|", fc.trace());
		assertInOut(0,0);
		assertClosed(cli,srv);
	}

	@Test(expected=UnexpectedMessageAlert.class)
	public void testChangeCipherSpecAfterHandshake() throws Exception {
		prepareConnection();
		
		clear(bytes(20,3,3, 0, 1, 1));
		try {
			srv.unwrap(in, out);
		}
		catch (TLSException e) {
			throw e.getAlert();
		}
	}
	
	@Test
	public void testClosingInbound() throws Exception {
		prepareConnection();

		assertClosing(cli,false,false);
		try {
			cli.closeInbound();
			fail();
		}
		catch(SessionIncidentException e) {
		}
		assertClosing(cli,true,false);
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertClosing(cli,true,true);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}
		assertClosing(srv,true,true);
		
		prepareEngines();
		
		try {
			cli.closeInbound();
			fail();
		}
		catch(SessionIncidentException e) {
		}
		assertClosing(cli,true,false);
		cli.closeInbound();
		clear();
		fc.fly(cli, in, out);
		assertClosing(cli,true,true);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}

		assertClosing(srv,true,true);
	}
	
	@Test
	public void testMinBufferSizes() throws Exception {
		prepareConnection();
		assertEquals(5 + 16384 + 1 + 255, cli.getMinNetworkBufferSize());
		assertEquals(16384 + 1, cli.getMinApplicationBufferSize());
		
		ByteBuffer net = ByteBuffer.allocate(cli.getMinNetworkBufferSize());
		ByteBuffer app = ByteBuffer.allocate(cli.getMinApplicationBufferSize());
		
		cli.wrap(app, net);
		assertEquals(16384, app.position());
		assertEquals(5 + 16384 + 1 + 16, net.position());
		
		net.flip();
		app.clear();
		srv.unwrap(net, app);
		assertFalse(net.hasRemaining());
		assertEquals(16384, app.position());
	}

	@Test
	public void testMaxBufferSizes() throws Exception {
		prepareConnection();
		assertEquals(cli.getMinApplicationBufferSize(), cli.getMaxApplicationBufferSize());
		assertEquals(cli.getMinNetworkBufferSize(), cli.getMaxNetworkBufferSize());	
		
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler, 150, 200);
		cli.init();
		cli.beginHandshake();
		assertEquals(33290, cli.getMaxNetworkBufferSize());
		assertEquals(24577, cli.getMaxApplicationBufferSize());

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler, 99, 200);
		cli.init();
		cli.beginHandshake();
		assertEquals(33290, cli.getMaxNetworkBufferSize());
		assertEquals(cli.getMinApplicationBufferSize(), cli.getMaxApplicationBufferSize());

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler, -1, 200);
		cli.init();
		cli.beginHandshake();
		assertEquals(33290, cli.getMaxNetworkBufferSize());
		assertEquals(cli.getMinApplicationBufferSize(), cli.getMaxApplicationBufferSize());

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler, 150, 99);
		cli.init();
		cli.beginHandshake();
		assertEquals(cli.getMinNetworkBufferSize(), cli.getMaxNetworkBufferSize());
		assertEquals(24577, cli.getMaxApplicationBufferSize());
		
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler, 150, -1);
		cli.init();
		cli.beginHandshake();
		assertEquals(cli.getMinNetworkBufferSize(), cli.getMaxNetworkBufferSize());
		assertEquals(24577, cli.getMaxApplicationBufferSize());
	}
	
	@Test
	public void testClosingAlertRcvAppData() throws Exception {
		prepareConnection();
		cli.beginHandshake();
		
		clear(random(100));
		cli.wrap(in, out);
		flip();
		in.put(10, (byte) (in.get(10)+1));
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadRecordMacAlert);
		}

		assertClosing(srv,true,false);
		FlightController fc = new FlightController();
		clear();
		fc.fly(srv, in, out);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		assertClosing(srv,true,true);
		flip();
		try {
			fc.fly(cli, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadRecordMacAlert);
		}
		assertClosing(cli,true,true);
		assertInOut(0,0);
		assertClosed(cli,srv);
	}

	@Test
	public void testClosingAlertSndAppData() throws Exception {
		prepareConnection();

		clear(random(100));
		handler.paddingException = new RuntimeException();
		try {
			cli.wrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}
		handler.paddingException = null;
		assertClosing(cli,true,false);
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertClosing(cli,true,true);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}
		assertClosing(cli,true,true);
		assertInOut(0,0);
		assertClosed(cli,srv);
	}

	@Test
	public void testClosingAlertSndClientHello
	() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.namedGroups(new NamedGroup(1000) {})
				.build(), 
				handler);
		cli.beginHandshake();
		cli.beginHandshake();
		assertSame(HandshakeStatus.NEED_WRAP, cli.getHandshakeStatus());

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();
		assertSame(HandshakeStatus.NEED_UNWRAP, srv.getHandshakeStatus());

		clear();
		try {
			cli.wrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}
		cli.beginHandshake();
		assertClosing(cli,true,false);
		assertInOut(0,0);
		
		FlightController fc = new FlightController();
		fc.fly(cli, in, out);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		assertClosing(cli,true,true);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}
		assertClosing(srv,true,true);
		assertInOut(0,0);
		assertClosed(cli,srv);
	}
	
	@Test
	public void testClosingAlertRcvClientHello() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);

		clear();
		cli.wrap(in, out);
		flip();
		in.put(8, (byte) (in.get(8)-1));
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof DecodeErrorAlert);
		}
		assertClosing(srv,true,false);
		FlightController fc = new FlightController();
		clear();
		fc.fly(srv, in, out);
		assertClosing(srv,true,true);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		flip();
		try {
			fc.fly(cli, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof DecodeErrorAlert);
		}
		assertClosing(srv,true,true);
		assertInOut(0,0);
		assertClosed(cli,srv);
	}

	@Test
	public void testClosingAlertSndServerHello() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		in.put(8,(byte) (in.get(8)-1));
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof DecodeErrorAlert);
		}
		assertClosing(srv,true,false);
		clear();
		fc.fly(srv, in, out);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		assertClosing(srv,true,true);
		flip();
		try {
			cli.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof DecodeErrorAlert);
		}
		assertClosing(cli,true,true);
		assertInOut(0,0);
		assertClosed(cli,srv);
	}
	
	@Test
	public void testClosingAlertRcvServerHello() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		in.put(8,(byte) (in.get(8)-1));	
		try {
			cli.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof DecodeErrorAlert);
		}
		assertClosing(cli,true,false);
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		assertClosing(cli,true,true);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof DecodeErrorAlert);
		}
		assertClosing(srv,true,true);
		assertInOut(0,0);
		assertClosed(cli,srv);
	}
	
	@Test
	public void testClosingAlertRcvEncryptedExtensions() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		in.put(in.remaining()-1,(byte) (in.get(in.remaining()-1)-1));	
		cli.unwrap(in, out);
		try {
			cli.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadRecordMacAlert);
		}
		assertClosing(cli,true,false);
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		assertClosing(cli,true,true);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadRecordMacAlert);
		}
		assertClosing(srv,true,true);
		assertInOut(0,0);
		assertClosed(cli,srv);
	}

	@Test
	public void testClosingAlertSndCertificates() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		handler.certificateSelectorException = new RuntimeException();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}
		assertClosing(srv,true,false);
		clear();
		fc.fly(srv, in, out);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		assertClosing(srv,true,true);
		flip();
		try {
			cli.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}
		assertClosing(cli,true,true);
		assertInOut(0,0);
		assertClosed(cli,srv);
	}
	
	@Test
	public void testKeyUpdate() throws Exception {
		handler.keyLimit = 16384;
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				new TestHandshakeHandler());
		srv.beginHandshake();
		
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:tt|T|w|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:tt|T|u|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertInOut(0,0);
		assertAppData(cli,srv,100,1000);
		
		byte[] data = random(1000);
		int keyUpdates = 0;
		
		for (int i=0; i<1000; ++i) {
			clear(data);
			cli.wrap(in, out);
			flip();
			srv.unwrap(in, out);
			if (cli.getHandshakeStatus() == NEED_WRAP) {
				++keyUpdates;
				clear();
				cli.wrap(in, out);
				flip();
				srv.unwrap(in, out);
				clear();
				srv.wrap(in, out);
				flip();
				cli.unwrap(in, out);
				continue;
			}
			assertArrayEquals(data, out());
		}
		assertTrue(keyUpdates > 10);
		
		keyUpdates = 0;
		for (int i=0; i<1000; ++i) {
			clear(data);
			cli.wrap(array(data, 0, 2), out);
			flip();
			srv.unwrap(in, out);
			if (cli.getHandshakeStatus() == NEED_WRAP) {
				++keyUpdates;
				clear();
				cli.wrap(in, out);
				flip();
				srv.unwrap(in, out);
				clear();
				srv.wrap(in, out);
				flip();
				cli.unwrap(in, out);
				continue;
			}
			assertArrayEquals(data, out());
		}
		assertTrue(keyUpdates > 10);
		
		keyUpdates = 0;
		for (int i=0; i<1000; ++i) {
			clear(data);
			srv.wrap(in, out);
			flip();
			cli.unwrap(in, out);
			if (cli.getHandshakeStatus() == NEED_WRAP) {
				++keyUpdates;
				clear();
				cli.wrap(in, out);
				flip();
				srv.unwrap(in, out);
				clear();
				srv.wrap(in, out);
				flip();
				cli.unwrap(in, out);
				continue;
			}
			assertArrayEquals(data, out());
		}		
		assertTrue(keyUpdates > 10);
	}
	
	@Test
	public void testUnderflow() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();
		
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		cli.wrap(in, out);
		int limit = in.limit();
		IEngineResult r;
		
		for (int i=0; i<limit; ++i) {
			in.limit(i);
			r = srv.unwrap(in, out);
			assertSame(Status.BUFFER_UNDERFLOW, r.getStatus());
			assertSame(HandshakeStatus.NEED_UNWRAP, r.getHandshakeStatus());
			assertEquals(0, r.bytesConsumed());
			assertEquals(0, r.bytesProduced());
			assertEquals(0, in.position());
		}
		in.limit(limit+1);
		r = srv.unwrap(in, out);
		assertEquals(1, in.remaining());
		assertSame(Status.OK, r.getStatus());
		assertSame(HandshakeStatus.NEED_WRAP, r.getHandshakeStatus());
		clear();
		r = srv.wrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(HandshakeStatus.NEED_WRAP, r.getHandshakeStatus());
		flip();
		cli.unwrap(in, out);
		assertEquals(0, in.remaining());
		
		clear();
		r = srv.wrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(HandshakeStatus.NEED_UNWRAP, r.getHandshakeStatus());
		flip();
		limit = in.limit();
		
		for (int i=0; i<limit; ++i) {
			in.limit(i);
			r = cli.unwrap(in, out);
			assertSame(Status.BUFFER_UNDERFLOW, r.getStatus());
			assertSame(HandshakeStatus.NEED_UNWRAP, r.getHandshakeStatus());
			assertEquals(0, r.bytesConsumed());
			assertEquals(0, r.bytesProduced());
			assertEquals(0, in.position());
		}
		in.limit(limit+1);
		r = cli.unwrap(in, out);
		assertEquals(1, in.remaining());
		assertSame(Status.OK, r.getStatus());
		assertSame(HandshakeStatus.NEED_WRAP, r.getHandshakeStatus());
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());

		clear(random(100));
		cli.wrap(in, out);
		flip();
		limit = in.limit();
		
		for (int i=0; i<limit; ++i) {
			in.limit(i);
			r = srv.unwrap(in, out);
			assertSame(Status.BUFFER_UNDERFLOW, r.getStatus());
			assertSame(HandshakeStatus.NOT_HANDSHAKING, r.getHandshakeStatus());
			assertEquals(0, r.bytesConsumed());
			assertEquals(0, r.bytesProduced());
			assertEquals(0, in.position());
		}
		in.limit(limit+1);
		r = srv.unwrap(in, out);
		assertEquals(1, in.remaining());
	}
	
	void assertOverflow(TLSEngine e, int expectedLimit, Status expectedStatus) throws Exception {
		IEngineResult r = null;
		
		for (int i=0; i<17000; ++i) {
			out.limit(i);
			r = e.wrap(in, out);
			if (r.getStatus() != Status.BUFFER_OVERFLOW) {
				break;
			}
			assertEquals(0, out.position());
			assertEquals(0, r.bytesConsumed());
			assertEquals(0, r.bytesProduced());
		}
		assertSame(expectedStatus, r.getStatus());
		assertEquals(expectedLimit, out.limit());
	}

	void assertOverflow(TLSEngine e, int expectedLimit) throws Exception {
		assertOverflow(e, expectedLimit, Status.OK);
	}
	
	void assertOverflow(TLSEngine e, ByteBuffer[] in, int expectedLimit) throws Exception {
		IEngineResult r = null;
		
		for (int i=0; i<17000; ++i) {
			out.limit(i);
			r = e.wrap(in, out);
			if (r.getStatus() != Status.BUFFER_OVERFLOW) {
				break;
			}
			assertEquals(0, out.position());
			assertEquals(0, r.bytesConsumed());
			assertEquals(0, r.bytesProduced());
		}
		assertSame(Status.OK, r.getStatus());
		assertEquals(expectedLimit, out.limit());
	}
	
	@Test
	public void testOverflow() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.cipherSuites(
						CipherSuite.TLS_AES_256_GCM_SHA384,
						CipherSuite.TLS_AES_128_GCM_SHA256)
				.namedGroups(
						NamedGroup.SECP256R1,
						NamedGroup.SECP521R1,
						NamedGroup.SECP384R1,
						NamedGroup.FFDHE2048,
						NamedGroup.FFDHE3072,
						NamedGroup.FFDHE4096,
						NamedGroup.FFDHE6144,
						NamedGroup.FFDHE8192)
				.signatureSchemes(
						SignatureScheme.ECDSA_SECP256R1_SHA256,
						SignatureScheme.ECDSA_SECP384R1_SHA384,
						SignatureScheme.ECDSA_SECP521R1_SHA512,
						SignatureScheme.RSA_PKCS1_SHA256,
						SignatureScheme.RSA_PKCS1_SHA384,
						SignatureScheme.RSA_PKCS1_SHA512,
						SignatureScheme.ECDSA_SHA1,
						SignatureScheme.RSA_PKCS1_SHA1)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.cipherSuites(
						CipherSuite.TLS_AES_256_GCM_SHA384,
						CipherSuite.TLS_AES_128_GCM_SHA256)
				.namedGroups(
						NamedGroup.SECP256R1,
						NamedGroup.SECP521R1,
						NamedGroup.SECP384R1,
						NamedGroup.FFDHE2048,
						NamedGroup.FFDHE3072,
						NamedGroup.FFDHE4096,
						NamedGroup.FFDHE6144,
						NamedGroup.FFDHE8192)
				.signatureSchemes(
						SignatureScheme.ECDSA_SECP256R1_SHA256,
						SignatureScheme.ECDSA_SECP384R1_SHA384,
						SignatureScheme.ECDSA_SECP521R1_SHA512,
						SignatureScheme.RSA_PKCS1_SHA256,
						SignatureScheme.RSA_PKCS1_SHA384,
						SignatureScheme.RSA_PKCS1_SHA512,
						SignatureScheme.ECDSA_SHA1,
						SignatureScheme.RSA_PKCS1_SHA1)
				.build(), 
				handler);
		srv.beginHandshake();
		
		assertOverflow(cli, 186);
		flip();
		srv.unwrap(in, out);
		assertEquals(0, in.remaining());
		
		clear();
		assertOverflow(srv, 128);
		flip();
		cli.unwrap(in, out);
		assertEquals(0, in.remaining());
		
		clear();
		assertOverflow(srv, 903);
		flip();
		cli.unwrap(in, out);
		assertEquals(0, in.remaining());
		
		clear();
		assertOverflow(cli, 74);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, cli.getHandshakeStatus());
		flip();
		srv.unwrap(in, out);
		assertEquals(0, in.remaining());
		assertSame(HandshakeStatus.NEED_WRAP, srv.getHandshakeStatus());
		
		clear();
		assertOverflow(srv, 73);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, srv.getHandshakeStatus());
		flip();
		cli.unwrap(in, out);
		assertEquals(0, in.remaining());
		assertSame(HandshakeStatus.NOT_HANDSHAKING, cli.getHandshakeStatus());

		handler.padding=0;
		clear(random(0));
		assertOverflow(cli, 5+1+16);
		clear();
		assertOverflow(cli, array(bytes(),0,0), 5+1+16);
		handler.padding=30;
		clear(random(0));
		assertOverflow(cli, 5+1+30+16);
		clear();
		assertOverflow(cli, array(bytes(),0,0), 5+1+30+16);
		
		handler.padding=0;
		clear(random(1));
		assertOverflow(cli, 1+5+1+16);
		clear();
		assertOverflow(cli, array(random(1),0,0), 1+5+1+16);
		handler.padding=30;
		clear(random(1));
		assertOverflow(cli, 1+5+1+30+16);
		clear();
		assertOverflow(cli, array(random(1),0,0), 1+5+1+30+16);
		
		handler.padding=0;
		clear(random(16384));
		assertOverflow(cli, 16384+5+1+16);
		clear();
		assertOverflow(cli, array(random(16384),0,10,100,1000), 16384+5+1+16);
		handler.padding=30;
		clear(random(16384));
		assertOverflow(cli, 16384+5+1+16);
		clear();
		assertOverflow(cli, array(random(16384),0,10,100,1000), 16384+5+1+16);

		handler.padding=0;
		clear(random(16383));
		assertOverflow(cli, 16383+5+1+16);
		clear();
		assertOverflow(cli, array(random(16383),0,10,100,1000), 16383+5+1+16);
		clear();
		assertOverflow(cli, array(random(16383),0), 16383+5+1+16);
		handler.padding=1;
		clear(random(16383));
		assertOverflow(cli, 16384+5+1+16);
		clear();
		assertOverflow(cli, array(random(16383),0,10,100,1000), 16384+5+1+16);
		clear();
		assertOverflow(cli, array(random(16383),0), 16384+5+1+16);
		
	}
	
	@Test
	public void testOverflowAlert() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.cipherSuites(CipherSuite.TLS_AES_128_GCM_SHA256)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.cipherSuites(CipherSuite.TLS_AES_256_GCM_SHA384)
				.build(), 
				handler);
		srv.beginHandshake();
		
		clear();
		cli.wrap(in, out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof HandshakeFailureAlert);
		}
		assertOverflow(srv, 7, Status.CLOSED);
		flip();
		try {
			cli.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof HandshakeFailureAlert);
		}
		assertInOut(0,0);
		
		prepareConnection();
		clear(random(10));
		cli.wrap(in, out);
		flip();
		in.put(10, (byte) (in.get(10)+1));
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadRecordMacAlert);
		}
		assertOverflow(srv, 5+2+1+16, Status.CLOSED);
		flip();
		try {
			cli.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadRecordMacAlert);
		}
		assertInOut(0,0);

		prepareConnection();
		clear(random(10));
		cli.wrap(in, out);
		flip();
		in.put(10, (byte) (in.get(10)+1));
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadRecordMacAlert);
		}
		handler.padding = 100;
		assertOverflow(srv, 5+2+1+100+16, Status.CLOSED);
		flip();
		try {
			cli.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadRecordMacAlert);
		}
		assertInOut(0,0);
		
	}
	
	@Test
	public void testCheckKeyLimit() throws Exception {
		prepareConnection();
		IHandshakeEngine h = handshaker(cli);
		assertFalse(h.needProduce());
		EngineResult r = new EngineResult(Status.OK, HandshakeStatus.FINISHED, 11,12);
		
		Cryptor c = new Cryptor(random(32), 16, 100) {};
		assertFalse(c.isKeyLimitReached());
		assertSame(r, cli.checkKeyLimit(c, r));
		assertFalse(h.needProduce());
		c.incProcessedBytes(101);
		assertTrue(c.isKeyLimitReached());
		assertFalse(c.isMarkedForUpdate());
		IEngineResult r2 = cli.checkKeyLimit(c, r);
		assertSame(HandshakeStatus.NEED_WRAP, r2.getHandshakeStatus());
		assertEquals(11, r2.bytesConsumed());
		assertEquals(12, r2.bytesProduced());
		assertTrue(h.needProduce());
		assertTrue(c.isMarkedForUpdate());

		prepareConnection();
		h = handshaker(cli);
		assertFalse(h.needProduce());
		c = new Cryptor(random(32), 16, 100) {
			public long getSequence() { return 0xffff_ffffL; }
		};
		assertFalse(c.isKeyLimitReached());
		assertSame(r, cli.checkKeyLimit(c, r));
		assertFalse(h.needProduce());
		c = new Cryptor(random(32), 16, 100) {
			public long getSequence() { return 0xffff_ffffL + 1; }
		};
		r2 = cli.checkKeyLimit(c, r);
		assertSame(NEED_WRAP, r2.getHandshakeStatus());

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.build(), 
				handler);
		cli.beginHandshake();
		clear();
		cli.wrap(in, out);
		assertSame(NEED_TASK, cli.getHandshakeStatus());
		h = handshaker(cli);
		assertFalse(h.needProduce());
		c = new Cryptor(random(32), 16, 100) {};
		c.incProcessedBytes(101);
		assertTrue(c.isKeyLimitReached());
		assertSame(r, cli.checkKeyLimit(c, r));
		assertFalse(c.isMarkedForUpdate());
		cli.getDelegatedTask().run();
		assertSame(r, cli.checkKeyLimit(c, r));
		assertFalse(c.isMarkedForUpdate());
		assertSame(NEED_WRAP, cli.getHandshakeStatus());
		cli.wrap(in, out);
		r2 = cli.checkKeyLimit(c, r);
		assertFalse(r == r2);
		assertTrue(c.isMarkedForUpdate());
	}
	
	@Test
	public void testWrapWithPendingTasks() throws Exception {
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.build(), 
				handler);
		cli.beginHandshake();
		clear();
		cli.wrap(in, out);
		assertSame(NEED_TASK, cli.getHandshakeStatus());
		Runnable task = cli.getDelegatedTask();
		assertSame(NEED_TASK, cli.getHandshakeStatus());
		assertSame(NEED_TASK, cli.wrap(in, out).getHandshakeStatus());
		assertInOut(0, 0);
		task.run();
		assertSame(NEED_WRAP, cli.getHandshakeStatus());
		assertSame(NEED_UNWRAP, cli.wrap(in, out).getHandshakeStatus());
		assertInOut(0, out.position());
		
		final AtomicReference<HandshakeStatus> ref = new AtomicReference<HandshakeStatus>();
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.build(), 
				handler) {
			
			@Override
			public HandshakeStatus getHandshakeStatus() {
				if (ref.get() != null) {
					return ref.get();
				}
				return super.getHandshakeStatus();
			}
		};
		cli.beginHandshake();
		clear();
		cli.wrap(in, out);
		assertSame(NEED_TASK, cli.getHandshakeStatus());
		cli.getDelegatedTask();
		ref.set(NEED_WRAP);
		assertSame(NEED_WRAP, cli.getHandshakeStatus());
		assertSame(NEED_WRAP, cli.wrap(in, out).getHandshakeStatus());
		assertInOut(0, 0);
	}

	@Test
	public void testUnwrapWithPendingTasks() throws Exception {
		final AtomicReference<HandshakeStatus> ref = new AtomicReference<HandshakeStatus>();
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.build(), 
				handler) {
			
			@Override
			public HandshakeStatus getHandshakeStatus() {
				if (ref.get() != null) {
					return ref.get();
				}
				return super.getHandshakeStatus();
			}
		};
		cli.beginHandshake();
		clear();
		cli.wrap(in, out);
		assertSame(NEED_TASK, cli.getHandshakeStatus());
		cli.getDelegatedTask();
		ref.set(NEED_UNWRAP);
		assertSame(NEED_UNWRAP, cli.getHandshakeStatus());
		assertSame(NEED_UNWRAP, cli.unwrap(in, out).getHandshakeStatus());
		assertInOut(0, 0);
	}
	
	@Test
	public void testWrapWithException() throws Exception {
		AtomicBoolean exception = new AtomicBoolean(true);
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler) {
			@Override
			public HandshakeStatus getHandshakeStatus() {
				if (exception.get()) {
					throw new NullPointerException();
				}
				return super.getHandshakeStatus();
			}
		};
		cli.beginHandshake();
		clear();
		try {
			cli.wrap(in, out);
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}
		exception.set(false);
		assertInOut(0, 0);
		assertClosing(cli, true, false);
		assertSame(NEED_WRAP, cli.getHandshakeStatus());
		exception.set(true);
		try {
			cli.wrap(in, out);
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof InternalErrorAlert);
		}
		exception.set(false);
		assertInOut(0, 0);
		assertClosing(cli, true, true);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, cli.getHandshakeStatus());
	}
	
	byte[] content(int size, ContentType type, int padding) {
		byte[] content = new byte[size+1+padding];
		
		content[size] = (byte) type.value();
		return content;
	}
	
	@Test
	public void testUnwrapDataTooBig() throws Exception {
		prepareConnection();
		Encryptor[] encryptors = encryptors(cli);
		
		in.clear();
		in.put(content(16385, ContentType.APPLICATION_DATA, 0));
		in.flip();
		Record.protect(in, encryptors[3], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Encrypted record is too big", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof RecordOverflowAlert);
		}
		
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		in.limit(in.getShort(3)+5);
		cli.unwrap(in, out);
		encryptors = encryptors(cli);

		in.clear();
		in.put(content(16385, ContentType.APPLICATION_DATA, 0));
		in.flip();
		Record.protect(in, encryptors[2], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Encrypted record is too big", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof RecordOverflowAlert);
		}
	}

	@Test
	public void testUnwrapEncryptedDataTooBig() throws Exception {
		prepareConnection();
		Encryptor[] encryptors = encryptors(cli);
		
		in.clear();
		in.put(content(16384+256-16-1+1, ContentType.APPLICATION_DATA, 0));
		in.flip();
		Record.protect(in, encryptors[3], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Encrypted record is too big", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof RecordOverflowAlert);
		}
		
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		in.limit(in.getShort(3)+5);
		cli.unwrap(in, out);
		encryptors = encryptors(cli);

		in.clear();
		in.put(content(16384+256-16-1+1, ContentType.APPLICATION_DATA, 0));
		in.flip();
		Record.protect(in, encryptors[2], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Encrypted record is too big", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof RecordOverflowAlert);
		}
	}
	
	@Test
	public void testUnwrapFragmentDataTooBig() throws Exception {
		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();
		clear(cat(bytes(ContentType.HANDSHAKE.value(),3,3,0x40,1), new byte[16385]));
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Record fragment is too big", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof RecordOverflowAlert);
		}
	}

	@Test
	public void testUnwrapDataWithoutNonZeroOctet() throws Exception {
		prepareConnection();
		Encryptor[] encryptors = encryptors(cli);
		
		in.clear();
		in.put(bytes(0,0,0,0));
		in.flip();
		Record.protect(in, encryptors[3], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("No non-zero octet in cleartext", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
		
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		in.limit(in.getShort(3)+5);
		cli.unwrap(in, out);
		encryptors = encryptors(cli);

		in.clear();
		in.put(bytes(0,0,0,0));
		in.flip();
		Record.protect(in, encryptors[2], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("No non-zero octet in cleartext", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
	}

	@Test
	public void testUnwrapDataWithUnexpectedContent() throws Exception {
		prepareConnection();
		Encryptor[] encryptors = encryptors(cli);
		
		in.clear();
		in.put(content(100, ContentType.CHANGE_CIPHER_SPEC, 0));
		in.flip();
		Record.protect(in, encryptors[3], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Received unexpected record content type (20)", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
		
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		in.limit(in.getShort(3)+5);
		cli.unwrap(in, out);
		encryptors = encryptors(cli);

		in.clear();
		in.put(content(100, ContentType.CHANGE_CIPHER_SPEC, 0));
		in.flip();
		Record.protect(in, encryptors[2], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Received unexpected record content type (20)", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
		
		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();
		clear(cat(bytes(100,3,3,0x40,0), new byte[16384]));
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Received unexpected record content type (100)", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}

	}

	@Test
	public void testUnwrapDataWithUnexpectedRecord() throws Exception {
		prepareConnection();
		Encryptor[] encryptors = encryptors(cli);
		
		in.clear();
		in.put(content(100, ContentType.HANDSHAKE, 0));
		in.flip();
		Record.protect(in, encryptors[3], out);
		flip();
		in.put(0, (byte) ContentType.HANDSHAKE.value());
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Unexpected encrypted record content type (22)", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
	}
	
	@Test
	public void testUnwrapDataWithBufferOverflow() throws Exception {
		prepareConnection();

		in.clear();
		out.clear();
		in.put(bytes(1,2,3,4,5));
		in.flip();
		cli.wrap(in, out);
		flip();
		out.limit(5);
		assertSame(Status.BUFFER_OVERFLOW, srv.unwrap(in, out).getStatus());
		out.limit(6);
		assertSame(Status.OK, srv.unwrap(in, out).getStatus());
	}

	@Test
	public void testUnwrapDataWithDirectBuffer() throws Exception {
		prepareConnection();

		in = ByteBuffer.allocateDirect(100000);
		out = ByteBuffer.allocateDirect(100000);
		in.put(bytes(1,2,3,4,5));
		in.flip();
		cli.wrap(in, out);
		flip();
		assertEquals(5, srv.unwrap(in, out).bytesProduced());
		handler.padding = 100;
		in.clear();
		out.clear();
		in.put(bytes(1,2,3,4,5));
		in.flip();
		cli.wrap(in, out);
		flip();
		assertEquals(5, srv.unwrap(in, out).bytesProduced());
		
		Encryptor[] encryptors = encryptors(cli);
		in.clear();
		out.clear();
		in.put(bytes(0,0,0,0));
		in.flip();
		Record.protect(in, encryptors[3], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("No non-zero octet in cleartext", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}

	}
	
	@Test
	public void testUnwrapInvalidAlertLength() throws Exception {
		prepareConnection();

		Encryptor[] encryptors = encryptors(cli);
		in.clear();
		out.clear();
		in.put(content(3, ContentType.ALERT, 0));
		in.flip();
		Record.protect(in, encryptors[3], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Invalid length of alert content", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof DecodeErrorAlert);
		}

		prepareConnection();

		encryptors = encryptors(cli);
		in.clear();
		out.clear();
		in.put(content(1, ContentType.ALERT, 0));
		in.flip();
		Record.protect(in, encryptors[3], out);
		flip();
		try {
			srv.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Invalid length of alert content", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof DecodeErrorAlert);
		}
		
	}

	void assertResult(IEngineResult r, Status status, HandshakeStatus hStatus, int consumed, int produced) {
		assertSame(status, r.getStatus());
		assertSame(hStatus, r.getHandshakeStatus());
		assertEquals(consumed, r.bytesConsumed());
		assertEquals(produced, r.bytesProduced());
	}
	
	@Test
	public void testUnwrapWithTask() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.build(), 
				handler);
		srv.beginHandshake();

		assertResult(cli.unwrap(in, out), Status.OK, NEED_TASK, 0, 0);
		Runnable t = cli.getDelegatedTask();
		assertSame(NEED_TASK, cli.getHandshakeStatus());
		assertNull(cli.getDelegatedTask());
		assertSame(NEED_TASK, cli.getHandshakeStatus());
		t.run();
		assertSame(NEED_WRAP, cli.getHandshakeStatus());
		cli.wrap(in, out);
		
		flip();
		assertSame(NEED_TASK, srv.unwrap(in, out).getHandshakeStatus());
		t = srv.getDelegatedTask();
		assertSame(NEED_TASK, srv.getHandshakeStatus());
		t.run();
		assertSame(NEED_TASK, srv.getHandshakeStatus());
		t = srv.getDelegatedTask();
		assertSame(NEED_TASK, srv.getHandshakeStatus());
		assertNull(cli.getDelegatedTask());
		assertSame(NEED_TASK, srv.getHandshakeStatus());
		t.run();
		assertSame(NEED_WRAP, srv.getHandshakeStatus());
		clear();
		srv.wrap(in, out);
		srv.wrap(in, out);
		
		flip();
		assertSame(NEED_UNWRAP, cli.unwrap(in, out).getHandshakeStatus());
		assertSame(NEED_TASK, cli.unwrap(in, out).getHandshakeStatus());
		t = cli.getDelegatedTask();
		assertSame(NEED_TASK, cli.getHandshakeStatus());
		assertNull(cli.getDelegatedTask());
		assertSame(NEED_TASK, cli.getHandshakeStatus());
		t.run();
		assertSame(NEED_UNWRAP, cli.getHandshakeStatus());
		assertSame(NEED_WRAP, cli.unwrap(in, out).getHandshakeStatus());
	}
	
	@Test
	public void testUnwrapInvalidChangeCipherSpec() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		out.put(bytes(20,3,3,0,2,1,1));
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Invalid change_cipher_spec message", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
		
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		srv.beginHandshake();

		fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		out.put(bytes(20,3,3,0,1,2));
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Invalid change_cipher_spec message", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}

	}

	@Test
	public void testUnwrapFragmentedData() throws Exception {
		prepareEngines();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		flip();
		int len = in.remaining();
		in.putShort(3, (short) 20);
		srv.unwrap(in, out);
		assertSame(NEED_UNWRAP, srv.getHandshakeStatus());

		in.position(in.position()-5);
		in.put(in.position(), (byte) ContentType.HANDSHAKE.value());
		in.putShort(in.position()+1, (short) 0x303);
		in.putShort(in.position()+3, (short) 20);
		srv.unwrap(in, out);
		assertSame(NEED_UNWRAP, srv.getHandshakeStatus());
		
		in.position(in.position()-5);
		in.put(in.position(), (byte) ContentType.HANDSHAKE.value());
		in.putShort(in.position()+1, (short) 0x303);
		in.putShort(in.position()+3, (short) (len - 40 - 5));
		srv.unwrap(in, out);
		assertSame(NEED_WRAP, srv.getHandshakeStatus());
		assertInOut(0,0);
		
		clear();
		srv.wrap(in, out);
		flip();
		cli.unwrap(in, out);
		
		prepareEngines();
		clear();
		fc.fly(cli, in, out);
		flip();
		len = in.remaining();
		in.putShort(3, (short) 4);
		srv.unwrap(in, out);
		assertSame(NEED_UNWRAP, srv.getHandshakeStatus());

		in.position(in.position()-5);
		in.put(in.position(), (byte) ContentType.HANDSHAKE.value());
		in.putShort(in.position()+1, (short) 0x303);
		in.putShort(in.position()+3, (short) 20);
		srv.unwrap(in, out);
		assertSame(NEED_UNWRAP, srv.getHandshakeStatus());
		
		in.position(in.position()-5);
		in.put(in.position(), (byte) ContentType.HANDSHAKE.value());
		in.putShort(in.position()+1, (short) 0x303);
		in.putShort(in.position()+3, (short) (len - 24 - 5));
		srv.unwrap(in, out);
		assertSame(NEED_WRAP, srv.getHandshakeStatus());
		assertInOut(0,0);
		
		clear();
		srv.wrap(in, out);
		flip();
		cli.unwrap(in, out);
		
	}
	
	@Test
	public void testCompatibilityMode() throws Exception {
		
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
	}

	@Test
	public void testCompatibilityModeWithHRR() throws Exception {
		
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.namedGroups(NamedGroup.SECP256R1, NamedGroup.SECP521R1)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.namedGroups(NamedGroup.SECP521R1)
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:ww|W|OK:uu|U|OK:uu|", fc.trace());

		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
	}
	
	@Test
	public void testServerBadCertificate() throws Exception {
		TestHandshakeHandler handler2 = new TestHandshakeHandler();
		handler2.certificateValidator.certificatesAlert = new BadCertificateAlert("");

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler2);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler);
		srv.beginHandshake();
		
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		try {
			fc.fly(cli, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadCertificateAlert);
			assertEquals("U|OK:uu|U|OK:uu|", fc.trace());
		}
	}	

	@Test
	public void testClientRequiredCertificate() throws Exception {
		TestHandshakeHandler handler2 = new TestHandshakeHandler();
		handler2.certificateSelector.certNames = new String[] {"rsasha384"};
		handler2.certificateSelector.signatureScheme = SignatureScheme.RSA_PKCS1_SHA384;

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.peerHost("host")
				.signatureSchemes(SignatureScheme.ECDSA_SECP521R1_SHA512)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.clientAuth(ClientAuth.REQUIRED)
				.signatureSchemes(SignatureScheme.ECDSA_SECP521R1_SHA512)
				.build(), 
				handler2);
		srv.beginHandshake();
		
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		ISession cs = (ISession)cli.getSession();
		ISession ss = (ISession)srv.getSession();
		assertEquals(1, cs.getLocalCertificates().length);
		assertEquals(1, ss.getLocalCertificates().length);
		assertEquals(1, ss.getPeerCertificates().length);
		assertEquals(1, cs.getPeerCertificates().length);
		
		assertArrayEquals(cs.getLocalCertificates()[0].getEncoded(), cert("rsasha256").getEncoded());
		assertArrayEquals(ss.getLocalCertificates()[0].getEncoded(), cert("rsasha384").getEncoded());
		assertArrayEquals(ss.getPeerCertificates()[0].getEncoded(), cert("rsasha256").getEncoded());
		assertArrayEquals(cs.getPeerCertificates()[0].getEncoded(), cert("rsasha384").getEncoded());

		CertificateCriteria criteria = handler2.certificateSelector.criteria;
		assertEquals(1, criteria.getSchemes().length);
		assertNull(criteria.getCertSchemes());
		assertTrue(criteria.isServer());
		assertTrue(handler2.certificateValidator.criteria.isServer());
		assertEquals("host", handler2.certificateValidator.criteria.getHostName());
		
		criteria = handler.certificateSelector.criteria;
		assertEquals(1, criteria.getSchemes().length);
		assertNull(criteria.getCertSchemes());
		assertFalse(criteria.isServer());
		assertFalse(handler.certificateValidator.criteria.isServer());
		assertEquals("host", handler.certificateValidator.criteria.getHostName());
	}	

	@Test
	public void testClientRequiredCertificateWithTask() throws Exception {
		TestHandshakeHandler handler2 = new TestHandshakeHandler();
		handler2.certificateSelector.certNames = new String[] {"rsasha384"};
		handler2.certificateSelector.signatureScheme = SignatureScheme.RSA_PKCS1_SHA384;

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.compatibilityMode(true)
				.signatureSchemes(SignatureScheme.ECDSA_SECP521R1_SHA512)
				.signatureSchemesCert(SignatureScheme.ECDSA_SECP256R1_SHA256)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.compatibilityMode(true)
				.clientAuth(ClientAuth.REQUIRED)
				.signatureSchemes(SignatureScheme.RSA_PKCS1_SHA256)
				.signatureSchemesCert(SignatureScheme.RSA_PKCS1_SHA384)
				.build(), 
				handler2);
		srv.beginHandshake();
		
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:tt|T|w|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:tt|T|u|U|OK:tt|T|w|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:tt|T|u|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		
		CertificateCriteria criteria = handler2.certificateSelector.criteria;
		assertEquals(1, criteria.getSchemes().length);
		assertSame(SignatureScheme.ECDSA_SECP521R1_SHA512, criteria.getSchemes()[0]);
		assertEquals(1, criteria.getCertSchemes().length);
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, criteria.getCertSchemes()[0]);
		
		criteria = handler.certificateSelector.criteria;
		assertEquals(1, criteria.getSchemes().length);
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, criteria.getSchemes()[0]);
		assertEquals(1, criteria.getCertSchemes().length);
		assertSame(SignatureScheme.RSA_PKCS1_SHA384, criteria.getCertSchemes()[0]);
	}	
	
	@Test
	public void testClientRequiredBadCertificate() throws Exception {
		TestHandshakeHandler handler2 = new TestHandshakeHandler();
		handler2.certificateValidator.certificatesAlert = new BadCertificateAlert("");

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.clientAuth(ClientAuth.REQUIRED)
				.build(), 
				handler2);
		srv.beginHandshake();
		
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadCertificateAlert);
			assertEquals("U|OK:uu|", fc.trace());
		}

		//no cert provided
		handler.certificateSelector.certNames = new String[0];
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.clientAuth(ClientAuth.REQUIRED)
				.build(), 
				handler2);
		srv.beginHandshake();
		
		fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof CertificateRequiredAlert);
			assertEquals("U|OK:uu|", fc.trace());
		}	
	}	

	@Test
	public void testClientRequestedBadCertificate() throws Exception {
		TestHandshakeHandler handler2 = new TestHandshakeHandler();
		handler2.certificateValidator.certificatesAlert = new BadCertificateAlert("");

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.clientAuth(ClientAuth.REQUESTED)
				.build(), 
				handler2);
		srv.beginHandshake();
		
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof BadCertificateAlert);
			assertEquals("U|OK:uu|", fc.trace());
		}

		//no cert provided
		handler.certificateSelector.certNames = new String[0];
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.clientAuth(ClientAuth.REQUESTED)
				.build(), 
				handler2);
		srv.beginHandshake();
		
		fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertNull(((ISession)cli.getSession()).getLocalCertificates());
		assertEquals(1, ((ISession)srv.getSession()).getLocalCertificates().length);
		assertNull(((ISession)srv.getSession()).getPeerCertificates());
		assertEquals(1, ((ISession)cli.getSession()).getPeerCertificates().length);
	}	

	@Test
	public void testClientNoneBadCertificate() throws Exception {
		TestHandshakeHandler handler2 = new TestHandshakeHandler();
		handler2.certificateValidator.certificatesAlert = new BadCertificateAlert("");

		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.build(), 
				handler);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.compatibilityMode(true)
				.clientAuth(ClientAuth.NONE)
				.build(), 
				handler2);
		srv.beginHandshake();
		
		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		assertNull(((ISession)cli.getSession()).getLocalCertificates());
		assertEquals(1, ((ISession)srv.getSession()).getLocalCertificates().length);
		assertNull(((ISession)srv.getSession()).getPeerCertificates());
		assertEquals(1, ((ISession)cli.getSession()).getPeerCertificates().length);
	}	
	
	@Test
	public void testUnwrapHandshakeUnexpectedMessage() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		HandshakeEngine s = new HandshakeEngine(false, params, handler, handler);
		HandshakeEngine c = (HandshakeEngine) handshaker(cli);
		
		HandshakeController ctl = new HandshakeController(c, s);
		ctl.run(false, HandshakeType.FINISHED);
		IHandshake h = ctl.get(false);
		assertNotNull(h);
		buffer.clear();
		h.getBytes(buffer);
		buffer.put((byte) 0);
		buffer.flip();
		try {
			cli.unwrapHandshake(buffer, 0, buffer.remaining(), 111);
			fail();
		}
		catch (UnexpectedMessageAlert e) {
			assertEquals("Received unexpected data after finished handshake", e.getMessage());
		}
	}

	@Test
	public void testUnwrapHandshakeFinished() throws Exception {
		handler.ticketInfos = new TicketInfo[0];
		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.clientAuth(ClientAuth.NONE)
				.build(), 
				handler);
		
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		HandshakeEngine c = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine s = (HandshakeEngine) handshaker(srv);
		
		HandshakeController ctl = new HandshakeController(c, s);
		ctl.run(true, HandshakeType.FINISHED);
		IHandshake h = ctl.get(true);
		assertNotNull(h);
		buffer.clear();
		h.getBytes(buffer);
		buffer.flip();
		assertResult(srv.unwrapHandshake(buffer, 0, buffer.remaining(), 111), 
				Status.OK,
				HandshakeStatus.FINISHED,
				111,
				0);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, srv.getHandshakeStatus());
	}

	@Test
	public void testUnwrapHandshakeNeedWrap() throws Exception {
		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.clientAuth(ClientAuth.NONE)
				.build(), 
				handler);
		
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		HandshakeEngine c = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine s = (HandshakeEngine) handshaker(srv);
		
		HandshakeController ctl = new HandshakeController(c, s);
		ctl.run(true, HandshakeType.FINISHED);
		IHandshake h = ctl.get(true);
		assertNotNull(h);
		buffer.clear();
		h.getBytes(buffer);
		buffer.flip();
		assertResult(srv.unwrapHandshake(buffer, 0, buffer.remaining(), 111), 
				Status.OK,
				HandshakeStatus.NEED_WRAP,
				111,
				0);
		assertSame(HandshakeStatus.NEED_WRAP, srv.getHandshakeStatus());
	}

	private void fillAggregator(TLSEngine engine) throws Exception {
		HandshakeAggregator aggr = aggregator(engine);
		Field f = HandshakeAggregator.class.getDeclaredField("remaining");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Queue<ByteBuffer> remaining = (Queue<ByteBuffer>) f.get(aggr); 
		remaining.add(buffer);
	}
	
	@Test
	public void testUnwrapRemainingUnexpectedMessage() throws Exception {
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.build(), 
				handler);
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		HandshakeEngine s = new HandshakeEngine(false, params, handler, handler);
		HandshakeEngine c = (HandshakeEngine) handshaker(cli);
		
		HandshakeController ctl = new HandshakeController(c, s);
		ctl.run(false, HandshakeType.FINISHED);
		IHandshake h = ctl.get(false);
		assertNotNull(h);
		buffer.clear();
		h.getBytes(buffer);
		buffer.put((byte) 0);
		buffer.flip();
		fillAggregator(cli);
		handshakeStatus(cli, HandshakeStatus.NEED_UNWRAP);
		assertSame(HandshakeStatus.NEED_UNWRAP, cli.getHandshakeStatus());
		
		try {
			clear();
			cli.unwrap(in, out);
			fail();
		}
		catch (TLSException e) {
			assertEquals("Received unexpected data after finished handshake", e.getAlert().getMessage());
			assertTrue(e.getAlert() instanceof UnexpectedMessageAlert);
		}
	}

	@Test
	public void testUnwrapRemainingFinished() throws Exception {
		handler.ticketInfos = new TicketInfo[0];
		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.clientAuth(ClientAuth.NONE)
				.build(), 
				handler);
		srv.beginHandshake();
		
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		HandshakeEngine c = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine s = (HandshakeEngine) handshaker(srv);
		
		HandshakeController ctl = new HandshakeController(c, s);
		ctl.run(true, HandshakeType.FINISHED);
		IHandshake h = ctl.get(true);
		assertNotNull(h);
		buffer.clear();
		h.getBytes(buffer);
		buffer.flip();
		fillAggregator(srv);
		
		clear();
		assertResult(srv.unwrap(in, out), 
				Status.OK,
				HandshakeStatus.FINISHED,
				0,
				0);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, srv.getHandshakeStatus());
	}

	@Test
	public void testUnwrapRemainingNeedWrap() throws Exception {
		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.clientAuth(ClientAuth.NONE)
				.build(), 
				handler);
		srv.beginHandshake();
		
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		HandshakeEngine c = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine s = (HandshakeEngine) handshaker(srv);
		
		HandshakeController ctl = new HandshakeController(c, s);
		ctl.run(true, HandshakeType.FINISHED);
		IHandshake h = ctl.get(true);
		assertNotNull(h);
		buffer.clear();
		h.getBytes(buffer);
		buffer.flip();
		fillAggregator(srv);
		
		clear();
		assertResult(srv.unwrap(in, out), 
				Status.OK,
				HandshakeStatus.NEED_WRAP,
				0,
				0);
		assertSame(HandshakeStatus.NEED_WRAP, srv.getHandshakeStatus());
	}

	@Test
	public void testALPN() throws Exception {
		TestHandshakeHandler handler2 = new TestHandshakeHandler();
		handler.protocol = "xxx";
		handler.ticketInfos = new TicketInfo[] {new TicketInfo(100)};
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.applicationProtocols("yyy","xxx")
				.build(), 
				handler2);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.applicationProtocols("xxx","yyy")
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		flip();
		assertInOut(0,0);		
		HandshakeEngine s = (HandshakeEngine) handshaker(srv);
		HandshakeEngine c = (HandshakeEngine) handshaker(cli);
		assertEquals("xxx", c.getState().getApplicationProtocol());
		assertEquals("xxx", s.getState().getApplicationProtocol());
		assertEquals("ALPN(yyy|xxx|)|VSN(snf4j.org)|CS|PN(xxx)|", handler.trace());
		assertEquals("CV|PN(xxx)|", handler2.trace());
		
		//Early data accepted
		byte[] data = random(90);
		handler2.earlyData.add(data);
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.applicationProtocols("xxx")
				.build(), 
				handler2);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.applicationProtocols("xxx","yyy")
				.build(), 
				handler);
		srv.beginHandshake();

		fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|Ued(90)|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|", fc.trace());
		flip();
		assertInOut(0,0);
		s = (HandshakeEngine) handshaker(srv);
		c = (HandshakeEngine) handshaker(cli);
		assertEquals("xxx", c.getState().getApplicationProtocol());
		assertEquals("xxx", s.getState().getApplicationProtocol());
		assertEquals("ALPN(xxx|)|VSN(snf4j.org)|PN(xxx)|", handler.trace());
		assertEquals("AED|PN(xxx)|", handler2.trace());

		//Early data rejected
		handler.protocol = "yyy";
		data = random(80);
		handler2.earlyData.add(data);
		cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.applicationProtocols("yyy")
				.build(), 
				handler2);
		cli.beginHandshake();

		srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.applicationProtocols("xxx","yyy")
				.build(), 
				handler);
		srv.beginHandshake();

		fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:ww|W|OK:uu|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:ww|W|OK:uu|U|OK:uu|", fc.trace());
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:uu|U|OK:ww|W|OK:fnh|NH|", fc.trace());
		flip();
		fc.fly(srv, in, out);
		assertEquals("U|OK:ww|W|OK:fnh|NH|", fc.trace());
		in.compact();
		in.put(bytes(1,2,3));
		in.flip();
		srv.wrap(in, out);
		flip();
		fc.fly(cli, in, out);
		assertEquals("U|OK:nhnh|U|OK:nhnh|", fc.trace());
		flip();
		assertInOut(3,0);
		s = (HandshakeEngine) handshaker(srv);
		c = (HandshakeEngine) handshaker(cli);
		assertEquals("yyy", c.getState().getApplicationProtocol());
		assertEquals("yyy", s.getState().getApplicationProtocol());
		assertEquals("ALPN(yyy|)|VSN(snf4j.org)|PN(yyy)|", handler.trace());
		assertEquals("RED|PN(yyy)|", handler2.trace());	
	}
	
	@Test
	public void testALPNRejected() throws Exception {
		TestHandshakeHandler handler2 = new TestHandshakeHandler();
		handler.selectProtocolAlert = new NoApplicationProtocolAlert("XXX");
		TLSEngine cli = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost(PEER_HOST)
				.peerPort(PEER_PORT)
				.applicationProtocols("yyy","xxx")
				.build(), 
				handler2);
		cli.beginHandshake();

		TLSEngine srv = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.applicationProtocols("xxx","yyy")
				.build(), 
				handler);
		srv.beginHandshake();

		FlightController fc = new FlightController();
		clear();
		fc.fly(cli, in, out);
		assertEquals("W|OK:uu|", fc.trace());
		flip();
		try {
			fc.fly(srv, in, out);
			fail();
		}
		catch (TLSException e) {
		}
		assertEquals("", fc.trace());
		fc.fly(srv, in, out);
		assertEquals("W|C:nhnh|NH|", fc.trace());
		flip();
		try {
			fc.fly(cli, in, out);
			fail();
		}
		catch (TLSException e) {
		}
		assertEquals("", fc.trace());
		assertClosed(cli,srv);
	}
}
