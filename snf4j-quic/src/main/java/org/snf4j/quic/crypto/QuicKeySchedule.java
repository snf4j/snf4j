/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.crypto;

import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.snf4j.tls.cipher.ICipherSuiteSpec;
import org.snf4j.tls.crypto.AbstractKeySchedule;
import org.snf4j.tls.crypto.IHkdf;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.DerivedSecrets;
import org.snf4j.tls.crypto.TrafficKeys;

/**
 * A key schedule providing the key derivation functions as defined
 * in the QUIC protocol specification.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class QuicKeySchedule extends AbstractKeySchedule {

	private static final byte[] CLIENT_IN = label("client in");

	private static final byte[] SERVER_IN = label("server in");
	
	private static final byte[] QUIC_KEY = label("quic key");

	private static final byte[] QUIC_IV = label("quic iv");

	private static final byte[] QUIC_HP = label("quic hp");

	private static final byte[] QUIC_KU = label("quic ku");
	
	private static final byte[] EMPTY = new byte[0];

	private final IHeaderProtection headerProtection;
	
	private final int hashLength;
	
	private byte[] clientInitialSecret;

	private byte[] serverInitialSecret;

	private byte[] clientNextGenerationSecret;

	private byte[] serverNextGenerationSecret;
	
	private static void checkDerived(Object o, String name) {
		if (o == null) {
			throw new IllegalStateException(name + " not derived");
		}
	}
	
	/**
	 * Constructs a QUIC key schedule for the given HMAC-based Extract-and-Expand
	 * Key Derivation Function (HKDF), cipher suite and header protection algorithm.
	 * 
	 * @param hkdf             the HKDF
	 * @param cipherSuiteSpec  the cipher suite
	 * @param headerProtection the header protection algorithm
	 */
	protected QuicKeySchedule(IHkdf hkdf, ICipherSuiteSpec cipherSuiteSpec, IHeaderProtection headerProtection) {
		super(hkdf, cipherSuiteSpec);
		this.headerProtection = headerProtection;
		hashLength = hashSpec.getHashLength();
	}

	/**
	 * Constructs a QUIC key schedule for the given HMAC-based Extract-and-Expand
	 * Key Derivation Function (HKDF) and cipher suite.
	 * 
	 * @param hkdf             the HKDF
	 * @param cipherSuiteSpec  the cipher suite
	 */
	public QuicKeySchedule(IHkdf hkdf, ICipherSuiteSpec cipherSuiteSpec) {
		this(hkdf, cipherSuiteSpec, AeadSpec.getHeaderProtection(cipherSuiteSpec.getAead().getId()));
	}
	
	/**
	 * Derives initial secrets from the given salt and the connection id.
	 * 
	 * @param salt         the initial salt as defined in the QUIC specification
	 * @param connectionId the connection id from the client's first Initial packet
	 * @throws InvalidKeyException if a key used during derivation process could not
	 *                             be created from the provided parameters
	 */
	public void deriveInitialSecrets(byte[] salt, byte[] connectionId) throws InvalidKeyException {
		eraseInitialSecrets();
		byte[] secret = hkdf.extract(salt, connectionId);
		byte[] clientInitialSecret = hkdfExpandLabel(secret,
				CLIENT_IN, 
				EMPTY, 
				hashLength);
		serverInitialSecret = hkdfExpandLabel(secret,
				SERVER_IN, 
				EMPTY, 
				hashLength);
		this.clientInitialSecret = clientInitialSecret;
	}
	
	/**
	 * Erases the initial secrets.
	 */
	public void eraseInitialSecrets() {
		if (clientInitialSecret != null) {
			Arrays.fill(clientInitialSecret, (byte)0);
			Arrays.fill(serverInitialSecret, (byte)0);
			clientInitialSecret = null;
			serverInitialSecret = null;
		}
	}
	
	private SecretKey createKey(byte[] key) {
		SecretKey secretKey = cipherSuiteSpec.getAead().createKey(key);
		
		Arrays.fill(key, (byte)0);
		return secretKey;
	}
	
	/**
	 * Derives the initial traffic keys.
	 * 
	 * @return the derived initial traffic keys
	 * @throws InvalidKeyException   if the keys could not be derived
	 * @throws IllegalStateException if the initial secrets are not derived yet or
	 *                               have been erased
	 */
	public TrafficKeys deriveInitialTrafficKeys() throws InvalidKeyException {
		checkDerived(clientInitialSecret, "Initial Secrets");
		byte[] civ = hkdfExpandLabel(clientInitialSecret,
				QUIC_IV,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] ckey = hkdfExpandLabel(clientInitialSecret,
				QUIC_KEY,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		byte[] siv = hkdfExpandLabel(serverInitialSecret,
				QUIC_IV,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] skey = hkdfExpandLabel(serverInitialSecret,
				QUIC_KEY,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		return new TrafficKeys(cipherSuiteSpec.getAead(), createKey(ckey), civ, createKey(skey), siv);
	}
	
	/**
	 * Derives the header protection keys.
	 * 
	 * @return the derived header protection keys
	 * @throws InvalidKeyException   if the keys could not be derived
	 * @throws IllegalStateException if the initial secrets are not derived yet or
	 *                               have been erased
	 */
	public SecretKeys deriveInitialHeaderProtectionKeys() throws InvalidKeyException {
		checkDerived(clientInitialSecret, "Initial Secrets");
		byte[] ckey = hkdfExpandLabel(clientInitialSecret,
				QUIC_HP,
				EMPTY,
				headerProtection.getKeyLength());
		byte[] skey = hkdfExpandLabel(serverInitialSecret,
				QUIC_HP,
				EMPTY,
				headerProtection.getKeyLength());
		return new SecretKeys(headerProtection.createKey(ckey), headerProtection.createKey(skey));
	}
	
	/**
	 * Derives the client early traffic key from the given TLS key schedule.
	 * 
	 * @param keySchedule the TLS key schedule
	 * @return the derived client early traffic key
	 * @throws InvalidKeyException   if the key could not be derived form the
	 *                               provided TLS key schedule
	 * @throws IllegalStateException if the given TLS key schedule is not properly
	 *                               initialized
	 */
	public TrafficKeys deriveEarlyTrafficKey(KeySchedule keySchedule) throws InvalidKeyException {
		return keySchedule.deriveEarlyTrafficKeys(QUIC_KEY, QUIC_IV);
	}

	/**
	 * Derives the client early header protection key from the given TLS key
	 * schedule.
	 * 
	 * @param keySchedule the TLS key schedule
	 * @return the derived client early header protection key
	 * @throws InvalidKeyException   if the key could not be derived form the
	 *                               provided TLS key schedule
	 * @throws IllegalStateException if the given TLS key schedule is not properly
	 *                               initialized
	 */
	public SecretKeys deriveEarlyHeaderProtectionKey(KeySchedule keySchedule) throws InvalidKeyException {
		DerivedSecrets secrets = keySchedule.deriveEarlySecrets(QUIC_HP, headerProtection.getKeyLength());
		SecretKeys keys = new SecretKeys(
				headerProtection.createKey(secrets.getClientSecret()));
		secrets.clear();
		return keys;
	}
	
	/**
	 * Derives the handshake traffic keys from the given TLS key schedule.
	 * 
	 * @param keySchedule the TLS key schedule
	 * @return the derived handshake traffic keys
	 * @throws InvalidKeyException   if the keys could not be derived form the
	 *                               provided TLS key schedule
	 * @throws IllegalStateException if the given TLS key schedule is not properly
	 *                               initialized
	 */
	public TrafficKeys deriveHandshakeTrafficKeys(KeySchedule keySchedule) throws InvalidKeyException {
		return keySchedule.deriveHandshakeTrafficKeys(QUIC_KEY, QUIC_IV);
	}

	private SecretKeys createKeys(DerivedSecrets secrets) {
		SecretKeys keys = new SecretKeys(
				headerProtection.createKey(secrets.getClientSecret()),
				headerProtection.createKey(secrets.getServerSecret()));
		secrets.clear();
		return keys;
	}
	
	/**
	 * Derives the handshake header protection keys from the given TLS key schedule.
	 * 
	 * @param keySchedule the TLS key schedule
	 * @return the derived handshake header protection keys
	 * @throws InvalidKeyException   if the keys could not be derived form the
	 *                               provided TLS key schedule
	 * @throws IllegalStateException if the given TLS key schedule is not properly
	 *                               initialized
	 */
	public SecretKeys deriveHandshakeHeaderProtectionKeys(KeySchedule keySchedule) throws InvalidKeyException {
		return createKeys(keySchedule.deriveHandshakeSecrets(QUIC_HP, headerProtection.getKeyLength()));
	}
	
	/**
	 * Derives the application traffic keys from the given TLS key schedule.
	 * 
	 * @param keySchedule the TLS key schedule
	 * @return the derived application traffic keys
	 * @throws InvalidKeyException   if the keys could not be derived form the
	 *                               provided TLS key schedule
	 * @throws IllegalStateException if the given TLS key schedule is not properly
	 *                               initialized
	 */
	public TrafficKeys deriveApplicationTrafficKeys(KeySchedule keySchedule) throws InvalidKeyException {
		return keySchedule.deriveApplicationTrafficKeys(QUIC_KEY, QUIC_IV);
	}

	/**
	 * Derives the application header protection keys from the given TLS key
	 * schedule.
	 * 
	 * @param keySchedule the TLS key schedule
	 * @return the derived application header protection keys
	 * @throws InvalidKeyException   if the keys could not be derived form the
	 *                               provided TLS key schedule
	 * @throws IllegalStateException if the given TLS key schedule is not properly
	 *                               initialized
	 */
	public SecretKeys deriveApplicationHeaderProtectionKeys(KeySchedule keySchedule) throws InvalidKeyException {
		return createKeys(keySchedule.deriveApplicationSecrets(QUIC_HP, headerProtection.getKeyLength()));
	}

	/**
	 * Derives the next generation traffic key secrets from the given TLS key
	 * schedule.
	 * <p>
	 * This method should be called once the application secrets are derived in the
	 * TLS key schedule.
	 * 
	 * @param keySchedule the TLS key schedule
	 * @throws InvalidKeyException   if keys used during derivation process could
	 *                               not be created from the provided TLS key
	 *                               schedule
	 * @throws IllegalStateException if the given TLS key schedule is not properly
	 *                               initialized
	 */
	public void deriveNextGenerationSecrets(KeySchedule keySchedule) throws InvalidKeyException {
		DerivedSecrets secrets = keySchedule.deriveApplicationSecrets(QUIC_KU, hashLength);
		
		clientNextGenerationSecret = secrets.getClientSecret();
		serverNextGenerationSecret = secrets.getServerSecret();
	}
	
	/**
	 * Erases the next generation traffic key secrets.
	 */
	public void eraseNextGenerationSecrets() {
		if (clientNextGenerationSecret != null) {
			Arrays.fill(clientNextGenerationSecret, (byte)0);
			clientNextGenerationSecret = null;
		}
		if (serverNextGenerationSecret != null) {
			Arrays.fill(serverNextGenerationSecret, (byte)0);
			serverNextGenerationSecret = null;
		}
	}
	
	/**
	 * Derives the next generation traffic keys.
	 * <p>
	 * NOTE: After calling this method the associated next generation traffic key
	 * secrets will be derived for the next traffic key derivation.
	 * 
	 * @return the derived next generation traffic keys
	 * @throws InvalidKeyException   if the keys could not be derived
	 * @throws IllegalStateException if the next generation traffic secrets are not
	 *                               derived yet or have been erased
	 */
	public TrafficKeys deriveNextGenerationTrafficKeys() throws InvalidKeyException {
		checkDerived(clientNextGenerationSecret, "Next Generation Secrets");
		byte[] civ = hkdfExpandLabel(clientNextGenerationSecret,
				QUIC_IV,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] ckey = hkdfExpandLabel(clientNextGenerationSecret,
				QUIC_KEY,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		byte[] cupdated = hkdfExpandLabel(clientNextGenerationSecret,
				QUIC_KU,
				EMPTY,
				hashLength);
		byte[] siv = hkdfExpandLabel(serverNextGenerationSecret,
				QUIC_IV,
				EMPTY,
				cipherSuiteSpec.getAead().getIvLength());
		byte[] skey = hkdfExpandLabel(serverNextGenerationSecret,
				QUIC_KEY,
				EMPTY,
				cipherSuiteSpec.getAead().getKeyLength());
		byte[] supdated = hkdfExpandLabel(serverNextGenerationSecret,
				QUIC_KU,
				EMPTY,
				hashLength);
		eraseNextGenerationSecrets();
		clientNextGenerationSecret = cupdated;
		serverNextGenerationSecret = supdated;
		return new TrafficKeys(cipherSuiteSpec.getAead(), createKey(ckey), civ, createKey(skey), siv);
	}
	
	@Override
	public void eraseAll() {
		eraseInitialSecrets();
		eraseNextGenerationSecrets();
	}
}
