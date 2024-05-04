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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QuicStateTest {
	
	@Test
	public void testConstructor() {
		QuicState s = new QuicState(true);
		assertTrue(s.isClientMode());
		assertNull(s.getDestinationId());
		assertEquals(0, s.getSourceId().length);

		s = new QuicState(false);
		assertFalse(s.isClientMode());
	}

	void assertContext(EncryptionContext ctx) {
		assertFalse(ctx.isErased());
	}
	
	@Test
	public void testGetContext() {
		QuicState s = new QuicState(true);
		assertContext(s.getContext(EncryptionLevel.INITIAL));
		assertContext(s.getContext(EncryptionLevel.EARLY_DATA));
		assertContext(s.getContext(EncryptionLevel.HANDSHAKE));
		assertContext(s.getContext(EncryptionLevel.APPLICATION_DATA));		
	}
	
	@Test
	public void testGetSpace() {
		QuicState s = new QuicState(true);

		PacketNumberSpace s1 = s.getSpace(EncryptionLevel.INITIAL);
		PacketNumberSpace s2 = s.getSpace(EncryptionLevel.EARLY_DATA);
		PacketNumberSpace s3 = s.getSpace(EncryptionLevel.HANDSHAKE);
		PacketNumberSpace s4 = s.getSpace(EncryptionLevel.APPLICATION_DATA);
		
		assertSame(PacketNumberSpace.Type.INITIAL, s1.getType());
		assertSame(s4, s2);
		assertSame(PacketNumberSpace.Type.HANDSHAKE, s3.getType());
		assertSame(PacketNumberSpace.Type.APPLICATION_DATA, s4.getType());
	}	
}
