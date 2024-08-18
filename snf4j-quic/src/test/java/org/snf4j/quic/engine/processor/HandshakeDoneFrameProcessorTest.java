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

import org.junit.Test;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.HandshakeState;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.engine.QuicState;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.frame.HandshakeDoneFrame;

public class HandshakeDoneFrameProcessorTest {

	@Test
	public void testProcess() throws Exception {
		QuicState s = new QuicState(true);
		QuicProcessor p = new QuicProcessor(s, null);
		HandshakeDoneFrameProcessor fp = new HandshakeDoneFrameProcessor();
		
		assertSame(FrameType.HANDSHAKE_DONE, fp.getType());
		fp.process(p, new HandshakeDoneFrame(), null);
		assertSame(HandshakeState.INIT, s.getHandshakeState());
		s.setHandshakeState(HandshakeState.DONE_WAITING);
		fp.process(p, new HandshakeDoneFrame(), null);
		assertSame(HandshakeState.DONE_RECEIVED, s.getHandshakeState());
	}
	
	@Test(expected=QuicException.class)
	public void testProcessEx() throws Exception {
		QuicState s = new QuicState(false);
		QuicProcessor p = new QuicProcessor(s, null);
		HandshakeDoneFrameProcessor fp = new HandshakeDoneFrameProcessor();
		fp.process(p, new HandshakeDoneFrame(), null);
	}
	
	@Test
	public void testSending() throws Exception {
		QuicState s = new QuicState(false);
		QuicProcessor p = new QuicProcessor(s, null);
		HandshakeDoneFrameProcessor fp = new HandshakeDoneFrameProcessor();
		
		fp.sending(p, new HandshakeDoneFrame(), null);
		assertSame(HandshakeState.INIT, s.getHandshakeState());
		s.setHandshakeState(HandshakeState.DONE_SENDING);
		fp.sending(p, new HandshakeDoneFrame(), null);
		assertSame(HandshakeState.DONE_SENT, s.getHandshakeState());
	}
	
	@Test
	public void testRecovery() {
		QuicState s = new QuicState(false);
		QuicProcessor p = new QuicProcessor(s, null);
		HandshakeDoneFrameProcessor fp = new HandshakeDoneFrameProcessor();
		
		PacketNumberSpace space = s.getSpace(EncryptionLevel.APPLICATION_DATA);
		assertTrue(space.frames().isEmpty());
		fp.recover(p, HandshakeDoneFrame.INSTANCE, space);
		assertFalse(space.frames().isEmpty());
		space.frames().fly(HandshakeDoneFrame.INSTANCE, 0);
		assertTrue(space.frames().isEmpty());
	}
}
