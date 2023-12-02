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
import static org.snf4j.tls.extension.ExtensionsUtil.find;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecryptErrorAlert;
import org.snf4j.tls.alert.HandshakeFailureAlert;
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.MissingExtensionAlert;
import org.snf4j.tls.alert.ProtocolVersionAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.alert.UnrecognizedNameAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IHash;
import org.snf4j.tls.crypto.IHkdf;
import org.snf4j.tls.crypto.IKeyExchange;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.extension.EarlyDataExtension;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IKeyShareExtension;
import org.snf4j.tls.extension.IPreSharedKeyExtension;
import org.snf4j.tls.extension.IPskKeyExchangeModesExtension;
import org.snf4j.tls.extension.IServerNameExtension;
import org.snf4j.tls.extension.ISignatureAlgorithmsExtension;
import org.snf4j.tls.extension.ISupportedGroupsExtension;
import org.snf4j.tls.extension.ISupportedVersionsExtension;
import org.snf4j.tls.extension.KeyShareEntry;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.ParsedKey;
import org.snf4j.tls.extension.PreSharedKeyExtension;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SignatureAlgorithmsCertExtension;
import org.snf4j.tls.extension.SignatureAlgorithmsExtension;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.extension.SupportedGroupsExtension;
import org.snf4j.tls.extension.SupportedVersionsExtension;
import org.snf4j.tls.handshake.Certificate;
import org.snf4j.tls.handshake.CertificateRequest;
import org.snf4j.tls.handshake.CertificateType;
import org.snf4j.tls.handshake.CertificateVerify;
import org.snf4j.tls.handshake.EncryptedExtensions;
import org.snf4j.tls.handshake.Finished;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IClientHello;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.ServerHello;
import org.snf4j.tls.handshake.ServerHelloRandom;
import org.snf4j.tls.record.RecordType;
import org.snf4j.tls.session.SessionTicket;
import org.snf4j.tls.session.UsedSession;

