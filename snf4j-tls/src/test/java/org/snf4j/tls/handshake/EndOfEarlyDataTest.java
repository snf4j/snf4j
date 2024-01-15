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

public class EndOfEarlyDataTest extends HandshakeTest {

	@Test
	public void testParseRealData() throws Alert {
		byte[] data = bytes(new int[] {
				0x05,0x00,0x00,0x00
		});
	
		IHandshake h = EndOfEarlyData.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertSame(HandshakeType.END_OF_EARLY_DATA, h.getType());

		EndOfEarlyData eoed = (EndOfEarlyData) h;
		assertNull(eoed.getExtensions());
		
		assertEquals(data.length-4, eoed.getDataLength());
		eoed.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, eoed.getLength());
	}

	@Test
	public void testParser() {
		assertSame(HandshakeType.END_OF_EARLY_DATA, EndOfEarlyData.getParser().getType());
		assertSame(EndOfEarlyData.getParser().getType(), EndOfEarlyData.getParser().getType());
	}

	@Test
	public void testParsingFailures() throws Exception {
		EndOfEarlyData eoed = new EndOfEarlyData();
		eoed.getBytes(buffer);
		buffer.put((byte) 255);
		byte[] bytes = buffer();
		
		for (int i=4; i<=bytes.length; ++i) {
			try {
				EndOfEarlyData.getParser().parse(array(bytes, 4), i-4, ExtensionDecoder.DEFAULT);
				if (i != bytes.length-1) {
					fail();
				}
			} catch (DecodeErrorAlert e) {
				assertEquals("Handshake message 'end_of_early_data' parsing failure: Inconsistent length", e.getMessage());
			}
		}		
	}	
}
