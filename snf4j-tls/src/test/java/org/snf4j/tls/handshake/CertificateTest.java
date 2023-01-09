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
import org.snf4j.tls.extension.CookieExtension;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SupportedGroupsExtension;

public class CertificateTest extends HandshakeTest {
	
	@Test
	public void testParseRealData() throws AlertException {
		byte[] data = bytes(new int[] {
				0x0b,0x00,0x00,0x0f,
				0x04,0x01,0x02,0x03,0x04,
				0x00,0x00,0x07,
				0x00,0x00,0x02,0x05,0x06,
				0x00,0x00
		});
	
		IHandshake h = Certificate.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertSame(HandshakeType.CERTIFICATE, h.getType());

		Certificate ct = (Certificate) h;
		assertArrayEquals(bytes(1,2,3,4), ct.getRequestContext());
		assertEquals(1, ct.getEntries().length);
		assertArrayEquals(bytes(5,6), ct.getEntries()[0].getData());
		assertEquals(0, ct.getEntries()[0].getExtensioins().size());
		
		assertEquals(data.length-4, ct.getDataLength());
		ct.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, ct.getLength());
		buffer.clear();

		data = bytes(new int[] {
				0x0b,0x00,0x00,0x04,
				0x00,
				0x00,0x00,0x00
		});
		
