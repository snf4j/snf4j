/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.core.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.session.ssl.SSLContextBuilderTest;
import org.snf4j.core.util.PemUtil.Label;

public class PemUtilTest {

	static final String[] PUBLIC_KEY = new String[] {
			"-----BEGIN PUBLIC KEY-----",
			"MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEn1LlwLN/KBYQRVH6HfIMTzfEqJOVztLe",
			"kLchp2hi78cCaMY81FBlYs8J9l7krc+M4aBeCGYFjba+hiXttJWPL7ydlE+5UG4U",
			"Nkn3Eos8EiZByi9DVsyfy9eejh+8AXgp",
			"-----END PUBLIC KEY-----"
	};
	
	static final String[] PRIVATE_KEY = new String[] {
			"-----BEGIN PRIVATE KEY-----",
			"MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQgVcB/UNPxalR9zDYAjQIf",
			"jojUDiQuGnSJrFEEzZPT/92hRANCAASc7UJtgnF/abqWM60T3XNJEzBv5ez9TdwK",
			"H0M6xpM2q+53wmsN/eYLdgtjgBd3DBmHtPilCkiFICXyaA8z9LkJ",
			"-----END PRIVATE KEY-----"
	};

	static final String[] CERTIFICATE = new String[] {
			"-----BEGIN CERTIFICATE-----",
			"MIICLDCCAdKgAwIBAgIBADAKBggqhkjOPQQDAjB9MQswCQYDVQQGEwJCRTEPMA0G",
			"A1UEChMGR251VExTMSUwIwYDVQQLExxHbnVUTFMgY2VydGlmaWNhdGUgYXV0aG9y",
			"aXR5MQ8wDQYDVQQIEwZMZXV2ZW4xJTAjBgNVBAMTHEdudVRMUyBjZXJ0aWZpY2F0",
			"ZSBhdXRob3JpdHkwHhcNMTEwNTIzMjAzODIxWhcNMTIxMjIyMDc0MTUxWjB9MQsw",
			"CQYDVQQGEwJCRTEPMA0GA1UEChMGR251VExTMSUwIwYDVQQLExxHbnVUTFMgY2Vy",
			"dGlmaWNhdGUgYXV0aG9yaXR5MQ8wDQYDVQQIEwZMZXV2ZW4xJTAjBgNVBAMTHEdu",
			"dVRMUyBjZXJ0aWZpY2F0ZSBhdXRob3JpdHkwWTATBgcqhkjOPQIBBggqhkjOPQMB",
			"BwNCAARS2I0jiuNn14Y2sSALCX3IybqiIJUvxUpj+oNfzngvj/Niyv2394BWnW4X",
			"uQ4RTEiywK87WRcWMGgJB5kX/t2no0MwQTAPBgNVHRMBAf8EBTADAQH/MA8GA1Ud",
			"DwEB/wQFAwMHBgAwHQYDVR0OBBYEFPC0gf6YEr+1KLlkQAPLzB9mTigDMAoGCCqG",
			"SM49BAMCA0gAMEUCIDGuwD1KPyG+hRf88MeyMQcqOFZD0TbVleF+UsAGQ4enAiEA",
			"l4wOuDwKQa+upc8GftXE2C//4mKANBC6It01gUaTIpo=",
			"-----END CERTIFICATE-----"
	};	
	
	byte[] pem(String[] lines, String newLine, String head, String foot) {
		StringBuilder sb = new StringBuilder(1000);
		int len = lines.length;
		
		sb.append(head);
		for (int i=0; i<len; ++i) {
			sb.append(lines[i]);
			if (i < len-1) {
				sb.append(newLine);
			}
		}
		sb.append(foot);
		return sb.toString().getBytes(StandardCharsets.US_ASCII);
	}
	
	byte[] body(String[] lines, String newLine) {
		StringBuilder sb = new StringBuilder(1000);
		int len = lines.length-1;
		
		for (int i=1; i<len; ++i) {
			sb.append(lines[i]);
			if (i < len-1) {
				sb.append(newLine);
			}
		}
		return sb.toString().getBytes(StandardCharsets.US_ASCII);
	}
	
