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
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.cid.ConnectionIdManager;
import org.snf4j.quic.cid.IDestinationPool;
import org.snf4j.quic.cid.ISourcePool;
import org.snf4j.quic.frame.PaddingFrame;
import org.snf4j.quic.frame.PingFrame;
import org.snf4j.quic.tp.TransportParameters;
import org.snf4j.quic.tp.TransportParametersBuilder;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.AeadEncrypt;

public class QuicStateTest extends CommonTest {
	
	TestConfig config;
	
	public void before() throws Exception {
		super.before();
		config = new TestConfig();
	}

	@Test
	public void testConstructor() {
		ITimeProvider time = new TestTime(10000);
		QuicState s = new QuicState(true, config, time);
		assertTrue(s.isClientMode());
		assertNotNull(s.getConnectionIdManager());
		assertNotNull(s.getEstimator());
		assertSame(config, s.getConfig());
		assertSame(time, s.getTime());
		assertEquals(3, s.getPeerAckDelayExponent());
		s.setPeerAckDelayExponent(6);
		assertEquals(6, s.getPeerAckDelayExponent());
		assertEquals(1200, s.getMaxUdpPayloadSize());
		s.setMaxUdpPayloadSize(1230);
		assertEquals(1230, s.getMaxUdpPayloadSize());
		assertFalse(s.isAddressValidatedByPeer());
		assertFalse(s.isAddressValidated());
		
		s = new QuicState(false);
		assertNotNull(s.getConnectionIdManager());
		assertTrue(s.isAddressValidatedByPeer());
		assertFalse(s.isAddressValidated());
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
		config.maxNumberOfStoredAckRanges = 2;
		QuicState s = new QuicState(true, config, new TestTime(1000000));

		PacketNumberSpace s1 = s.getSpace(EncryptionLevel.INITIAL);
		PacketNumberSpace s2 = s.getSpace(EncryptionLevel.EARLY_DATA);
		PacketNumberSpace s3 = s.getSpace(EncryptionLevel.HANDSHAKE);
		PacketNumberSpace s4 = s.getSpace(EncryptionLevel.APPLICATION_DATA);
		
		assertSame(PacketNumberSpace.Type.INITIAL, s1.getType());
		assertSame(s4, s2);
		assertSame(PacketNumberSpace.Type.HANDSHAKE, s3.getType());
		assertSame(PacketNumberSpace.Type.APPLICATION_DATA, s4.getType());
		
		s1.acks().add(0, 1100000);
		s1.acks().add(20, 1100000);
		s1.acks().add(40, 1100000);
		assertEquals(2, s1.acks().build(10, 1100000, 1).getRanges().length);
		
		s2.acks().add(0, 1100000);
		s2.acks().add(20, 1100000);
		s2.acks().add(40, 1100000);
		assertEquals(2, s2.acks().build(10, 1100000, 1).getRanges().length);
		
		s3.acks().add(0, 1100000);
		s3.acks().add(20, 1100000);
		s3.acks().add(40, 1100000);
		assertEquals(2, s3.acks().build(10, 1100000, 1).getRanges().length);

		config.maxNumberOfStoredAckRanges = 4;
		s = new QuicState(true, config, new TestTime(1000000));

		s1 = s.getSpace(EncryptionLevel.INITIAL);
		s1.acks().add(0, 1100000);
		s1.acks().add(20, 1100000);
		s1.acks().add(40, 1100000);
		s1.acks().add(60, 1100000);
		s1.acks().add(80, 1100000);
		s1.acks().add(80, 1100000);
		assertEquals(4, s1.acks().build(10, 1100000, 1).getRanges().length);	
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
		config.maxActiveConnectionIdLimit = 14;
		QuicState s = new QuicState(true, config, new TestTime());
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
		b.activeConnectionIdLimit(14);
		s.consumeTransportParameters(b.build());
		assertEquals(14, s.getConnectionIdManager().getSourcePool().getLimit());
		b.activeConnectionIdLimit(15);
		s.consumeTransportParameters(b.build());
		assertEquals(14, s.getConnectionIdManager().getSourcePool().getLimit());
		
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
		
		b.ackDelayExponent(20);
		s.consumeTransportParameters(b.build());
		assertEquals(20, s.getPeerAckDelayExponent());
		b.ackDelayExponent(21);
		assertConsumeFailure(s, b.build(), "Invalid ack_delay_exponent");
		b.ackDelayExponent(3);
		
		b.maxAckDelay(16383);
		s.consumeTransportParameters(b.build());
		assertEquals(16383, s.getPeerMaxAckDelay());
		b.maxAckDelay(16384);
		assertConsumeFailure(s, b.build(), "Invalid max_ack_delay");
		b.maxAckDelay(25);
	}
	
