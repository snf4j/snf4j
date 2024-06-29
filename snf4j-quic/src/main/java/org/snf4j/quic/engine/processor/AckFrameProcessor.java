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

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.FlyingFrames;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.frame.AckFrame;
import org.snf4j.quic.frame.AckRange;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.PacketType;
import org.snf4j.quic.tp.TransportParameters;

class AckFrameProcessor implements IFrameProcessor<AckFrame> {

	@Override
	public FrameType getType() {
		return FrameType.ACK;
	}

	@Override
	public void process(QuicProcessor p, AckFrame frame, IPacket packet) throws QuicException {
		EncryptionLevel level = packet.getType().encryptionLevel();
		PacketNumberSpace space = p.state.getSpace(level);
		FlyingFrames largest = space.frames().getFlying(frame.getLargestPacketNumber());
		boolean addSample = false;
		
		for (AckRange range: frame.getRanges()) {
			long to = range.getTo();
			
			for (long pn = range.getFrom(); pn >= to; pn--) {
				FlyingFrames fframes = space.frames().getFlying(pn);
				
				if (fframes != null) {
					if (fframes.getSentBytes() == 0) {
						throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Unexpected acked packet number");
					}
					if (largest != null && !addSample) {
						addSample = fframes.isAckEliciting();
					}
				}
				space.updateAcked(pn);
			}
		}
		
		if (!p.state.isAddressValidatedByPeer()) {
			if (packet.getType() == PacketType.HANDSHAKE) {
				p.state.setAddressValidatedByPeer();
			}
		}
		
		if (addSample) {
			int exponent = level == EncryptionLevel.APPLICATION_DATA
					? p.state.getPeerAckDelayExponent()
					: TransportParameters.DEFAULT_ACK_DELAY_EXPONENT;
					
			p.state.getEstimator().addSample(
					p.currentTime,
					largest.getSentTime(),
					frame.getDelay() << exponent,
					level);
		}
	}

	@Override
	public void sending(QuicProcessor p, AckFrame frame, IPacket packet) {
	}

}
