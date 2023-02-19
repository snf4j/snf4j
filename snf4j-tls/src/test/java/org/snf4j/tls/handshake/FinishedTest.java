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

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.extension.ExtensionDecoder;

public class FinishedTest extends HandshakeTest {
	
	@Test
	public void testParseRealData() throws Alert {
		byte[] data = bytes(new int[] {
				0x14,0x00,0x00,0x04,
				0x01,0x02,0x03,0x04
		});
	
		IHandshake h = Finished.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertSame(HandshakeType.FINISHED, h.getType());

		Finished f = (Finished) h;
		assertArrayEquals(bytes(1,2,3,4), f.getVerifyData());
		assertNull(f.getExtensions());
		
		assertEquals(data.length-4, f.getDataLength());
		f.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, f.getLength());
	}
	
	@Test
	public void testParser() {
		assertSame(HandshakeType.FINISHED, Finished.getParser().getType());
		assertSame(Finished.getParser().getType(), Finished.getParser().getType());
	}

	@Test
	public void testgetVerifyData() throws Exception {
		byte[] data = bytes(32, (byte)1, (byte)2, (byte)3);
		Finished cv = new Finished(data);
		cv.getBytes(buffer);
		assertEquals(32, cv.getDataLength());
		assertArrayEquals(cat(bytes(20,0,0,32),data), buffer());
		cv = (Finished) Finished.getParser().parse(array(buffer(), 4), 32, ExtensionDecoder.DEFAULT);
		assertArrayEquals(data, cv.getVerifyData());
	}

}
