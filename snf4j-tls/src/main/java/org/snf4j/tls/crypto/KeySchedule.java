/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2024 SNF4J contributors
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

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.snf4j.tls.Args;
import org.snf4j.tls.cipher.ICipherSuiteSpec;
import org.snf4j.tls.cipher.IHashSpec;
import org.snf4j.tls.handshake.HandshakeType;

public class KeySchedule extends AbstractKeySchedule {
	
	private static final byte[] DERIVED = label("derived");
	
	private static final byte[] RESUMPTION = label("resumption");
	
	private static final byte[] RES_BINDER = label("res binder");
	
	private static final byte[] RES_MASTER = label("res master");

	private static final byte[] EXT_BINDER = label("ext binder");
	
	private static final byte[] C_E_TRAFFIC = label("c e traffic");
	
	private static final byte[] FINISHED = label("finished");

	private static final byte[] C_HS_TRAFFIC = label("c hs traffic");

	private static final byte[] S_HS_TRAFFIC = label("s hs traffic");

	private static final byte[] C_AP_TRAFFIC = label("c ap traffic");

	private static final byte[] S_AP_TRAFFIC = label("s ap traffic");
	
	private static final byte[] TRAFFIC_UPD = label("traffic upd");
	
	private static final byte[] KEY = label("key");

	private static final byte[] IV = label("iv");
	
	private static final byte[] EMPTY = new byte[0];

	private final ITranscriptHash transcriptHash;
	
	private final int hashLength;
	
	private final byte[] emptyHash;

	private boolean usingPsk;
	
	private boolean externalPsk;
	
	private byte[] earlySecret;
	
	private byte[] earlyTrafficSecret;

	private byte[] binderKey;
	
	private byte[] handshakeSecret;

	private byte[] clientHandshakeTrafficSecret;

	private byte[] serverHandshakeTrafficSecret;

	private byte[] masterSecret;

	private byte[] clientApplicationTrafficSecret;

	private byte[] serverApplicationTrafficSecret;
	
	private byte[] resumptionMasterSecret;

	private static void checkDerived(Object o, String name) {
		if (o == null) {
			throw new IllegalStateException(name + " not derived");
		}
	}
	
	public KeySchedule(IHkdf hkdf, ITranscriptHash transcriptHash, ICipherSuiteSpec cipherSuiteSpec) {
		super(hkdf, cipherSuiteSpec);
		Args.checkNull(transcriptHash, "transcriptHash");
		this.transcriptHash = transcriptHash;
		emptyHash = hashSpec.getEmptyHash();
		hashLength = hashSpec.getHashLength();
	}

	public KeySchedule(IHkdf hkdf, ITranscriptHash transcriptHash, IHashSpec hashSpec) {
		super(hkdf, hashSpec);
		Args.checkNull(transcriptHash, "transcriptHash");
		this.transcriptHash = transcriptHash;
		emptyHash = hashSpec.getEmptyHash();
		hashLength = hashSpec.getHashLength();
	}
		
	public ITranscriptHash getTranscriptHash() {
		return transcriptHash;
	}
		
	public boolean isUsingPsk() {
		return usingPsk;
	}
	
	public void deriveEarlySecret(byte[] psk, boolean externalPsk) throws InvalidKeyException {
		Args.checkNull(psk, "psk");
		byte[] earlySecret = hkdf.extract(new byte[hashLength], psk);
		eraseEarlySecret();
		this.earlySecret = earlySecret;
		this.externalPsk = externalPsk;
		usingPsk = true;
	}