public class ClientHelloConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.CLIENT_HELLO;
	}
	
	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws Alert {
		boolean secondClientHello;
		
		switch (state.getState()) {
		case SRV_WAIT_1_CH:
			secondClientHello = false;
			break;
			
		case SRV_WAIT_2_CH:
			secondClientHello = true;
			break;
			
		default:
			throw new UnexpectedMessageAlert("Unexpected ClientHello");
		}
		
		IClientHello clientHello = (IClientHello) handshake;
		
		if (clientHello.getLegacyVersion() != EngineDefaults.LEGACY_VERSION) {
			throw new ProtocolVersionAlert("Invalid legacy version");
		}
		
		ISupportedVersionsExtension versions = find(handshake, ExtensionType.SUPPORTED_VERSIONS);
		if (versions == null) {
			throw new ProtocolVersionAlert("No support for TLS 1.2 or prior");
		}
		
		int negotiatedVersion = -1;
		for (int version: versions.getVersions()) {
			if (version == 0x0304) {
				negotiatedVersion = version;
			}
		}
		if (negotiatedVersion == -1) {
			throw new ProtocolVersionAlert("No support for TLS 1.3 by peer");
		}
		
		byte[] compressions = clientHello.getLegacyCompressionMethods();
		if (compressions.length != 1 || compressions[0] != 0) {
			throw new IllegalParameterAlert("Invalid compression methods");
		}
		
		CipherSuite cipherSuite = findMatch(
				state.getParameters().getCipherSuites(), 
				clientHello.getCipherSuites());
		if (cipherSuite == null) {
			throw new HandshakeFailureAlert("Failed to negotiate cipher suite");
		}
		else if (state.getCipherSuite() != null && !state.getCipherSuite().equals(cipherSuite)) {
			throw new IllegalParameterAlert("Negotiated cipher suite mismatch");
		}
		
		IKeyShareExtension keyShare = find(handshake, ExtensionType.KEY_SHARE);
		if (keyShare == null) {
			throw new MissingExtensionAlert("Missing key_share extension in ClientHello");
		}
		
		ISupportedGroupsExtension supportedGroups = find(handshake, ExtensionType.SUPPORTED_GROUPS);
		if (supportedGroups == null) {
			throw new MissingExtensionAlert("Missing supported_groups extension in ClientHello");
		}

		ISignatureAlgorithmsExtension signAlgorithms = find(handshake, ExtensionType.SIGNATURE_ALGORITHMS);
		if (signAlgorithms == null) {
			throw new MissingExtensionAlert("Missing signature_algorithms extension in ClientHello");
		}
		
		IPskKeyExchangeModesExtension preSharedKeyModes = find(handshake, ExtensionType.PSK_KEY_EXCHANGE_MODES);
		if (preSharedKeyModes != null) {
			state.setPskModes(preSharedKeyModes.getModes());
		}
		
		boolean earlyData = find(handshake, ExtensionType.EARLY_DATA) != null;
		if (secondClientHello && earlyData) {
			throw new IllegalParameterAlert("Early data extension not permitted in second ClientHello");
		}
		
		IPreSharedKeyExtension preSharedKey = find(handshake, ExtensionType.PRE_SHARED_KEY);
		if (preSharedKey != null) {
			if (preSharedKey != handshake.getExtensions().get(handshake.getExtensions().size()-1)) {
				throw new IllegalParameterAlert("PSK extension not the last extension");
			}
			
			if (preSharedKeyModes == null) {
				throw new MissingExtensionAlert("Missing psk_key_exchange_modes extension in ClientHello");
			}
			
			if (!state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE)) {
				preSharedKey = null;
				if (earlyData) {
					state.setEarlyDataContext(new EarlyDataContext(
							cipherSuite, 
							true, 
							state.getHandler().getMaxEarlyDataSize()));
					earlyData = false;
				}
			}
		}
		else if (earlyData) {
			throw new MissingExtensionAlert("Missing pre_shared_key extension in ClientHello");
		}
		
		KeyShareEntry keyShareEntry;
		if (state.getNamedGroup() != null) {
			KeyShareEntry[] entries = keyShare.getEntries();
			
			if (entries.length != 1 || !state.getNamedGroup().equals(entries[0].getNamedGroup())) {
				throw new IllegalParameterAlert("Negotiated key share group mismatch");
			}
			keyShareEntry = entries[0];
		}
		else {
			keyShareEntry = KeyShareEntry.findMatch(
					keyShare.getEntries(), 
					state.getParameters().getNamedGroups());
		}

		NamedGroup namedGroup = null;
		if (keyShareEntry != null) {
			namedGroup = find(supportedGroups.getGroups(), keyShareEntry.getNamedGroup());
			if (namedGroup == null) {
				throw new IllegalParameterAlert("KeyShareEntry not correspond with supported_groups extension");
			}
		}
		else if (earlyData) {
			state.setEarlyDataContext(new EarlyDataContext(
					cipherSuite, 
					true, 
					state.getHandler().getMaxEarlyDataSize()));
			earlyData = false;
		}
		if (namedGroup == null) {
			namedGroup = findMatch(
					state.getParameters().getNamedGroups(), 
					supportedGroups.getGroups());
			if (namedGroup == null) {
				throw new HandshakeFailureAlert("Failed to negotiate supported group");
			}
		}
		
		IServerNameExtension serverName = find(handshake, ExtensionType.SERVER_NAME);
		if (serverName == null) {
			if (state.getParameters().isServerNameRequired()) {
				throw new MissingExtensionAlert("Missing server_name extension in ClientHello");
			}
		}
		else if	(!state.getHandler().verify(serverName)) {
			throw new UnrecognizedNameAlert("Host name '" + serverName.getHostName() + "' is unrecognized");
		}
		else {
			state.setHostName(serverName.getHostName());
		}
		
		boolean partInitialization = false;
		UsedSession resumed = null;
		if (preSharedKey != null) {
			if (keyShareEntry != null) {
				resumed = state.getHandler().getSessionManager().useSession(
						preSharedKey.getOfferedPsks(),
						cipherSuite.spec().getHashSpec());
			}
			else {
				partInitialization = true;
			}
		}
		
		if (!state.isInitialized()) {
			try {
				IHash hash = cipherSuite.spec().getHashSpec().getHash();
				ITranscriptHash th = state.getTranscriptHash();
				
				if (th == null) {
					th = new TranscriptHash(hash.createMessageDigest());
				}
				
				if (partInitialization) {
					state.setTranscriptHash(th);
					state.getTranscriptHash().update(handshake.getType(), data);
				}
				else {
					IHkdf hkdf = new Hkdf(hash.createMac());

					state.initialize(new KeySchedule(hkdf, th, cipherSuite.spec()), cipherSuite);
					if (resumed != null) {
						state.getKeySchedule().deriveEarlySecret(resumed.getTicket().getPsk(), false);
						state.getKeySchedule().deriveBinderKey();

						int bindersLen = PreSharedKeyExtension.bindersLength(preSharedKey.getOfferedPsks());
						int i = data.length;
						ByteBuffer[] dupData = new ByteBuffer[i];
						ByteBuffer dupBuf;

						while(--i >= 0) {
							dupBuf = data[i].duplicate();
							dupData[i] = dupBuf;
							if (bindersLen > 0) {
								int len = Math.min(dupBuf.remaining(), bindersLen);

								if (len > 0) {
									bindersLen -= len;
									dupBuf.limit(dupBuf.limit()-len);
								}
							}
						}

						byte[] expectedBinder = state.getKeySchedule().computePskBinder(dupData);
						byte[] binder = preSharedKey.getOfferedPsks()[resumed.getSelectedIdentity()].getBinder();

						state.getKeySchedule().eraseBinderKey();
						if (!Arrays.equals(expectedBinder, binder)) {
							state.getHandler().getSessionManager().putTicket(
									resumed.getSession(), 
									resumed.getTicket());
							throw new DecryptErrorAlert("Invalid PSK binder");
						}
						state.setSession(resumed.getSession());
					}
					else {
						if (earlyData) {
							state.setEarlyDataContext(new EarlyDataContext(
									cipherSuite, 
									true, 
									state.getHandler().getMaxEarlyDataSize()));
							earlyData = false;
						}
						state.getKeySchedule().deriveEarlySecret();
					}
					state.getTranscriptHash().update(handshake.getType(), data);
					if (earlyData) {
						SessionTicket ticket = resumed.getTicket();
						
						if (resumed.getSelectedIdentity() == 0 
								&& ticket.getCipherSuite().equals(state.getCipherSuite())
								&& ticket.getMaxEarlyDataSize() > 0) {
							state.getKeySchedule().deriveEarlyTrafficSecret();
							state.getListener().onNewTrafficSecrets(state, RecordType.ZERO_RTT);
							state.getListener().onNewReceivingTraficKey(state, RecordType.ZERO_RTT);
							state.setEarlyDataContext(new EarlyDataContext(
									ticket.getCipherSuite(), 
									ticket.getMaxEarlyDataSize()));
						}
						else {
							state.setEarlyDataContext(new EarlyDataContext(
									cipherSuite, 
									true, 
									state.getHandler().getMaxEarlyDataSize()));
							earlyData = false;
						}
					}
				}
			} catch (Alert e) {
				throw e;
			} catch (Exception e) {
				throw new InternalErrorAlert("Failed to create key schedule", e);
			}			
		}
		else {
			state.getTranscriptHash().update(handshake.getType(), data);
		}
		
		if (keyShareEntry == null) {
			if (state.getState() == MachineState.SRV_WAIT_2_CH) {
				throw new InternalErrorAlert("Unexpected second HelloRetryRquest");
			}

			List<IExtension> extensions = new ArrayList<IExtension>();

			state.setNamedGroup(namedGroup);
			extensions.add(new SupportedVersionsExtension(
					ISupportedVersionsExtension.Mode.SERVER_HELLO, 
					negotiatedVersion));
			extensions.add(new KeyShareExtension(namedGroup));
			
			ServerHello helloRetryRequest = new ServerHello(
					EngineDefaults.LEGACY_VERSION,
					ServerHelloRandom.getHelloRetryRequestRandom(),
					clientHello.getLegacySessionId(),
					cipherSuite,
					(byte)0,
					extensions);
			ConsumerUtil.produceHRR(state, helloRetryRequest, RecordType.INITIAL);
			
			if (clientHello.getLegacySessionId().length > 0) {
				state.getListener().produceChangeCipherSpec(state);
			}
			state.changeState(MachineState.SRV_WAIT_2_CH);
			return;
		}
		state.setVersion(negotiatedVersion);
		
		DelegatedTaskMode taskMode = state.getParameters().getDelegatedTaskMode();
		AbstractEngineTask task = new KeyExchangeTask(
				namedGroup, 
				keyShareEntry.getParsedKey(), 
				clientHello.getLegacySessionId(),
				resumed != null ? resumed.getSelectedIdentity() : -1,
				state.getParameters().getSecureRandom()); 
		if (taskMode.all()) {
			state.addTask(task);
		}
		else {
			task.run(state);
		}
		
		if (resumed != null) {
			task = new CertificateTask();
			if (!taskMode.all()) {
				taskMode = DelegatedTaskMode.NONE;
			}
		}
		else {
			ISignatureAlgorithmsExtension signAlgorithmsCert = find(handshake, ExtensionType.SIGNATURE_ALGORITHMS_CERT);
			task = new CertificateTask(
					state.getHandler().getCertificateSelector(),
					new CertificateCriteria(
							true,
							CertificateType.X509,
							state.getHostName(),
							signAlgorithms.getSchemes(),
							signAlgorithmsCert == null ? null : signAlgorithmsCert.getSchemes(),
							state.getParameters().getSignatureSchemes().clone()
							));
		}
		if (taskMode.certificates()) {
			state.changeState(MachineState.SRV_WAIT_TASK);
			state.addTask(task);
		}
		else {
			task.run(state);
		}
	}

	static class KeyExchangeTask extends AbstractEngineTask {

		private final NamedGroup namedGroup;
		
		private final ParsedKey parsedKey;
		
		private final byte[] legacySessionId;
		
		private final int selectedIdentity;
		
		private final SecureRandom random;
		
		private volatile byte[] secret;
		
		private volatile PublicKey publicKey;
		
		KeyExchangeTask(NamedGroup namedGroup, ParsedKey parsedKey, byte[] legacySessionId, int selectedIdentity, SecureRandom random) {
			this.namedGroup = namedGroup;
			this.parsedKey = parsedKey;
			this.legacySessionId = legacySessionId;
			this.selectedIdentity = selectedIdentity;
			this.random = random;
		}
		
		@Override
		public boolean isProducing() {
			return true;
		}
		
		@Override
		public String name() {
			return "Key exchange";
		}
		
		@Override
		void execute() throws Exception {
			IKeyExchange keyExchange = namedGroup.spec().getKeyExchange();
			KeyPair pair = keyExchange.generateKeyPair(random);
			publicKey = pair.getPublic();
			secret = keyExchange.generateSecret(
					pair.getPrivate(), 
					namedGroup.spec().generateKey(parsedKey),
					random);
		}

		@Override
		public void finish(EngineState state) throws Alert {
			List<IExtension> extensions = new ArrayList<IExtension>();
			
			extensions.add(new SupportedVersionsExtension(
					ISupportedVersionsExtension.Mode.SERVER_HELLO, 
					state.getVersion()));
			extensions.add(new KeyShareExtension(
					IKeyShareExtension.Mode.SERVER_HELLO, 
					new KeyShareEntry(namedGroup, publicKey)));
			if (selectedIdentity >= 0) {
				extensions.add(new PreSharedKeyExtension(selectedIdentity));
			}
			
			byte[] random = new byte[32];
			
			state.getParameters().getSecureRandom().nextBytes(random);

			ServerHello serverHello = new ServerHello(
					EngineDefaults.LEGACY_VERSION,
					random,
					legacySessionId,
					state.getCipherSuite(),
					(byte)0,
					extensions);
			ConsumerUtil.prepare(state, serverHello, RecordType.INITIAL, RecordType.HANDSHAKE);

			if (legacySessionId.length > 0 && !state.hadState(MachineState.SRV_WAIT_2_CH)) {
				state.getListener().prepareChangeCipherSpec(state);
			}
			
			try {
				state.getKeySchedule().deriveHandshakeSecret(secret);
				state.getKeySchedule().deriveHandshakeTrafficSecrets();
			}
			catch (Exception e) {
				throw new InternalErrorAlert("Failed to derive handshake secret", e);
			}
			state.getListener().onNewTrafficSecrets(state, RecordType.HANDSHAKE);
			Arrays.fill(secret, (byte)0);
			
			String hostName = state.getHostName();
			extensions = new ArrayList<IExtension>();
			if (hostName != null)  {
				extensions.add(new ServerNameExtension());
			}
			if (state.getEarlyDataContext().getState() == EarlyDataState.PROCESSING) {
				extensions.add(new EarlyDataExtension());
				state.getListener().onNewReceivingTraficKey(state, RecordType.ZERO_RTT);
			}
			else {
				state.getListener().onNewReceivingTraficKey(state, RecordType.HANDSHAKE);
			}
			extensions.add(new SupportedGroupsExtension(state.getParameters().getNamedGroups()));
			
			EncryptedExtensions encryptedExtensions = new EncryptedExtensions(extensions);
			ConsumerUtil.prepare(state, encryptedExtensions, RecordType.HANDSHAKE);	
		}
	}
	
	static class CertificateTask extends AbstractCertificateTask {

		CertificateTask(ICertificateSelector selector, CertificateCriteria criteria) {
			super(selector, criteria);
		}

		CertificateTask() {
		}
		
		@Override
		public void finish(EngineState state) throws Alert {
			MachineState nextState = state.getEarlyDataContext().getState() == EarlyDataState.PROCESSING 
					? MachineState.SRV_WAIT_EOED 
					: MachineState.SRV_WAIT_FINISHED;
			
			if (certificates != null) {
				IEngineParameters params = state.getParameters();
				
				if (params.getClientAuth() != ClientAuth.NONE) {
					List<IExtension> extensions = new ArrayList<IExtension>();
					SignatureScheme[] signSchemes;
					
					extensions.add(new SignatureAlgorithmsExtension(params.getSignatureSchemes()));
					signSchemes = params.getSignatureSchemesCert();
					if (signSchemes != null) {
						extensions.add(new SignatureAlgorithmsCertExtension(signSchemes));
					}
					CertificateRequest certificateRequest = new CertificateRequest(extensions);
					ConsumerUtil.prepare(state, certificateRequest, RecordType.HANDSHAKE);
					nextState = MachineState.SRV_WAIT_CERT;
				}
				
				state.getSessionInfo().localCerts(certificates.getCertificates());
				Certificate certificate = new Certificate(new byte[0], certificates.getEntries());
				ConsumerUtil.prepare(state, certificate, RecordType.HANDSHAKE);	

				byte[] signature = ConsumerUtil.sign(state.getTranscriptHash().getHash(HandshakeType.CERTIFICATE, false), 
						certificates.getAlgorithm(), 
						certificates.getPrivateKey(), 
						false,
						params.getSecureRandom());
				CertificateVerify certificateVerify = new CertificateVerify(certificates.getAlgorithm(), signature);
				ConsumerUtil.prepare(state, certificateVerify, RecordType.HANDSHAKE);	
			}
			
			try {
				Finished finished = new Finished(state.getKeySchedule().computeServerVerifyData());
				ConsumerUtil.prepare(state, finished, RecordType.HANDSHAKE, RecordType.APPLICATION);
				state.getKeySchedule().deriveMasterSecret();
				state.getKeySchedule().deriveApplicationTrafficSecrets();
			} catch (Exception e) {
				throw new InternalErrorAlert("Failed to compute server verify data", e);
			}
			state.getListener().onNewTrafficSecrets(state, RecordType.APPLICATION);
			state.changeState(nextState);
		}

	}
}
