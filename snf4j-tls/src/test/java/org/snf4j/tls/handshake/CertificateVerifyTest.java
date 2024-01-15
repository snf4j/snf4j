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
import org.snf4j.tls.extension.SignatureScheme;

public class CertificateVerifyTest extends HandshakeTest {
	
	@Test
	public void testParseRealData() throws Alert {
		byte[] data = bytes(new int[] {
				0x0f,0x00,0x00,0x08,
				0x04,0x03,
				0x00,0x04,
				0x01,0x02,0x03,0x04,
		});
	
		IHandshake h = CertificateVerify.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertSame(HandshakeType.CERTIFICATE_VERIFY, h.getType());
		
		CertificateVerify cv = (CertificateVerify) h;
		assertArrayEquals(bytes(1,2,3,4), cv.getSignature());
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, cv.getAlgorithm());
		assertNull(cv.getExtensions());
		
		assertEquals(data.length-4, cv.getDataLength());
		cv.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, cv.getLength());
		buffer.clear();

		data = bytes(new int[] {
				0x0f,0x00,0x00,0x04,
				0x04,0x03,
				0x00,0x00,
		});

		cv = (CertificateVerify) CertificateVerify.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertArrayEquals(bytes(), cv.getSignature());
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, cv.getAlgorithm());
		
		assertEquals(data.length-4, cv.getDataLength());
		cv.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, cv.getLength());
		buffer.clear();
	}
	
	@Test
	public void testParser() {
		assertSame(HandshakeType.CERTIFICATE_VERIFY, CertificateVerify.getParser().getType());
		assertSame(CertificateVerify.getParser().getType(), CertificateVerify.getParser().getType());
	}
	
	@Test
	public void testGetAlgorithm() throws Exception {
		CertificateVerify cv = new CertificateVerify(SignatureScheme.ECDSA_SECP384R1_SHA384,bytes());
		cv.getBytes(buffer);
		assertEquals(4, cv.getDataLength());
		assertArrayEquals(bytes(15,0,0,4,5,3,0,0), buffer(0,8));
		cv = (CertificateVerify) CertificateVerify.getParser().parse(array(buffer(), 4), 4, ExtensionDecoder.DEFAULT);
		assertSame(SignatureScheme.ECDSA_SECP384R1_SHA384, cv.getAlgorithm());
		assertEquals(0, cv.getSignature().length);
	}

	@Test
	public void testGetSignature() throws Exception {
		CertificateVerify cv = new CertificateVerify(SignatureScheme.ECDSA_SECP384R1_SHA384,bytes(1,2,3));
		cv.getBytes(buffer);
		assertEquals(7, cv.getDataLength());
		assertArrayEquals(bytes(15,0,0,7,5,3), buffer(0,6));
		assertArrayEquals(bytes(0,3,1,2,3), buffer(6,5));
		cv = (CertificateVerify) CertificateVerify.getParser().parse(array(buffer(), 4), 7, ExtensionDecoder.DEFAULT);
		assertSame(SignatureScheme.ECDSA_SECP384R1_SHA384, cv.getAlgorithm());
		assertArrayEquals(bytes(1,2,3), cv.getSignature());
		buffer.clear();
		
		byte[] signature = bytes(0x1f0, (byte)1, (byte)2, (byte)3);
		cv = new CertificateVerify(SignatureScheme.ECDSA_SECP384R1_SHA384,signature);
		cv.getBytes(buffer);
		assertEquals(0x1f0+4, cv.getDataLength());
		assertArrayEquals(bytes(15,0,1,0xf4,5,3), buffer(0,6));
		assertArrayEquals(cat(bytes(1,0xf0),signature), buffer(6,2+signature.length));
		cv = (CertificateVerify) CertificateVerify.getParser().parse(array(buffer(), 4), 0x1f0+4, ExtensionDecoder.DEFAULT);
		assertSame(SignatureScheme.ECDSA_SECP384R1_SHA384, cv.getAlgorithm());
		assertArrayEquals(signature, cv.getSignature());
	}
	
	@Test
	public void testParsingFailures() throws Exception {

		CertificateVerify cv = new CertificateVerify(SignatureScheme.ECDSA_SECP384R1_SHA384, bytes(1,2,3,4));
		cv.getBytes(buffer);
		buffer.put((byte) 255);
		byte[] bytes = buffer();
		
		for (int i=4; i<=bytes.length; ++i) {
			try {
				CertificateVerify.getParser().parse(array(bytes, 4), i-4, ExtensionDecoder.DEFAULT);
				if (i != bytes.length-1) {
					fail();
				}
			} catch (DecodeErrorAlert e) {
				assertEquals("Handshake message 'certificate_verify' parsing failure: Inconsistent length", e.getMessage());
			}
		}		
	}

}
