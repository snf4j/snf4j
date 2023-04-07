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

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IHash;
import org.snf4j.tls.crypto.IHkdf;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.extension.ExtensionValidator;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IExtensionValidator;
import org.snf4j.tls.extension.IKeyShareExtension;
import org.snf4j.tls.extension.ISupportedVersionsExtension;
import org.snf4j.tls.extension.KeyShareEntry;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.OfferedPsk;
import org.snf4j.tls.extension.PreSharedKeyExtension;
import org.snf4j.tls.extension.PskIdentity;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.extension.PskKeyExchangeModesExtension;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SignatureAlgorithmsExtension;
import org.snf4j.tls.extension.SupportedGroupsExtension;
import org.snf4j.tls.extension.SupportedVersionsExtension;
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.HandshakeDecoder;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.IHandshakeDecoder;
import org.snf4j.tls.handshake.IServerHello;
import org.snf4j.tls.handshake.ServerHelloRandom;
import org.snf4j.tls.record.RecordType;
import org.snf4j.tls.session.ISession;
import org.snf4j.tls.session.ISessionManager;
import org.snf4j.tls.session.SessionTicket;

public class HandshakeEngine implements IHandshakeEngine {
	
	private final static Random RANDOM = new Random();
	
	private final static IHandshakeConsumer[] CONSUMERS;
	
	private final static SessionTicket[] EMPTY = new SessionTicket[0];
	
	private static void addConsumer(IHandshakeConsumer[] consumers, IHandshakeConsumer consumer) {
		CONSUMERS[consumer.getType().value()] = consumer;
	}
	
	static {
		CONSUMERS = new IHandshakeConsumer[21];
		addConsumer(CONSUMERS, new ClientHelloConsumer());	
		addConsumer(CONSUMERS, new ServerHelloConsumer());	
		addConsumer(CONSUMERS, new EncryptedExtensionsConsumer());
		addConsumer(CONSUMERS, new CertificateConsumer());
		addConsumer(CONSUMERS, new CertificateVerifyConsumer());
		addConsumer(CONSUMERS, new FinishedConsumer());
		addConsumer(CONSUMERS, new NewSessionTicketConsumer());
	}
	
	private final IHandshakeDecoder decoder;
	
	private final IExtensionValidator extensionValidator;
	
	private final EngineState state;
	
	public HandshakeEngine(boolean clientMode, IEngineParameters parameters, IEngineHandler handler, IEngineStateListener listener) {
		this(clientMode, parameters, handler, listener, HandshakeDecoder.DEFAULT);
	}

	public HandshakeEngine(boolean clientMode, IEngineParameters parameters, IEngineHandler handler, IEngineStateListener listener, IHandshakeDecoder decoder) {
		this.decoder = decoder;
		state = new EngineState(
				clientMode ? MachineState.CLI_INIT : MachineState.SRV_INIT, 
				parameters, 
				handler,
				listener);
		extensionValidator = ExtensionValidator.DEFAULT;
	}
	
	@Override
	public IEngineHandler getHandler() {
		return state.getHandler();
	}
	
	@Override
	public void consume(ByteBuffer[] srcs, int remaining) throws Alert {
		consume(ByteBufferArray.wrap(srcs), remaining);
	}

