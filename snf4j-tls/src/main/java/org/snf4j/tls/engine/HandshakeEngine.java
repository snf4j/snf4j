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
import java.util.List;
import java.util.Random;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.extension.ExtensionValidator;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IExtensionValidator;
import org.snf4j.tls.extension.IKeyShareExtension;
import org.snf4j.tls.extension.ISupportedVersionsExtension;
import org.snf4j.tls.extension.KeyShareEntry;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.NamedGroup;
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

public class HandshakeEngine implements IHandshakeEngine {
	
	private final static Random RANDOM = new Random();
	
	private final static IHandshakeConsumer[] CONSUMERS;
	
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
			List<IExtension> extensions = handshake.getExtensioins();
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
	public boolean hasProducingTask() {
		return state.hasProducingTasks();
	}

	@Override
	public boolean hasPendingTasks() throws Alert {
		return state.hasPendingTasks();
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
		if (state.isClientMode()) {
			state.changeState(MachineState.CLI_START);
		}
		else {
			state.changeState(MachineState.SRV_START);
			return;
		}
		
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
		
		String serverName = params.getServerName();
		NamedGroup[] groups = params.getNamedGroups();
		
		if (serverName != null) {
			extensions.add(new ServerNameExtension(serverName));
		}
		extensions.add(new SupportedVersionsExtension(ISupportedVersionsExtension.Mode.CLIENT_HELLO, 0x0304));
		extensions.add(new SupportedGroupsExtension(groups));
		extensions.add(new SignatureAlgorithmsExtension(params.getSignatureSchemes()));
		
		try {
			int offered = Math.min(groups.length, params.getNumberOfOfferedSharedKeys());
			KeyShareEntry[] entries = new KeyShareEntry[offered];
			
			for (int i=0; i<offered; ++i) {
				NamedGroup group = groups[i];
				KeyPair pair = group.spec().getKeyExchange().generateKeyPair();
				
				entries[i] = new KeyShareEntry(group, pair.getPublic());
				state.storePrivateKey(group, pair.getPrivate());
			}
			extensions.add(new KeyShareExtension(IKeyShareExtension.Mode.CLIENT_HELLO, entries));
		} catch (Exception e) {
			throw new InternalErrorAlert("Failed to generate exchange key", e);
		}
		
		ClientHello clientHello = new ClientHello(
				EngineDefaults.LEGACY_VERSION, 
				random, 
				legacySessionId, 
				state.getParameters().getCipherSuites(), 
				new byte[1], 
				extensions);
		
		state.setClientHello(clientHello);
		state.produce(new ProducedHandshake(clientHello, RecordType.INITIAL));
		state.changeState(MachineState.CLI_WAIT_SH);
	}
}
