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
 * The QUIC transport parameters builder.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class TransportParametersBuilder {

	private byte[] originalDestinationId;
	
	private long maxIdleTimeout;
	
	private byte[] statelessResetToken;
	
	private long maxUdpPayloadSize = 65527;

	private long iniMaxData;
	
	private long iniMaxStreamDataBidiLocal;
	
	private long iniMaxStreamDataBidiRemote;
	
	private long iniMaxStreamDataUni;
	
	private long iniMaxStreamsBidi;
	
	private long iniMaxStreamsUni;
	
	private long ackDelayExponent = 3;
	
	private long maxAckDelay = 25;
	
	private boolean disableActiveMigration;
	
	private PreferredAddress preferredAddress;
	
	private long activeConnectionIdLimit = 2;
	
	private byte[] iniSourceId;
	
	private byte[] retrySourceId;
	
	/**
	 * Constructs a transport parameters builder configured with default values.
	 */
	public TransportParametersBuilder() {
	}
	
	/**
	 * Sets the original_destination_connection_id parameter.
	 * <p>
	 * Default value: null
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder originalDestinationId(byte[] value) {
		originalDestinationId = value;
		return this;
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
	 * Sets the max_idle_timeout parameter.
	 * <p>
	 * Default value: 0
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder maxIdleTimeout(long value) {
		maxIdleTimeout = value;
		return this;
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
	 * Sets the stateless_reset_token parameter.
	 * <p>
	 * Default value: null
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder statelessResetToken(byte[] value) {
		this.statelessResetToken = value;
		return this;
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
	 * Sets the max_udp_payload_size parameter.
	 * <p>
	 * Default value: 65527
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder maxUdpPayloadSize(long value) {
		this.maxUdpPayloadSize = value;
		return this;
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
	 * Sets the initial_max_data parameter.
	 * <p>
	 * Default value: 0
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder iniMaxData(long value) {
		this.iniMaxData = value;
		return this;
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
	 * Sets the initial_max_stream_data_bidi_local parameter.
	 * <p>
	 * Default value: 0
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder iniMaxStreamDataBidiLocal(long value) {
		this.iniMaxStreamDataBidiLocal = value;
		return this;
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
	 * Sets the initial_max_stream_data_bidi_remote parameter.
	 * <p>
	 * Default value: 0
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder iniMaxStreamDataBidiRemote(long value) {
		this.iniMaxStreamDataBidiRemote = value;
		return this;
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
	 * Sets the initial_max_stream_data_uni parameter.
	 * <p>
	 * Default value: 0
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder iniMaxStreamDataUni(long value) {
		this.iniMaxStreamDataUni = value;
		return this;
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
	 * Sets the initial_max_streams_bidi parameter.
	 * <p>
	 * Default value: 0
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder iniMaxStreamsBidi(long value) {
		this.iniMaxStreamsBidi = value;
		return this;
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
	 * Sets the initial_max_streams_uni parameter.
	 * <p>
	 * Default value: 0
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder iniMaxStreamsUni(long value) {
		this.iniMaxStreamsUni = value;
		return this;
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
	 * Sets the ack_delay_exponent parameter.
	 * <p>
	 * Default value: 3
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder ackDelayExponent(long value) {
		this.ackDelayExponent = value;
		return this;
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
	 * Sets the max_ack_delay parameter.
	 * <p>
	 * Default value: 25
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder maxAckDelay(long value) {
		this.maxAckDelay = value;
		return this;
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
	 * Sets the disable_active_migration parameter.
	 * <p>
	 * Default value: false
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder disableActiveMigration(boolean value) {
		this.disableActiveMigration = value;
		return this;
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
	 * Sets the preferred_address parameter.
	 * <p>
	 * Default value: null
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder preferredAddress(PreferredAddress value) {
		this.preferredAddress = value;
		return this;
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
	 * Sets the active_connection_id_limit parameter.
	 * <p>
	 * Default value: 2
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder activeConnectionIdLimit(long value) {
		this.activeConnectionIdLimit = value;
		return this;
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
	 * Sets the initial_source_connection_id parameter.
	 * <p>
	 * Default value: null
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder iniSourceId(byte[] value) {
		this.iniSourceId = value;
		return this;
	}

	/**
	 * Returns the retry_source_connection_id parameter
	 * 
	 * @return the retry_source_connection_id parameter
	 */
	public byte[] retrySourceId() {
		return retrySourceId;
	}

	/**
	 * Sets the retry_source_connection_id parameter.
	 * <p>
	 * Default value: null
	 * 
	 * @param value the parameter value
	 * @return this builder
	 */
	public TransportParametersBuilder retrySourceId(byte[] value) {
		this.retrySourceId = value;
		return this;
	}

	/**
	 * Builds a representation of the transport parameters based on the current
	 * configuration of this builder.
	 * 
	 * @return the representation of the transport parameters
	 */
	public TransportParameters build() {
		return new TransportParameters(
				originalDestinationId,
				maxIdleTimeout,
				statelessResetToken,
				maxUdpPayloadSize,
				iniMaxData,
				iniMaxStreamDataBidiLocal,
				iniMaxStreamDataBidiRemote,
				iniMaxStreamDataUni,
				iniMaxStreamsBidi,
				iniMaxStreamsUni,
				ackDelayExponent,
				maxAckDelay,
				disableActiveMigration,
				preferredAddress,
				activeConnectionIdLimit,
				iniSourceId,
				retrySourceId
				);		
	}
}
