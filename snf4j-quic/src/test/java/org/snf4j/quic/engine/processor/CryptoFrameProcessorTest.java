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
package org.snf4j.quic.engine.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.engine.CryptoFragmenter;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.engine.QuicState;
import org.snf4j.quic.engine.crypto.ProducedCrypto;
import org.snf4j.quic.frame.CryptoFrame;
import org.snf4j.quic.frame.FrameType;

public class CryptoFrameProcessorTest extends CommonTest {
	
	@Test
	public void testAll() {
		CryptoFrameProcessor p = new CryptoFrameProcessor();
		
		assertSame(FrameType.CRYPTO, p.getType());
		p.sending(null, null, null);
	}

	ProducedCrypto current(CryptoFragmenter fragmenter) throws Exception {
		Field f = CryptoFragmenter.class.getDeclaredField("current");
		f.setAccessible(true);
		ProducedCrypto pc =  (ProducedCrypto) f.get(fragmenter);
		f.set(fragmenter, null);
		return pc;
	}
	
	@Test
	public void testRecovery() throws Exception {
		QuicState s = new QuicState(false);
		QuicProcessor p = new QuicProcessor(s, null);
		CryptoFrameProcessor fp = new CryptoFrameProcessor();
		CryptoFragmenter fragmenter = new CryptoFragmenter(s, null, null, p);
		p.setFragmenter(fragmenter);
		
		ByteBuffer data = ByteBuffer.wrap(bytes("0102030405"));
		CryptoFrame frame = new CryptoFrame(2, data);
		PacketNumberSpace space = s.getSpace(EncryptionLevel.INITIAL);
		assertFalse(fragmenter.hasPending());
		fp.recover(p, frame, space);
		assertTrue(fragmenter.hasPending());
		ProducedCrypto pc = current(fragmenter);
		assertEquals(2, pc.getOffset());
		assertSame(data, pc.getData());
		assertSame(EncryptionLevel.INITIAL, pc.getEncryptionLevel());
		
		data = ByteBuffer.wrap(bytes("01020304050607"));
		frame = new CryptoFrame(3, data);
		space = s.getSpace(EncryptionLevel.HANDSHAKE);
		assertFalse(fragmenter.hasPending());
		fp.recover(p, frame, space);
		assertTrue(fragmenter.hasPending());
		pc = current(fragmenter);
		assertEquals(3, pc.getOffset());
		assertSame(data, pc.getData());
		assertSame(EncryptionLevel.HANDSHAKE, pc.getEncryptionLevel());

		data = ByteBuffer.wrap(bytes("0102030405060708"));
		frame = new CryptoFrame(4, data);
		space = s.getSpace(EncryptionLevel.APPLICATION_DATA);
		assertFalse(fragmenter.hasPending());
		fp.recover(p, frame, space);
		assertTrue(fragmenter.hasPending());
		pc = current(fragmenter);
		assertEquals(4, pc.getOffset());
		assertSame(data, pc.getData());
		assertSame(EncryptionLevel.APPLICATION_DATA, pc.getEncryptionLevel());
	}
}
