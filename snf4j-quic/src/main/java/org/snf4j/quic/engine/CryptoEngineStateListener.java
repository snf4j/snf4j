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
package org.snf4j.quic.engine;

import java.security.GeneralSecurityException;
import org.snf4j.quic.QuicAlert;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.crypto.AeadSpec;
import org.snf4j.quic.crypto.QuicKeySchedule;
import org.snf4j.quic.crypto.SecretKeys;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.cipher.ICipherSuiteSpec;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IAeadDecrypt;
import org.snf4j.tls.crypto.IAeadEncrypt;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TrafficKeys;
import org.snf4j.tls.engine.IEngineProducer;
import org.snf4j.tls.engine.IEngineState;
import org.snf4j.tls.engine.IEngineStateListener;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.ExtensionsUtil;
import org.snf4j.tls.extension.IEarlyDataExtension;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IClientHello;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.KeyUpdateRequest;
import org.snf4j.tls.record.RecordType;

/**
 * The default listener for the QUIC cryptographic engine. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CryptoEngineStateListener implements IEngineStateListener {

	private final static CipherSuite INITAL_CIPHER = CipherSuite.TLS_AES_128_GCM_SHA256;
	
	private final static Alert[] HANDSHAKE_ALERTS = new Alert[25];
	
	private final static int CLIENT_HELLO = HandshakeType.CLIENT_HELLO.value();
	
	private final static int NEW_SESSION_TICKET = HandshakeType.NEW_SESSION_TICKET.value();
	
	static {
		HANDSHAKE_ALERTS[HandshakeType.KEY_UPDATE.value()] = new UnexpectedMessageAlert(
				"Unexpected key_update handshake message");
		HANDSHAKE_ALERTS[HandshakeType.END_OF_EARLY_DATA.value()] = new QuicAlert(
				TransportError.PROTOCOL_VIOLATION, 
				"Unexpected end_of_early_data handshake message");
	}
	
	private final QuicState state;
	
	private QuicKeySchedule keySchedule;
		
	/**
	 * Constructs a listener associated with the given QUIC state object.
	 * 
	 * @param state the QUIC state object
	 */
	public CryptoEngineStateListener(QuicState state) {
		this.state = state;
	}
	
	private static Encryptor encryptor(TrafficKeys tKeys, SecretKeys hpKeys, boolean client) throws GeneralSecurityException {
		IAeadEncrypt aead = tKeys.getAeadEncrypt(client);
		return new Encryptor(
				aead, 
				new HeaderProtector(aead.getAead(), hpKeys.getKey(client)),
				tKeys.getIv(client),
				AeadSpec.getConfidentialityLimit(aead.getAead().getId()));
	}

	private static Encryptor encryptor(TrafficKeys tKeys, HeaderProtector protector, boolean client) throws GeneralSecurityException {
		IAeadEncrypt aead = tKeys.getAeadEncrypt(client);
		return new Encryptor(
				aead, 
				protector,
				tKeys.getIv(client),
				AeadSpec.getConfidentialityLimit(aead.getAead().getId()));
	}
	
	private static Decryptor decryptor(TrafficKeys tKeys, SecretKeys hpKeys, boolean client) throws GeneralSecurityException {
		IAeadDecrypt aead = tKeys.getAeadDecrypt(!client);
		return new Decryptor(
				aead, 
				new HeaderProtector(aead.getAead(), hpKeys.getKey(!client)),
				tKeys.getIv(!client),
				AeadSpec.getConfidentialityLimit(aead.getAead().getId()),
				AeadSpec.getIntegrityLimit(aead.getAead().getId()));
	}

	private static Decryptor decryptor(TrafficKeys tKeys, HeaderProtector protector, boolean client) throws GeneralSecurityException {
		IAeadDecrypt aead = tKeys.getAeadDecrypt(!client);
		return new Decryptor(
				aead, 
				protector,
				tKeys.getIv(!client),
				AeadSpec.getConfidentialityLimit(aead.getAead().getId()),
				AeadSpec.getIntegrityLimit(aead.getAead().getId()));
	}
	
	/**
	 * A callback method that should be called to initialize the encryption
	 * context for the initial encryption level.
	 * 
	 * @param salt         the initial salt
	 * @param connectionId the Destination Connection ID field from the client's
	 *                     first Initial packet
	 * @throws GeneralSecurityException if an error occurred during derivation of
	 *                                  the initial secrets or keys
	 */
	public void onInit(byte[] salt, byte[] connectionId) throws GeneralSecurityException {
		QuicKeySchedule keySchedule = new QuicKeySchedule(
				new Hkdf(INITAL_CIPHER.spec().getHashSpec().getHash().createMac()),
				INITAL_CIPHER.spec());		
		TrafficKeys trKeys;
		SecretKeys hpKeys;

		keySchedule.deriveInitialSecrets(salt, connectionId);
		trKeys = keySchedule.deriveInitialTrafficKeys();
		hpKeys = keySchedule.deriveInitialHeaderProtectionKeys();
		keySchedule.eraseInitialSecrets();
		
		EncryptionContext ctx = state.getContext(EncryptionLevel.INITIAL);
		ctx.setEncryptor(encryptor(trKeys, hpKeys, state.isClientMode()));
		ctx.setDecryptor(decryptor(trKeys, hpKeys, state.isClientMode()));
	}
	
	/**
	 * A callback method that should be called to derive new keys for the next key
	 * phase in the encryption context for the application data encryption level.
	 * <p>
	 * If the encryption context for the application data encryption level already
	 * has next keys this method simply does nothing.
	 * 
	 * @throws GeneralSecurityException if an error occurred during derivation of
	 *                                  the next secrets or keys
	 */
	public void onNextKeys() throws GeneralSecurityException {
		EncryptionContext ctx = state.getContext(EncryptionLevel.APPLICATION_DATA);
		
		if (!ctx.hasNextKeys()) {
			TrafficKeys keys = keySchedule.deriveNextGenerationTrafficKeys();

			ctx.setEncryptor(encryptor(keys, ctx.getEncryptor().getProtector(), state.isClientMode()), KeyPhase.NEXT);
			ctx.setDecryptor(decryptor(keys, ctx.getDecryptor().getProtector(), state.isClientMode()), KeyPhase.NEXT);
			keys.clear();
		}
	}
	
	@Override
	public void onNewTrafficSecrets(IEngineState tlsState, RecordType recordType) throws Alert {
		TrafficKeys trKeys, nextKeys = null;
		SecretKeys hpKeys;
		
		try {
			KeySchedule tlsSchedule = tlsState.getKeySchedule();
			EncryptionLevel level;
			
			if (keySchedule == null) {
				ICipherSuiteSpec cipherSpec = tlsSchedule.getCipherSuiteSpec();

				keySchedule = new QuicKeySchedule(
						new Hkdf(cipherSpec.getHashSpec().getHash().createMac()),
						cipherSpec);
			}

			switch (recordType) {
			case ZERO_RTT:
				level = EncryptionLevel.EARLY_DATA;
				trKeys = keySchedule.deriveEarlyTrafficKey(tlsSchedule);
				hpKeys = keySchedule.deriveEarlyHeaderProtectionKey(tlsSchedule);
				break;

			case HANDSHAKE:
				level = EncryptionLevel.HANDSHAKE;
				trKeys = keySchedule.deriveHandshakeTrafficKeys(tlsSchedule);
				hpKeys = keySchedule.deriveHandshakeHeaderProtectionKeys(tlsSchedule);
				break;

			case APPLICATION:
				level = EncryptionLevel.APPLICATION_DATA;
				trKeys = keySchedule.deriveApplicationTrafficKeys(tlsSchedule);
				hpKeys = keySchedule.deriveApplicationHeaderProtectionKeys(tlsSchedule);
				keySchedule.deriveNextGenerationSecrets(tlsSchedule);
				nextKeys = keySchedule.deriveNextGenerationTrafficKeys();
				break;
				
			default:
				throw new IllegalStateException("Unexpected record type: " + recordType);
			}
			
			EncryptionContext ctx = state.getContext(level);
			boolean client = state.isClientMode();
			
			if (client) {
				ctx.setEncryptor(encryptor(trKeys, hpKeys, true));
				if (trKeys.getKey(false) != null) {
					ctx.setDecryptor(decryptor(trKeys, hpKeys, true));
				}
			}
			else {
				if (trKeys.getKey(false) != null) {
					ctx.setEncryptor(encryptor(trKeys, hpKeys, false));
				}
				ctx.setDecryptor(decryptor(trKeys, hpKeys, false));
			}
			if (nextKeys != null) {
				ctx.setEncryptor(encryptor(nextKeys, hpKeys, client), KeyPhase.NEXT);
				ctx.setDecryptor(decryptor(nextKeys, hpKeys, client), KeyPhase.NEXT);
				nextKeys.clear();
			}
			trKeys.clear();
			hpKeys.clear();
		}
		catch (Exception e) {
			throw new InternalErrorAlert("Failed to derive new traffic keys", e);
		}
	}

	@Override
	public void onNewReceivingTraficKey(IEngineState tlsState, RecordType recordType) throws Alert {
	}

	@Override
	public void onNewSendingTraficKey(IEngineState tlsState, RecordType recordType) throws Alert {
	}

	@Override
	public void onKeyUpdate(IEngineState tlsState, KeyUpdateRequest request) {
	}

	@Override
	public void onHandshake(IEngineState tlsState, IHandshake handshake) throws Alert {
		Alert alert = HANDSHAKE_ALERTS[handshake.getType().value()];
		
		if (alert != null) {
			throw alert;
		}
		
		int type = handshake.getType().value();
		
		if (type == CLIENT_HELLO) {
			IClientHello clientHello = (IClientHello) handshake;
			
			if (clientHello.getLegacySessionId().length > 0) {
				throw new QuicAlert(TransportError.PROTOCOL_VIOLATION, "Prohibited compatibility mode");
			}
		}
		else if (type == NEW_SESSION_TICKET) {
			IEarlyDataExtension earlyData = ExtensionsUtil.find(handshake, ExtensionType.EARLY_DATA);
			
			if (earlyData != null && earlyData.getMaxSize() != 0xffffffffL) {
				throw new QuicAlert(TransportError.PROTOCOL_VIOLATION, "Invalid max_early_data_size value in ticket");
			}
		}
	}

	@Override
	public void onHandshakeCreate(IEngineState state, IHandshake handshake, boolean isHRR) {
	}
	
	@Override
	public void onCleanup(IEngineState tlsState) {
		keySchedule.eraseAll();
		keySchedule = null;
	}

	@Override
	public void produceChangeCipherSpec(IEngineProducer producer) {
	}

	@Override
	public void prepareChangeCipherSpec(IEngineProducer producer) {
	}

}
