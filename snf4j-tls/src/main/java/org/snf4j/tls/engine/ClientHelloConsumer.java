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
import org.snf4j.tls.alert.UnrecognizedNameAlertException;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IHash;
import org.snf4j.tls.crypto.IHkdf;
import org.snf4j.tls.crypto.IKeyExchange;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IKeyShareExtension;
import org.snf4j.tls.extension.IServerNameExtension;
import org.snf4j.tls.extension.ISignatureAlgorithmsExtension;
import org.snf4j.tls.extension.ISupportedGroupsExtension;
import org.snf4j.tls.extension.ISupportedVersionsExtension;
import org.snf4j.tls.extension.KeyShareEntry;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.ParsedKey;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SupportedGroupsExtension;
import org.snf4j.tls.extension.SupportedVersionsExtension;
import org.snf4j.tls.handshake.Certificate;
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

public class ClientHelloConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.CLIENT_HELLO;
	}
	
	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws AlertException {
		if (state.getState() != MachineState.SRV_START) {
			throw new UnexpectedMessageAlertException("Unexpected ClientHello");
		}
		
		IClientHello clientHello = (IClientHello) handshake;
		
		if (clientHello.getLegacyVersion() != EngineDefaults.LEGACY_VERSION) {
			throw new ProtocolVersionAlertException("Invalid legacy version");
		}
		
		ISupportedVersionsExtension versions = find(handshake, ExtensionType.SUPPORTED_VERSIONS);
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
		
		byte[] compressions = clientHello.getLegacyCompressionMethods();
		if (compressions.length != 1 || compressions[0] != 0) {
			throw new IllegalParameterAlertException("Invalid compression methods");
		}
		
		CipherSuite cipherSuite = findMatch(
				state.getParameters().getCipherSuites(), 
				clientHello.getCipherSuites());
		if (cipherSuite == null) {
			throw new HandshakeFailureAlertException("Failed to negotiate cipher suite");
		}
		else if (state.getCipherSuite() != null && !state.getCipherSuite().equals(cipherSuite)) {
			throw new IllegalParameterAlertException("Negotiated cipher suite mismatch");
		}
		
		IKeyShareExtension keyShare = find(handshake, ExtensionType.KEY_SHARE);
		if (keyShare == null) {
			throw new MissingExtensionAlertException("Missing key_share extension in ClientHello");
		}
		
		ISupportedGroupsExtension supportedGroups = find(handshake, ExtensionType.SUPPORTED_GROUPS);
		if (supportedGroups == null) {
			throw new MissingExtensionAlertException("Missing supported_groups extension in ClientHello");
		}

		ISignatureAlgorithmsExtension signAlgorithms = find(handshake, ExtensionType.SIGNATURE_ALGORITHMS);
		if (signAlgorithms == null) {
			throw new MissingExtensionAlertException("Missing signature_algorithms extension in ClientHello");
		}
		
		KeyShareEntry keyShareEntry;
		if (state.getNamedGroup() != null) {
			KeyShareEntry[] entries = keyShare.getEntries();
			
			if (entries.length != 1 || !state.getNamedGroup().equals(entries[0].getNamedGroup())) {
				throw new IllegalParameterAlertException("Negotiated key share group mismatch");
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
		
		IServerNameExtension serverName = find(handshake, ExtensionType.SERVER_NAME);
		if (serverName == null) {
			if (state.getParameters().isServerNameRequired()) {
				throw new MissingExtensionAlertException("Missing server_name extension in ClientHello");
			}
		}
		else if	(!state.getHandler().verify(serverName)) {
			throw new UnrecognizedNameAlertException("Host name '" + serverName.getHostName() + "' is unrecognized");
		}
		else {
			state.setHostName(serverName.getHostName());
		}
		
		if (!state.isInitialized()) {
			try {
				IHash hash = cipherSuite.spec().getHashSpec().getHash();
				ITranscriptHash th = new TranscriptHash(hash.createMessageDigest());
				IHkdf hkdf = new Hkdf(hash.createMac());
				
				state.initialize(new KeySchedule(hkdf, th, cipherSuite.spec()), th, cipherSuite);
				state.getKeySchedule().deriveEarlySecret();
				state.getKeySchedule().deriveEarlyTrafficSecret();
				state.getListener().onEarlyTrafficSecret(state);
			} catch (Exception e) {
				throw new InternalErrorAlertException("Failed to create key schedule", e);
			}			
		}
		ConsumerUtil.updateTranscriptHash(state, handshake.getType(), data);
		
		List<IExtension> extensions = new ArrayList<IExtension>();

		extensions.add(new SupportedVersionsExtension(
				ISupportedVersionsExtension.Mode.SERVER_HELLO, 
				negotiatedVersion));
		if (keyShareEntry == null) {
			state.setNamedGroup(namedGroup);
			extensions.add(new KeyShareExtension(namedGroup));
			ServerHello helloRetryRequest = new ServerHello(
					EngineDefaults.LEGACY_VERSION,
					ServerHelloRandom.getHelloRetryRequestRandom(),
					clientHello.getLegacySessionId(),
					cipherSuite,
					(byte)0,
					extensions);
			ConsumerUtil.produceHRR(state, helloRetryRequest, RecordType.INITIAL);
			return;
		}
		state.setVersion(negotiatedVersion);
		state.changeState(MachineState.SRV_NEGOTIATED);
		
		DelegatedTaskMode taskMode = state.getParameters().getDelegatedTaskMode();
		AbstractEngineTask task = new KeyExchangeTask(
				namedGroup, 
				keyShareEntry.getParsedKey(), 
				clientHello.getLegacySessionId()); 
		if (taskMode.all()) {
			state.addTask(task);
		}
		else {
			task.run(state);
		}
		
		ISignatureAlgorithmsExtension signAlgorithmsCert = find(handshake, ExtensionType.SIGNATURE_ALGORITHMS_CERT);
		task = new CertificateTask(
				state.getHandler().getCertificateSelector(),
				new CertificateCriteria(
					CertificateType.X509,
					state.getHostName(),
					signAlgorithms.getSchemes(),
					signAlgorithmsCert == null ? null : signAlgorithmsCert.getSchemes()
				));
		if (taskMode.certificates()) {
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
		
		private volatile byte[] secret;
		
		private volatile PublicKey publicKey;
		
		KeyExchangeTask(NamedGroup namedGroup, ParsedKey parsedKey, byte[] legacySessionId) {
			this.namedGroup = namedGroup;
			this.parsedKey = parsedKey;
			this.legacySessionId = legacySessionId;
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
			KeyPair pair = keyExchange.generateKeyPair();
			publicKey = namedGroup.spec().generateKey(parsedKey);
			secret = keyExchange.generateSecret(pair.getPrivate(), publicKey);
		}

		@Override
		public void finish(EngineState state) throws AlertException {
			List<IExtension> extensions = new ArrayList<IExtension>();
			
			extensions.add(new SupportedVersionsExtension(
					ISupportedVersionsExtension.Mode.SERVER_HELLO, 
					state.getVersion()));
			try {
				state.getKeySchedule().deriveHandshakeSecret(secret);
				state.getKeySchedule().deriveHandshakeTrafficSecrets();
				state.getListener().onHandshakeTrafficSecrets(state);
				state.getListener().onReceivingTraficKey(RecordType.HANDSHAKE);
			}
			catch (Exception e) {
				throw new InternalErrorAlertException("Failed to derive handshake secret", e);
			}
			Arrays.fill(secret, (byte)0);
			extensions.add(new KeyShareExtension(
					IKeyShareExtension.Mode.SERVER_HELLO, 
					new KeyShareEntry(namedGroup, publicKey)));
			
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
			
			String hostName = state.getHostName();
			extensions = new ArrayList<IExtension>();
			if (hostName != null)  {
				extensions.add(new ServerNameExtension());
			}
			extensions.add(new SupportedGroupsExtension(state.getParameters().getNamedGroups()));
			
			EncryptedExtensions encryptedExtensions = new EncryptedExtensions(extensions);
			ConsumerUtil.prepare(state, encryptedExtensions, RecordType.HANDSHAKE);	
		}
	}
	
	static class CertificateTask extends AbstractEngineTask {

		private final ICertificateSelector selector;
		
		private final CertificateCriteria criteria;
		
		private volatile SelectedCertificates certificates;
		
		CertificateTask(ICertificateSelector selector, CertificateCriteria criteria) {
			this.selector = selector;
			this.criteria = criteria;
		}
		
		@Override
		public boolean isProducing() {
			return true;
		}

		@Override
		public String name() {
			return "Certificate";
		}

		@Override
		void execute() throws Exception {
			certificates = selector.selectCertificates(criteria);
		}
		
		@Override
		public void finish(EngineState state) throws AlertException {
			Certificate certificate = new Certificate(new byte[0], certificates.getEntries());
			ConsumerUtil.prepare(state, certificate, RecordType.HANDSHAKE);	
			
			byte[] signature = ConsumerUtil.sign(state.getTranscriptHash().getHash(HandshakeType.CERTIFICATE, false), 
					certificates.getAlgorithm(), 
					certificates.getPrivateKey(), 
					false);
			CertificateVerify certificateVerify = new CertificateVerify(certificates.getAlgorithm(), signature);
			ConsumerUtil.prepare(state, certificateVerify, RecordType.HANDSHAKE);	
			
			try {
				Finished finished = new Finished(state.getKeySchedule().computeServerVerifyData());
				ConsumerUtil.prepare(state, finished, RecordType.HANDSHAKE, RecordType.APPLICATION);
				state.getKeySchedule().deriveMasterSecret();
				state.getKeySchedule().deriveApplicationTrafficSecrets();
				state.getListener().onApplicationTrafficSecrets(state);
			} catch (Exception e) {
				throw new InternalErrorAlertException("Failed to compute server verify data", e);
			}
			state.changeState(MachineState.SRV_WAIT_FINISHED);
		}

	}
}
