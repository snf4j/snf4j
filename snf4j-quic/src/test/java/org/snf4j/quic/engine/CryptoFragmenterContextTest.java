/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.frame.MultiPaddingFrame;
import org.snf4j.quic.packet.IPacket;

public class CryptoFragmenterContextTest extends CommonTest {

	static final byte[] INITIAL_SALT_V1 = bytes("38762cf7f55934b34d179ae6a4c80cadccbb7f0a");

	static final byte[] DEST_CID = bytes("8394c8f03e515708");

	QuicState state;
	
	byte[] dcid, scid;
	
	CryptoFragmenterContext ctx;
	
	CryptoEngineStateListener listener;
	
	@Override
	public void before() throws Exception {
		super.before();
		state = new QuicState(true);
		scid = state.getConnectionIdManager().getSourceId();
		state.getConnectionIdManager().getDestinationPool().add(bytes("00010203"));
		dcid = state.getConnectionIdManager().getDestinationId();
		ctx = new CryptoFragmenterContext(state);
		
		listener = new CryptoEngineStateListener(state);
	}
	
	@Test
	public void testInit() throws Exception {
		assertFalse(ctx.init(EncryptionLevel.INITIAL));
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		assertTrue(ctx.init(EncryptionLevel.INITIAL));
		int headerLen = 1 + 4 + 1 + dcid.length + 1 + scid.length + 1 + 4 + 4 + 16;
		IPacket p = ctx.packet(new MultiPaddingFrame(1200 - headerLen));
		assertNotNull(p);
		assertNull(ctx.packet(new MultiPaddingFrame(1200 - headerLen + 1)));
		Encryptor e = state.getContext(EncryptionLevel.INITIAL).getEncryptor();
		assertEquals(1195, p.getLength(-1, 16));
		assertEquals(1195, ctx.length(p));
		
		assertFalse(ctx.init(EncryptionLevel.EARLY_DATA));
		state.getContext(EncryptionLevel.EARLY_DATA).setEncryptor(e);
		assertTrue(ctx.init(EncryptionLevel.EARLY_DATA));
		headerLen = 1 + 4 + 1 + dcid.length + 1 + scid.length + 4 + 4 + 16;
		p = ctx.packet(new MultiPaddingFrame(1200 - headerLen));
		assertNotNull(p);
		assertNull(ctx.packet(new MultiPaddingFrame(1200 - headerLen + 1)));
		assertEquals(1195, p.getLength(-1, 16));
		assertEquals(1195, ctx.length(p));

		assertFalse(ctx.init(EncryptionLevel.HANDSHAKE));
		state.getContext(EncryptionLevel.HANDSHAKE).setEncryptor(e);
		assertTrue(ctx.init(EncryptionLevel.HANDSHAKE));
		headerLen = 1 + 4 + 1 + dcid.length + 1 + scid.length + 4 + 4 + 16;
		p = ctx.packet(new MultiPaddingFrame(1200 - headerLen));
		assertNotNull(p);
		assertNull(ctx.packet(new MultiPaddingFrame(1200 - headerLen + 1)));
		assertEquals(1195, p.getLength(-1, 16));
		assertEquals(1195, ctx.length(p));

		assertFalse(ctx.init(EncryptionLevel.APPLICATION_DATA));
		state.getContext(EncryptionLevel.APPLICATION_DATA).setEncryptor(e);
		assertTrue(ctx.init(EncryptionLevel.APPLICATION_DATA));
		headerLen = 1 + dcid.length + 4 + 16;
		p = ctx.packet(new MultiPaddingFrame(1200 - headerLen));
		assertNotNull(p);
		assertNull(ctx.packet(new MultiPaddingFrame(1200 - headerLen + 1)));
		assertEquals(1197, p.getLength(-1, 16));
		assertEquals(1197, ctx.length(p));
	}
	
	@Test
	public void testAdd() throws Exception {
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		assertTrue(ctx.init(EncryptionLevel.INITIAL));
		int headerLen = 1 + 4 + 1 + dcid.length + 1 + scid.length + 1 + 4 + 4 + 16;
		IPacket p = ctx.packet(new MultiPaddingFrame(1200 - headerLen - 1));
		assertEquals(1, p.getFrames().size());
		assertTrue(ctx.add(p, new MultiPaddingFrame(1)));
		assertEquals(2, p.getFrames().size());
		assertFalse(ctx.add(p, new MultiPaddingFrame(1)));
		assertEquals(2, p.getFrames().size());
	}
	
	@Test
	public void testNoRemaining() throws Exception {
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		assertTrue(ctx.init(EncryptionLevel.INITIAL));
		int headerLen = 1 + 4 + 1 + dcid.length + 1 + scid.length + 1 + 4 + 4 + 16;
		ctx.remaining -= 1200-headerLen-1;
		assertFalse(ctx.noRemaining());
		ctx.remaining--;
		assertTrue(ctx.noRemaining());		
	}
	
	@Test
	public void testCommitRemaining() throws Exception {
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		assertTrue(ctx.init(EncryptionLevel.INITIAL));
		assertEquals(1200, ctx.remaining);
		ctx.remaining -= 200;
		assertEquals(1000, ctx.remaining);
		ctx.commitRemaining();
		assertEquals(1000, ctx.remaining);
		ctx.remaining -= 200;
		assertEquals(800, ctx.remaining);
		ctx.rollbackRemaining();
		assertEquals(1000, ctx.remaining);
	}
	
	@Test
	public void testPadding() throws Exception {
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		ctx.init(EncryptionLevel.INITIAL);
		IPacket p = ctx.packet(new MultiPaddingFrame(1));
		int remaining = ctx.remaining;
		
		for (int i=1; i<1163; ++i, ++i) {
			ctx.remaining = remaining;
			p.getFrames().clear();
			p.getFrames().add(new MultiPaddingFrame(i));
			ctx.remaining -= ctx.length(p);
			ctx.padding(p);
			assertEquals("i=" + i, 1200, ctx.length(p));
		}

		for (int i=1; i<1163; ++i, ++i) {
			ctx.remaining = remaining-i;
			p.getFrames().clear();
			p.getFrames().add(new MultiPaddingFrame(1));
			ctx.remaining -= ctx.length(p);
			ctx.padding(p);
			if (i == 1115) {
				assertEquals("i=" + i, 1200-i+1, ctx.length(p));
			}
			else {
				assertEquals("i=" + i, 1200-i, ctx.length(p));
			}
		}
	
		state.setMaxUdpPayloadSize(1201);
		ctx.remaining = 1201-1116;
		p.getFrames().clear();
		p.getFrames().add(new MultiPaddingFrame(1));
		ctx.remaining -= ctx.length(p);
		ctx.padding(p);
		assertEquals(1201-1117, ctx.length(p));
	}
	
	@Test
	public void testAckTime() {
		state = new QuicState(true, new TestConfig(), new TestTime(1000,2000));
		ctx = new CryptoFragmenterContext(state);
		
		assertEquals(1000, ctx.ackTime());
		assertEquals(1000, ctx.ackTime());
		assertEquals(2000, state.getTime().nanoTime());
	}
}
