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
package org.snf4j.tls.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.handshake.HandshakeType;

public class AbstractExtensionsParserTest extends ExtensionTest {

	@Test
	public void testReset() throws Alert {
		Parser p = new Parser(0, 0xffff, new TestExtensionParser(ExtensionType.SERVER_NAME));
		
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(0, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0,4,0,0,0,0), 0),6);
		assertTrue(p.isComplete());
		assertEquals(1, p.getExtensions().size());
		assertEquals(6, p.getConsumedBytes());
		p.reset();
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(0, p.getConsumedBytes());
	}
	
	@Test
	public void testIsComplete() throws Alert {
		Parser p = new Parser(0, 0xffff, new TestExtensionParser(ExtensionType.SERVER_NAME));

		//Test data: 0,8,0,0,0,0,0,1,0,0
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(0, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(), 0),0);
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(0, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0), 0),1);
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(0, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0,8), 0),2);
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(2, p.getConsumedBytes());

		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(), 0),0);
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0), 0),1);
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0,0), 0),2);
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0,0,0), 0),3);
		assertFalse(p.isComplete());
		assertEquals(0, p.getExtensions().size());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0,0,0,0), 0),4);
		assertFalse(p.isComplete());
		assertEquals(1, p.getExtensions().size());
		assertEquals(6, p.getConsumedBytes());

		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(), 0),0);
		assertFalse(p.isComplete());
		assertEquals(1, p.getExtensions().size());
		assertEquals(6, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0), 0),1);
		assertFalse(p.isComplete());
		assertEquals(1, p.getExtensions().size());
		assertEquals(6, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0,1), 0),2);
		assertFalse(p.isComplete());
		assertEquals(1, p.getExtensions().size());
		assertEquals(6, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0,1,0), 0),3);
		assertFalse(p.isComplete());
		assertEquals(1, p.getExtensions().size());
		assertEquals(6, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array(bytes(0,1,0,0), 0),4);
		assertTrue(p.isComplete());
		assertEquals(2, p.getExtensions().size());
		assertEquals(10, p.getConsumedBytes());
	}
	
	@Test
	public void testGetConsumedBytes() throws Alert {
		Parser p = new Parser(0, 0xffff, new TestExtensionParser(ExtensionType.SERVER_NAME));
		
		ByteBufferArray array = ByteBufferArray.wrap(array(bytes(0,4,0,0,0,0),0));
		p.parse(HandshakeType.CLIENT_HELLO, array,0);
		assertEquals(6, array.remaining());
		assertEquals(0, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array,1);
		assertEquals(6, array.remaining());
		assertEquals(0, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array,2);
		assertEquals(4, array.remaining());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array,1);
		assertEquals(4, array.remaining());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array,2);
		assertEquals(4, array.remaining());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array,3);
		assertEquals(4, array.remaining());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array,4);
		assertEquals(0, array.remaining());
		assertEquals(6, p.getConsumedBytes());

		p.reset();
		array = ByteBufferArray.wrap(array(bytes(0,9,0,0,0,5,0,0,2,97,98),0));
		p.parse(HandshakeType.CLIENT_HELLO, array,10);
		assertEquals(9, array.remaining());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array,8);
		assertEquals(9, array.remaining());
		assertEquals(2, p.getConsumedBytes());
		p.parse(HandshakeType.CLIENT_HELLO, array,9);
		assertEquals(0, array.remaining());
		assertEquals(11, p.getConsumedBytes());
		
	}
	
	void assertMinMaxSizes(int min, int max, byte[] bytes, String message) {
		Parser p = new Parser(min, max, new TestExtensionParser(ExtensionType.SERVER_NAME));
		
		try {
			p.parse(HandshakeType.CLIENT_HELLO, array(bytes, 0), bytes.length);
			if (message != null) {
				fail();
			}
		}
		catch (Alert e) {
			if (message != null) {
				assertEquals(message, e.getMessage());
			}
			else {
				fail();
			}
		}
	}
	
	@Test
	public void testMinMaxSizes() {
		assertMinMaxSizes(0, 0xffff, bytes(), null);
		assertMinMaxSizes(0, 0xffff, bytes(0), null);
		assertMinMaxSizes(0, 0xffff, bytes(0,0), null);
		assertMinMaxSizes(0, 0xffff, bytes(255,255), null);

		assertMinMaxSizes(1, 0xffff, bytes(0,0), "Extensions data too small");
		assertMinMaxSizes(0, 0xffff, bytes(255,255), null);
		assertMinMaxSizes(0, 0xffff, bytes(0,1), null);
		
		assertMinMaxSizes(0, 10, bytes(0,11), "Extensions data too big");
		assertMinMaxSizes(0, 10, bytes(0,10), null);
		assertMinMaxSizes(0, 0xfffe, bytes(255,255), "Extensions data too big");
		assertMinMaxSizes(0, 0xfffe, bytes(255,254), null);
	}
	
	class Parser extends AbstractExtensionsParser {

		final ExtensionDecoder decoder;
		
		protected Parser(int minLength, int maxLength, IExtensionParser... parsers) {
			super(minLength, maxLength);
			decoder = new ExtensionDecoder();
			for (IExtensionParser parser: parsers) {
				decoder.addParser(parser);
			}
		}

		@Override
		protected IExtension parseExtension(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws Alert {
			return decoder.decode(handshakeType, srcs, remaining);
		}
		
	}
}