	@Override
	public void consume(ByteBufferArray srcs, int remaining) throws Alert {
		ByteBuffer[] data = srcs.array().clone();
		
		if (data.length == 1) {
			ByteBuffer dup = data[0].duplicate();
			
			dup.limit(dup.position() + remaining);
			data[0] = dup;
		}
		else {
			List<ByteBuffer> list = new ArrayList<ByteBuffer>(data.length);
			int left = remaining;
			int i = srcs.arrayIndex();
			
			for (;;) {
				ByteBuffer dup = data[i++].duplicate();
				
				list.add(dup);
				if (dup.remaining() < left) {
					left -= dup.remaining();
				}
				else {
					dup.limit(dup.position() + left);
					break;
				}
			}
			data = list.toArray(new ByteBuffer[list.size()]);
		}
		
		IHandshake handshake = decoder.decode(srcs, remaining);
		HandshakeType type = handshake.getType();
		int value = type.value();
		
		if (!handshake.isKnown()) {
			throw new UnexpectedMessageAlert("Unknown handshake type: " + value);
		}
		
		IHandshakeConsumer consumer = value < CONSUMERS.length ? CONSUMERS[value] : null;
		
		if (consumer != null) {
			List<IExtension> extensions = handshake.getExtensions();
			boolean isHRR = false;

			if (value == HandshakeType.SERVER_HELLO.value()) {
				if (ServerHelloRandom.isHelloRetryRequest((IServerHello)handshake)) {
					isHRR = true;
				}
			}
			if (extensions != null) {
				if (isHRR) {
					for (IExtension extension: extensions) {
						if (!extensionValidator.isAllowedInHelloRetryRequest(extension.getType())) {
							throw new IllegalParameterAlert(
									"Extension " + 
									extension.getType().name() + 
									" not allowed in hello_retry_request");
						}
					}					
				}
				else {
					for (IExtension extension: extensions) {
						if (!extensionValidator.isAllowed(extension.getType(), type)) {
							throw new IllegalParameterAlert(
									"Extension " + 
									extension.getType().name() + 
									" not allowed in " + 
									type.name());
						}
					}					
				}
			}
			consumer.consume(state, handshake, data, isHRR);
		}
		else {
			throw new UnexpectedMessageAlert("Unexpected handshake type: " + value);
		}
	}
	
	@Override
	public boolean needProduce() {
		return state.hasProduced();
	}
	
	@Override
	public ProducedHandshake[] produce() throws Alert {
		return state.getProduced();
	}

	@Override
	public boolean updateTasks() throws Alert {
		return state.updateTasks();
	}

	@Override
	public boolean hasProducingTask() {
		return state.hasProducingTasks();
	}

	@Override
	public boolean hasRunningTask() {
		return state.hasRunningTasks();
	}
	
	@Override
	public boolean hasTask() {
		return state.hasTasks();
	}
	
	@Override
	public Runnable getTask() {
		return state.getTask();
	}
	
	@Override
	public boolean isStarted() {
		return state.getState().isStarted();
	}
	
	@Override
	public boolean isConnected() {
		return state.getState().isConnected();
	}
	
	@Override
	public boolean isClientMode() {
		return state.isClientMode();
	}
	
	@Override
	public int getMaxFragmentLength() {
		return state.getMaxFragmentLength();
	}
	
	@Override
	public void start() throws Alert {
		if (isStarted()) {
			throw new InternalErrorAlert("Handshake has already started");
		}
		if (!state.isClientMode()) {
			state.changeState(MachineState.SRV_WAIT_1_CH);
			return;
		}
		
		IEngineParameters params = state.getParameters();
		NamedGroup[] groups = params.getNamedGroups();
		groups = Arrays.copyOf(groups, Math.min(groups.length, params.getNumberOfOfferedSharedKeys()));
		
		KeyExchangeTask task = new KeyExchangeTask(groups);
		if (groups.length > 0 && params.getDelegatedTaskMode().all()) {
			state.changeState(MachineState.CLI_WAIT_TASK);
			state.addTask(task);
		}
		else {
			task.run(state);
		}
	}
	
	static class KeyExchangeTask extends AbstractEngineTask {

		private final NamedGroup[] namedGroups;
		
		private volatile KeyPair[] pairs;
		
