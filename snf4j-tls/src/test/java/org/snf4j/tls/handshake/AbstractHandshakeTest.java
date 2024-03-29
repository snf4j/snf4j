/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;
import org.snf4j.tls.extension.IExtension;

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
	
	@Test
	public void testPrepare() {
		Handshake e = new Handshake(HandshakeType.CLIENT_HELLO, bytes(0,5,0,0,2,97,98));
		
		assertFalse(e.isPrepared());
		e.getBytes(buffer);
		assertArrayEquals(bytes(1,0,0,7,0,5,0,0,2,97,98), buffer());
		e.data[6] = 99;
		buffer.clear();
		e.getBytes(buffer);
		assertArrayEquals(bytes(1,0,0,7,0,5,0,0,2,97,99), buffer());
		
		byte[] prepared = e.prepare();
		
		assertTrue(e.isPrepared());
		assertArrayEquals(bytes(1,0,0,7,0,5,0,0,2,97,99), prepared);
		buffer.clear();
		e.getBytes(buffer);
		assertArrayEquals(bytes(1,0,0,7,0,5,0,0,2,97,99), buffer());
		e.data[6] = 98;
		buffer.clear();
		e.getBytes(buffer);
		assertArrayEquals(bytes(1,0,0,7,0,5,0,0,2,97,99), buffer());
		buffer.clear();
		
		prepared = e.prepare();
		assertArrayEquals(bytes(1,0,0,7,0,5,0,0,2,97,98), prepared);
		e.getBytes(buffer);
		assertArrayEquals(bytes(1,0,0,7,0,5,0,0,2,97,98), buffer());
	}

	@Test
	public void testGetPrepared() {
		Handshake e = new Handshake(HandshakeType.CLIENT_HELLO, bytes(0,5,0,0,2,97,98));
		
		assertFalse(e.isPrepared());
		byte[] prepared = e.prepare();
		assertTrue(e.isPrepared());
		assertSame(prepared, e.getPrepared());
		
		e = new Handshake(HandshakeType.CLIENT_HELLO, bytes(0,5,0,0,2,97,98));
		byte[] prepared2 = e.getPrepared();
		assertTrue(e.isPrepared());
		assertSame(prepared2, e.getPrepared());
		assertArrayEquals(prepared, prepared2);
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

		@Override
		public List<IExtension> getExtensions() {
			return null;
		}
	}
}
