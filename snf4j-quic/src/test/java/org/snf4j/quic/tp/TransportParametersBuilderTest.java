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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;

public class TransportParametersBuilderTest extends CommonTest {

	ITransportParametersFormatter formatter = TransportParametersFormatter.INSTANCE;
	
	ITransportParametersParser parser = TransportParametersParser.INSTANCE;

	TransportParametersBuilder b1,b2;
	
	TransportParameters p1,p2;
	
	@Override
	public void before() throws Exception {
		super.before();
		b1 = new TransportParametersBuilder();
		b2 = new TransportParametersBuilder();
		p1 = null;
		p2 = null;
	}
	
	@Test
	public void testDefaults() throws Exception {
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertArrayEquals(bytes(""), bytes());
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertArrayEquals(bytes(""), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		
		buffer.clear();
		formatter.format(false, p2, buffer);
		assertArrayEquals(bytes(""), bytes());
		buffer.clear();
		formatter.format(true, p2, buffer);
		assertArrayEquals(bytes(""), bytes());
	}
	
	@Test
	public void testOriginalDestinationId() throws Exception {
		assertNotNull(b1.originalDestinationId(bytes("0001020304")));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0005 0001020304"), bytes());
		buffer.flip();
		parser.parse(true, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertArrayEquals(bytes("0001020304"), p2.originalDestinationId());
		assertArrayEquals(bytes("0001020304"), b2.originalDestinationId());
		
		buffer.clear();
		b1.originalDestinationId(bytes(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("003f" + hex(bytes(63))), bytes());
		buffer.flip();
		parser.parse(true, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertArrayEquals(bytes(63), p2.originalDestinationId());

		buffer.clear();
		b1.originalDestinationId(bytes(64));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("004040" + hex(bytes(64))), bytes());
		buffer.flip();
		parser.parse(true, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertArrayEquals(bytes(64), p2.originalDestinationId());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes(""), bytes());	
	}
	
	@Test
	public void testmaxIdleTimeout() throws Exception {
		assertNotNull(b1.maxIdleTimeout(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0101 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.maxIdleTimeout());
		assertEquals(63, b2.maxIdleTimeout());
	
		buffer.clear();
		assertNotNull(b1.maxIdleTimeout(64));
		p1 = b1.build();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0102 4040"), bytes());
		buffer.flip();
		parser.parse(true, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(64, p2.maxIdleTimeout());
		assertEquals(64, b2.maxIdleTimeout());
	
		buffer.clear();
		assertNotNull(b1.maxIdleTimeout(0));
		p1 = b1.build();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes(""), bytes());
	}
	
	@Test
	public void testStatelessResetToken() throws Exception {
		assertNotNull(b1.statelessResetToken(bytes("0001020304")));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0205 0001020304"), bytes());
		buffer.flip();
		parser.parse(true, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertArrayEquals(bytes("0001020304"), p2.statelessResetToken());
		assertArrayEquals(bytes("0001020304"), b2.statelessResetToken());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes(""), bytes());
		
		buffer.clear();
		b1.statelessResetToken(null);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}
	
	@Test
	public void testMaxUdpPayloadSize() throws Exception {
		assertNotNull(b1.maxUdpPayloadSize(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0301 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.maxUdpPayloadSize());
		assertEquals(63, b2.maxUdpPayloadSize());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0301 3f"), bytes());
		
		buffer.clear();
		b1.maxUdpPayloadSize(65527);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}
	
	@Test
	public void testIniMaxData() throws Exception {
		assertNotNull(b1.iniMaxData(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0401 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.iniMaxData());
		assertEquals(63, b2.iniMaxData());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0401 3f"), bytes());
		
		buffer.clear();
		b1.iniMaxData(0);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}

	@Test
	public void testIniMaxStreamDataBidiLocal() throws Exception {
		assertNotNull(b1.iniMaxStreamDataBidiLocal(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0501 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.iniMaxStreamDataBidiLocal());
		assertEquals(63, b2.iniMaxStreamDataBidiLocal());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0501 3f"), bytes());
		
		buffer.clear();
		b1.iniMaxStreamDataBidiLocal(0);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}

	@Test
	public void testIniMaxStreamDataBidiRemote() throws Exception {
		assertNotNull(b1.iniMaxStreamDataBidiRemote(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0601 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.iniMaxStreamDataBidiRemote());
		assertEquals(63, b2.iniMaxStreamDataBidiRemote());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0601 3f"), bytes());
		
		buffer.clear();
		b1.iniMaxStreamDataBidiRemote(0);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}

	@Test
	public void testIniMaxStreamDataUni() throws Exception {
		assertNotNull(b1.iniMaxStreamDataUni(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0701 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.iniMaxStreamDataUni());
		assertEquals(63, b2.iniMaxStreamDataUni());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0701 3f"), bytes());
		
		buffer.clear();
		b1.iniMaxStreamDataUni(0);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}

	@Test
	public void testIniMaxStreamsBidi() throws Exception {
		assertNotNull(b1.iniMaxStreamsBidi(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0801 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.iniMaxStreamsBidi());
		assertEquals(63, b2.iniMaxStreamsBidi());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0801 3f"), bytes());
		
		buffer.clear();
		b1.iniMaxStreamsBidi(0);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}
	
	@Test
	public void testIniMaxStreamsUni() throws Exception {
		assertNotNull(b1.iniMaxStreamsUni(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0901 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.iniMaxStreamsUni());
		assertEquals(63, b2.iniMaxStreamsUni());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0901 3f"), bytes());
		
		buffer.clear();
		b1.iniMaxStreamsUni(0);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}
	
	@Test
	public void testAckDelayExponent() throws Exception {
		assertNotNull(b1.ackDelayExponent(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0a01 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.ackDelayExponent());
		assertEquals(63, b2.ackDelayExponent());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0a01 3f"), bytes());
		
		buffer.clear();
		b1.ackDelayExponent(3);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}

	@Test
	public void testMaxAckDelay() throws Exception {
		assertNotNull(b1.maxAckDelay(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0b01 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.maxAckDelay());
		assertEquals(63, b2.maxAckDelay());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0b01 3f"), bytes());
		
		buffer.clear();
		b1.maxAckDelay(25);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}
	
	@Test
	public void testDisableActiveMigration() throws Exception {
		assertNotNull(b1.disableActiveMigration(true));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0c00"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertTrue(p2.disableActiveMigration());
		assertTrue(b2.disableActiveMigration());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0c00"), bytes());
		
		buffer.clear();
		b1.disableActiveMigration(false);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}

	@Test
	public void testPreferredAddress() throws Exception {
		PreferredAddress pa = new PreferredAddress(
				(Inet4Address)InetAddress.getByAddress(bytes("10121314")),
				0x356,
				(Inet6Address)InetAddress.getByAddress(bytes(16)),
				0x357,
				bytes("01020304"),
				bytes(16));
		assertNotNull(b1.preferredAddress(pa));
		assertSame(pa, b1.preferredAddress());
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0d2d 10121314 0356 000102030405060708090a0b0c0d0e0f 0357 04 01020304 000102030405060708090a0b0c0d0e0f"), bytes());
		buffer.flip();
		parser.parse(true, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(pa.getIp4(), p2.preferredAddress().getIp4());
		assertEquals(pa.getIp4Port(), p2.preferredAddress().getIp4Port());
		assertEquals(pa.getIp6(), p2.preferredAddress().getIp6());
		assertEquals(pa.getIp6Port(), p2.preferredAddress().getIp6Port());
		assertArrayEquals(pa.getConnectionId(), p2.preferredAddress().getConnectionId());
		assertArrayEquals(pa.getResetToken(), p2.preferredAddress().getResetToken());

		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes(""), bytes());
	
		buffer.clear();
		b1.preferredAddress(null);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());

		pa = new PreferredAddress(
				null,
				0,
				(Inet6Address)InetAddress.getByAddress(bytes(16)),
				0x357,
				bytes("01020304"),
				bytes(16));
		assertNotNull(b1.preferredAddress(pa));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0d2d 00000000 0000 000102030405060708090a0b0c0d0e0f 0357 04 01020304 000102030405060708090a0b0c0d0e0f"), bytes());
		buffer.flip();
		parser.parse(true, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertNull(p2.preferredAddress().getIp4());
		assertEquals(0, p2.preferredAddress().getIp4Port());
		assertEquals(pa.getIp6(), p2.preferredAddress().getIp6());
		assertEquals(pa.getIp6Port(), p2.preferredAddress().getIp6Port());
		assertArrayEquals(pa.getConnectionId(), p2.preferredAddress().getConnectionId());
		assertArrayEquals(pa.getResetToken(), p2.preferredAddress().getResetToken());

		buffer.clear();
		pa = new PreferredAddress(
				(Inet4Address)InetAddress.getByAddress(bytes("10121314")),
				0x356,
				null,
				0,
				bytes("01020304"),
				bytes("101112131415161718191a1b1c1d1e1f"));
		assertNotNull(b1.preferredAddress(pa));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0d2d 10121314 0356 00000000000000000000000000000000 0000 04 01020304 101112131415161718191a1b1c1d1e1f"), bytes());
		buffer.flip();
		parser.parse(true, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(pa.getIp4(), p2.preferredAddress().getIp4());
		assertEquals(pa.getIp4Port(), p2.preferredAddress().getIp4Port());
		assertNull(p2.preferredAddress().getIp6());
		assertEquals(0, p2.preferredAddress().getIp6Port());
		assertArrayEquals(pa.getConnectionId(), p2.preferredAddress().getConnectionId());
		assertArrayEquals(pa.getResetToken(), p2.preferredAddress().getResetToken());

		parser.parse(true, buffer("0d2d 00000000 0356 000102030405060708090a0b0c0d0e0f 0357 04 01020304 000102030405060708090a0b0c0d0e0f"), buffer.remaining(), b2);
		p2 = b2.build();
		assertArrayEquals(bytes("00000000"), p2.preferredAddress().getIp4().getAddress());
		assertEquals(0x0356, p2.preferredAddress().getIp4Port());
		assertArrayEquals(bytes("000102030405060708090a0b0c0d0e0f"), p2.preferredAddress().getIp6().getAddress());
		assertEquals(0x0357, p2.preferredAddress().getIp6Port());
		assertArrayEquals(bytes("01020304"), p2.preferredAddress().getConnectionId());
		assertArrayEquals(bytes("000102030405060708090a0b0c0d0e0f"), p2.preferredAddress().getResetToken());

		parser.parse(true, buffer("0d2d 01020304 0356 00000000000000000000000000000000 0357 04 01020304 000102030405060708090a0b0c0d0e0f"), buffer.remaining(), b2);
		p2 = b2.build();
		assertArrayEquals(bytes("01020304"), p2.preferredAddress().getIp4().getAddress());
		assertEquals(0x0356, p2.preferredAddress().getIp4Port());
		assertArrayEquals(bytes("00000000000000000000000000000000"), p2.preferredAddress().getIp6().getAddress());
		assertEquals(0x0357, p2.preferredAddress().getIp6Port());
		assertArrayEquals(bytes("01020304"), p2.preferredAddress().getConnectionId());
		assertArrayEquals(bytes("000102030405060708090a0b0c0d0e0f"), p2.preferredAddress().getResetToken());
	}
	
	@Test(expected=QuicException.class)
	public void testInvalidIp() throws Exception {
		PPreferredAddress.ip(bytes(3));
	}
	
	@Test
	public void testActiveConnectionIdLimit() throws Exception {
		assertNotNull(b1.activeConnectionIdLimit(63));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0e01 3f"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertEquals(63, p2.activeConnectionIdLimit());
		assertEquals(63, b2.activeConnectionIdLimit());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0e01 3f"), bytes());
		
		buffer.clear();
		b1.activeConnectionIdLimit(2);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}
	
	@Test
	public void testIniSourceId() throws Exception {
		assertNotNull(b1.iniSourceId(bytes("0001020304")));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0f05 0001020304"), bytes());
		buffer.flip();
		parser.parse(false, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertArrayEquals(bytes("0001020304"), p2.iniSourceId());
		assertArrayEquals(bytes("0001020304"), b2.iniSourceId());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes("0f05 0001020304"), bytes());
		
		buffer.clear();
		b1.iniSourceId(bytes("0102"));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("0f02 0102"), bytes());
	}
	
	@Test
	public void testRetrySourceId() throws Exception {
		assertNotNull(b1.retrySourceId(bytes("0001020304")));
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes("1005 0001020304"), bytes());
		buffer.flip();
		parser.parse(true, buffer, buffer.remaining(), b2);
		p2 = b2.build();
		assertArrayEquals(bytes("0001020304"), p2.retrySourceId());
		assertArrayEquals(bytes("0001020304"), b2.retrySourceId());
		
		buffer.clear();
		formatter.format(true, p1, buffer);
		assertEquals(buffer.position(), formatter.length(true, p1));
		assertArrayEquals(bytes(""), bytes());
		
		buffer.clear();
		b1.retrySourceId(null);
		p1 = b1.build();
		formatter.format(false, p1, buffer);
		assertEquals(buffer.position(), formatter.length(false, p1));
		assertArrayEquals(bytes(""), bytes());
	}

}