	@Test
	public void testProduceTransportParameters() {
		QuicState s = new QuicState(true, config, TimeProvider.INSTANCE);
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
		assertEquals(3, p.ackDelayExponent());
		assertEquals(25, p.maxAckDelay());

		s = new QuicState(false, config, TimeProvider.INSTANCE);
		config.ackDelayExponent = 7;
		config.maxAckDelay = 11;
		s.getConnectionIdManager().getSourcePool().issue();
		s.getConnectionIdManager().setOriginalId(bytes("00010203"));
		p = s.produceTransportParameters();
		assertArrayEquals(s.getConnectionIdManager().getSourceId(), p.iniSourceId());
		assertEquals(2, p.activeConnectionIdLimit());
		assertEquals(1200, p.maxUdpPayloadSize());
		assertArrayEquals(bytes("00010203"), p.originalDestinationId());
		assertNull(p.retrySourceId());
		assertArrayEquals(s.getConnectionIdManager().getSourcePool().get(0).getResetToken(), p.statelessResetToken());
		assertEquals(7, p.ackDelayExponent());
		assertEquals(11, p.maxAckDelay());
		
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
	
	@Test
	public void testGetEncryptorLevel() throws Exception {
		QuicState s = new QuicState(true);
		assertNull(s.getEncryptorLevel());
		
		byte[] key = bytes("11223344556677889900112233445566");
		byte[] iv = bytes("112233445566778899001122");
		HeaderProtector hpe = new HeaderProtector(AESAead.AEAD_AES_128_GCM, AESAead.AEAD_AES_128_GCM.createKey(key));
		Encryptor e = new Encryptor(new AeadEncrypt(AESAead.AEAD_AES_128_GCM.createKey(key),AESAead.AEAD_AES_128_GCM),hpe,iv,1000);
		s.getContext(EncryptionLevel.INITIAL).setEncryptor(e);
		assertSame(EncryptionLevel.INITIAL, s.getEncryptorLevel());
	}
	
	@Test
	public void testSetVersion() {
		QuicState s = new QuicState(true);
		
		assertSame(Version.V1, s.getVersion());
		s.setVersion(Version.V0);
		assertSame(Version.V0, s.getVersion());
	}
	
	@Test
	public void testSetHandshakeState() {
		QuicState s = new QuicState(true);

		assertSame(HandshakeState.INIT, s.getHandshakeState());
		s.setHandshakeState(HandshakeState.STARTING);
		assertSame(HandshakeState.STARTING, s.getHandshakeState());
	}
	
	@Test
	public void testIsHandshakeConfirmed() {
		QuicState s = new QuicState(true);

		assertFalse(s.isHandshakeConfirmed());
		for (HandshakeState hs: HandshakeState.values()) {
			switch (hs) {
			case DONE:
			case DONE_SENDING:
			case DONE_RECEIVED:
				break;
			default:
				s.setHandshakeState(hs);		
			}
		}
		assertFalse(s.isHandshakeConfirmed());
		s.setHandshakeState(HandshakeState.DONE);
		assertTrue(s.isHandshakeConfirmed());
		s.setHandshakeState(HandshakeState.CLOSING);
		assertTrue(s.isHandshakeConfirmed());			
		s = new QuicState(true);
		assertFalse(s.isHandshakeConfirmed());
		s.setHandshakeState(HandshakeState.DONE_SENDING);
		assertTrue(s.isHandshakeConfirmed());
		s = new QuicState(true);
		assertFalse(s.isHandshakeConfirmed());
		s.setHandshakeState(HandshakeState.DONE_RECEIVED);
		assertTrue(s.isHandshakeConfirmed());
	}
	
	@Test
	public void testConnectionIdManager() throws Exception {
		config.connectionIdLength = 0;
		config.activeConnectionIdLimit = 3;
		QuicState s = new QuicState(true, config, TimeProvider.INSTANCE);
		
		ConnectionIdManager mgr = s.getConnectionIdManager();
		ISourcePool src = mgr.getSourcePool();
		IDestinationPool dst = mgr.getDestinationPool();
		byte[] scid = src.issue().getId();
		assertEquals(0, scid.length);
		dst.add(0, bytes("00010203"), null);
		dst.add(1, bytes("01020304"), null);
		dst.add(2, bytes("02030405"), null);
		try {
			dst.add(4, bytes("03040506"), null);
			fail();
		}
		catch (QuicException e) {
			assertEquals("Connection id limit exceeded", e.getMessage());
		}
	}

	@Test
	public void testIsAddressValidated() {
		QuicState s = new QuicState(true);
		assertFalse(s.isAddressValidated());
		s.setAddressValidated();
		assertTrue(s.isAddressValidated());
		s.setAddressValidated();
		assertTrue(s.isAddressValidated());
	}
	
	@Test
	public void testIsAddressValidatedByPeer() {
		QuicState s = new QuicState(true);
		assertFalse(s.isAddressValidatedByPeer());
		for (HandshakeState hs: HandshakeState.values()) {
			switch (hs) {
			case DONE:
			case DONE_SENDING:
			case DONE_RECEIVED:
				break;
				
			default:
				s.setHandshakeState(hs);
			}
		}
		assertFalse(s.isAddressValidatedByPeer());
		s.setHandshakeState(HandshakeState.DONE);
		assertTrue(s.isAddressValidatedByPeer());
		s = new QuicState(true);
		assertFalse(s.isAddressValidatedByPeer());
		s.setHandshakeState(HandshakeState.DONE_SENDING);
		assertTrue(s.isAddressValidatedByPeer());
		s = new QuicState(true);
		assertFalse(s.isAddressValidatedByPeer());
		s.setHandshakeState(HandshakeState.DONE_RECEIVED);
		assertTrue(s.isAddressValidatedByPeer());

		s = new QuicState(true);
		assertFalse(s.isAddressValidatedByPeer());
		s.setAddressValidatedByPeer();
		assertTrue(s.isAddressValidatedByPeer());
		s.setAddressValidatedByPeer();
		assertTrue(s.isAddressValidatedByPeer());
	}
	
	@Test
	public void testisAckElicitingInFlight() {	
		QuicState s = new QuicState(true);
		assertFalse(s.isAckElicitingInFlight());
		s.getSpace(EncryptionLevel.INITIAL).updateAckElicitingInFlight(1);
		assertTrue(s.isAckElicitingInFlight());
		s.getSpace(EncryptionLevel.INITIAL).updateAckElicitingInFlight(-1);
		assertFalse(s.isAckElicitingInFlight());
		s.getSpace(EncryptionLevel.HANDSHAKE).updateAckElicitingInFlight(1);
		assertTrue(s.isAckElicitingInFlight());
		s.getSpace(EncryptionLevel.HANDSHAKE).updateAckElicitingInFlight(-1);
		assertFalse(s.isAckElicitingInFlight());
		s.getSpace(EncryptionLevel.APPLICATION_DATA).updateAckElicitingInFlight(1);
		assertTrue(s.isAckElicitingInFlight());
		s.getSpace(EncryptionLevel.HANDSHAKE).updateAckElicitingInFlight(1);
		s.getSpace(EncryptionLevel.INITIAL).updateAckElicitingInFlight(1);
		assertTrue(s.isAckElicitingInFlight());
	}
	
	@Test
	public void testIsBlocked() {
		QuicState s = new QuicState(true);
		assertFalse(s.getAntiAmplificator().isBlocked());
		assertFalse(s.getCongestion().isBlocked());
		assertFalse(s.isBlocked());
		
		s.getCongestion().onPacketSent(12001);
		assertFalse(s.getAntiAmplificator().isBlocked());
		assertTrue(s.getCongestion().isBlocked());
		assertTrue(s.isBlocked());
		
		s = new QuicState(false);
		assertTrue(s.getAntiAmplificator().isBlocked());
		assertFalse(s.getCongestion().isBlocked());
		assertTrue(s.isBlocked());
		
		s.getCongestion().onPacketSent(12001);
		assertTrue(s.getAntiAmplificator().isBlocked());
		assertTrue(s.getCongestion().isBlocked());
		assertTrue(s.isBlocked());
		
		s.getCongestion().onPacketSent(-12001);
		s.getAntiAmplificator().incReceived(1200);;
		assertFalse(s.getAntiAmplificator().isBlocked());
		assertFalse(s.getCongestion().isBlocked());
		assertFalse(s.isBlocked());
	}
	
	@Test
	public void testNeedSend() throws Exception {
		QuicState s = new QuicState(true);
		assertFalse(s.needSend());
		PacketNumberSpace space = s.getSpace(EncryptionLevel.INITIAL);
		space.frames().add(PingFrame.INSTANCE);
		assertFalse(s.needSend());
		new CryptoEngineStateListener(s).onInit(bytes(16), bytes(8));
		assertTrue(s.needSend());
		EncryptionContext ctx = s.getContext(EncryptionLevel.INITIAL);
		Encryptor enc = ctx.getEncryptor();
		space.frames().fly(PingFrame.INSTANCE, 0);
		assertFalse(s.needSend());
		space.frames().add(PingFrame.INSTANCE);
		assertTrue(s.needSend());
		ctx.erase();
		assertFalse(s.needSend());
		
		space = s.getSpace(EncryptionLevel.HANDSHAKE);
		space.frames().add(PingFrame.INSTANCE);
		assertFalse(s.needSend());
		ctx = s.getContext(EncryptionLevel.HANDSHAKE);
		ctx.setEncryptor(enc);
		assertTrue(s.needSend());
		space.frames().fly(PingFrame.INSTANCE, 0);
		assertFalse(s.needSend());
		space.frames().add(PingFrame.INSTANCE);
		assertTrue(s.needSend());
		ctx.erase();
		assertFalse(s.needSend());
		
		space = s.getSpace(EncryptionLevel.APPLICATION_DATA);
		space.frames().add(PingFrame.INSTANCE);
		assertFalse(s.needSend());
		ctx = s.getContext(EncryptionLevel.EARLY_DATA);
		ctx.setEncryptor(enc);
		assertTrue(s.needSend());
		space.frames().fly(PingFrame.INSTANCE, 0);
		assertFalse(s.needSend());
		space.frames().add(PingFrame.INSTANCE);
		assertTrue(s.needSend());
		ctx.erase();
		assertFalse(s.needSend());
		
		ctx = s.getContext(EncryptionLevel.APPLICATION_DATA);
		ctx.setEncryptor(enc);
		assertTrue(s.needSend());
		space.frames().fly(PingFrame.INSTANCE, 1);
		assertFalse(s.needSend());
		space.frames().add(PingFrame.INSTANCE);
		assertTrue(s.needSend());		
	}
	
	@Test
	public void testEraseKeys() {
		QuicState s = new QuicState(true);
		EncryptionContext ctx = s.getContext(EncryptionLevel.INITIAL);
		PacketNumberSpace space = s.getSpace(EncryptionLevel.INITIAL);
		space.frames().add(PingFrame.INSTANCE);
		space.frames().add(PaddingFrame.INSTANCE);
		space.frames().fly(PingFrame.INSTANCE, 0);
		assertTrue(space.frames().hasFlying());
		space.acks().add(0, 1000);
		assertFalse(ctx.isErased());
		assertFalse(space.frames().isEmpty());
		assertFalse(space.acks().isEmpty());
		assertTrue(space.frames().hasFlying());
		s.eraseKeys(EncryptionLevel.INITIAL, 1000);
		assertTrue(ctx.isErased());
		assertTrue(space.frames().isEmpty());
		assertFalse(space.frames().hasFlying());
		assertTrue(space.acks().isEmpty());
	}
}
