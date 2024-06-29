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
package org.snf4j.quic.frame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.Version;
import org.snf4j.quic.packet.InitialPacket;
import org.snf4j.quic.packet.PacketType;
import org.snf4j.quic.packet.RetryPacket;
import org.snf4j.quic.packet.ZeroRttPacket;

public class FrameInfoTest extends CommonTest {

	static String[] DATA = new String[] {
			"0x00	PADDING	Section 19.1	IH01	NP",
			"0x01	PING	Section 19.2	IH01",
			"0x02-0x03	ACK	Section 19.3	IH_1	NC",
			"0x04	RESET_STREAM	Section 19.4	__01",
			"0x05	STOP_SENDING	Section 19.5	__01",
			"0x06	CRYPTO	Section 19.6	IH_1",
			"0x07	NEW_TOKEN	Section 19.7	___1",
			"0x08-0x0f	STREAM	Section 19.8	__01	F",
			"0x10	MAX_DATA	Section 19.9	__01",
			"0x11	MAX_STREAM_DATA	Section 19.10	__01",
			"0x12-0x13	MAX_STREAMS	Section 19.11	__01",
			"0x14	DATA_BLOCKED	Section 19.12	__01",
			"0x15	STREAM_DATA_BLOCKED	Section 19.13	__01",
			"0x16-0x17	STREAMS_BLOCKED	Section 19.14	__01",
			"0x18	NEW_CONNECTION_ID	Section 19.15	__01	P",
			"0x19	RETIRE_CONNECTION_ID	Section 19.16	__01",
			"0x1a	PATH_CHALLENGE	Section 19.17	__01	P",
			"0x1b	PATH_RESPONSE	Section 19.18	___1	P",
			"0x1c	CONNECTION_CLOSE	Section 19.19	IH01	N",
			"0x1d	CONNECTION_CLOSE	Section 19.19	__01	N",
			"0x1e	HANDSHAKE_DONE	Section 19.20	___1",
	};
	
	@Test
	public void testAll() {
		FrameInfo info = FrameInfo.of(Version.V1);

		for (int i=0; i<DATA.length; ++i) {
			String[] d = DATA[i].split("\t");
			String pkts = d[3];
			String spec = d.length > 4 ? d[4] : "";
			String ids = d[0];
			
			int p = ids.indexOf('-');
			int id1, id2;
			if (p != -1) {
				id1 = Integer.parseInt(ids.substring(2, p), 16);
				id2 = Integer.parseInt(ids.substring(p+3, p+5), 16);
			}
			else {
				id1 = Integer.parseInt(ids.substring(2), 16);
				id2 = id1;
			}
			
			for (;id1 <=id2; ++id1) {
				assertEquals(!spec.contains("N"), info.isAckEliciting(id1));
				assertEquals(!spec.contains("C"), info.isCongestionControlled(id1));
				assertEquals(spec.contains("P"), info.isPathProbing(id1));
				assertEquals(spec.contains("F"), info.isFlowControlled(id1));
				
				assertEquals(d[1], pkts.contains("I"),info.isValid(PacketType.INITIAL, id1));
				assertEquals(d[1], pkts.contains("H"),info.isValid(PacketType.HANDSHAKE, id1));
				assertEquals(d[1], pkts.contains("0"),info.isValid(PacketType.ZERO_RTT, id1));
				assertEquals(d[1], pkts.contains("1"),info.isValid(PacketType.ONE_RTT, id1));
				assertFalse(info.isValid(PacketType.VERSION_NEGOTIATION, id1));
				assertFalse(info.isValid(PacketType.RETRY, id1));
			}
		}
	}
	
	@Test
	public void testIsAckEliciting() {
		FrameInfo info = FrameInfo.of(Version.V1);
		InitialPacket p = new InitialPacket(bytes(10), 10, bytes(8), Version.V1, bytes());
		List<IFrame> frames = p.getFrames();
		
		assertFalse(info.isAckEliciting(p));
		frames.add(new PaddingFrame());
		assertFalse(info.isAckEliciting(p));
		frames.add(new MultiPaddingFrame(10));
		assertFalse(info.isAckEliciting(p));
		frames.add(new AckFrame(10,10));
		assertFalse(info.isAckEliciting(p));
		frames.add(new PingFrame());
		assertTrue(info.isAckEliciting(p));
		frames.clear();
		frames.add(new PaddingFrame());
		assertFalse(info.isAckEliciting(p));
		frames.add(new CryptoFrame(0, ByteBuffer.allocate(10)));
		assertTrue(info.isAckEliciting(p));
		assertFalse(info.isAckEliciting(new RetryPacket(bytes(), bytes(), Version.V1, bytes(16), bytes(16))));
	}	
	
	@Test
	public void testIsValid() {
		FrameInfo info = FrameInfo.of(Version.V1);
		ZeroRttPacket p = new ZeroRttPacket(bytes(10), 10, bytes(8), Version.V1);
		List<IFrame> frames = p.getFrames();

		assertTrue(info.isValid(p));
		assertTrue(info.isValid(new RetryPacket(bytes(), bytes(), Version.V1, bytes(16), bytes(16))));
		frames.add(new PaddingFrame());
		assertTrue(info.isValid(p));
		frames.add(new AckFrame(20, 1000));
		assertFalse(info.isValid(p));
	}
	
	@Test
	public void testIsCongestionControlled() {
		FrameInfo info = FrameInfo.of(Version.V1);
		ZeroRttPacket p = new ZeroRttPacket(bytes(10), 10, bytes(8), Version.V1);
		List<IFrame> frames = p.getFrames();
		
		assertFalse(info.isCongestionControlled(p));
		assertFalse(info.isCongestionControlled(new RetryPacket(bytes(), bytes(), Version.V1, bytes(16), bytes(16))));
		frames.add(new AckFrame(20, 1000));
		assertFalse(info.isCongestionControlled(p));
		frames.add(new PaddingFrame());
		assertTrue(info.isCongestionControlled(p));
	}
}