	@Test
	public void testRead() {
		List<byte[]> l;
		String[] s;
		byte[] b, pem;
		int i;
		
		l = PemUtil.read(Label.PUBLIC_KEY, pem(PUBLIC_KEY, "\r\n", "", ""));
		assertEquals(1, l.size());
		b = Base64.getMimeDecoder().decode(body(PUBLIC_KEY, "\r\n"));
		assertArrayEquals(b, l.get(0));
		b = Base64.getDecoder().decode(body(PUBLIC_KEY, ""));
		assertArrayEquals(b, l.get(0));
		
		assertEquals(0, PemUtil.read(Label.PUBLIC_KEY, pem(PRIVATE_KEY, "\r\n", "", "")).size());

		l = PemUtil.read("PUBLIC KEY", pem(PUBLIC_KEY, "\r\n", "", ""));
		assertEquals(1, l.size());
		assertArrayEquals(b, l.get(0));

		s = new String[] {
				"-----BEGIN PUBLIC KEY-----",
				"ABCD",
				"-----END PUBLIC KEY-----",
				"-----BEGIN PUBLIC KEY-----",
				"-----END PUBLIC KEY-----"
		};
		pem = pem(s, "", "", ""); 
		i = s[0].length() + s[1].length() + s[2].length();
		assertEquals(2, (l = PemUtil.read("PUBLIC KEY", pem, 0, pem.length)).size());
		assertEquals(3, l.get(0).length);
		assertEquals(0, l.get(1).length);
		assertEquals(1, (l = PemUtil.read("PUBLIC KEY", pem, 1, pem.length-1)).size());
		assertEquals(0, l.get(0).length);
		assertEquals(1, (l = PemUtil.read("PUBLIC KEY", pem, i, pem.length-i)).size());
		assertEquals(0, l.get(0).length);
		assertEquals(0, (l = PemUtil.read("PUBLIC KEY", pem, i+1, pem.length-i-1)).size());
		assertEquals(1, (l = PemUtil.read("PUBLIC KEY", pem, 0, i)).size());
		assertEquals(3, l.get(0).length);
		assertEquals(0, (l = PemUtil.read("PUBLIC KEY", pem, 0, i-1)).size());
		assertEquals(1, (l = PemUtil.read("PUBLIC KEY", pem, i, pem.length-i)).size());
		assertEquals(0, l.get(0).length);
		assertEquals(0, (l = PemUtil.read("PUBLIC KEY", pem, i, pem.length-i-1)).size());

		assertEquals(2, (l = PemUtil.read(Label.PUBLIC_KEY, pem, 0, pem.length)).size());
		assertEquals(3, l.get(0).length);
		assertEquals(0, l.get(1).length);
		assertEquals(1, (l = PemUtil.read(Label.PUBLIC_KEY, pem, 1, pem.length-1)).size());
		assertEquals(0, l.get(0).length);
		assertEquals(1, (l = PemUtil.read(Label.PUBLIC_KEY, pem, i, pem.length-i)).size());
		assertEquals(0, l.get(0).length);
		assertEquals(0, (l = PemUtil.read(Label.PUBLIC_KEY, pem, i+1, pem.length-i-1)).size());
		assertEquals(1, (l = PemUtil.read(Label.PUBLIC_KEY, pem, 0, i)).size());
		assertEquals(3, l.get(0).length);
		assertEquals(0, (l = PemUtil.read(Label.PUBLIC_KEY, pem, 0, i-1)).size());
		assertEquals(1, (l = PemUtil.read(Label.PUBLIC_KEY, pem, i, pem.length-i)).size());
		assertEquals(0, l.get(0).length);
		assertEquals(0, (l = PemUtil.read(Label.PUBLIC_KEY, pem, i, pem.length-i-1)).size());
	}
	
	@Test
	public void testMultiRead() {
		List<byte[]> l;
		String[] s;
		
		s = new String[] {
				"-----BEGIN PUBLIC KEY-----",
				"AA\nA=",
				"-----END PUBLIC KEY-----",
				"123",
				"-----BEGIN PUBLIC KEY-----",
				"BAA=",
				"-----END PUBLIC KEY-----"
		};
		l = PemUtil.read(Label.PUBLIC_KEY, pem(s, "\r\n", "", ""));
		assertEquals(2, l.size());
		assertEquals("AAA=", Base64.getEncoder().encodeToString(l.get(0)));
		assertEquals("BAA=", Base64.getEncoder().encodeToString(l.get(1)));
		
		s = new String[] {
				"-----BEGIN PUBLIC KEY-----",
				"AA\nA=",
				"-----END PUBLIC KEY-----",
				"-----BEGIN PRIVATE KEY-----",
				"CCA=",
				"-----END PRIVATE KEY-----",
				"-----BEGIN PUBLIC KEY-----",
				"BAA=",
				"-----END PUBLIC KEY-----"
		};
		l = PemUtil.read(Label.PRIVATE_KEY, pem(s, "\r\n", "", ""));
		assertEquals(1, l.size());
		assertEquals("CCA=", Base64.getEncoder().encodeToString(l.get(0)));
	}
	
