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
package org.snf4j.tls.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.handshake.HandshakeType;

public class TranscriptHashTest extends CommonTest {

	static byte[] hash(byte[]... messages) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		
		for (byte[] message: messages) {
			md.update(message);
		}
		return md.digest();
	}
	
	static byte[] ascii(String s) {
		return s.getBytes(StandardCharsets.US_ASCII);
	}
	
	static ByteBuffer[] asciiBuffers(String s, int split) {
		byte[] ascii = ascii(s);
		int partSize = ascii.length / split;
		int off = 0;
		int remaining = ascii.length;
		ByteBuffer[] buffers = new ByteBuffer[split];
		
		for (int i=0; i<split; ++i) {
			byte[] part = new byte[(split == i+1) ? remaining : partSize];

			System.arraycopy(ascii, off, part, 0, part.length);
			remaining -= part.length;
			off += part.length;
			buffers[i] =  ByteBuffer.wrap(part);
		}
		return buffers;
	}
	
	void assertHash(String expected, byte[] value) throws NoSuchAlgorithmException {
		assertArrayEquals(hash(ascii(expected)), value);
	}
	
	void assertIllegalAgument(TranscriptHash th, HandshakeType t, Boolean client) {
		try {
			if (client == null) {
				th.getHash(t);
			}
			else {
				th.getHash(t, client);
			}
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		if (client == null) {
			try {
				th.getHash(t, ascii(""));
				fail();
			}
			catch (IllegalArgumentException e) {
			}
		}
	}
	
	@Test
	public void testGetHashFunction() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		
		th.update(HandshakeType.CLIENT_HELLO, ascii("CH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		th.update(HandshakeType.SERVER_HELLO, ascii("SH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		MessageDigest md = th.getHashFunction();
		assertNotSame(md, th.getHashFunction());
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, asciiBuffers("EE", 1));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CH", md.digest("CH".getBytes()));
		
		th = new TranscriptHash(MessageDigest.getInstance("SHA-384"));
		th.update(HandshakeType.CLIENT_HELLO, ascii("CH"));
		th.update(HandshakeType.SERVER_HELLO, ascii("SH"));
		md = th.getHashFunction();
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, asciiBuffers("EE",2));
		assertArrayEquals(md.digest("CHSHEE".getBytes()), th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		
		if (JAVA8) {
			th = new TranscriptHash(new MD(MessageDigest.getInstance("SHA-256"), null));
			md = th.getHashFunction();
			assertEquals("SHA-256", md.getAlgorithm());
			assertHash("", md.digest());
			th = new TranscriptHash(new MD(MessageDigest.getInstance("SHA-256"), "SHA-xxx"));
			try {
				md = th.getHashFunction();
				fail();
			} catch (UnsupportedOperationException e) {}
		}
	}
	
	@Test
	public void testGetAlgorithm() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		assertEquals("SHA-256", th.getAlgorithm());
		th = new TranscriptHash(MessageDigest.getInstance("SHA-384"));
		assertEquals("SHA-384", th.getAlgorithm());
	}

	@Test
	public void testGetHashLength() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		assertEquals(32, th.getHashLength());
		th = new TranscriptHash(MessageDigest.getInstance("SHA-384"));
		assertEquals(48, th.getHashLength());
	}
	
	@Test
	public void testGetHashArguments() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		assertHash("", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("", th.getHash(HandshakeType.END_OF_EARLY_DATA));
		assertHash("", th.getHash(HandshakeType.CLIENT_HELLO, ascii("")));
		assertHash("", th.getHash(HandshakeType.SERVER_HELLO, ascii("")));
		assertHash("", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS, ascii("")));
		assertHash("", th.getHash(HandshakeType.END_OF_EARLY_DATA, ascii("")));
		assertIllegalAgument(th, HandshakeType.CERTIFICATE_REQUEST, null);
		assertIllegalAgument(th, HandshakeType.CERTIFICATE, null);
		assertIllegalAgument(th, HandshakeType.CERTIFICATE_VERIFY, null);
		assertIllegalAgument(th, HandshakeType.FINISHED, null);

		assertIllegalAgument(th, HandshakeType.CLIENT_HELLO, false);
		assertIllegalAgument(th, HandshakeType.SERVER_HELLO, false);
		assertIllegalAgument(th, HandshakeType.ENCRYPTED_EXTENSIONS, false);
		assertIllegalAgument(th, HandshakeType.END_OF_EARLY_DATA, false);
		assertHash("", th.getHash(HandshakeType.CERTIFICATE_REQUEST, false));
		assertHash("", th.getHash(HandshakeType.CERTIFICATE, false));
		assertHash("", th.getHash(HandshakeType.CERTIFICATE_VERIFY, false));
		assertHash("", th.getHash(HandshakeType.FINISHED,false));

		assertIllegalAgument(th, HandshakeType.CLIENT_HELLO, true);
		assertIllegalAgument(th, HandshakeType.SERVER_HELLO, true);
		assertIllegalAgument(th, HandshakeType.ENCRYPTED_EXTENSIONS, true);
		assertIllegalAgument(th, HandshakeType.END_OF_EARLY_DATA, true);
		assertIllegalAgument(th, HandshakeType.CERTIFICATE_REQUEST, true);
		assertHash("", th.getHash(HandshakeType.CERTIFICATE, true));
		assertHash("", th.getHash(HandshakeType.CERTIFICATE_VERIFY, true));
		assertHash("", th.getHash(HandshakeType.FINISHED,true));
	}
	
	@Test
	public void testUnsupportedClone() throws Exception {
		MD md = new MD(MessageDigest.getInstance("SHA-256"), null);
		TranscriptHash th = new TranscriptHash(md);
		
		th.update(HandshakeType.CLIENT_HELLO, ascii("CH"));
		try {
			th.update(HandshakeType.SERVER_HELLO, ascii("SH"));
			fail();
		} catch (UnsupportedOperationException e) {}
	}

	void assertIllegalState(TranscriptHash th, HandshakeType t) {
		try {
			th.update(t, ascii("XX"));
			fail();
		}
		catch (IllegalStateException e) {
		}
	}
	
	@Test
	public void testUpdate() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		
		try {
			th.update(HandshakeType.KEY_UPDATE, ascii("KU"));
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {}
		try {
			th.update(HandshakeType.NEW_SESSION_TICKET, ascii("KU"));
			fail();
		} catch (IllegalArgumentException e) {}
		th.update(HandshakeType.CLIENT_HELLO, ascii("CH"));
		assertIllegalState(th, HandshakeType.CLIENT_HELLO);
		th.update(HandshakeType.SERVER_HELLO, ascii("SH"));
		assertIllegalState(th, HandshakeType.SERVER_HELLO);
		assertIllegalState(th, HandshakeType.CLIENT_HELLO);
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, ascii("EE"));
		assertIllegalState(th, HandshakeType.SERVER_HELLO);
		assertIllegalState(th, HandshakeType.CLIENT_HELLO);
		assertIllegalState(th, HandshakeType.ENCRYPTED_EXTENSIONS);
		
	}
	
	@Test
	public void testGetHash() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));

		assertHash("", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("", th.getHash(HandshakeType.CLIENT_HELLO, ascii("")));
		assertHash("XX", th.getHash(HandshakeType.CLIENT_HELLO, ascii("XX")));
		assertHash("", th.getHash(HandshakeType.FINISHED, false));
		assertHash("", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CLIENT_HELLO, ascii("CH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("", th.getHash(HandshakeType.CLIENT_HELLO, ascii("")));
		assertHash("XX", th.getHash(HandshakeType.CLIENT_HELLO, ascii("XX")));
		assertHash("CH", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CH", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.SERVER_HELLO, ascii("SH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CH", th.getHash(HandshakeType.SERVER_HELLO, ascii("")));
		assertHash("CHXX", th.getHash(HandshakeType.SERVER_HELLO, ascii("XX")));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSH", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, ascii("EE"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEE", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEE", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.FINISHED, ascii("SF"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEESF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEESF", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.FINISHED, ascii("CF"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEESF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEESFCF", th.getHash(HandshakeType.FINISHED, true));
	}

	@Test
	public void testGetHashAllMessages() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));

		assertHash("", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("", th.getHash(HandshakeType.FINISHED, false));
		assertHash("", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CLIENT_HELLO, ascii("CH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CH", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CH", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.SERVER_HELLO, ascii("SH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSH", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, ascii("EE"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEE", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEE", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CERTIFICATE_REQUEST, ascii("CR"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECR", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECR", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CERTIFICATE, ascii("C"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CERTIFICATE_VERIFY, ascii("CV"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.CERTIFICATE_VERIFY,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.FINISHED, ascii("SF"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.CERTIFICATE_VERIFY,false));
		assertHash("CHSHEECRCCVSF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRCCVSF", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.END_OF_EARLY_DATA, ascii("ED"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.CERTIFICATE_VERIFY,false));
		assertHash("CHSHEECRCCVSF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRCCVSFED", th.getHash(HandshakeType.END_OF_EARLY_DATA));
		assertHash("CHSHEECRCCVSFED", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CERTIFICATE, ascii("c"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.CERTIFICATE_VERIFY,false));
		assertHash("CHSHEECRCCVSF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRCCVSFED", th.getHash(HandshakeType.END_OF_EARLY_DATA));
		assertHash("CHSHEECRCCVSFEDc", th.getHash(HandshakeType.CERTIFICATE, true));
		assertHash("CHSHEECRCCVSFEDc", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CERTIFICATE_VERIFY, ascii("cv"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.CERTIFICATE_VERIFY,false));
		assertHash("CHSHEECRCCVSF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRCCVSFED", th.getHash(HandshakeType.END_OF_EARLY_DATA));
		assertHash("CHSHEECRCCVSFEDc", th.getHash(HandshakeType.CERTIFICATE, true));
		assertHash("CHSHEECRCCVSFEDccv", th.getHash(HandshakeType.CERTIFICATE_VERIFY, true));
		assertHash("CHSHEECRCCVSFEDccv", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.FINISHED, ascii("CF"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.CERTIFICATE_VERIFY,false));
		assertHash("CHSHEECRCCVSF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRCCVSFED", th.getHash(HandshakeType.END_OF_EARLY_DATA));
		assertHash("CHSHEECRCCVSFEDc", th.getHash(HandshakeType.CERTIFICATE, true));
		assertHash("CHSHEECRCCVSFEDccv", th.getHash(HandshakeType.CERTIFICATE_VERIFY, true));
		assertHash("CHSHEECRCCVSFEDccvCF", th.getHash(HandshakeType.FINISHED, true));
	}
	
	@Test
	public void testUpdateHelloRetryRequest() throws NoSuchAlgorithmException {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		
		try {
			th.updateHelloRetryRequest(ascii("HRR"));
			fail();
		} catch (IllegalStateException e) {}
		th.update(HandshakeType.CLIENT_HELLO, ascii("ch1"));
		assertHash("ch1", th.getHash(HandshakeType.CLIENT_HELLO));
		th.updateHelloRetryRequest(ascii("HRR"));
		th.update(HandshakeType.CLIENT_HELLO, ascii("CH"));
		th.update(HandshakeType.SERVER_HELLO, ascii("SH"));
		
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] digest = md.digest("ch1".getBytes());
		md = MessageDigest.getInstance("SHA-256");
		md.update(new byte[] {(byte)254,0,0,32});
		md.update(digest);
		md.update("HRR".getBytes());
		md.update("CH".getBytes());
		md.update("SH".getBytes());
		digest = md.digest();
		assertArrayEquals(digest, th.getHash(HandshakeType.SERVER_HELLO));

		md.reset();;
		digest = md.digest("ch1".getBytes());
		md = MessageDigest.getInstance("SHA-256");
		md.update(new byte[] {(byte)254,0,0,32});
		md.update(digest);
		md.update("HRR".getBytes());
		md.update("CH".getBytes());
		digest = md.digest();
		assertArrayEquals(digest, th.getHash(HandshakeType.CLIENT_HELLO));

		th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		th.update(HandshakeType.CLIENT_HELLO, ascii("ch1"));
		th.updateHelloRetryRequest(asciiBuffers("HRR",1));
		th.update(HandshakeType.CLIENT_HELLO, ascii("CH"));
		th.update(HandshakeType.SERVER_HELLO, ascii("SH"));
		assertArrayEquals(digest, th.getHash(HandshakeType.CLIENT_HELLO));

		th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		th.update(HandshakeType.CLIENT_HELLO, ascii("ch1"));
		th.updateHelloRetryRequest(asciiBuffers("HRR",2));
		th.update(HandshakeType.CLIENT_HELLO, ascii("CH"));
		th.update(HandshakeType.SERVER_HELLO, ascii("SH"));
		assertArrayEquals(digest, th.getHash(HandshakeType.CLIENT_HELLO));
		
	}
	
	static class MD extends MessageDigest {
		
		MessageDigest md;
		
		MD(MessageDigest md, String algorithm) {
			super(algorithm == null ? md.getAlgorithm() : algorithm);
			if (JAVA8) {
				try {
					Field f = MessageDigest.class.getDeclaredField("provider");
					f.setAccessible(true);
					f.set(this, md.getProvider());
				} catch (Exception e) {
				}
			}
			this.md = md;
		}

		@Override
		protected void engineUpdate(byte input) {
		}

		@Override
		protected void engineUpdate(byte[] input, int offset, int len) {
		}

		@Override
		protected byte[] engineDigest() {
			return null;
		}

		@Override
		protected void engineReset() {
		}

		@Override
		public Object clone() throws CloneNotSupportedException {
			throw new CloneNotSupportedException();
		}
	}
}
