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
package org.snf4j.quic.engine;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.frame.AckFrameBuilder;

/**
 * A packet number space.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class PacketNumberSpace {
	
	/** Types of packet number spaces */
	public enum Type {
		
		/** The Initial packet number space */
		INITIAL, 
		
		/** The Handshake packet number space */
		HANDSHAKE, 

		/** The Application Data packet number space */
		APPLICATION_DATA
	};
	
	private final PacketNumberSpace.Type type;
	
	private final FrameManager frames = new FrameManager();
	
	private final AckFrameBuilder acks;
	
	private long next;
	
	private long largestAcked = -1;
	
	private long largestProcessed = -1;
	
	private long lastAckElicitingTime;
	
	private long lossTime;
	
	private int ackElicitingInFlight;
	
	private long ecnCeCount;
	
	/**
	 * Constructs a packet number space of the given type.
	 * 
	 * @param type          the type of the packet number space
	 * @param ackRangeLimit the limit of stored ACK ranges
	 */
	public PacketNumberSpace(Type type, int ackRangeLimit) {
		this.type = type;
		this.acks = new AckFrameBuilder(ackRangeLimit);
	}
	
	/**
	 * Returns the next packet number for this packet number space.
	 * 
	 * @return the next packet number
	 */
	public long next() {
		return next++;
	}
	
	/**
	 * Return the type of this packet number space.
	 * 
	 * @return the type of this packet number space
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Updates the state of this packet number space after receiving of an
	 * acknowledgement.
	 * <p>
	 * This method updates the associated frame manager by calling
	 * {@link FrameManager#ack(long)}
	 * 
	 * @param pn the packet number being acknowledged
	 * @throws QuicException if the packet number has an unexpected value
	 * @return the frames that was acknowledged, or {@code null} if there were no
	 *         frames to acknowledged (were already acknowledged)
	 */
	public FlyingFrames updateAcked(long pn) throws QuicException {
		FlyingFrames fframes = frames.ack(pn);
		if (pn > largestAcked) {
			largestAcked = pn;
		}
		return fframes;
	}
	
	/**
	 * Returns the associated ACK frame builder.
	 * 
	 * @return the ACK frame builder
	 */
	public AckFrameBuilder acks() {
		return acks;
	}
	
	/**
	 * Returns the associated frame manager.
	 * 
	 * @return the frame manager
	 */
	public FrameManager frames() {
		return frames;
	}
	
	/**
	 * Tells if there are frames in this packet number space that are ready to be
	 * sent.
	 * 
	 * @return {@code true} if there are frames ready to be sent
	 */
	public boolean needSend() {
		if (!acks.isEmpty()) {
			return true;
		}
		return !frames.isEmpty();	
	}
	
	/**
	 * Returns the maximum packet number that was acknowledged by peer in this
	 * packet number space.
	 * 
	 * @return the maximum packet number that was acknowledged, or -1 if no packet
	 *         has been acknowledged yet
	 */
	public long getLargestAcked() {
		return largestAcked;
	}

	/**
	 * Updates the state of this packet number space after successfully processing
	 * of a packet.
	 * 
	 * @param pn the packet number of a packet being successfully processed
	 */
	public void updateProcessed(long pn) {
		if (pn > largestProcessed) {
			largestProcessed = pn;
		}
	}
	
	/**
	 * Returns the maximum packet number of a packet that was successfully processed
	 * in this packet number space.
	 * 
	 * @return the maximum packet number that was successfully processed, or -1 if
	 *         no packet has been processed yet
	 */
	public long getLargestProcessed() {
		return largestProcessed;
	}

	/**
	 * Returns the time in nanoseconds the most recent ack-eliciting packet was
	 * sent.
	 * 
	 * @return the time, or 0 if no ack-eliciting packet was sent yet
	 */
	public long getLastAckElicitingTime() {
		return lastAckElicitingTime;
	}

	/**
	 * Sets the time in nanoseconds the most recent ack-eliciting packet was sent.
	 * 
	 * @param lastAckElicitingTime the time
	 */
	public void setLastAckElicitingTime(long lastAckElicitingTime) {
		this.lastAckElicitingTime = lastAckElicitingTime;
	}

	/**
	 * Returns the time in nanoseconds at which the next packet in that packet
	 * number space can be considered lost based on exceeding the reordering window
	 * in time.
	 * 
	 * @return the time, or 0 if it has not been set yet
	 */
	public long getLossTime() {
		return lossTime;
	}

	/**
	 * Sets the time in nanoseconds at which the next packet in that packet number
	 * space can be considered lost based on exceeding the reordering window in
	 * time.
	 * 
	 * @param lossTime the time
	 */
	public void setLossTime(long lossTime) {
		this.lossTime = lossTime;
	}

	/**
	 * Returns the number of ack-eliciting packets currently in flight in this
	 * space.
	 * 
	 * @return the number of ack-eliciting packets currently in flight
	 */
	public int getAckElicitingInFlight() {
		return ackElicitingInFlight;
	}

	/**
	 * Updates the number of ack-eliciting packets currently in flight in this space
	 * by the given increment (positive or negative).
	 * 
	 * @param increment the increment or decrement if negative
	 */
	public void updateAckElicitingInFlight(int increment) {
		ackElicitingInFlight += increment;
	}

	/**
	 * Clears the number of ack-eliciting packets currently in flight in this space.
	 */
	public void clearAckElicitingInFlight() {
		ackElicitingInFlight = 0;
	}
	
	/**
	 * Returns the total number of packets received with the ECN-CE codepoint in
	 * this space
	 * 
	 * @return the total number of packets received with the ECN-CE codepoint
	 */
	public long getEcnCeCount() {
		return ecnCeCount;
	}

	/**
	 * Sets the total number of packets received with the ECN-CE codepoint in this
	 * space
	 * 
	 * @param ecnCeCount the total number of packets received with the ECN-CE
	 *                   codepoint
	 */
	public void setEcnCeCount(long ecnCeCount) {
		this.ecnCeCount = ecnCeCount;
	}
	
}
