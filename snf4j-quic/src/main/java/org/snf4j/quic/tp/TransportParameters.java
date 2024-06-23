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
package org.snf4j.quic.tp;

/**
 * An object representing the QUIC transport parameters.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class TransportParameters {

	/** 
	 * The default value of the maximum UDP payload size 
	 * */
	public final static int DEFAULT_MAX_UDP_PAYLOAD_SIZE = 65527;
	
	/** 
	 * The default value of the acknowledgment delay exponent 
	 */
	public final static int DEFAULT_ACK_DELAY_EXPONENT = 3;
	
	/** 
	 * The default value of the maximum acknowledgment delay 
	 */
	public final static int DEFAULT_MAX_ACK_DELAY = 25;
	
	/**
	 * The default value of the the maximum number of connection IDs from the peer
	 * that an endpoint is willing to store
	 */
	public final static int DEFAULT_ACTIVE_CONNECTION_ID_LIMIT = 2;

	private final byte[] originalDestinationId;

	private final long maxIdleTimeout;

	private final byte[] statelessResetToken;

	private final long maxUdpPayloadSize;

	private final long iniMaxData;

	private final long iniMaxStreamDataBidiLocal;

	private final long iniMaxStreamDataBidiRemote;

	private final long iniMaxStreamDataUni;

	private final long iniMaxStreamsBidi;

	private final long iniMaxStreamsUni;

	private final long ackDelayExponent;

	private final long maxAckDelay;

	private final boolean disableActiveMigration;

	private final PreferredAddress preferredAddress;

	private final long activeConnectionIdLimit;

	private final byte[] iniSourceId;

	private final byte[] retrySourceId;

	/**
	 * Constructs a representation of the QUIC transport parameters.
	 * 
	 * @param originalDestinationId      the original_destination_connection_id
	 *                                   parameter
	 * @param maxIdleTimeout             the max_idle_timeout parameter
	 * @param statelessResetToken        the stateless_reset_token parameter
	 * @param maxUdpPayloadSize          the max_udp_payload_size parameter
	 * @param iniMaxData                 the initial_max_data parameter
	 * @param iniMaxStreamDataBidiLocal  the initial_max_stream_data_bidi_local
	 *                                   parameter
	 * @param iniMaxStreamDataBidiRemote the initial_max_stream_data_bidi_remote
	 *                                   parameter
	 * @param iniMaxStreamDataUni        the initial_max_stream_data_uni parameter
	 * @param iniMaxStreamsBidi          the initial_max_streams_bidi parameter
	 * @param iniMaxStreamsUni           the initial_max_streams_uni parameter
	 * @param ackDelayExponent           the ack_delay_exponent parameter
	 * @param maxAckDelay                the max_ack_delay parameter
	 * @param disableActiveMigration     the disable_active_migration parameter
	 * @param preferredAddress           the preferred_address parameter
	 * @param activeConnectionIdLimit    the active_connection_id_limit parameter
	 * @param iniSourceId                the initial_source_connection_id parameter
	 * @param retrySourceId              the retry_source_connection_id parameter
	 */
	public TransportParameters(byte[] originalDestinationId, long maxIdleTimeout, byte[] statelessResetToken,
			long maxUdpPayloadSize, long iniMaxData, long iniMaxStreamDataBidiLocal, long iniMaxStreamDataBidiRemote,
			long iniMaxStreamDataUni, long iniMaxStreamsBidi, long iniMaxStreamsUni, long ackDelayExponent,
			long maxAckDelay, boolean disableActiveMigration, PreferredAddress preferredAddress,
			long activeConnectionIdLimit, byte[] iniSourceId, byte[] retrySourceId) {
		this.originalDestinationId = originalDestinationId;
		this.maxIdleTimeout = maxIdleTimeout;
		this.statelessResetToken = statelessResetToken;
		this.maxUdpPayloadSize = maxUdpPayloadSize;
		this.iniMaxData = iniMaxData;
		this.iniMaxStreamDataBidiLocal = iniMaxStreamDataBidiLocal;
		this.iniMaxStreamDataBidiRemote = iniMaxStreamDataBidiRemote;
		this.iniMaxStreamDataUni = iniMaxStreamDataUni;
		this.iniMaxStreamsBidi = iniMaxStreamsBidi;
		this.iniMaxStreamsUni = iniMaxStreamsUni;
		this.ackDelayExponent = ackDelayExponent;
		this.maxAckDelay = maxAckDelay;
		this.disableActiveMigration = disableActiveMigration;
		this.preferredAddress = preferredAddress;
		this.activeConnectionIdLimit = activeConnectionIdLimit;
		this.iniSourceId = iniSourceId;
		this.retrySourceId = retrySourceId;
	}

	/**
	 * Returns the original_destination_connection_id parameter
	 * 
	 * @return the original_destination_connection_id parameter
	 */
	public byte[] originalDestinationId() {
		return originalDestinationId;
	}

	/**
	 * Returns the max_idle_timeout parameter
	 * 
	 * @return the max_idle_timeout parameter
	 */
	public long maxIdleTimeout() {
		return maxIdleTimeout;
	}

	/**
	 * Returns the stateless_reset_token parameter
	 * 
	 * @return the stateless_reset_token parameter
	 */
	public byte[] statelessResetToken() {
		return statelessResetToken;
	}

	/**
	 * Returns the max_udp_payload_size parameter
	 * 
	 * @return the max_udp_payload_size parameter
	 */
	public long maxUdpPayloadSize() {
		return maxUdpPayloadSize;
	}

	/**
	 * Returns the initial_max_data parameter
	 * 
	 * @return the initial_max_data parameter
	 */
	public long iniMaxData() {
		return iniMaxData;
	}

	/**
	 * Returns the initial_max_stream_data_bidi_local parameter
	 * 
	 * @return the initial_max_stream_data_bidi_local parameter
	 */
	public long iniMaxStreamDataBidiLocal() {
		return iniMaxStreamDataBidiLocal;
	}

	/**
	 * Return the initial_max_stream_data_bidi_remote parameter
	 * 
	 * @return the initial_max_stream_data_bidi_remote parameter
	 */
	public long iniMaxStreamDataBidiRemote() {
		return iniMaxStreamDataBidiRemote;
	}

	/**
	 * Returns the initial_max_stream_data_uni parameter
	 * 
	 * @return the initial_max_stream_data_uni parameter
	 */
	public long iniMaxStreamDataUni() {
		return iniMaxStreamDataUni;
	}

	/**
	 * Returns the initial_max_streams_bidi parameter
	 * 
	 * @return the initial_max_streams_bidi parameter
	 */
	public long iniMaxStreamsBidi() {
		return iniMaxStreamsBidi;
	}

	/**
	 * Returns the initial_max_streams_uni parameter
	 * 
	 * @return the initial_max_streams_uni parameter
	 */
	public long iniMaxStreamsUni() {
		return iniMaxStreamsUni;
	}

	/**
	 * Returns the ack_delay_exponent parameter
	 * 
	 * @return the ack_delay_exponent parameter
	 */
	public long ackDelayExponent() {
		return ackDelayExponent;
	}

	/**
	 * Returns the max_ack_delay parameter
	 * 
	 * @return the max_ack_delay parameter
	 */
	public long maxAckDelay() {
		return maxAckDelay;
	}

	/**
	 * Returns the disable_active_migration parameter
	 * 
	 * @return the disable_active_migration parameter
	 */
	public boolean disableActiveMigration() {
		return disableActiveMigration;
	}

	/**
	 * Returns the preferred_address parameter
	 * 
	 * @return the preferred_address parameter
	 */
	public PreferredAddress preferredAddress() {
		return preferredAddress;
	}

	/**
	 * Returns the active_connection_id_limit parameter
	 * 
	 * @return the active_connection_id_limit parameter
	 */
	public long activeConnectionIdLimit() {
		return activeConnectionIdLimit;
	}

	/**
	 * Returns the initial_source_connection_id parameter
	 * 
	 * @return the initial_source_connection_id parameter
	 */
	public byte[] iniSourceId() {
		return iniSourceId;
	}

	/**
	 * Returns the retry_source_connection_id parameter
	 * 
	 * @return the retry_source_connection_id parameter
	 */
	public byte[] retrySourceId() {
		return retrySourceId;
	}

}
