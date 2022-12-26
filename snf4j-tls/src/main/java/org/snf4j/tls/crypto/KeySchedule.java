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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.snf4j.tls.Args;
import org.snf4j.tls.cipher.ICipherSuiteSpec;
import org.snf4j.tls.handshake.HandshakeType;

public class KeySchedule {
	
	private static final String LABEL_PREFIX = "tls13 ";
	
	private static final byte[] DERIVED = label("derived");
	
	private static final byte[] RES_BINDER = label("res binder");

	private static final byte[] EXT_BINDER = label("ext binder");
	
	private static final byte[] C_E_TRAFFIC = label("c e traffic");
	
	private static final byte[] FINISHED = label("finished");

	private static final byte[] C_HS_TRAFFIC = label("c hs traffic");

	private static final byte[] S_HS_TRAFFIC = label("s hs traffic");
	
	private static final byte[] KEY = label("key");

	private static final byte[] IV = label("iv");
	
	private static final byte[] EMPTY = new byte[0];
	
	private final IHkdf hkdf;
	
	private final ITranscriptHash transcriptHash;
	
	private final ICipherSuiteSpec cipherSuiteSpec;
	
	private final int hashLength;
	
	private final byte[] emptyHash;
	
	private boolean externalPsk;
	
	private byte[] earlySecret;
	
	private byte[] earlyTrafficSecret;

	private byte[] binderKey;
	
	private byte[] handshakeSecret;

	private byte[] clientHandshakeTrafficSecret;

	private byte[] serverHandshakeTrafficSecret;
	
	private SecretKey clientKey;
	
	private SecretKey serverKey;
	
	private byte[] clientIv;

	private byte[] serverIv;
	
	private static byte[] label(String label) {
		return (LABEL_PREFIX + label).getBytes(StandardCharsets.US_ASCII);
	}
	
	private static void checkDerived(Object o, String name) {
		if (o == null) {
			throw new IllegalStateException(name + " not derived");
		}
	}
	
	public KeySchedule(IHkdf hkdf, ITranscriptHash transcriptHash, ICipherSuiteSpec cipherSuiteSpec) {
		Args.checkNull(hkdf, "hkdf");
		Args.checkNull(transcriptHash, "transcriptHash");
		Args.checkNull(cipherSuiteSpec, "cipherSuiteSpec");
		this.hkdf = hkdf;
		this.transcriptHash = transcriptHash;
		this.cipherSuiteSpec = cipherSuiteSpec;
		emptyHash = cipherSuiteSpec.getHashSpec().getEmptyHash();
		hashLength = cipherSuiteSpec.getHashSpec().getHashLength();
	}
		
	public void deriveEarlySecret(byte[] psk, boolean externalPsk) throws InvalidKeyException {
		Args.checkNull(psk, "psk");
		byte[] earlySecret = hkdf.extract(new byte[hashLength], psk);
		eraseEarlySecret();
		this.earlySecret = earlySecret;
		this.externalPsk = externalPsk;
	}

	public void deriveEarlySecret() throws InvalidKeyException {
		byte[] zero = new byte[hashLength];
		byte[] earlySecret = hkdf.extract(zero, zero);
		eraseEarlySecret();
		this.earlySecret = earlySecret;
		this.externalPsk = false;
	}
	
	public void eraseEarlySecret() {
		if (earlySecret != null) {
			Arrays.fill(earlySecret, (byte)0);
			earlySecret = null;
		}
	}
	
	public void deriveBinderKey() throws InvalidKeyException {
		checkDerived(earlySecret, "Early Secret");
		eraseBinderKey();
		binderKey = hkdfExpandLabel(earlySecret, 
				externalPsk ? EXT_BINDER : RES_BINDER, 
				emptyHash, 
				hashLength);
	}

	public void eraseBinderKey() {
		if (binderKey != null) {
			Arrays.fill(binderKey, (byte)0);
			binderKey = null;
		}
	}
	
	public void deriveEarlyTrafficSecret() throws InvalidKeyException {
		checkDerived(earlySecret, "Early Secret");
		eraseEarlyTrafficSecret();
		earlyTrafficSecret = hkdfExpandLabel(earlySecret, 
				C_E_TRAFFIC, 
				transcriptHash.getHash(HandshakeType.CLIENT_HELLO), 
				hashLength);
	}

	public void eraseEarlyTrafficSecret() {
		if (earlyTrafficSecret != null) {
			Arrays.fill(earlyTrafficSecret, (byte)0);
			earlyTrafficSecret = null;
		}
	}
	
	public byte[] computePskBinder(byte[] truncatedClientHello) throws InvalidKeyException {
		checkDerived(binderKey, "Binder Key");
		byte[] hash = transcriptHash.getHash(HandshakeType.CLIENT_HELLO, truncatedClientHello);
		byte[] finishedKey = hkdfExpandLabel(binderKey,
				FINISHED,
				EMPTY,
				hashLength);
		byte[] hmac = hkdf.extract(finishedKey, hash);
		Arrays.fill(finishedKey, (byte)0);
		return hmac;
	}
	