		KeyExchangeTask(NamedGroup[] namedGroups) {
			this.namedGroups = namedGroups;
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
			byte[] random = new byte[32];
			byte[] legacySessionId;
			IEngineParameters params = state.getParameters();
			
			params.getSecureRandom().nextBytes(random);
			if (params.isCompatibilityMode()) {
				legacySessionId = new byte[32];
				RANDOM.nextBytes(legacySessionId);
			}
			else {
				legacySessionId = new byte[0];
			}
			
			List<IExtension> extensions = new ArrayList<IExtension>();
			
			String peerHost = params.getPeerHost();
			NamedGroup[] groups = params.getNamedGroups();
			CipherSuite[] cipherSuites = params.getCipherSuites();
			
			if (peerHost != null) {
				extensions.add(new ServerNameExtension(peerHost));
			}
			extensions.add(new SupportedVersionsExtension(ISupportedVersionsExtension.Mode.CLIENT_HELLO, 0x0304));
			extensions.add(new SupportedGroupsExtension(groups));
			extensions.add(new SignatureAlgorithmsExtension(params.getSignatureSchemes()));
			PskKeyExchangeMode[] modes = PskKeyExchangeMode.implemented(params.getPskKeyExchangeModes());
			
			try {
				int offered = pairs.length;
				KeyShareEntry[] entries = new KeyShareEntry[offered];
				
				for (int i=0; i<offered; ++i) {
					entries[i] = new KeyShareEntry(namedGroups[i], pairs[i].getPublic());
					state.storePrivateKey(namedGroups[i], pairs[i].getPrivate());
				}
				extensions.add(new KeyShareExtension(IKeyShareExtension.Mode.CLIENT_HELLO, entries));
			} catch (Exception e) {
				throw new InternalErrorAlert("Failed to generate exchange key", e);
			}
			
			SessionTicket[] tickets = EMPTY;
			OfferedPsk[] psks = null;
			
			if (modes.length > 0) {
				extensions.add(new PskKeyExchangeModesExtension(modes));
				ISession session = state.getSession();
				ISessionManager manager;
				
				if (session == null) {
					manager = state.getHandler().getSessionManager();
					if (peerHost != null) {
						session = manager.getSession(peerHost, params.getPeerPort());
						state.setSession(session);
					}
				}
				else {
					manager = session.getManager();
				}
				
				if (session != null && session.isValid()) {
					tickets = session.getManager().getTickets(session);
					int ticketsLen = tickets.length;
					
					if (ticketsLen > 0) {
						int offerHashes = 0;
						int offered = 0;
						
						for (CipherSuite cipherSuite: cipherSuites) {
							offerHashes |= 1 << cipherSuite.spec().getHashSpec().getOrdinal();
						}
						
						for (int i=0; i<ticketsLen; ++i) {
							if ((offerHashes & (1 << tickets[i].getHashSpec().getOrdinal())) == 0) {
								tickets[i] = null;
							}
							else {
								++offered;
							}
						}
						
						if (offered > 0) {
							int i = 0;
							
							psks = new OfferedPsk[offered];
							for (SessionTicket ticket: tickets) {
								if (ticket != null) {
									long obfuscatedAge = (System.currentTimeMillis() - ticket.getCreationTime() + ticket.getAgeAdd()) % 0x1_0000_0000L;

									psks[i++] = new OfferedPsk(
											new PskIdentity(ticket.getTicket(), obfuscatedAge), 
											new byte[ticket.getHashSpec().getHashLength()]);
								}
							}
							extensions.add(new PreSharedKeyExtension(psks));
						}						
					}
				}
			}
			
			ClientHello clientHello = new ClientHello(
					EngineDefaults.LEGACY_VERSION, 
					random, 
					legacySessionId, 
					cipherSuites, 
					new byte[1], 
					extensions);
			
			if (psks != null) {
				int i = 0;
				byte[] truncated = clientHello.prepare();
				int truncatedLength = truncated.length - PreSharedKeyExtension.bindersLength(psks);
				
				try {
					for (SessionTicket ticket: tickets) {
						IHash hash = ticket.getHashSpec().getHash();
						ITranscriptHash th = new TranscriptHash(hash.createMessageDigest());
						IHkdf hkdf = new Hkdf(hash.createMac());
						KeySchedule keyScheduler = new KeySchedule(hkdf, th, ticket.getHashSpec());
						
						keyScheduler.deriveEarlySecret(ticket.getPsk(), false);
						keyScheduler.deriveBinderKey();
						byte[] binder = keyScheduler.computePskBinder(truncated, truncatedLength);
						System.arraycopy(binder, 0, psks[i++].getBinder(), 0, binder.length);
						state.addPskContext(new PskContext(keyScheduler, ticket));
					}
					PreSharedKeyExtension.updateBinders(truncated, truncatedLength, psks);
				}
				catch (Exception e) {
					throw new InternalErrorAlert("Failed to bind PSK", e);
				}
			}
			
			state.setClientHello(clientHello);
			state.produce(new ProducedHandshake(clientHello, RecordType.INITIAL));
			state.changeState(MachineState.CLI_WAIT_1_SH);
		}

		@Override
		void execute() throws Exception {
			int len = namedGroups.length;
			KeyPair[] pairs = new KeyPair[len];
			
			for (int i=0; i<namedGroups.length; ++i) {
				NamedGroup group = namedGroups[i];
				pairs[i] = group.spec().getKeyExchange().generateKeyPair();
			}
			this.pairs = pairs;
		}
	}
}
