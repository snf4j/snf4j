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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicAlert;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.tp.TransportParameters;
import org.snf4j.quic.tp.TransportParametersBuilder;

public class QuicStateTest extends CommonTest {
	
	@Test
	public void testConstructor() {
		QuicState s = new QuicState(true);
		assertTrue(s.isClientMode());
		assertNotNull(s.getConnectionIdManager());

		s = new QuicState(false);
		assertNotNull(s.getConnectionIdManager());
	}

	void assertContext(EncryptionContext ctx) {
		assertFalse(ctx.isErased());
	}
	
	@Test
	public void testGetContext() {
		QuicState s = new QuicState(true);
		assertContext(s.getContext(EncryptionLevel.INITIAL));
		assertContext(s.getContext(EncryptionLevel.EARLY_DATA));
		assertContext(s.getContext(EncryptionLevel.HANDSHAKE));
		assertContext(s.getContext(EncryptionLevel.APPLICATION_DATA));		
	}
	
	@Test
	public void testGetSpace() {
		QuicState s = new QuicState(true);

		PacketNumberSpace s1 = s.getSpace(EncryptionLevel.INITIAL);
		PacketNumberSpace s2 = s.getSpace(EncryptionLevel.EARLY_DATA);
		PacketNumberSpace s3 = s.getSpace(EncryptionLevel.HANDSHAKE);
		PacketNumberSpace s4 = s.getSpace(EncryptionLevel.APPLICATION_DATA);
		
		assertSame(PacketNumberSpace.Type.INITIAL, s1.getType());
		assertSame(s4, s2);
		assertSame(PacketNumberSpace.Type.HANDSHAKE, s3.getType());
		assertSame(PacketNumberSpace.Type.APPLICATION_DATA, s4.getType());
	}	
	
	void assertConsumeFailure(QuicState state, TransportParameters p, String msg) throws Exception {
		try {
			state.consumeTransportParameters(p);
			fail();
		} catch (QuicAlert e) {
			assertSame(TransportError.TRANSPORT_PARAMETER_ERROR, e.getTransportError());
			assertEquals(msg,  e.getMessage());
		}
	}
	
	@Test
	public void testConsumeTransportParameters() throws Exception {
		TransportParametersBuilder b = new TransportParametersBuilder();
		QuicState s = new QuicState(true);
		s.getConnectionIdManager().getOriginalId();
		assertConsumeFailure(s, b.build(), "Missing original_destination_connection_id");
		b.originalDestinationId(bytes("0001"));
		assertConsumeFailure(s, b.build(), "Not matching original_destination_connection_id");
		b.originalDestinationId(s.getConnectionIdManager().getOriginalId());

		s.getConnectionIdManager().getDestinationPool().add(bytes("010203"));
		assertConsumeFailure(s, b.build(), "Missing initial_source_connection_id");
		b.iniSourceId(bytes("010204"));
		assertConsumeFailure(s, b.build(), "Not matching initial_source_connection_id");
		b.iniSourceId(bytes("010203"));
		s.consumeTransportParameters(b.build());
		
		assertEquals(2, s.getConnectionIdManager().getSourcePool().getLimit());
		b.activeConnectionIdLimit(10);
		s.consumeTransportParameters(b.build());
		assertEquals(10, s.getConnectionIdManager().getSourcePool().getLimit());
		b.activeConnectionIdLimit(11);
		s.consumeTransportParameters(b.build());
		assertEquals(10, s.getConnectionIdManager().getSourcePool().getLimit());
		
		assertEquals(1200, s.getMaxUdpPayloadSize());
		b.maxUdpPayloadSize(1201);
		s.consumeTransportParameters(b.build());
		assertEquals(1200, s.getMaxUdpPayloadSize());
		b.maxUdpPayloadSize(1199);
		assertConsumeFailure(s, b.build(), "Invalid max_udp_payload_size");
		b.maxUdpPayloadSize(1200);
		
		b.statelessResetToken(bytes(15));
		assertConsumeFailure(s, b.build(), "Invalid length of stateless_reset_token");
		assertNull(s.getConnectionIdManager().getDestinationPool().get(0).getResetToken());
		b.statelessResetToken(bytes(16));
		s.consumeTransportParameters(b.build());
		assertArrayEquals(bytes(16), s.getConnectionIdManager().getDestinationPool().get(0).getResetToken());
		
		b.retrySourceId(bytes("00010304"));
		assertConsumeFailure(s, b.build(), "Unexpected retry_source_connection_id");
		b.retrySourceId(null);
		s.getConnectionIdManager().setRetryId(bytes("000102"));
		assertConsumeFailure(s, b.build(), "Missing retry_source_connection_id");
		b.retrySourceId(bytes("000101"));
		assertConsumeFailure(s, b.build(), "Not matching retry_source_connection_id");
		b.retrySourceId(bytes("000102"));
		s.consumeTransportParameters(b.build());
	}
	
	@Test
	public void testProduceTransportParameters() {
		QuicState s = new QuicState(true);
		s.getConnectionIdManager().getSourcePool().issue();
		s.getConnectionIdManager().setOriginalId(bytes("00010203"));
		s.getConnectionIdManager().setRetryId(bytes("11121314"));
		TransportParameters p = s.produceTransportParameters();
		assertArrayEquals(s.getConnectionIdManager().getSourceId(), p.iniSourceId());
		assertEquals(2, p.activeConnectionIdLimit());
		assertEquals(1200, p.maxUdpPayloadSize());
		assertNull(p.originalDestinationId());
		assertNull(p.retrySourceId());
		assertNull(p.statelessResetToken());

		s = new QuicState(false);
		s.getConnectionIdManager().getSourcePool().issue();
		s.getConnectionIdManager().setOriginalId(bytes("00010203"));
		p = s.produceTransportParameters();
		assertArrayEquals(s.getConnectionIdManager().getSourceId(), p.iniSourceId());
		assertEquals(2, p.activeConnectionIdLimit());
		assertEquals(1200, p.maxUdpPayloadSize());
		assertArrayEquals(bytes("00010203"), p.originalDestinationId());
		assertNull(p.retrySourceId());
		assertArrayEquals(s.getConnectionIdManager().getSourcePool().get(0).getResetToken(), p.statelessResetToken());

		s = new QuicState(false);
		s.getConnectionIdManager().getSourcePool().issue();
		s.getConnectionIdManager().setOriginalId(bytes("00010203"));
		s.getConnectionIdManager().setRetryId(bytes("11121314"));
		p = s.produceTransportParameters();
		assertArrayEquals(s.getConnectionIdManager().getSourceId(), p.iniSourceId());
		assertEquals(2, p.activeConnectionIdLimit());
		assertEquals(1200, p.maxUdpPayloadSize());
		assertArrayEquals(bytes("00010203"), p.originalDestinationId());
		assertArrayEquals(s.getConnectionIdManager().getRetryId(), p.retrySourceId());
		assertArrayEquals(s.getConnectionIdManager().getSourcePool().get(0).getResetToken(), p.statelessResetToken());
	}
}
