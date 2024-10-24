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

import java.util.LinkedList;
import java.util.List;

import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.FlyingFrames;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.frame.AckFrame;
import org.snf4j.quic.frame.AckRange;
import org.snf4j.quic.frame.EcnAckFrame;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.PacketType;
import org.snf4j.quic.tp.TransportParameters;

class AckFrameProcessor implements IFrameProcessor<AckFrame> {
	
	private final static ILogger LOG = LoggerFactory.getLogger(AckFrameProcessor.class);
	
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
		List<FlyingFrames> newlyAcked = new LinkedList<>();
		
		for (AckRange range: frame.getRanges()) {
			long to = range.getTo();
			
			for (long pn = range.getFrom(); pn >= to; pn--) {
				FlyingFrames fframes = space.frames().getFlying(pn);
				
				if (fframes != null) {
					if (fframes.getSentBytes() == 0) {
						throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Unexpected acked packet number");
					}
					newlyAcked.add(fframes);
					if (largest != null && !addSample) {
						addSample = fframes.isAckEliciting();
					}
					if (fframes.isAckEliciting()) {
						space.updateAckElicitingInFlight(-1);
						if (p.trace) {
							LOG.trace("Ack-eliciting packets decreased to {} in {} space for {}",
									space.getAckElicitingInFlight(),
									space.getType(),
									p.state.getSession());
						}
					}
				}
				space.updateAcked(pn);
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
		else if (newlyAcked.isEmpty()) {
			return;
		}
		
		if (!p.state.isAddressValidatedByPeer()) {
			if (packet.getType() == PacketType.HANDSHAKE) {
				p.state.setAddressValidatedByPeer();
			}
		}
		
		if (frame.hasEcnCounts() && largest != null) {
			p.state.getCongestion().processEcn((EcnAckFrame) frame, space, largest);
		}
		
		List<FlyingFrames> lost = p.state.getLossDetector().detectAndRemoveLostPackets(space, p.currentTime);
		if (!lost.isEmpty()) {
			p.state.getCongestion().onPacketsLost(lost);
			p.recover(space, lost);
		}
		p.state.getCongestion().onPacketAcked(newlyAcked);
		
		if (p.state.isAddressValidatedByPeer()) {
			p.state.getLossDetector().resetPtoCount();
		}
		p.state.getLossDetector().setLossDetectionTimer(p.currentTime, false);
	}

	@Override
	public void sending(QuicProcessor p, AckFrame frame, IPacket packet) {
	}

	@Override
	public void recover(QuicProcessor p, AckFrame frame, PacketNumberSpace space) {	
	}

}