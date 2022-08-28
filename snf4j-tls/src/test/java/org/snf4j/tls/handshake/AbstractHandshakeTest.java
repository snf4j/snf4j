/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.tls.handshake;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.junit.Test;

public class AbstractHandshakeTest extends HandshakeTest {

	@Test
	public void testGetBytes() {
		Handshake e = new Handshake(HandshakeType.CLIENT_HELLO, bytes(0,5,0,0,2,97,98));
		assertSame(HandshakeType.CLIENT_HELLO, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(bytes(1,0,0,7,0,5,0,0,2,97,98), buffer());
		buffer.clear();
		buffer.limit(11);
		e.getBytes(buffer);
		assertArrayEquals(bytes(1,0,0,7,0,5,0,0,2,97,98), buffer());
		buffer.clear();
		buffer.limit(10);
		try {
			e.getBytes(buffer);
			fail();
		} catch (BufferOverflowException ex) {}

		buffer.clear();
		e = new Handshake(HandshakeType.SERVER_HELLO, new byte[0x10102]);
		e.getBytes(buffer);
		assertArrayEquals(bytes(2,1,1,2,0,0), buffer(0,6));
	}
	
	class Handshake extends AbstractHandshake {

		final byte[] data;
		
		Handshake(HandshakeType type, byte[] data) {
			super(type);
			this.data = data;
		}

		@Override
		public int getDataLength() {
			return data.length;
		}

		@Override
		protected void getData(ByteBuffer buffer) {
			buffer.put(data);
		}

		@Override
		public boolean isKnown() {
			return true;
		}
	}
}
