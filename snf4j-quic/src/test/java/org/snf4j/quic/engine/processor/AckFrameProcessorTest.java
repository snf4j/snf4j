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
package org.snf4j.quic.engine.processor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.engine.QuicState;
import org.snf4j.quic.frame.AckFrame;
import org.snf4j.quic.frame.AckRange;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.frame.PingFrame;
import org.snf4j.quic.packet.HandshakePacket;

public class AckFrameProcessorTest extends CommonTest {

	@Test
	public void testSending() {
		AckFrameProcessor p = new AckFrameProcessor();
		
		assertSame(FrameType.ACK, p.getType());
		p.sending(null, null, null);
	}

	@Test
	public void testProcess() throws Exception {
		QuicState s = new QuicState(true);
		QuicProcessor p = new QuicProcessor(s, null);
		AckFrameProcessor ap = new AckFrameProcessor();
		HandshakePacket packet = new HandshakePacket(bytes(), 0, bytes(), Version.V1);
		
		try {
			ap.process(p, new AckFrame(0,1000), packet);
			fail();
		}
		catch (QuicException e) {
		}
		PacketNumberSpace space = s.getSpace(EncryptionLevel.HANDSHAKE);
		space.frames().fly(new PingFrame(), 0);
		assertFalse(space.frames().allAcked());
		ap.process(p, new AckFrame(0,1000), packet);
		assertTrue(space.frames().allAcked());
		ap.process(p, new AckFrame(0,1000), packet);
		
		AckRange[] ranges = new AckRange[] {new AckRange(9,8), new AckRange(6,4)};
		try {
			ap.process(p, new AckFrame(ranges,1000), packet);
			fail();
		}
		catch (QuicException e) {
		}
		space.frames().fly(new PingFrame(), 6);
		try {
			ap.process(p, new AckFrame(ranges,1000), packet);
			fail();
		}
		catch (QuicException e) {
		}
		space.frames().fly(new PingFrame(), 9);
		space.frames().fly(new PingFrame(), 8);
		space.frames().fly(new PingFrame(), 6);
		space.frames().fly(new PingFrame(), 5);
		space.frames().fly(new PingFrame(), 4);
		assertFalse(space.frames().allAcked());
		ap.process(p, new AckFrame(ranges,1000), packet);
		assertTrue(space.frames().allAcked());
	}
}
