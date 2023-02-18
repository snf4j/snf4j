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
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import org.snf4j.tls.IntConstant;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.MissingExtensionAlert;
import org.snf4j.tls.alert.ProtocolVersionAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IHash;
import org.snf4j.tls.crypto.IHkdf;
import org.snf4j.tls.crypto.IKeyExchange;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.ExtensionsUtil;
import org.snf4j.tls.extension.ICookieExtension;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IKeyShareExtension;
import org.snf4j.tls.extension.ISupportedGroupsExtension;
import org.snf4j.tls.extension.ISupportedVersionsExtension;
import org.snf4j.tls.extension.KeyShareEntry;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IClientHello;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.IServerHello;
import org.snf4j.tls.record.RecordType;

public class ServerHelloConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.SERVER_HELLO;
	}

	private void consumeHRR(EngineState state, IServerHello serverHello, ByteBuffer[] data, IKeyShareExtension keyShare) throws Alert {
		ConsumerUtil.updateHRRTranscriptHash(state, data);
		
		IClientHello clientHello = state.getClientHello();
		List<IExtension> extensions = clientHello.getExtensioins();
		NamedGroup namedGroup = null;
		boolean changed = false;
		
		if (keyShare != null) {
			int size = extensions.size();
			NamedGroup group = keyShare.getNamedGroup();
			
			for (int i=0; i<size; ++i) {
				IExtension extension = extensions.get(i);
				
				if (extension.getType().equals(ExtensionType.KEY_SHARE)) {
					PrivateKey key = state.getPrivateKey(group);
					
					state.clearPrivateKeys();
					if (key == null) {
						ISupportedGroupsExtension suppGroups = ExtensionsUtil.find(clientHello, ExtensionType.SUPPORTED_GROUPS);
						
						if (IntConstant.find(suppGroups.getGroups(), group) == null) {
							throw new IllegalParameterAlert("Unexpected supported group in HelloRetryRequest");
						}
						namedGroup = group;
						break;
					}
					else {
						KeyShareEntry[] entries = ((IKeyShareExtension)extension).getEntries();

						if (entries.length > 1) {
							int j = 0;
							
							for (;;) {
								KeyShareEntry entry = entries[j++];
								
								if (entry.getNamedGroup().equals(group)) {
									extensions.set(i, new KeyShareExtension(
											IKeyShareExtension.Mode.CLIENT_HELLO, 
											entry));
									break;
								}
							}
							state.storePrivateKey(group, key);
							changed = true;
						}
					}
				}
			}
		}

		ICookieExtension cookie = find(serverHello, ExtensionType.COOKIE);
		
		if (namedGroup == null) {
			if (cookie != null) {
				extensions.add(0, cookie);
				changed = true;
			}
			
			if (!changed) {
				throw new IllegalParameterAlert("No change after HelloRetryRequest");
			}
			
			//TODO: skip if early data is offered
			if (state.getParameters().isCompatibilityMode()) {
				state.getListener().produceChangeCipherSpec(state);
			}
			ConsumerUtil.produce(state, clientHello, RecordType.INITIAL, RecordType.HANDSHAKE);
		}
		else {
			KeyExchangeTask task = new KeyExchangeTask(cookie, namedGroup);
			if (state.getParameters().getDelegatedTaskMode().all()) {
				state.changeState(MachineState.CLI_WAIT_TASK);
				state.addTask(task);
			}
			else {
				task.run(state);
			}
		}
	}
	
	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws Alert {
		if (state.getState() == MachineState.CLI_WAIT_2_SH) {
			if (isHRR) {
				throw new UnexpectedMessageAlert("Unexpected HelloRetryRequest");
			}
		}
		else if (state.getState() != MachineState.CLI_WAIT_1_SH) {
			throw new UnexpectedMessageAlert("Unexpected ServerHello");
		}
		
		IServerHello serverHello = (IServerHello) handshake;

		if (serverHello.getLegacyVersion() != EngineDefaults.LEGACY_VERSION) {
			throw new ProtocolVersionAlert("Invalid legacy version");
		}
		
		if (!Arrays.equals(serverHello.getLegacySessionId(), state.getClientHello().getLegacySessionId())) {
			throw new IllegalParameterAlert("Unexpexted value of legacy session id");
		}
		
		CipherSuite cipherSuite = IntConstant.find(
				state.getClientHello().getCipherSuites(), 
				serverHello.getCipherSuite());
		if (cipherSuite == null) {
			throw new IllegalParameterAlert("Not offered cipher suite");	
		}
		
		if (serverHello.getLegacyCompressionMethod() != 0) {
			throw new IllegalParameterAlert("Invalid compression method");	
		}
		
		ISupportedVersionsExtension versions = find(handshake, ExtensionType.SUPPORTED_VERSIONS);
		if (versions == null) {
			throw new ProtocolVersionAlert("Missing supported_version extension in ServerHello");
		}
		int negotiatedVersion = versions.getVersions()[0]; 
		if (negotiatedVersion != 0x0304) {
			throw new IllegalParameterAlert("Invalid TLS version");	
		}
		
		IKeyShareExtension keyShare = find(handshake, ExtensionType.KEY_SHARE);
		if (keyShare == null && !isHRR) {
			throw new MissingExtensionAlert("Missing key_share extension in ServerHello");
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
				throw new InternalErrorAlert("Failed to create key schedule", e);
			}			
		}
		
		if (isHRR) {
			consumeHRR(state, serverHello, data, keyShare);
			return;
		}
		
		ConsumerUtil.updateTranscriptHash(state, handshake.getType(), data);
		
		NamedGroup namedGroup = keyShare.getEntries()[0].getNamedGroup();
		PrivateKey privateKey = state.getPrivateKey(namedGroup);
		if (privateKey == null) {
			throw new IllegalParameterAlert("Unexpected supported group in ServerHello");
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
			throw new InternalErrorAlert("Failed to derive handshake secret", e);
		}
		state.setVersion(negotiatedVersion);
		state.changeState(MachineState.CLI_WAIT_EE);
	}

	static class KeyExchangeTask extends AbstractEngineTask {

		private final ICookieExtension cookie;
		
		private final NamedGroup namedGroup;
		
		private volatile KeyPair pair;
		
		KeyExchangeTask(ICookieExtension cookie, NamedGroup namedGroup) {
			this.cookie = cookie;
			this.namedGroup = namedGroup;
		}
		
		@Override
		public String name() {
			return "Key exchange";
		}

		@Override
		public boolean isProducing() {
			return true;
		}

		@Override
		public void finish(EngineState state) throws Alert {
			IClientHello clientHello = state.getClientHello();
			List<IExtension> extensions = clientHello.getExtensioins();
			int size = extensions.size();
			KeyShareEntry entry = new KeyShareEntry(namedGroup, pair.getPublic());

			state.storePrivateKey(namedGroup, pair.getPrivate());
			for (int i=0; i<size; ++i) {
				IExtension extension = extensions.get(i);
				
				if (extension.getType().equals(ExtensionType.KEY_SHARE)) {
					
					extensions.set(i, new KeyShareExtension(IKeyShareExtension.Mode.CLIENT_HELLO, entry));
				}
			}
			if (cookie != null) {
				extensions.add(0, cookie);
			}
			ConsumerUtil.produce(state, clientHello, RecordType.INITIAL, RecordType.HANDSHAKE);
			state.changeState(MachineState.CLI_WAIT_2_SH);
		}

		@Override
		void execute() throws Exception {
			pair = namedGroup.spec().getKeyExchange().generateKeyPair();
		}
	}
}
