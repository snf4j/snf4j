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

import static org.snf4j.tls.IntConstant.findMatch;
import static org.snf4j.tls.IntConstant.find;
import static org.snf4j.tls.extension.ExtensionType.KEY_SHARE;
import static org.snf4j.tls.extension.ExtensionType.SUPPORTED_GROUPS;
import static org.snf4j.tls.extension.ExtensionType.SUPPORTED_VERSIONS;
import static org.snf4j.tls.extension.ExtensionsUtil.find;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.HandshakeFailureAlertException;
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
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IKeyShareExtension;
import org.snf4j.tls.extension.ISupportedGroupsExtension;
import org.snf4j.tls.extension.ISupportedVersionsExtension;
import org.snf4j.tls.extension.KeyShareEntry;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.SupportedVersionsExtension;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IClientHello;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.ServerHello;
import org.snf4j.tls.record.RecordType;

public class ClientHelloConsumer extends AbstractConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.CLIENT_HELLO;
	}
	
	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws AlertException {
		if (state.getState() != MachineState.SRV_START) {
			throw new UnexpectedMessageAlertException("Unexpected ClientHello message");
		}
		
		IClientHello clientHello = (IClientHello) handshake;
		
		if (clientHello.getLegacyVersion() != EngineDefaults.LEGACY_VERSION) {
			throw new ProtocolVersionAlertException("Invalid legacy version");
		}
		
		ISupportedVersionsExtension versions = find(handshake, SUPPORTED_VERSIONS);
		if (versions == null) {
			throw new ProtocolVersionAlertException("No support for TLS 1.2 or prior");
		}
		
		int negotiatedVersion = -1;
		for (int version: versions.getVersions()) {
			if (version == 0x0304) {
				negotiatedVersion = version;
			}
		}
		if (negotiatedVersion == -1) {
			throw new ProtocolVersionAlertException("No support for TLS 1.3 by peer");
		}
		
		CipherSuite cipherSuite = findMatch(
				state.getParameters().getCipherSuites(), 
				clientHello.getCipherSuites());
		if (cipherSuite == null) {
			throw new HandshakeFailureAlertException("Failed to negotiate cipher suite");
		}
		
		IKeyShareExtension keyShare = find(handshake, KEY_SHARE);
		if (keyShare == null) {
			throw new MissingExtensionAlertException("Missing key_share extension in ClientHello");
		}
		
		ISupportedGroupsExtension supportedGroups = find(handshake, SUPPORTED_GROUPS);
		if (supportedGroups == null) {
			throw new MissingExtensionAlertException("Missing supported_groups extension in ClientHello");
		}
		
		KeyShareEntry keyShareEntry = KeyShareEntry.findMatch(
				keyShare.getEntries(), 
				state.getParameters().getNamedGroups());

		NamedGroup namedGroup = null;
		if (keyShareEntry != null) {
			namedGroup = find(supportedGroups.getGroups(), keyShareEntry.getNamedGroup());
			if (namedGroup == null) {
				throw new IllegalParameterAlertException("KeyShareEntry not correspond with supported_groups extension");
			}
		}
		if (namedGroup == null) {
			namedGroup = findMatch(
					state.getParameters().getNamedGroups(), 
					supportedGroups.getGroups());
			if (namedGroup == null) {
				throw new HandshakeFailureAlertException("Failed to negotiate supported group");
			}
		}
		
		if (!state.isInitialized()) {
			IHash hash = cipherSuite.spec().getHashSpec().getHash();

			try {
				ITranscriptHash th = new TranscriptHash(hash.createMessageDigest());
				IHkdf hkdf = new Hkdf(hash.createMac());
				
				state.initialize(new KeySchedule(hkdf, th, cipherSuite.spec()), th);
				state.getKeySchedule().deriveEarlySecret();
			} catch (Exception e) {
				throw new InternalErrorAlertException("Failed to create key schedule", e);
			}			
		}
		updateTranscriptHash(state, handshake.getType(), data);
		
		List<IExtension> extensions = new ArrayList<IExtension>();

		extensions.add(new SupportedVersionsExtension(
				ISupportedVersionsExtension.Mode.SERVER_HELLO, 
				negotiatedVersion));
		if (keyShareEntry == null) {
			extensions.add(new KeyShareExtension(namedGroup));
			ServerHello helloRetryRequest = new ServerHello(
					EngineDefaults.LEGACY_VERSION,
					ServerHelloRandom.getHelloRetryRequestRandom(),
					clientHello.getLegacySessionId(),
					cipherSuite,
					(byte)0,
					extensions);
			produceHRR(state, helloRetryRequest, RecordType.INITIAL);
			return;
		}
		
		try {
			IKeyExchange keyExchange = namedGroup.spec().getKeyExchange();
			KeyPair pair = keyExchange.generateKeyPair();
			PublicKey publicKey = namedGroup.spec().generateKey(keyShareEntry.getParsedKey());
			byte[] secret = keyExchange.generateSecret(pair.getPrivate(), publicKey);
			
			state.getKeySchedule().deriveHandshakeSecret(secret);
			Arrays.fill(secret, (byte)0);
			extensions.add(new KeyShareExtension(
					IKeyShareExtension.Mode.SERVER_HELLO, 
					new KeyShareEntry(namedGroup, publicKey)));
		} catch (Exception e) {
			throw new InternalErrorAlertException("Failed to derive handshake secret", e);
		}
		
		byte[] random = new byte[32];
		
		state.getParameters().getSecureRandom().nextBytes(random);

		ServerHello serverHello = new ServerHello(
				EngineDefaults.LEGACY_VERSION,
				random,
				clientHello.getLegacySessionId(),
				cipherSuite,
				(byte)0,
				extensions);
		produce(state, serverHello, RecordType.INITIAL);
	}

}
