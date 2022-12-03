package org.snf4j.tls.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;
import org.snf4j.tls.handshake.HandshakeType;

public class TranscriptHashTest {

	static byte[] hash(byte[]... messages) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		
		for (byte[] message: messages) {
			md.update(message);
		}
		return md.digest();
	}
	
	static byte[] bytes(String s) {
		return s.getBytes(StandardCharsets.US_ASCII);
	}
	
	void assertHash(String expected, byte[] value) throws NoSuchAlgorithmException {
		assertArrayEquals(hash(bytes(expected)), value);
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
	}
	
	@Test
	public void testGetHashArguments() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		assertHash("", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("", th.getHash(HandshakeType.END_OF_EARLY_DATA));
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
		MD md = new MD(MessageDigest.getInstance("SHA-256"));
		TranscriptHash th = new TranscriptHash(md);
		
		th.update(HandshakeType.CLIENT_HELLO, bytes("CH"));
		try {
			th.update(HandshakeType.SERVER_HELLO, bytes("SH"));
			fail();
		} catch (UnsupportedOperationException e) {}
	}

	void assertIllegalState(TranscriptHash th, HandshakeType t) {
		try {
			th.update(t, bytes("XX"));
			fail();
		}
		catch (IllegalStateException e) {
		}
	}
	
	@Test
	public void testUpdate() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		
		try {
			th.update(HandshakeType.KEY_UPDATE, bytes("KU"));
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {}
		try {
			th.update(HandshakeType.NEW_SESSION_TICKET, bytes("KU"));
			fail();
		} catch (IllegalArgumentException e) {}
		th.update(HandshakeType.CLIENT_HELLO, bytes("CH"));
		assertIllegalState(th, HandshakeType.CLIENT_HELLO);
		th.update(HandshakeType.SERVER_HELLO, bytes("SH"));
		assertIllegalState(th, HandshakeType.SERVER_HELLO);
		assertIllegalState(th, HandshakeType.CLIENT_HELLO);
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, bytes("EE"));
		assertIllegalState(th, HandshakeType.SERVER_HELLO);
		assertIllegalState(th, HandshakeType.CLIENT_HELLO);
		assertIllegalState(th, HandshakeType.ENCRYPTED_EXTENSIONS);
		
	}
	
	@Test
	public void testGetHash() throws Exception {
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));

		assertHash("", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("", th.getHash(HandshakeType.FINISHED, false));
		assertHash("", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CLIENT_HELLO, bytes("CH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CH", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CH", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.SERVER_HELLO, bytes("SH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSH", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, bytes("EE"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEE", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEE", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.FINISHED, bytes("SF"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEESF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEESF", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.FINISHED, bytes("CF"));
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
		th.update(HandshakeType.CLIENT_HELLO, bytes("CH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CH", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CH", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.SERVER_HELLO, bytes("SH"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSH", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, bytes("EE"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEE", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEE", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CERTIFICATE_REQUEST, bytes("CR"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECR", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECR", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CERTIFICATE, bytes("C"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CERTIFICATE_VERIFY, bytes("CV"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.CERTIFICATE_VERIFY,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.FINISHED, bytes("SF"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.CERTIFICATE_VERIFY,false));
		assertHash("CHSHEECRCCVSF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRCCVSF", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.END_OF_EARLY_DATA, bytes("ED"));
		assertHash("CH", th.getHash(HandshakeType.CLIENT_HELLO));
		assertHash("CHSH", th.getHash(HandshakeType.SERVER_HELLO));
		assertHash("CHSHEE", th.getHash(HandshakeType.ENCRYPTED_EXTENSIONS));
		assertHash("CHSHEECR", th.getHash(HandshakeType.CERTIFICATE_REQUEST,false));
		assertHash("CHSHEECRC", th.getHash(HandshakeType.CERTIFICATE,false));
		assertHash("CHSHEECRCCV", th.getHash(HandshakeType.CERTIFICATE_VERIFY,false));
		assertHash("CHSHEECRCCVSF", th.getHash(HandshakeType.FINISHED, false));
		assertHash("CHSHEECRCCVSFED", th.getHash(HandshakeType.END_OF_EARLY_DATA));
		assertHash("CHSHEECRCCVSFED", th.getHash(HandshakeType.FINISHED, true));
		th.update(HandshakeType.CERTIFICATE, bytes("c"));
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
		th.update(HandshakeType.CERTIFICATE_VERIFY, bytes("cv"));
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
		th.update(HandshakeType.FINISHED, bytes("CF"));
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
			th.updateHelloRetryRequest(bytes("HRR"));
			fail();
		} catch (IllegalStateException e) {}
		th.update(HandshakeType.CLIENT_HELLO, bytes("ch1"));
		assertHash("ch1", th.getHash(HandshakeType.CLIENT_HELLO));
		th.updateHelloRetryRequest(bytes("HRR"));
		th.update(HandshakeType.CLIENT_HELLO, bytes("CH"));
		th.update(HandshakeType.SERVER_HELLO, bytes("SH"));
		
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
	}
	
	static class MD extends MessageDigest {
		
		MessageDigest md;
		
		protected MD(MessageDigest md) {
			super(md.getAlgorithm());
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
		
		public Object clone() throws CloneNotSupportedException {
			throw new CloneNotSupportedException();
		}
	}
}