	protected SecretKey createKey(byte[] key, ICipherSuiteSpec cipherSuiteSpec) {
		return cipherSuiteSpec.getAead().createKey(key);
	}
	
	private SecretKey createKey(byte[] key) {
		SecretKey secretKey = createKey(key, cipherSuiteSpec);
		
		Arrays.fill(key, (byte)0);
		return secretKey;
	}
	
	public void deriveEarlyTrafficKeys() throws InvalidKeyException {
		checkDerived(earlyTrafficSecret, "Early Traffic Secret");
		eraseTrafficKeys();
		byte[] iv = hkdfExpandLabel(earlyTrafficSecret,
				IV,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] key = hkdfExpandLabel(earlyTrafficSecret,
				KEY,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		clientIv = iv;
		clientKey = createKey(key);
	}
	
	public void deriveHandshakeSecret(byte[] sharedSecret) throws InvalidKeyException {
		checkDerived(earlySecret, "Early Secret");
		byte[] derived = hkdfExpandLabel(earlySecret,
				DERIVED,
				emptyHash,
				hashLength);
		byte[] handshakeSecret = hkdf.extract(derived, sharedSecret);
		Arrays.fill(derived, (byte)0);
		eraseHandshakeSecret();
		this.handshakeSecret = handshakeSecret;
	}
	
	public void eraseHandshakeSecret() {
		if (handshakeSecret != null) {
			Arrays.fill(handshakeSecret, (byte)0);
			handshakeSecret = null;
		}
	}
	
	public void deriveHandshakeTrafficSecrets() throws InvalidKeyException {
		checkDerived(handshakeSecret, "Handshake Secret");
		eraseHandshakeTrafficSecrets();
		byte[] clientHandshakeTrafficSecret = hkdfExpandLabel(handshakeSecret, 
				C_HS_TRAFFIC, 
				transcriptHash.getHash(HandshakeType.SERVER_HELLO), 
				hashLength);
		serverHandshakeTrafficSecret = hkdfExpandLabel(handshakeSecret, 
				S_HS_TRAFFIC, 
				transcriptHash.getHash(HandshakeType.SERVER_HELLO), 
				hashLength);
		this.clientHandshakeTrafficSecret = clientHandshakeTrafficSecret;
	}

	public void eraseHandshakeTrafficSecrets() {
		if (clientHandshakeTrafficSecret != null) {
			Arrays.fill(clientHandshakeTrafficSecret, (byte)0);
			Arrays.fill(serverHandshakeTrafficSecret, (byte)0);
			serverHandshakeTrafficSecret = null;
			clientHandshakeTrafficSecret = null;
		}
	}
	
	public void deriveHandshakeTrafficKeys() throws InvalidKeyException {
		checkDerived(clientHandshakeTrafficSecret, "Handshake Traffic Secrets");
		eraseTrafficKeys();
		byte[] civ = hkdfExpandLabel(clientHandshakeTrafficSecret,
				IV,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] ckey = hkdfExpandLabel(clientHandshakeTrafficSecret,
				KEY,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		byte[] siv = hkdfExpandLabel(serverHandshakeTrafficSecret,
				IV,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] skey = hkdfExpandLabel(serverHandshakeTrafficSecret,
				KEY,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		clientIv = civ;
		clientKey = createKey(ckey);
		serverIv = siv;
		serverKey = createKey(skey);
	}
	
	public void eraseTrafficKeys() {
		if (clientKey != null) {
			Arrays.fill(clientIv, (byte)0);
			clientIv = null;
			try {
				clientKey.destroy();
			} catch (DestroyFailedException ignored) {
				//Ignore
			}
			clientKey = null;
		}
		if (serverKey != null) {
			Arrays.fill(serverIv, (byte)0);
			serverIv = null;
			try {
				serverKey.destroy();
			} catch (DestroyFailedException ignored) {
				//Ignore
			}
			serverKey = null;
		}
	}
	
	
	byte[] hkdfExpandLabel(byte[] secret, String label, byte[] context, int length) throws InvalidKeyException {
		return hkdfExpandLabel(secret, label(label), context, length);
	}
	
	byte[] hkdfExpandLabel(byte[] secret, byte[] label, byte[] context, int length) throws InvalidKeyException {
		byte[] buf = new byte[2 + 1 + label.length + 1 + context.length];
		
		buf[0] = (byte) (length >> 8);
		buf[1] = (byte) length;
		buf[2] = (byte) label.length;
		System.arraycopy(label, 0, buf, 3, label.length);
		buf[3+label.length] = (byte) context.length;
		if (context.length > 0) {
			System.arraycopy(context, 0, buf, buf.length-context.length, context.length);
		}
		return hkdf.expand(secret, buf, length);
	}
}
