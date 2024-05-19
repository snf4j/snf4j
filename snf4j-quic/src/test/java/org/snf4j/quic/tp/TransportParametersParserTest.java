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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.tls.alert.Alert;

public class TransportParametersParserTest extends CommonTest {

	final ITransportParametersParser parser = TransportParametersParser.INSTANCE;
	
	TestBuilder builder;	
	
	@Override
	public void before() {
		builder = new TestBuilder();
	}
	
	@Test
	public void testParse() throws Exception {
		parser.parse(true, buffer(""), buffer.remaining(), builder);
		assertEquals("", builder.trace());
		parser.parse(true, buffer(
				"000102 01024040 020103 030104 040105 050106 060107 070108 080109 09010a 0a010b 0b010c 0c00 0e010f 0f02a1a2 1003b1b2b3"
				), buffer.remaining(), builder);
		assertEquals("0=02|1=64|2=03|3=4|4=5|5=6|6=7|7=8|8=9|9=10|10=11|11=12|12=t|14=15|15=a1a2|16=b1b2b3|", builder.trace());
		assertEquals(0, buffer.remaining());
		parser.parse(false, buffer(
				"01024040 030104 040105 050106 060107 070108 080109 09010a 0a010b 0b010c 0c00 0e010f 0f02a1a2"
				), buffer.remaining(), builder);
		assertEquals("1=64|3=4|4=5|5=6|6=7|7=8|8=9|9=10|10=11|11=12|12=t|14=15|15=a1a2|", builder.trace());
		assertEquals(0, buffer.remaining());

		parser.parse(true, buffer(
				"000102 1102fffe 01024040 020103"
				), buffer.remaining(), builder);
		assertEquals("0=02|1=64|2=03|", builder.trace());
		assertEquals(0, buffer.remaining());
	
	}
	
	void assertFailure(boolean client, String hex, String message) throws Exception {
		try {
			parser.parse(client, buffer(hex), buffer.remaining(), builder);
			fail();
		}
		catch (Alert e) {
			assertEquals(message, e.getMessage());
		}
	}
	
	@Test
	public void testParseFailures() throws Exception {
		String msgFormat = "Extension 'quic_transport_parameters' parsing failure: Invalid format";
		String msgLen = "Extension 'quic_transport_parameters' parsing failure: Inconsistent length";
		
		assertFailure(true, "00", msgLen);
		assertFailure(true, "0001", msgLen);
		assertFailure(true, "000200", msgLen);
		assertFailure(true, "000240", msgLen);
		assertFailure(true, "01020000", msgFormat);
		assertFailure(true, "0c0100", msgFormat);
		assertFailure(false, "000100", "Unexpected original_destination_connection_id");
		assertFailure(false, "020100", "Unexpected stateless_reset_token");
		assertFailure(false, "100100", "Unexpected retry_source_connection_id");
		assertFailure(true, "0d18 00000000 00000 000102030405060708090a0b0c0d0e0f 0000", msgFormat);
		assertFailure(true, "0d2a 00000000 0000 000102030405060708090a0b0c0d0e0f 0000 02 0102 000102030405060708090a0b0c0d0e0f", msgFormat);
		assertFailure(true, "0d2b 00000000 0000 000102030405060708090a0b0c0d0e0f 0000 01 0102 000102030405060708090a0b0c0d0e0f", msgFormat);
	}
	
	class TestBuilder extends TransportParametersBuilder {
	
		StringBuilder trace = new StringBuilder();
		
		void trace(int id, String v) {
			trace.append(id).append('=').append(v).append('|');
		}

		void trace(int id, long v) {
			trace.append(id).append('=').append(v).append('|');
		}

		void trace(int id, byte[] v) {
			if (v == null) {
				trace.append(id).append('|');
			}
			else {
				trace.append(id).append('=').append(v == null ? "-" : hex(v)).append('|');
			}
		}
		
		String trace() {
			String s = trace.toString();
			trace.setLength(0);
			return s;
		}
		
		public TransportParametersBuilder originalDestinationId(byte[] id) {
			trace(0, id);
			return super.originalDestinationId(id);
		}
				
		public TransportParametersBuilder maxIdleTimeout(long value) {
			trace(1, value);
			return super.maxIdleTimeout(value);
		}
		
		public TransportParametersBuilder statelessResetToken(byte[] statelessResetToken) {
			trace(2, statelessResetToken);
			return super.statelessResetToken(statelessResetToken);
		}

		public TransportParametersBuilder maxUdpPayloadSize(long maxUdpPayloadSize) {
			trace(3, maxUdpPayloadSize);
			return super.maxUdpPayloadSize(maxUdpPayloadSize);
		}

		public TransportParametersBuilder iniMaxData(long iniMaxData) {
			trace(4, iniMaxData);
			return super.iniMaxData(iniMaxData);
		}

		public TransportParametersBuilder iniMaxStreamDataBidiLocal(long iniMaxStreamDataBidiLocal) {
			trace(5, iniMaxStreamDataBidiLocal);
			return super.iniMaxStreamDataBidiLocal(iniMaxStreamDataBidiLocal);
		}

		public TransportParametersBuilder iniMaxStreamDataBidiRemote(long iniMaxStreamDataBidiRemote) {
			trace(6, iniMaxStreamDataBidiRemote);
			return super.iniMaxStreamDataBidiRemote(iniMaxStreamDataBidiRemote);
		}

		public TransportParametersBuilder iniMaxStreamDataUni(long iniMaxStreamDataUni) {
			trace(7, iniMaxStreamDataUni);
			return super.iniMaxStreamDataUni(iniMaxStreamDataUni);
		}

		public TransportParametersBuilder iniMaxStreamsBidi(long iniMaxStreamsBidi) {
			trace(8, iniMaxStreamsBidi);
			return super.iniMaxStreamsBidi(iniMaxStreamsBidi);
		}

		public TransportParametersBuilder iniMaxStreamsUni(long iniMaxStreamsUni) {
			trace(9, iniMaxStreamsUni);
			return super.iniMaxStreamsUni(iniMaxStreamsUni);
		}

		public TransportParametersBuilder ackDelayExponent(long ackDelayExponent) {
			trace(10, ackDelayExponent);
			return super.ackDelayExponent(ackDelayExponent);
		}

		public TransportParametersBuilder maxAckDelay(long maxAckDelay) {
			trace(11, maxAckDelay);
			return super.maxAckDelay(maxAckDelay);
		}

		public TransportParametersBuilder disableActiveMigration(boolean disableActiveMigration) {
			trace(12, disableActiveMigration ? "t" : "f");
			return super.disableActiveMigration(disableActiveMigration);
		}

		public TransportParametersBuilder preferredAddress(PreferredAddress preferredAddress) {
			trace(13, "a");
			return super.preferredAddress(preferredAddress);
		}

		public TransportParametersBuilder activeConnectionIdLimit(long activeConnectionIdLimit) {
			trace(14, activeConnectionIdLimit);
			return super.activeConnectionIdLimit(activeConnectionIdLimit);
		}

		public TransportParametersBuilder iniSourceId(byte[] iniSourceId) {
			trace(15, iniSourceId);
			return super.iniSourceId(iniSourceId);
		}

		public TransportParametersBuilder retrySourceId(byte[] retrySourceId) {
			trace(16, retrySourceId);
			return super.retrySourceId(retrySourceId);
		}
	}
}