	public void deriveEarlySecret() throws InvalidKeyException {
		byte[] zero = new byte[hashLength];
		byte[] earlySecret = hkdf.extract(zero, zero);
		eraseEarlySecret();
		this.earlySecret = earlySecret;
		this.externalPsk = false;
		usingPsk = false;
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
	
	public byte[] computePskBinder(byte[] clientHello, int length) throws InvalidKeyException {
		checkDerived(binderKey, "Binder Key");
		byte[] hash = transcriptHash.getHash(HandshakeType.CLIENT_HELLO, clientHello, length);
		byte[] finishedKey = hkdfExpandLabel(binderKey,
				FINISHED,
				EMPTY,
				hashLength);
		byte[] hmac = hkdf.extract(finishedKey, hash);
		Arrays.fill(finishedKey, (byte)0);
		return hmac;
	}

	public byte[] computePskBinder(ByteBuffer[] clientHello) throws InvalidKeyException {
		checkDerived(binderKey, "Binder Key");
		byte[] hash = transcriptHash.getHash(HandshakeType.CLIENT_HELLO, clientHello);
		byte[] finishedKey = hkdfExpandLabel(binderKey,
				FINISHED,
				EMPTY,
				hashLength);
		byte[] hmac = hkdf.extract(finishedKey, hash);
		Arrays.fill(finishedKey, (byte)0);
		return hmac;
	}
	
	public byte[] computePsk(byte[] ticketNonce) throws InvalidKeyException {
		checkDerived(resumptionMasterSecret, "Resumption Master Secret");
		return hkdfExpandLabel(resumptionMasterSecret,
				RESUMPTION,
				ticketNonce,
				hashLength);
	}
	
	private SecretKey createKey(byte[] key) {
		SecretKey secretKey = cipherSuiteSpec.getAead().createKey(key);
		
		Arrays.fill(key, (byte)0);
		return secretKey;
	}
	
	public TrafficKeys deriveEarlyTrafficKeys() throws InvalidKeyException {
		return deriveEarlyTrafficKeys(KEY, IV);
	}

	public TrafficKeys deriveEarlyTrafficKeys(byte[] keyLabel, byte[] ivLabel) throws InvalidKeyException {
		checkDerived(earlyTrafficSecret, "Early Traffic Secret");
		byte[] iv = hkdfExpandLabel(earlyTrafficSecret,
				ivLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] key = hkdfExpandLabel(earlyTrafficSecret,
				keyLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		return new TrafficKeys(cipherSuiteSpec.getAead(), createKey(key), iv);
	}
	
	public DerivedSecrets deriveEarlySecrets(byte[] label, int length) throws InvalidKeyException {
		checkDerived(earlyTrafficSecret, "Early Traffic Secret");
		byte[] secret = hkdfExpandLabel(earlyTrafficSecret,
				label,
				EMPTY,
				length);
		return new DerivedSecrets(secret);
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

	public byte[] computeServerVerifyData() throws InvalidKeyException {
		checkDerived(serverHandshakeTrafficSecret, "Handshake Traffic Secrets");
		byte[] hash = transcriptHash.getHash(HandshakeType.CERTIFICATE_VERIFY, false);
		byte[] finishedKey = hkdfExpandLabel(serverHandshakeTrafficSecret,
				FINISHED,
				EMPTY,
				hashLength);
		byte[] hmac = hkdf.extract(finishedKey, hash);
		Arrays.fill(finishedKey, (byte)0);
		return hmac;
	}

	public byte[] computeClientVerifyData() throws InvalidKeyException {
		checkDerived(clientHandshakeTrafficSecret, "Handshake Traffic Secrets");
		byte[] hash = transcriptHash.getHash(HandshakeType.CERTIFICATE_VERIFY, true);
		byte[] finishedKey = hkdfExpandLabel(clientHandshakeTrafficSecret,
				FINISHED,
				EMPTY,
				hashLength);
		byte[] hmac = hkdf.extract(finishedKey, hash);
		Arrays.fill(finishedKey, (byte)0);
		return hmac;
	}
	
	public void eraseHandshakeTrafficSecrets() {
		if (clientHandshakeTrafficSecret != null) {
			Arrays.fill(clientHandshakeTrafficSecret, (byte)0);
			Arrays.fill(serverHandshakeTrafficSecret, (byte)0);
			serverHandshakeTrafficSecret = null;
			clientHandshakeTrafficSecret = null;
		}
	}
	
	public TrafficKeys deriveHandshakeTrafficKeys() throws InvalidKeyException {
		return deriveHandshakeTrafficKeys(KEY, IV);
	}

	public TrafficKeys deriveHandshakeTrafficKeys(byte[] keyLabel, byte[] ivLabel) throws InvalidKeyException {
		checkDerived(clientHandshakeTrafficSecret, "Handshake Traffic Secrets");
		byte[] civ = hkdfExpandLabel(clientHandshakeTrafficSecret,
				ivLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] ckey = hkdfExpandLabel(clientHandshakeTrafficSecret,
				keyLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		byte[] siv = hkdfExpandLabel(serverHandshakeTrafficSecret,
				ivLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] skey = hkdfExpandLabel(serverHandshakeTrafficSecret,
				keyLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		return new TrafficKeys(cipherSuiteSpec.getAead(), createKey(ckey), civ, createKey(skey), siv);
	}
	
	public DerivedSecrets deriveHandshakeSecrets(byte[] label, int length) throws InvalidKeyException {
		checkDerived(clientHandshakeTrafficSecret, "Handshake Traffic Secrets");
		byte[] csecret = hkdfExpandLabel(clientHandshakeTrafficSecret,
				label,
				EMPTY,
				length);
		byte[] ssecret = hkdfExpandLabel(serverHandshakeTrafficSecret,
				label,
				EMPTY,
				length);
		return new DerivedSecrets(csecret, ssecret);
	}
	
	public void deriveMasterSecret() throws InvalidKeyException {
		checkDerived(handshakeSecret, "Handshake Secret");
		byte[] derived = hkdfExpandLabel(handshakeSecret,
				DERIVED,
				emptyHash,
				hashLength);
		byte[] masterSecret = hkdf.extract(derived, new byte[hashLength]);
		Arrays.fill(derived, (byte)0);
		eraseMasterSecret();
		this.masterSecret = masterSecret;
	}
	
	public void eraseMasterSecret() {
		if (masterSecret != null) {
			Arrays.fill(masterSecret, (byte)0);
			masterSecret = null;
		}
	}
	
	public void deriveApplicationTrafficSecrets() throws InvalidKeyException {
		checkDerived(masterSecret, "Master Secret");
		eraseApplicationTrafficSecrets();
		byte[] clientApplicationTrafficSecret = hkdfExpandLabel(masterSecret, 
				C_AP_TRAFFIC, 
				transcriptHash.getHash(HandshakeType.FINISHED, false), 
				hashLength);
		serverApplicationTrafficSecret = hkdfExpandLabel(masterSecret, 
				S_AP_TRAFFIC, 
				transcriptHash.getHash(HandshakeType.FINISHED, false), 
				hashLength);
		this.clientApplicationTrafficSecret = clientApplicationTrafficSecret;
	}

	public void eraseApplicationTrafficSecrets() {
		eraseApplicationTrafficSecret(true);
		eraseApplicationTrafficSecret(false);
	}
	
	private void eraseApplicationTrafficSecret(boolean client) {
		if (client) {
			if (clientApplicationTrafficSecret != null) {
				Arrays.fill(clientApplicationTrafficSecret, (byte)0);
				clientApplicationTrafficSecret = null;
			}
		}
		else {
			if (serverApplicationTrafficSecret != null) {
				Arrays.fill(serverApplicationTrafficSecret, (byte)0);
				serverApplicationTrafficSecret = null;
			}
		}
	}
	
	public void deriveResumptionMasterSecret() throws InvalidKeyException {
		checkDerived(masterSecret, "Master Secret");
		eraseResumptionMasterSecret();
		resumptionMasterSecret = hkdfExpandLabel(masterSecret, 
				RES_MASTER, 
				transcriptHash.getHash(HandshakeType.FINISHED, true), 
				hashLength);
	}

	public void eraseResumptionMasterSecret() {
		if (resumptionMasterSecret != null) {
			Arrays.fill(resumptionMasterSecret, (byte)0);
			resumptionMasterSecret = null;
		}
	}
	
	public TrafficKeys deriveApplicationTrafficKeys() throws InvalidKeyException {
		return deriveApplicationTrafficKeys(KEY, IV);
	}

	public TrafficKeys deriveApplicationTrafficKeys(byte[] keyLabel, byte[] ivLabel) throws InvalidKeyException {
		checkDerived(clientApplicationTrafficSecret, "Application Traffic Secrets");
		byte[] civ = hkdfExpandLabel(clientApplicationTrafficSecret,
				ivLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] ckey = hkdfExpandLabel(clientApplicationTrafficSecret,
				keyLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		byte[] siv = hkdfExpandLabel(serverApplicationTrafficSecret,
				ivLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] skey = hkdfExpandLabel(serverApplicationTrafficSecret,
				keyLabel,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		return new TrafficKeys(cipherSuiteSpec.getAead(), createKey(ckey), civ, createKey(skey), siv);
	}
	
	public DerivedSecrets deriveApplicationSecrets(byte[] label, int length) throws InvalidKeyException {
		checkDerived(clientApplicationTrafficSecret, "Application Traffic Secrets");
		byte[] csecret = hkdfExpandLabel(clientApplicationTrafficSecret,
				label,
				EMPTY,
				length);
		byte[] ssecret = hkdfExpandLabel(serverApplicationTrafficSecret,
				label,
				EMPTY,
				length);
		return new DerivedSecrets(csecret, ssecret);
	}
	
	public TrafficKeys deriveNextGenerationTrafficKey(boolean client) throws InvalidKeyException {
		if (client) {
			checkDerived(clientApplicationTrafficSecret, "Application Traffic Secrets");
			byte[] updated = hkdfExpandLabel(clientApplicationTrafficSecret,
					TRAFFIC_UPD,
					EMPTY,
					hashLength);
			byte[] iv = hkdfExpandLabel(updated,
					IV,
					EMPTY,
					cipherSuiteSpec.getAead().getIvLength());
			byte[] key = hkdfExpandLabel(updated,
					KEY,
					EMPTY,
					cipherSuiteSpec.getAead().getKeyLength());
			eraseApplicationTrafficSecret(client);
			clientApplicationTrafficSecret = updated;
			return new TrafficKeys(cipherSuiteSpec.getAead(), createKey(key), iv, null, null);
		}

		checkDerived(serverApplicationTrafficSecret, "Application Traffic Secrets");
		byte[] updated = hkdfExpandLabel(serverApplicationTrafficSecret,
				TRAFFIC_UPD,
				EMPTY,
				hashLength);
		byte[] iv = hkdfExpandLabel(updated,
				IV,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] key = hkdfExpandLabel(updated,
				KEY,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		eraseApplicationTrafficSecret(client);
		serverApplicationTrafficSecret = updated;
		return new TrafficKeys(cipherSuiteSpec.getAead(), null, null, createKey(key), iv);
	}

	@Override
	public void eraseAll() {	
		eraseApplicationTrafficSecrets();
		eraseBinderKey();
		eraseEarlySecret();
		eraseEarlyTrafficSecret();
		eraseHandshakeSecret();
		eraseHandshakeTrafficSecrets();
		eraseMasterSecret();
		eraseResumptionMasterSecret();
	}
	
}