		ct = (Certificate) Certificate.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);

		assertArrayEquals(bytes(), ct.getRequestContext());
		assertEquals(0, ct.getEntries().length);
		
		assertEquals(data.length-4, ct.getDataLength());
		ct.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, ct.getLength());
		buffer.clear();
		
		data = bytes(new int[] {
				0x0b,0x00,0x00,0x09,
				0x00,
				0x00,0x00,0x05,
				0x00,0x00,0x00,
				0x00,0x00
		});
		
		ct = (Certificate) Certificate.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);

		assertArrayEquals(bytes(), ct.getRequestContext());
		assertEquals(1, ct.getEntries().length);
		assertArrayEquals(bytes(), ct.getEntries()[0].getData());
		assertEquals(0, ct.getEntries()[0].getExtensioins().size());
		
		assertEquals(data.length-4, ct.getDataLength());
		ct.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, ct.getLength());
		buffer.clear();
	}
	
	@Test
	public void testParser() {
		assertSame(HandshakeType.CERTIFICATE, Certificate.getParser().getType());
		assertSame(Certificate.getParser().getType(), Certificate.getParser().getType());
	}

	@Test
	public void testGetRequestContext() throws Exception {
		Certificate ct = new Certificate(bytes(33,34), new ICertificateEntry[0]);
		ct.getBytes(buffer);
		assertEquals(6, ct.getDataLength());
		assertArrayEquals(bytes(11,0,0,6,2,33,34), buffer(0,7));
		ct = (Certificate) Certificate.getParser().parse(array(buffer(), 4), 6, ExtensionDecoder.DEFAULT);
		assertArrayEquals(bytes(33,34), ct.getRequestContext());
		buffer.clear();
		
		ct = new Certificate(bytes(), new ICertificateEntry[0]);
		ct.getBytes(buffer);
		assertEquals(4, ct.getDataLength());
		assertArrayEquals(bytes(11,0,0,4,0,0,0,0), buffer());
		ct = (Certificate) Certificate.getParser().parse(array(buffer(), 4), 4, ExtensionDecoder.DEFAULT);
		assertArrayEquals(bytes(), ct.getRequestContext());
	}
	
	ICertificateEntry[] entries(List<IExtension> extensions, byte[]... certs) {
		ICertificateEntry[] entries = new ICertificateEntry[certs.length];
		for(int i=0; i<certs.length; ++i) {
			entries[i] = new CertificateEntry(certs[i], extensions);
		}
		return entries;
	}
	
	@Test
	public void testGetEntries() throws Exception {
		List<IExtension> extensions = new ArrayList<IExtension>();
		Certificate ct = new Certificate(bytes(), entries(extensions, bytes(1,2,3)));
		ct.getBytes(buffer);
		assertEquals(12, ct.getDataLength());
		assertArrayEquals(bytes(11,0,0,12,0), buffer(0,5));
		assertArrayEquals(bytes(0,0,8,0,0,3,1,2,3,0,0), buffer(5,11));
		ct = (Certificate) Certificate.getParser().parse(array(buffer(), 4), 12, ExtensionDecoder.DEFAULT);
		assertEquals(1, ct.getEntries().length);
		assertArrayEquals(bytes(1,2,3), ct.getEntries()[0].getData());
		assertEquals(0, ct.getEntries()[0].getExtensioins().size());
		buffer.clear();
		
		byte[] data = bytes(0x1ffff, (byte)1, (byte)2, (byte)3);
		ct = new Certificate(bytes(), entries(extensions, data));
		ct.getBytes(buffer);
		assertEquals(131080, ct.getDataLength());
		assertArrayEquals(bytes(11,2,0,8,0), buffer(0,5));
		assertArrayEquals(bytes(2,0,4,1,255,255,1,2,2,2,2), buffer(5,11));
		ct = (Certificate) Certificate.getParser().parse(array(buffer(), 4), 131080, ExtensionDecoder.DEFAULT);
		assertEquals(1, ct.getEntries().length);
		assertArrayEquals(data, ct.getEntries()[0].getData());
		assertEquals(0, ct.getEntries()[0].getExtensioins().size());
		buffer.clear();
		
		data = bytes(0x1ffff-5, (byte)1, (byte)2, (byte)3);
		ct = new Certificate(bytes(), entries(extensions, data));
		ct.getBytes(buffer);
		assertEquals(131080-5, ct.getDataLength());
		assertArrayEquals(bytes(11,2,0,3,0), buffer(0,5));
		assertArrayEquals(bytes(1,255,255,1,255,250,1,2,2,2,2), buffer(5,11));
		ct = (Certificate) Certificate.getParser().parse(array(buffer(), 4), 131080-5, ExtensionDecoder.DEFAULT);
		assertEquals(1, ct.getEntries().length);
		assertArrayEquals(data, ct.getEntries()[0].getData());
		assertEquals(0, ct.getEntries()[0].getExtensioins().size());
		buffer.clear();
		
		data = bytes(0x1ffff-9, (byte)1, (byte)2, (byte)3);
		ct = new Certificate(bytes(), entries(extensions, data));
		ct.getBytes(buffer);
		assertEquals(131080-9, ct.getDataLength());
		assertArrayEquals(bytes(11,1,255,255,0), buffer(0,5));
		assertArrayEquals(bytes(1,255,251,1,255,246,1,2,2,2,2), buffer(5,11));
		ct = (Certificate) Certificate.getParser().parse(array(buffer(), 4), 131080-9, ExtensionDecoder.DEFAULT);
		assertEquals(1, ct.getEntries().length);
		assertArrayEquals(data, ct.getEntries()[0].getData());
		assertEquals(0, ct.getEntries()[0].getExtensioins().size());
		buffer.clear();

		ct = new Certificate(bytes(), entries(extensions, bytes(1), bytes(2,3)));
		ct.getBytes(buffer);
		assertEquals(17, ct.getDataLength());
		assertArrayEquals(bytes(11,0,0,17,0), buffer(0,5));
		assertArrayEquals(bytes(0,0,13,0,0,1,1,0,0,0,0,2,2,3,0,0), buffer(5,16));
		ct = (Certificate) Certificate.getParser().parse(array(buffer(), 4), 17, ExtensionDecoder.DEFAULT);
		assertEquals(2, ct.getEntries().length);
		assertArrayEquals(bytes(1), ct.getEntries()[0].getData());
		assertEquals(0, ct.getEntries()[0].getExtensioins().size());
		assertArrayEquals(bytes(2,3), ct.getEntries()[1].getData());
		assertEquals(0, ct.getEntries()[1].getExtensioins().size());
		assertEquals(0, ct.getExtensioins().size());
		buffer.clear();
	
		extensions.add(new ServerNameExtension("abc"));
		extensions.add(new CookieExtension(bytes(1,2,3)));
		ct = new Certificate(bytes(), entries(extensions, bytes(1)));
		ct.getBytes(buffer);
		assertEquals(31, ct.getDataLength());
		ct = (Certificate) Certificate.getParser().parse(array(buffer(), 4), 31, ExtensionDecoder.DEFAULT);
		assertEquals(1, ct.getEntries().length);
		assertArrayEquals(bytes(1), ct.getEntries()[0].getData());
		assertEquals(2, ct.getEntries()[0].getExtensioins().size());
		assertSame(ExtensionType.SERVER_NAME, ct.getEntries()[0].getExtensioins().get(0).getType());
		assertSame(ExtensionType.COOKIE, ct.getEntries()[0].getExtensioins().get(1).getType());
		assertEquals(2,ct.getExtensioins().size());
		assertSame(ExtensionType.SERVER_NAME, ct.getExtensioins().get(0).getType());
		assertSame(ExtensionType.COOKIE, ct.getExtensioins().get(1).getType());
		buffer.clear();

		ct = new Certificate(bytes(), entries(extensions, bytes(1),bytes(2)));
		ct.getBytes(buffer);
		assertEquals(58, ct.getDataLength());
		ct = (Certificate) Certificate.getParser().parse(array(buffer(), 4), 58, ExtensionDecoder.DEFAULT);
		assertEquals(2, ct.getEntries().length);
		assertArrayEquals(bytes(1), ct.getEntries()[0].getData());
		assertEquals(2, ct.getEntries()[0].getExtensioins().size());
		assertArrayEquals(bytes(2), ct.getEntries()[1].getData());
		assertEquals(2, ct.getEntries()[1].getExtensioins().size());
		assertSame(ExtensionType.SERVER_NAME, ct.getEntries()[0].getExtensioins().get(0).getType());
		assertSame(ExtensionType.COOKIE, ct.getEntries()[0].getExtensioins().get(1).getType());
		assertSame(ExtensionType.SERVER_NAME, ct.getEntries()[1].getExtensioins().get(0).getType());
		assertSame(ExtensionType.COOKIE, ct.getEntries()[1].getExtensioins().get(1).getType());
		assertEquals(4,ct.getExtensioins().size());
		assertSame(ExtensionType.SERVER_NAME, ct.getExtensioins().get(0).getType());
		assertSame(ExtensionType.COOKIE, ct.getExtensioins().get(1).getType());
		assertSame(ExtensionType.SERVER_NAME, ct.getExtensioins().get(2).getType());
		assertSame(ExtensionType.COOKIE, ct.getExtensioins().get(3).getType());
		buffer.clear();
	}
	
	@Test
	public void testParsingFailures() throws Exception {
		List<IExtension> extensions = new ArrayList<IExtension>();
		extensions.add(new SupportedGroupsExtension(NamedGroup.X25519, NamedGroup.X448));
		extensions.add(new ServerNameExtension());

		Certificate ct = new Certificate(new byte[10], entries(extensions, bytes(2,3), bytes(4,5,6)));
		ct.getBytes(buffer);
		buffer.put((byte) 255);
		byte[] bytes = buffer();
		
		for (int i=4; i<=bytes.length; ++i) {
			try {
				Certificate.getParser().parse(array(bytes, 4), i-4, ExtensionDecoder.DEFAULT);
				if (i != bytes.length-1) {
					fail();
				}
			} catch (DecodeErrorAlertException e) {
				assertEquals("Handshake message 'certificate' parsing failure: Inconsistent length", e.getMessage());
			}
		}		
		bytes[37+4] += 15;
		try {
			Certificate.getParser().parse(array(bytes, 4), bytes.length-5, ExtensionDecoder.DEFAULT);
			fail();
		} catch (DecodeErrorAlertException e) {
			assertEquals("Handshake message 'certificate' parsing failure: Inconsistent length", e.getMessage());
		}
		bytes[37+4] -= 15;

		bytes[42+4] += 1;
		try {
			Certificate.getParser().parse(array(bytes, 4), bytes.length-5, ExtensionDecoder.DEFAULT);
			fail();
		} catch (DecodeErrorAlertException e) {
			assertEquals("Handshake message 'certificate' parsing failure: Inconsistent length", e.getMessage());
		}
	}

}
