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

import java.util.List;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.cid.IDestinationPool;
import org.snf4j.quic.cid.IPool;
import org.snf4j.quic.engine.CryptoEngineAdapter;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.FlyingFrames;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.engine.QuicState;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.frame.IFrame;
import org.snf4j.quic.packet.ILongHeaderPacket;
import org.snf4j.quic.packet.IPacket;

/**
 * The QUIC packet/frame processor.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class QuicProcessor {

	@SuppressWarnings("rawtypes")
	private final static IFrameProcessor[] FRAME_PROCESSORS = new IFrameProcessor[FrameType.values().length];
	
	static {
		add(new PaddingFrameProcessor());
		add(new PingFrameProcessor());
		add(new AckFrameProcessor());
		add(new CryptoFrameProcessor());
		add(new HandshakeDoneFrameProcessor());
	}
	
	@SuppressWarnings("rawtypes")
	private static void add(IFrameProcessor processor) {
		FRAME_PROCESSORS[processor.getType().ordinal()] = processor;
	}
	
	final QuicState state;
	
	final CryptoEngineAdapter adapter;
	
	/** Current time in nanoseconds */ 
	long currentTime;
	
	/**
	 * Constructs a processor associated with given QUIC state and cryptographic
	 * engine adapter.
	 * 
	 * @param state   the QUIC state
	 * @param adapter the cryptographic engine adapter
	 */
	public QuicProcessor(QuicState state, CryptoEngineAdapter adapter) {
		this.state = state;
		this.adapter = adapter;
	}

	/**
	 * Called as soon as a PDU payload has been received. 
	 * <p>
	 * It is called once and always before calling {@link #process(IPacket, boolean)} for all
	 * packets in the PDU payload.
	 */
	public void preProcess() {	
		currentTime = state.getTime().nanoTime();
	}
	
	/**
	 * Processes a received QUIC packet that has been initially validated.
	 * 
	 * @param packet       the received QUIC packet
	 * @param ackEliciting tells is the packet is ack eliciting
	 * @throws QuicException if an error occurred
	 */
	@SuppressWarnings("unchecked")
	public void process(IPacket packet, boolean ackEliciting) throws QuicException {
		List<IFrame> frames = packet.getFrames();
		
		if (frames != null) {

			switch (packet.getType()) {
			case INITIAL:
				IDestinationPool pool = state.getConnectionIdManager().getDestinationPool();
				
				if (pool.get(IPool.INITIAL_SEQUENCE_NUMBER) == null) {
					pool.add(((ILongHeaderPacket)packet).getSourceId());
				}
				if (state.isClientMode()) {
					state.setAddressValidated();
				}
				break;
			
			case HANDSHAKE:
				if (!state.isAddressValidated()) {
					state.setAddressValidated();
				}
				break;
				
			default:
			}
			
			for (IFrame frame: frames) {
				FRAME_PROCESSORS[frame.getType().ordinal()].process(this, frame, packet);
			}
			
			PacketNumberSpace space = state.getSpace(packet.getType().encryptionLevel());
			
			space.updateProcessed(packet.getPacketNumber());
			if (ackEliciting) {
				space.acks().add(packet.getPacketNumber(), currentTime);
			}
		}
	}
	
	/**
	 * Called as soon as the full PDU payload is ready to be sent.
	 * <p>
	 * It is called once and always before calling the {@link #sending} method for
	 * all packets in the PDU payload.
	 */
	public void preSending() {	
		currentTime = state.getTime().nanoTime();
	}
	
	/**
	 * Processes a QUIC packet that is part of the PDU payload that is ready to be
	 * sent.
	 * 
	 * @param packet       the QUIC packet
	 * @param ackEliciting tells if the packet is ack eliciting
	 * @param inFlight     tells if the packet counts toward bytes in flight
	 * @param sendingBytes the number of bytes being sent in the packet
	 */
	@SuppressWarnings("unchecked")
	public void sending(IPacket packet, boolean ackEliciting, boolean inFlight, int sendingBytes) {
		List<IFrame> frames = packet.getFrames();
		
		if (frames != null) {
			EncryptionLevel level = packet.getType().encryptionLevel();
			PacketNumberSpace space = state.getSpace(level);
			FlyingFrames fframes = space.frames().getFlying(packet.getPacketNumber());
			
			if (fframes != null) {
				fframes.onSending(
						currentTime, 
						sendingBytes, 
						ackEliciting, 
						inFlight);
				if (ackEliciting) {
					space.updateAckElicitingInFlight(1);
				}
			}
			if (inFlight) {
				if (ackEliciting) {
					space.setLastAckElicitingTime(currentTime);
				}
				state.getCongestion().onPacketSent(sendingBytes);
				state.getLossDetector().setLossDetectionTimer(currentTime, false);
			}
			for (IFrame frame: frames) {
				FRAME_PROCESSORS[frame.getType().ordinal()].sending(this, frame, packet);
			}
		}
	}
}
