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
 * An {@code enum} defining QUIC transport parameters types. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum TransportParameterType {

	ORIGINAL_DESTINATION_CONNECTION_ID(0x00,"original_destination_connection_id"),

	MAX_IDLE_TIMEOUT(0x01,"max_idle_timeout"),

	STATELESS_RESET_TOKEN(0x02,"stateless_reset_token"),

	MAX_UDP_PAYLOAD_SIZE(0x03,"max_udp_payload_size"),

	INITIAL_MAX_DATA(0x04,"initial_max_data"),

	INITIAL_MAX_STREAM_DATA_BIDI_LOCAL(0x05,"initial_max_stream_data_bidi_local"),

	INITIAL_MAX_STREAM_DATA_BIDI_REMOTE(0x06,"initial_max_stream_data_bidi_remote"),

	INITIAL_MAX_STREAM_DATA_UNI(0x07,"initial_max_stream_data_uni"),

	INITIAL_MAX_STREAMS_BIDI(0x08,"initial_max_streams_bidi"),

	INITIAL_MAX_STREAMS_UNI(0x09,"initial_max_streams_uni"),

	ACK_DELAY_EXPONENT(0x0a,"ack_delay_exponent"),

	MAX_ACK_DELAY(0x0b,"max_ack_delay"),

	DISABLE_ACTIVE_MIGRATION(0x0c,"disable_active_migration"),

	PREFERRED_ADDRESS(0x0d,"preferred_address"),

	ACTIVE_CONNECTION_ID_LIMIT(0x0e,"active_connection_id_limit"),

	INITIAL_SOURCE_CONNECTION_ID(0x0f,"initial_source_connection_id"),

	RETRY_SOURCE_CONNECTION_ID(0x10,"retry_source_connection_id");
	
	private final int id;
	
	private final String name;
	
	TransportParameterType(int id, String name) {
		this.id = id;
		this.name = name;
	}

	/**
	 * Returns the identifier of the type.
	 * 
	 * @return the identifier of the type
	 */
	public int typeId() {
		return id;
	}

	/**
	 * Returns the name of the type.
	 * 
	 * @return the name of the type
	 */
	public String typeName() {
		return name;
	}
	
}
