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
	 */
	public void updateAcked(long pn) throws QuicException {
		frames.ack(pn);
		if (pn > largestAcked) {
			largestAcked = pn;
		}
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
	 * Returns the maximum packet number that was acknowledged by peer in this
	 * packet number space.
	 * 
	 * @return the maximum packet number that was acknowledged
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
	 * @return the maximum packet number that was successfully processed
	 */
	public long getLargestProcessed() {
		return largestProcessed;
	}
	
	
}
