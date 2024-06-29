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

import java.util.LinkedList;
import java.util.List;

import org.snf4j.quic.frame.IFrame;

/**
 * A holder for flying frames carried in one packet.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class FlyingFrames {

	private final List<IFrame> frames = new LinkedList<>();
	
	private long sentTime;
	
	private int sentBytes;
	
	private boolean ackEliciting;
	
	private boolean inFlight;
	
	/**
	 * Constructs an empty holder for flying frames.
	 */
	public FlyingFrames() {
	}
	
	/**
	 * Return a list of flying frames.
	 * 
	 * @return a list of flying frames
	 */
	public List<IFrame> getFrames() {
		return frames;
	}
	
	/**
	 * Called on sending of the packet carrying the frames in this holder.
	 * 
	 * @param sentTime     the time the packet was sent
	 * @param sentBytes    the number of bytes sent in the packet
	 * @param ackEliciting determines whether the packet is ack-eliciting
	 * @param inFlight     determines whether the packet counts toward bytes in
	 *                     flight
	 */
	public void onSending(long sentTime, int sentBytes, boolean ackEliciting, boolean inFlight) {
		this.sentTime = sentTime;
		this.sentBytes = sentBytes;
		this.ackEliciting = ackEliciting;
		this.inFlight = inFlight;
	}
	
	/**
	 * Returns the time in nanoseconds the frames in this holder were sent.
	 * <p>
	 * NOTE: Before using this value first check if it is initialized. To do that
	 * check if {@link #getSentBytes()} returns value different than 0.
	 * 
	 * @return the time the frames in this holder were sent
	 */
	public long getSentTime() {
		return sentTime;
	}

	/**
	 * Returns the number of bytes sent in the packet carrying the frames in this
	 * holder.
	 * 
	 * @return the number of bytes sent in the packet, or 0 if the value is not
	 *         initialized yet (the frames were not sent yet)
	 */
	public int getSentBytes() {
		return sentBytes;
	}

	/**
	 * Tells whether the packet carrying the frames in this holder is ack-eliciting.
	 * <p>
	 * NOTE: Before using this value first check if it is initialized. To do that
	 * check if {@link #getSentBytes()} returns value different than 0.
	 * 
	 * @return {@code true} if the packet carrying the frames in this holder is
	 *         ack-eliciting.
	 */
	public boolean isAckEliciting() {
		return ackEliciting;
	}

	/**
	 * Tells whether the packet carrying the frames in this holder counts toward
	 * bytes in flight.
	 * <p>
	 * NOTE: Before using this value first check if it is initialized. To do that
	 * check if {@link #getSentBytes()} returns value different than 0.
	 * 
	 * @return {@code true} if the packet counts toward bytes in flight
	 */
	public boolean isInFlight() {
		return inFlight;
	}

}