	@Test
	public void testReadFailures() {
		String[] s;
		
		s = new String[] {
				"-----BEGIN PUBLIC KEY----",
				"AAA=",
				"-----END PUBLIC KEY-----"
		};
		assertEquals(0, PemUtil.read(Label.PUBLIC_KEY, pem(s, "\r\n", "", "")).size());

		s = new String[] {
				"-----BEGIN PUBLIC KEY-----",
				"AAA=",
				"-----END PRIVATE KEY-----"
		};
		assertEquals(0, PemUtil.read(Label.PUBLIC_KEY, pem(s, "\r\n", "", "")).size());

		s = new String[] {
				"-----BEGIN PUBLIC KEY-----",
				"AAA=",
				"-----END PUBLIC KEY----"
		};
		assertEquals(0, PemUtil.read(Label.PUBLIC_KEY, pem(s, "\r\n", "", "")).size());

		s = new String[] {
				"-----BEGIN PUBLIC KEY-----",
				"AAA=",
		};
		assertEquals(0, PemUtil.read(Label.PUBLIC_KEY, pem(s, "\r\n", "", "")).size());

		s = new String[] {
				"-----BEGIN PUBLIC KEY-----",
				"A=",
				"-----END PUBLIC KEY-----"
		};
		assertNull(PemUtil.read(Label.PUBLIC_KEY, pem(s, "\r\n", "", "")));
	}	
	
	@Test
	public void testReadCertificate() throws Exception {
		byte[] pem = pem(CERTIFICATE, "\r\n", "", "");
		byte[] der = PemUtil.read(Label.CERTIFICATE, pem).get(0);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");

		X509Certificate cert1 = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(pem));
		X509Certificate cert2 = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
		assertEquals(cert1, cert2);
		
		Security.getProviders();
	}
	
	@Test
	public void testReadInputStream() throws Exception {
		ByteArrayInputStream in;
		List<byte[]> l;
		String[] s;
		
		s = new String[] {
				"-----BEGIN PUBLIC KEY-----",
				"ABCD",
				"-----END PUBLIC KEY-----"
		};
		in = new ByteArrayInputStream(pem(s, "\r\n", "", ""));
		assertEquals(1, (l = PemUtil.read(Label.PUBLIC_KEY, in)).size());
		assertEquals("ABCD", Base64.getEncoder().encodeToString(l.get(0)));
		in = new ByteArrayInputStream(pem(s, "\r\n", "", ""));
		assertEquals(1, (l = PemUtil.read("PUBLIC KEY", in)).size());
		assertEquals("ABCD", Base64.getEncoder().encodeToString(l.get(0)));

		byte[] encoded = new byte[8192*4];
		Arrays.fill(encoded, (byte)'B');
		s = new String[] {
				"-----BEGIN PUBLIC KEY-----",
				new String(encoded),
				"-----END PUBLIC KEY-----"
		};
		in = new ByteArrayInputStream(pem(s, "\r\n", "", ""));
		assertEquals(1, (l = PemUtil.read(Label.PUBLIC_KEY, in)).size());
		assertEquals(8192*3, l.get(0).length);
	}
	
	@Test
	public void testReadFile() throws Exception {
		File file = new File(SSLContextBuilderTest.class.getClassLoader().getResource("cert.pem").getFile());
		List<byte[]> l;

		assertEquals(1, (l = PemUtil.read(Label.CERTIFICATE, file)).size());
		assertTrue(l.get(0).length > 0);
		assertEquals(1, (l = PemUtil.read("CERTIFICATE", file)).size());
		assertTrue(l.get(0).length > 0);
	}	
	
	@Test
	public void testSilentClose() throws Exception {
		TestCloseable tc = new TestCloseable();
		
		PemUtil.silentClose(tc);
		assertTrue(tc.closed);
		tc = new TestCloseable();
		tc.exception = new IOException();
		PemUtil.silentClose(tc);
		assertNull(tc.exception);
	}
	
	class TestCloseable implements Closeable {
		
		boolean closed;
		
		IOException exception;
		
		@Override
		public void close() throws IOException {
			if (exception != null) {
				try {
					throw exception;
				}
				finally {
					exception = null;
				}
			}
			closed = true;
		}
	}
}
