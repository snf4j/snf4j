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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.HandshakeState;
import org.snf4j.quic.engine.QuicState;
import org.snf4j.quic.engine.TestConfig;
import org.snf4j.quic.engine.TestTime;
import org.snf4j.quic.frame.ConnectionCloseFrame;

public class ConnectionCloseFrameProcessorTest {
	
	@Test
	public void testProcessError() throws Exception {
		QuicState s = new QuicState(true, new TestConfig(), new TestTime(100000000L));
		QuicProcessor p = new QuicProcessor(s, null);
		ConnectionCloseFrameProcessor cp = new ConnectionCloseFrameProcessor();
		
		ConnectionCloseFrame f = new ConnectionCloseFrame(1111, "ABC");
		assertNull(p.error);
		cp.process(p, f, null);
		assertSame(TransportError.APPLICATION_ERROR, p.error.getTransportError());
		assertEquals(1111, p.error.getErrorCode());
		assertEquals("ABC", p.error.getMessage());
		
		f = new ConnectionCloseFrame(1112, 1, "abc");
		cp.process(p, f, null);
		assertSame(TransportError.APPLICATION_ERROR, p.error.getTransportError());
		assertEquals(1111, p.error.getErrorCode());
		assertEquals("ABC", p.error.getMessage());

		p = new QuicProcessor(s, null);
		cp.process(p, f, null);
		assertSame(TransportError.PROTOCOL_VIOLATION, p.error.getTransportError());
		assertEquals(TransportError.PROTOCOL_VIOLATION.code(), p.error.getErrorCode());
		assertEquals("abc", p.error.getMessage());
		
		p = new QuicProcessor(s, null);
		f = new ConnectionCloseFrame(0, 1, null);
		cp.process(p, f, null);
		assertSame(TransportError.NO_ERROR, p.error.getTransportError());
		assertEquals(0, p.error.getErrorCode());
		assertEquals("", p.error.getMessage());

		p = new QuicProcessor(s, null);
		f = new ConnectionCloseFrame(0x111, 1, null);
		cp.process(p, f, null);
		assertSame(TransportError.CRYPTO_ERROR, p.error.getTransportError());
		assertEquals(0x111, p.error.getErrorCode());
		assertEquals("", p.error.getMessage());		
	}

	@Test
	public void testProcess() throws Exception {
		QuicState s = new QuicState(true, new TestConfig(), new TestTime(100000000L));
		QuicProcessor p = new QuicProcessor(s, null);
		ConnectionCloseFrameProcessor cp = new ConnectionCloseFrameProcessor();
		ConnectionCloseFrame f = new ConnectionCloseFrame(1111, "ABC");
		
		assertSame(HandshakeState.INIT, s.getHandshakeState());
		cp.process(p, f, null);
		assertSame(HandshakeState.CLOSE_PRE_SENDING_2, s.getHandshakeState());
		cp.process(p, f, null);
		assertSame(HandshakeState.CLOSE_PRE_SENDING_2, s.getHandshakeState());
		s.setHandshakeState(HandshakeState.CLOSE_SENDING);
		cp.process(p, f, null);
		assertSame(HandshakeState.CLOSE_PRE_SENDING_2, s.getHandshakeState());
		s.setHandshakeState(HandshakeState.CLOSE_WAITING);
		cp.process(p, f, null);
		assertSame(HandshakeState.CLOSE_PRE_DRAINING, s.getHandshakeState());
	}

	@Test
	public void testSending() throws Exception {
		QuicState s = new QuicState(true, new TestConfig(), new TestTime(100000000L));
		QuicProcessor p = new QuicProcessor(s, null);
		ConnectionCloseFrameProcessor cp = new ConnectionCloseFrameProcessor();
		ConnectionCloseFrame f = new ConnectionCloseFrame(1111, "ABC");
		
		assertSame(HandshakeState.INIT, s.getHandshakeState());
		cp.sending(p, f, null);
		assertSame(HandshakeState.INIT, s.getHandshakeState());
		s.setHandshakeState(HandshakeState.CLOSE_SENDING);
		cp.sending(p, f, null);
		assertSame(HandshakeState.CLOSE_PRE_WAITING, s.getHandshakeState());
		cp.sending(p, f, null);
		assertSame(HandshakeState.CLOSE_PRE_WAITING, s.getHandshakeState());
		s.setHandshakeState(HandshakeState.CLOSE_SENDING_2);
		cp.sending(p, f, null);
		assertSame(HandshakeState.CLOSE_PRE_DRAINING_2, s.getHandshakeState());
		cp.sending(p, f, null);
		assertSame(HandshakeState.CLOSE_PRE_DRAINING_2, s.getHandshakeState());
		
		s.setHandshakeState(HandshakeState.CLOSE_SENDING);
		s.getSpace(EncryptionLevel.APPLICATION_DATA).frames().add(f);
		cp.sending(p, f, null);
		assertSame(HandshakeState.CLOSE_SENDING, s.getHandshakeState());
		s.getSpace(EncryptionLevel.APPLICATION_DATA).frames().clear();
		cp.sending(p, f, null);
		assertSame(HandshakeState.CLOSE_PRE_WAITING, s.getHandshakeState());
	}
	
	@Test
	public void testRecover() {
		ConnectionCloseFrameProcessor p = new ConnectionCloseFrameProcessor();
		p.recover(null, null, null);
		
	}
	
}
