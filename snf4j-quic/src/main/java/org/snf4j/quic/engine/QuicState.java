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

import java.security.SecureRandom;
import java.util.Arrays;

import org.snf4j.quic.QuicAlert;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.cid.ConnectionIdManager;
import org.snf4j.quic.cid.IPool;
import org.snf4j.quic.packet.PacketUtil;
import org.snf4j.quic.tp.TransportParameterType;
import org.snf4j.quic.tp.TransportParameters;
import org.snf4j.quic.tp.TransportParametersBuilder;
import org.snf4j.tls.alert.Alert;

/**
 * The state of the QUIC engine. This object keeps all information related to
 * the state of the QUIC engine.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class QuicState {

	private final EncryptionContext[] contexts = new EncryptionContext[EncryptionLevel.values().length];
	
	private final PacketNumberSpace[] spaces = new PacketNumberSpace[contexts.length];

	private final IEncryptionContextListener contextsListener = new IEncryptionContextListener() {

		@Override
		public void onNewEncryptor(EncryptionLevel level, KeyPhase phase) {
			encryptorLevel = level;
		}

		@Override
		public void onNewDecryptor(EncryptionLevel level, KeyPhase phase) {
		}
	};
	
	private final ConnectionIdManager manager;
	
	private final boolean clientMode;

	private HandshakeState handshakeState = HandshakeState.INIT;
	
	private final int maxActiveConnectionIdLimit = 10;
	
	private int maxUdpPayloadSize = PacketUtil.MIN_MAX_UDP_PAYLOAD_SIZE;
	
	private EncryptionLevel encryptorLevel;
	
	private Version version = Version.V1;
	
	/**
	 * Constructs a QUIC state for the given role.
	 * 
	 * @param clientMode determines the role (client/server) for the state
	 */
	public QuicState(boolean clientMode) {
		this.clientMode = clientMode;
		manager = new ConnectionIdManager(clientMode, 8, 2, new SecureRandom());
		
		contexts[EncryptionLevel.EARLY_DATA.ordinal()] = new EncryptionContext(EncryptionLevel.EARLY_DATA, 10,contextsListener);
		contexts[EncryptionLevel.INITIAL.ordinal()] = new EncryptionContext(EncryptionLevel.INITIAL, 10, contextsListener);
		contexts[EncryptionLevel.HANDSHAKE.ordinal()] = new EncryptionContext(EncryptionLevel.HANDSHAKE, 10, contextsListener);
		contexts[EncryptionLevel.APPLICATION_DATA.ordinal()] = new EncryptionContext(EncryptionLevel.APPLICATION_DATA, 10, contextsListener);
		
		spaces[EncryptionLevel.EARLY_DATA.ordinal()] = new PacketNumberSpace(PacketNumberSpace.Type.APPLICATION_DATA);
		spaces[EncryptionLevel.INITIAL.ordinal()] = new PacketNumberSpace(PacketNumberSpace.Type.INITIAL);
		spaces[EncryptionLevel.HANDSHAKE.ordinal()] = new PacketNumberSpace(PacketNumberSpace.Type.HANDSHAKE);
		spaces[EncryptionLevel.APPLICATION_DATA.ordinal()] = spaces[EncryptionLevel.EARLY_DATA.ordinal()];
	}
	
	/**
	 * Tells if this state object is for a client or server.
	 * 
	 * @return {@code true} if this state object is for a client
	 */
	public boolean isClientMode() {
		return clientMode;
	}
	
	/**
	 * Returns the associated encryption context for the given encryption level.
	 * 
	 * @param level the encryption level
	 * @return the encryption context for the given encryption level
	 */
	public EncryptionContext getContext(EncryptionLevel level) {
		return contexts[level.ordinal()];
	}
	
	/**
	 * Returns the associated packet number space for the given encryption level.
	 * 
	 * @param level the encryption level
	 * @return the packet number space for the given encryption level
	 */
	public PacketNumberSpace getSpace(EncryptionLevel level) {
		return spaces[level.ordinal()];
	}

	/**
	 * Returns the associated connection id manager.
	 * 
	 * @return the associated connection id manager
	 */
	public ConnectionIdManager getConnectionIdManager() {
		return manager;
	}

	/**
	 * Returns the maximum UDP payload size that can be safely send to a peer.
	 * 
	 * @return the maximum UDP payload size
	 */
	public int getMaxUdpPayloadSize() {
		return maxUdpPayloadSize;
	}

	/**
	 * Sets the maximum UDP payload size that can be safely send to a peer.
	 * 
	 * @param size the maximum UDP payload size
	 */
	public void setMaxUdpPayloadSize(int size) {
		maxUdpPayloadSize = size;
	}
	
	/**
	 * Produces the transport parameters representing the current configuration of
	 * this state object.
	 * 
	 * @return the transport parameters
	 */
	public TransportParameters produceTransportParameters() {
		TransportParametersBuilder builder = new TransportParametersBuilder();
		
		builder.iniSourceId(manager.getSourcePool().get(IPool.INITIAL_SEQUENCE_NUMBER).getId());	
		builder.activeConnectionIdLimit(manager.getDestinationPool().getLimit());
		builder.maxUdpPayloadSize(maxUdpPayloadSize);
		if (!clientMode) {
			builder.originalDestinationId(manager.getOriginalId());
			if (manager.hasRetryId()) {
				builder.retrySourceId(manager.getRetryId());
			}
			builder.statelessResetToken(manager.getSourcePool().get(IPool.INITIAL_SEQUENCE_NUMBER).getResetToken());
		}
		return builder.build();
	}
	
	private static QuicAlert missing(TransportParameterType p)  {
		return new QuicAlert(TransportError.TRANSPORT_PARAMETER_ERROR, "Missing " + p.typeName());
	}

	private static QuicAlert notMatching(TransportParameterType p)  {
		return new QuicAlert(TransportError.TRANSPORT_PARAMETER_ERROR, "Not matching " + p.typeName());
	}

	private static QuicAlert unexpected(TransportParameterType p)  {
		return new QuicAlert(TransportError.TRANSPORT_PARAMETER_ERROR, "Unexpected " + p.typeName());
	}
	private static QuicAlert invalid(TransportParameterType p)  {
		return new QuicAlert(TransportError.TRANSPORT_PARAMETER_ERROR, "Invalid " + p.typeName());
	}

	private static QuicAlert invalidLength(TransportParameterType p)  {
		return new QuicAlert(TransportError.TRANSPORT_PARAMETER_ERROR, "Invalid length of " + p.typeName());
	}
	
	/**
	 * Consumes the given transport parameters received from a peer.
	 * 
	 * @param params the transport parameters
	 * @throws Alert if an error occurred during validation of the transport
	 *               parameters
	 */
	public void consumeTransportParameters(TransportParameters params) throws Alert {
		byte[] bytes;
		long value;

		if (clientMode) {
			bytes = params.originalDestinationId();
			if (bytes == null) {
				throw missing(TransportParameterType.ORIGINAL_DESTINATION_CONNECTION_ID);
			}
			if (!Arrays.equals(manager.getOriginalId(), bytes)) {
				throw notMatching(TransportParameterType.ORIGINAL_DESTINATION_CONNECTION_ID);
			}
			
			bytes = params.retrySourceId();
			if (bytes == null) {
				if (manager.hasRetryId()) {
					throw missing(TransportParameterType.RETRY_SOURCE_CONNECTION_ID);
				}
			}
			else {
				if (manager.hasRetryId()) {
					if (!Arrays.equals(manager.getRetryId(), params.retrySourceId())) {
						throw notMatching(TransportParameterType.RETRY_SOURCE_CONNECTION_ID);
					}
				}
				else {
					throw unexpected(TransportParameterType.RETRY_SOURCE_CONNECTION_ID);
				}
			}
			
			bytes = params.statelessResetToken();
			if (bytes != null) {
				if (bytes.length != 16) {
					throw invalidLength(TransportParameterType.STATELESS_RESET_TOKEN);
				}
				manager.getDestinationPool().updateResetToken(bytes);
			}
		}
		
		bytes = params.iniSourceId();
		if (bytes == null) {
			throw missing(TransportParameterType.INITIAL_SOURCE_CONNECTION_ID);
		}
		if (!Arrays.equals(manager.getDestinationPool().get(IPool.INITIAL_SEQUENCE_NUMBER).getId(), bytes)) {
			throw notMatching(TransportParameterType.INITIAL_SOURCE_CONNECTION_ID);			
		}
		
		manager.getSourcePool().setLimit((int) Math.min(maxActiveConnectionIdLimit, params.activeConnectionIdLimit()));
		
		value = params.maxUdpPayloadSize();
		if (value < PacketUtil.MIN_MAX_UDP_PAYLOAD_SIZE) {
			throw invalid(TransportParameterType.MAX_UDP_PAYLOAD_SIZE);
		}
		maxUdpPayloadSize = (int) Math.min(maxUdpPayloadSize, value);
	}

	/**
	 * Returns the encryption level of the current encryptor.
	 * 
	 * @return the encryption level of the current encryptor, or {@code null} if no
	 *         encryptor is set yet
	 */
	public EncryptionLevel getEncryptorLevel() {
		return encryptorLevel;
	}

	/**
	 * Returns currently used version of the QUIC protocol.
	 * 
	 * @return the current version
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Set the currently used version of the QUIC protocol.
	 * 
	 * @param version the current version
	 */
	public void setVersion(Version version) {
		this.version = version;
	}

	/**
	 * Returns the current handshake state.
	 * 
	 * @return the current handshake state
	 */
	public HandshakeState getHandshakeState() {
		return handshakeState;
	}

	/**
	 * Sets the current handshake state.
	 * 
	 * @param handshakeState the current handshake state
	 */
	public void setHandshakeState(HandshakeState handshakeState) {
		this.handshakeState = handshakeState;
	}
	
}
