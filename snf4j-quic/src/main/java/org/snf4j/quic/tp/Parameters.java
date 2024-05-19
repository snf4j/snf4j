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

class Parameters {
	
	final static IParameter[] PARAMS = new IParameter[17];
	
	private static void add(IParameter parser) {
		PARAMS[parser.getType().typeId()] = parser;
	}
	
	static {
		add(new PByteArray(TransportParameterType.ORIGINAL_DESTINATION_CONNECTION_ID,
				ParameterMode.SERVER,
				(b,v) -> b.originalDestinationId(v), 
				(p) -> {return p.originalDestinationId();}));
		add(new PLong(TransportParameterType.MAX_IDLE_TIMEOUT,
				ParameterMode.BOTH,
				(b,v) -> b.maxIdleTimeout(v),
				(p) -> {return p.maxIdleTimeout();}));
		add(new PByteArray(TransportParameterType.STATELESS_RESET_TOKEN,
				ParameterMode.SERVER,
				(b,v) -> b.statelessResetToken(v), 
				(p) -> {return p.statelessResetToken();}));
		add(new PLong(TransportParameterType.MAX_UDP_PAYLOAD_SIZE,
				ParameterMode.BOTH,
				65527,
				(b,v) -> b.maxUdpPayloadSize(v), 
				(p) -> {return p.maxUdpPayloadSize();}));
		add(new PLong(TransportParameterType.INITIAL_MAX_DATA,
				ParameterMode.BOTH,
				(b,v) -> b.iniMaxData(v), 
				(p) -> {return p.iniMaxData();}));
		add(new PLong(TransportParameterType.INITIAL_MAX_STREAM_DATA_BIDI_LOCAL,
				ParameterMode.BOTH,
				(b,v) -> b.iniMaxStreamDataBidiLocal(v), 
				(p) -> {return p.iniMaxStreamDataBidiLocal();}));
		add(new PLong(TransportParameterType.INITIAL_MAX_STREAM_DATA_BIDI_REMOTE,
				ParameterMode.BOTH,
				(b,v) -> b.iniMaxStreamDataBidiRemote(v), 
				(p) -> {return p.iniMaxStreamDataBidiRemote();}));
		add(new PLong(TransportParameterType.INITIAL_MAX_STREAM_DATA_UNI,
				ParameterMode.BOTH,
				(b,v) -> b.iniMaxStreamDataUni(v), 
				(p) -> {return p.iniMaxStreamDataUni();}));
		add(new PLong(TransportParameterType.INITIAL_MAX_STREAMS_BIDI,
				ParameterMode.BOTH,
				(b,v) -> b.iniMaxStreamsBidi(v), 
				(p) -> {return p.iniMaxStreamsBidi();}));
		add(new PLong(TransportParameterType.INITIAL_MAX_STREAMS_UNI,
				ParameterMode.BOTH,
				(b,v) -> b.iniMaxStreamsUni(v), 
				(p) -> {return p.iniMaxStreamsUni();}));
		add(new PLong(TransportParameterType.ACK_DELAY_EXPONENT,
				ParameterMode.BOTH,
				3,
				(b,v) -> b.ackDelayExponent(v), 
				(p) -> {return p.ackDelayExponent();}));
		add(new PLong(TransportParameterType.MAX_ACK_DELAY,
				ParameterMode.BOTH,
				25,
				(b,v) -> b.maxAckDelay(v), 
				(p) -> {return p.maxAckDelay();}));
		add(new PBoolean(TransportParameterType.DISABLE_ACTIVE_MIGRATION,
				ParameterMode.BOTH,
				(b,v) -> b.disableActiveMigration(v), 
				(p) -> {return p.disableActiveMigration();}));
		add(new PPreferredAddress(TransportParameterType.PREFERRED_ADDRESS,
				ParameterMode.SERVER));
		add(new PLong(TransportParameterType.ACTIVE_CONNECTION_ID_LIMIT,
				ParameterMode.BOTH,
				2,
				(b,v) -> b.activeConnectionIdLimit(v), 
				(p) -> {return p.activeConnectionIdLimit();}));
		add(new PByteArray(TransportParameterType.INITIAL_SOURCE_CONNECTION_ID,
				ParameterMode.BOTH,
				(b,v) -> b.iniSourceId(v), 
				(p) -> {return p.iniSourceId();}));
		add(new PByteArray(TransportParameterType.RETRY_SOURCE_CONNECTION_ID,
				ParameterMode.SERVER,
				(b,v) -> b.retrySourceId(v), 
				(p) -> {return p.retrySourceId();}));
	}
	
	private Parameters() {}
}
