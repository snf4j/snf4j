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

import static org.snf4j.quic.engine.HandshakeState.DONE_RECEIVED;
import static org.snf4j.quic.engine.HandshakeState.DONE_SENDING;
import static org.snf4j.quic.engine.HandshakeState.DONE_SENT;
import static org.snf4j.quic.engine.HandshakeState.DONE_WAITING;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.frame.HandshakeDoneFrame;
import org.snf4j.quic.packet.IPacket;

class HandshakeDoneFrameProcessor implements IFrameProcessor<HandshakeDoneFrame> {

	@Override
	public FrameType getType() {
		return FrameType.HANDSHAKE_DONE;
	}

	@Override
	public void process(QuicProcessor p, HandshakeDoneFrame frame, IPacket packet) throws QuicException {
		if (!p.state.isClientMode()) {
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Received unexpected HANDSHAKE_DONE frame");
		}
		if (p.state.getHandshakeState() == DONE_WAITING) {
			p.state.setHandshakeState(DONE_RECEIVED);
		}
	}

	@Override
	public void sending(QuicProcessor p, HandshakeDoneFrame frame, IPacket packet) {
		if (p.state.getHandshakeState() == DONE_SENDING) {
			p.state.setHandshakeState(DONE_SENT);
		}
	}

	@Override
	public void recover(QuicProcessor p, HandshakeDoneFrame frame, PacketNumberSpace space) {
		space.frames().add(frame);
	}
}
