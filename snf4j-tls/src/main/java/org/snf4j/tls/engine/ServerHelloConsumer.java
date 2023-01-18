/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.engine;

import static org.snf4j.tls.extension.ExtensionsUtil.find;

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import org.snf4j.tls.IntConstant;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.IllegalParameterAlertException;
import org.snf4j.tls.alert.InternalErrorAlertException;
import org.snf4j.tls.alert.MissingExtensionAlertException;
import org.snf4j.tls.alert.ProtocolVersionAlertException;
import org.snf4j.tls.alert.UnexpectedMessageAlertException;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IHash;
import org.snf4j.tls.crypto.IHkdf;
import org.snf4j.tls.crypto.IKeyExchange;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IKeyShareExtension;
import org.snf4j.tls.extension.ISupportedVersionsExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.IServerHello;
import org.snf4j.tls.record.RecordType;

public class ServerHelloConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.SERVER_HELLO;
	}

	private void consumeHRR(EngineState state, IHandshake handshake, ByteBuffer[] data) {
		ConsumerUtil.updateHRRTranscriptHash(state, data);
	}
	
	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws AlertException {
		if (state.getState() != MachineState.CLI_WAIT_SH) {
			throw new UnexpectedMessageAlertException("Unexpected ServerHello");
		}
		
		IServerHello serverHello = (IServerHello) handshake;

		if (serverHello.getLegacyVersion() != EngineDefaults.LEGACY_VERSION) {
			throw new ProtocolVersionAlertException("Invalid legacy version");
		}
		
		if (!Arrays.equals(serverHello.getLegacySessionId(), state.getClientHello().getLegacySessionId())) {
			throw new IllegalParameterAlertException("Unexpexted value of legacy session id");
		}
		
		CipherSuite cipherSuite = IntConstant.find(
				state.getClientHello().getCipherSuites(), 
				serverHello.getCipherSuite());
		if (cipherSuite == null) {
			throw new IllegalParameterAlertException("Not offered cipher suite");	
		}
		
		if (serverHello.getLegacyCompressionMethod() != 0) {
			throw new IllegalParameterAlertException("Invalid compression method");	
		}
		
		ISupportedVersionsExtension versions = find(handshake, ExtensionType.SUPPORTED_VERSIONS);
		if (versions == null) {
			throw new ProtocolVersionAlertException("Missing supported_version extension in ServerHello");
		}
		int negotiatedVersion = versions.getVersions()[0]; 
		if (negotiatedVersion != 0x0304) {
			throw new IllegalParameterAlertException("Invalid TLS version");	
		}
		
		IKeyShareExtension keyShare = find(handshake, ExtensionType.KEY_SHARE);
		if (keyShare == null) {
			throw new MissingExtensionAlertException("Missing key_share extension in ServerHello");
		}
		
		if (!state.isInitialized()) {
			try {
				IHash hash = cipherSuite.spec().getHashSpec().getHash();
				ITranscriptHash th = new TranscriptHash(hash.createMessageDigest());
				IHkdf hkdf = new Hkdf(hash.createMac());
				
				state.initialize(new KeySchedule(hkdf, th, cipherSuite.spec()), th, cipherSuite);
				state.getTranscriptHash().update(HandshakeType.CLIENT_HELLO, state.getClientHello().prepare());
				state.getKeySchedule().deriveEarlySecret();	
				state.getKeySchedule().deriveEarlyTrafficSecret();
				state.getListener().onEarlyTrafficSecret(state);
			} catch (Exception e) {
				throw new InternalErrorAlertException("Failed to create key schedule", e);
			}			
		}
		
		if (isHRR) {
			consumeHRR(state, handshake, data);
			return;
		}
		
		ConsumerUtil.updateTranscriptHash(state, handshake.getType(), data);
		
		NamedGroup namedGroup = keyShare.getEntries()[0].getNamedGroup();
		PrivateKey privateKey = state.getPrivateKey(namedGroup);
		if (privateKey == null) {
			throw new IllegalParameterAlertException("Unexpected supported group in ServerHello");
		}
		try {
			IKeyExchange keyExchange = namedGroup.spec().getKeyExchange();
			PublicKey publicKey = namedGroup.spec().generateKey(keyShare.getEntries()[0].getParsedKey());
			byte[] secret = keyExchange.generateSecret(privateKey, publicKey);
			
			state.getKeySchedule().deriveHandshakeSecret(secret);
			state.getKeySchedule().deriveHandshakeTrafficSecrets();
			state.getListener().onHandshakeTrafficSecrets(state);
			state.getListener().onReceivingTraficKey(RecordType.HANDSHAKE);
		} 
		catch (Exception e) {
			throw new InternalErrorAlertException("Failed to derive handshake secret", e);
		}
		state.changeState(MachineState.CLI_WAIT_EE);
	}

}
