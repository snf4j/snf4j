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
package org.snf4j.tls.handshake;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.extension.ExtensionDecoder;

public class KeyUpdateTest extends HandshakeTest {
	
	@Test
	public void testParseRealData() throws Alert {
		byte[] data = bytes(("18 00 00 01 00").replace(" ", ""));
		
		IHandshake h = KeyUpdate.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertSame(HandshakeType.KEY_UPDATE, h.getType());
		
		KeyUpdate ku = (KeyUpdate) h;
		assertNull(ku.getExtensions());
		assertSame(KeyUpdateRequest.UPDATE_NOT_REQUESTED, ku.getRequest());
		
		assertEquals(1, ku.getDataLength());
		ku.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, ku.getLength());
		buffer.clear();
		
		data = bytes(("18 00 00 01 01").replace(" ", ""));
		ku = (KeyUpdate) KeyUpdate.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertSame(KeyUpdateRequest.UPDATE_REQUESTED, ku.getRequest());
		assertEquals(1, ku.getDataLength());
		ku.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, ku.getLength());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgument1() {
		new KeyUpdate(null);
	}
	
	@Test
	public void testParser() {
		assertSame(HandshakeType.KEY_UPDATE, KeyUpdate.getParser().getType());
		assertSame(KeyUpdate.getParser().getType(), KeyUpdate.getParser().getType());
	}
	
	@Test
	public void testRequest() {
		KeyUpdate ku = new KeyUpdate(false);
		assertSame(KeyUpdateRequest.UPDATE_NOT_REQUESTED, ku.getRequest());
		ku = new KeyUpdate(true);
		assertSame(KeyUpdateRequest.UPDATE_REQUESTED, ku.getRequest());
		ku = new KeyUpdate(KeyUpdateRequest.UPDATE_NOT_REQUESTED);
		assertSame(KeyUpdateRequest.UPDATE_NOT_REQUESTED, ku.getRequest());
		ku = new KeyUpdate(KeyUpdateRequest.UPDATE_REQUESTED);
		assertSame(KeyUpdateRequest.UPDATE_REQUESTED, ku.getRequest());
	}
	
	@Test
	public void testParsingFailures() throws Exception {
		KeyUpdate ku = new KeyUpdate(false);
		
		ku.getBytes(buffer);
		buffer.put((byte) 255);
		byte[] bytes = buffer();
		
		for (int i=4; i<=bytes.length; ++i) {
			try {
				KeyUpdate.getParser().parse(array(bytes, 4), i-4, ExtensionDecoder.DEFAULT);
				if (i != bytes.length-1) {
					fail();
				}
			} catch (DecodeErrorAlert e) {
				assertEquals("Handshake message 'key_update' parsing failure: Inconsistent length", e.getMessage());
			}
		}

	}
}
