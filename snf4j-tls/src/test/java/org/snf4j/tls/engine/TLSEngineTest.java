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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import static org.snf4j.core.engine.HandshakeStatus.NEED_WRAP;

import org.junit.Test;
import org.snf4j.tls.alert.UnexpectedMessageAlert;

public class TLSEngineTest extends EngineTest {

	ByteBuffer in;
	
	ByteBuffer out;
	
	private static final String PEER_HOST = "snf4j.org";
	
	private static final int PEER_PORT = 100;
	
	@Override
	public void before() throws Exception {
		super.before();
		in = ByteBuffer.allocate(100000);
		out = ByteBuffer.allocate(100000);
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
	
	void prepareTickets(long... maxSizes) throws Exception {
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
		catch (UnexpectedMessageAlert e) {
			assertEquals("Early data is too big", e.getMessage());
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
		catch (UnexpectedMessageAlert e) {
			assertEquals("Early data is too big", e.getMessage());
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
		catch (UnexpectedMessageAlert e) {
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
}
