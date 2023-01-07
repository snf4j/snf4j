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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.DecodeErrorAlertException;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SupportedGroupsExtension;

public class EncryptedExtensionsTest extends HandshakeTest {

	@Override
	public void before() throws Exception {
		super.before();
	}
	
	@Test
	public void testParseRealData() throws AlertException {
		byte[] data = bytes(new int[] {
				0x08,0x00,0x00,0x0c,
				0x00,0x0a,0x00,0x2b,0x00,0x02,0x03,0x04,
				0x00,0x00,0x00,0x00
		});
		
		IHandshake h = EncryptedExtensions.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertSame(HandshakeType.ENCRYPTED_EXTENSIONS, h.getType());

		EncryptedExtensions ee = (EncryptedExtensions) h;
		assertEquals(2, ee.getExtensioins().size());
		assertSame(ExtensionType.SUPPORTED_VERSIONS, ee.getExtensioins().get(0).getType());
		assertSame(ExtensionType.SERVER_NAME, ee.getExtensioins().get(1).getType());

		assertEquals(data.length-4, ee.getDataLength());
		ee.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, ee.getLength());
		buffer.clear();
		
		data = bytes(new int[] {
				0x08,0x00,0x00,0x02,0x00,0x00
		});
		
		ee = (EncryptedExtensions) EncryptedExtensions.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertEquals(0, ee.getExtensioins().size());

		assertEquals(data.length-4, ee.getDataLength());
		ee.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, ee.getLength());
	}
	
	@Test
	public void testParser() {
		assertSame(HandshakeType.ENCRYPTED_EXTENSIONS, EncryptedExtensions.getParser().getType());
		assertSame(EncryptedExtensions.getParser().getType(), EncryptedExtensions.getParser().getType());
	}

	@Test
	public void testGetExtensions() throws Exception {
		List<IExtension> extensions = new ArrayList<IExtension>();
		extensions.add(new SupportedGroupsExtension(NamedGroup.X25519, NamedGroup.X448));
		extensions.add(new ServerNameExtension());
		
		EncryptedExtensions ee = new EncryptedExtensions(extensions);
		ee.getBytes(buffer);
		assertEquals(16, ee.getDataLength());
		assertArrayEquals(bytes(8,0,0,16,0,14,0,10,0,6,0,4,0,0x1d,0,0x1e,0,0,0,0), buffer(0,20));
		ee = (EncryptedExtensions) EncryptedExtensions.getParser().parse(array(buffer(), 4), 16, ExtensionDecoder.DEFAULT);
		assertEquals(2, ee.getExtensioins().size());	
		buffer.clear();
		
		extensions.clear();
		ee = new EncryptedExtensions(extensions);
		ee.getBytes(buffer);
		assertEquals(2, ee.getDataLength());
		assertArrayEquals(bytes(8,0,0,2,0,0), buffer(0,6));
		ee = (EncryptedExtensions) EncryptedExtensions.getParser().parse(array(buffer(), 4), 2, ExtensionDecoder.DEFAULT);
		assertEquals(0, ee.getExtensioins().size());	
	}

	@Test
	public void testParsingFailures() throws Exception {
		List<IExtension> extensions = new ArrayList<IExtension>();
		extensions.add(new SupportedGroupsExtension(NamedGroup.X25519, NamedGroup.X448));
		extensions.add(new ServerNameExtension());

		EncryptedExtensions ch = new EncryptedExtensions(extensions);
		ch.getBytes(buffer);
		buffer.put((byte) 255);
		byte[] bytes = buffer();
		
		for (int i=4; i<=bytes.length; ++i) {
			try {
				EncryptedExtensions.getParser().parse(array(bytes, 4), i-4, ExtensionDecoder.DEFAULT);
				if (i != bytes.length-1) {
					fail();
				}
			} catch (DecodeErrorAlertException e) {
				assertEquals("Handshake message 'encrypted_extensions' parsing failure: Inconsistent length", e.getMessage());
			}
		}		
	}

}
