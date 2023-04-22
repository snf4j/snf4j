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
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.extension.CookieExtension;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SupportedGroupsExtension;

public class CertificateRequestTest extends HandshakeTest {

	@Test
	public void testParseRealData() throws Exception {
		byte[] data = bytes(new int[] {
				0x0d,0x00,0x00,0x07,
				0x04,0x01,0x02,0x03,0x04,
				0x00,0x00
		});

		IHandshake h = CertificateRequest.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertSame(HandshakeType.CERTIFICATE_REQUEST, h.getType());

		CertificateRequest cr = (CertificateRequest) h;
		assertArrayEquals(bytes(1,2,3,4), cr.getContext());
		assertEquals(0, cr.getExtensions().size());
		
		assertEquals(data.length-4, cr.getDataLength());
		cr.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, cr.getLength());
		buffer.clear();

		data = bytes(new int[] {
				0x0d,0x00,0x00,0x03,
				0x00,
				0x00,0x00
		});

		cr = (CertificateRequest) CertificateRequest.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertArrayEquals(bytes(), cr.getContext());
		assertEquals(0, cr.getExtensions().size());
		
		assertEquals(data.length-4, cr.getDataLength());
		cr.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, cr.getLength());
		buffer.clear();
	}
	
	@Test
	public void testParser() {
		assertSame(HandshakeType.CERTIFICATE_REQUEST, CertificateRequest.getParser().getType());
		assertSame(Certificate.getParser(), Certificate.getParser());
	}

	@Test
	public void testGetContext() throws Exception {
		List<IExtension> extensions = new ArrayList<IExtension>();
		
		CertificateRequest cr = new CertificateRequest(extensions);
		cr.getBytes(buffer);
		assertEquals(3, cr.getDataLength());
		assertArrayEquals(bytes(13,0,0,3,0,0,0), buffer());
		cr = (CertificateRequest) CertificateRequest.getParser().parse(array(buffer(), 4), 3, ExtensionDecoder.DEFAULT);
		assertArrayEquals(bytes(), cr.getContext());
		assertEquals(0, cr.getExtensions().size());
		buffer.clear();
		
		extensions.add(new CookieExtension(bytes(10,11)));
		cr = new CertificateRequest(bytes(1,2,3), extensions);
		cr.getBytes(buffer);
		assertEquals(14, cr.getDataLength());
		assertArrayEquals(bytes(13,0,0,14,3,1,2,3,0,8,0,44,0,4,0,2,10,11), buffer());
		cr = (CertificateRequest) CertificateRequest.getParser().parse(array(buffer(), 4), 14, ExtensionDecoder.DEFAULT);
		assertArrayEquals(bytes(1,2,3), cr.getContext());
		assertEquals(1,cr.getExtensions().size());
		assertArrayEquals(bytes(10,11),((CookieExtension)cr.getExtensions().get(0)).getCookie());
	}
	
	@Test
	public void testParsingFailures() throws Exception {
		List<IExtension> extensions = new ArrayList<IExtension>();
		extensions.add(new SupportedGroupsExtension(NamedGroup.X25519, NamedGroup.X448));
		extensions.add(new ServerNameExtension());
		
		CertificateRequest cr = new CertificateRequest(bytes(1,2,3,4,5), extensions);
		
		cr.getBytes(buffer);
		buffer.put((byte) 255);
		byte[] bytes = buffer();
		
		for (int i=4; i<=bytes.length; ++i) {
			try {
				CertificateRequest.getParser().parse(array(bytes, 4), i-4, ExtensionDecoder.DEFAULT);
				if (i != bytes.length-1) {
					fail();
				}
			} catch (DecodeErrorAlert e) {
				assertEquals("Handshake message 'certificate_request' parsing failure: Inconsistent length", e.getMessage());
			}
		}
	}	
}
