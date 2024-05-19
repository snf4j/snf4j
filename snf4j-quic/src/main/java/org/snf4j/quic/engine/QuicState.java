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
import org.snf4j.quic.cid.ConnectionIdManager;
import org.snf4j.quic.cid.IPool;
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

	private final static int MIN_MAX_UDP_PAYLOAD_SIZE = 1200;
	
	private final EncryptionContext[] contexts = new EncryptionContext[EncryptionLevel.values().length];
	
	private final PacketNumberSpace[] spaces = new PacketNumberSpace[contexts.length];

	private final ConnectionIdManager manager;
	
	private final boolean clientMode;

	private final int maxActiveConnectionIdLimit = 10;
	
	private int maxUdpPayloadSize = MIN_MAX_UDP_PAYLOAD_SIZE;
	
	public QuicState(boolean clientMode) {
		this.clientMode = clientMode;
		manager = new ConnectionIdManager(clientMode, 8, 2, new SecureRandom());
		for (int i=0; i<contexts.length; ++i) {
			contexts[i] = new EncryptionContext(10);
		}
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

	public ConnectionIdManager getConnectionIdManager() {
		return manager;
	}

	
	public int getMaxUdpPayloadSize() {
		return maxUdpPayloadSize;
	}

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
		if (value < MIN_MAX_UDP_PAYLOAD_SIZE) {
			throw invalid(TransportParameterType.MAX_UDP_PAYLOAD_SIZE);
		}
		maxUdpPayloadSize = (int) Math.min(maxUdpPayloadSize, value);
	}
	
}
