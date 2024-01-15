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
package org.snf4j.tls.extension;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.junit.Test;

public class AbstractExtensionTest extends ExtensionTest {

	@Test
	public void testGetBytes() {
		Extension e = new Extension(ExtensionType.SERVER_NAME, bytes(0,5,0,0,2,97,98));
		assertSame(ExtensionType.SERVER_NAME, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,0,0,7,0,5,0,0,2,97,98), buffer());
		buffer.clear();
		buffer.limit(11);
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,0,0,7,0,5,0,0,2,97,98), buffer());
		buffer.clear();
		buffer.limit(10);
		try {
			e.getBytes(buffer);
			fail();
		} catch (BufferOverflowException ex) {}
	}
	
	class Extension extends AbstractExtension {

		final byte[] data;
		
		protected Extension(ExtensionType type, byte[] data) {
			super(type);
			this.data = data;
		}

		@Override
		public int getDataLength() {
			return data.length;
		}

		@Override
		public boolean isKnown() {
			return true;
		}

		@Override
		protected void getData(ByteBuffer buffer) {
			buffer.put(data);
		}
		
	}
}
