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

import static org.snf4j.quic.engine.HandshakeState.CLOSE_PRE_DRAINING;
import static org.snf4j.quic.engine.HandshakeState.CLOSE_PRE_DRAINING_2;
import static org.snf4j.quic.engine.HandshakeState.CLOSE_PRE_SENDING_2;
import static org.snf4j.quic.engine.HandshakeState.CLOSE_PRE_WAITING;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.HandshakeState;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.engine.QuicState;
import org.snf4j.quic.frame.ConnectionCloseFrame;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.packet.IPacket;

class ConnectionCloseFrameProcessor implements IFrameProcessor<ConnectionCloseFrame> {

	@Override
	public FrameType getType() {
		return FrameType.CONNECTION_CLOSE;
	}

	@Override
	public void process(QuicProcessor p, ConnectionCloseFrame frame, IPacket packet) throws QuicException {
		HandshakeState hstate = p.state.getHandshakeState();
		
		if (p.error == null) {
			if (frame.getTypeValue() == ConnectionCloseFrame.APPLICATION_TYPE) {
				p.error = new QuicException(
						TransportError.APPLICATION_ERROR, 
						frame.getError(), 
						frame.getReason());
			}
			else {
				long errorCode = frame.getError();
				TransportError error = TransportError.of(errorCode);
				
				if (error == null) {
					error = TransportError.PROTOCOL_VIOLATION;
					errorCode = error.code();
				}
				p.error = new QuicException(
						error, 
						errorCode, 
						frame.getReason());
			}
		}
		
		if (!hstate.isClosing()) {
			hstate = CLOSE_PRE_SENDING_2;
		}
		else {
			switch (hstate) {
			case CLOSE_SENDING:
				hstate = CLOSE_PRE_SENDING_2;
				break;
				
			case CLOSE_WAITING:
				hstate = CLOSE_PRE_DRAINING;
				break;
				
			default:
				return;
			}
		}
		p.state.setHandshakeState(hstate);
	}

	@Override
	public void sending(QuicProcessor p, ConnectionCloseFrame frame, IPacket packet) {
		QuicState state = p.state;
		
		for (EncryptionLevel level: EncryptionLevel.values()) {
			if (state.getSpace(level).frames().has(FrameType.CONNECTION_CLOSE)) {
				return;
			}
		}
		
		HandshakeState hstate;
		
		switch (state.getHandshakeState()) {
		case CLOSE_SENDING:
			hstate = CLOSE_PRE_WAITING;
			break;
			
		case CLOSE_SENDING_2:
			hstate = CLOSE_PRE_DRAINING_2;
			break;
			
		default:
			return;
		}
		state.setHandshakeState(hstate);
	}

	@Override
	public void recover(QuicProcessor p, ConnectionCloseFrame frame, PacketNumberSpace space) {
	}

}
