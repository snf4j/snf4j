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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.frame.IFrame;

/**
 * A QUIC frame manager.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class FrameManager {
	
	private final Queue<IFrame> frames = new LinkedList<>();
	
	private final Map<Long,FlyingFrames> flying = new HashMap<>();
	
	private final List<IFrame> lost = new LinkedList<>();
	
	private long largest = -1;
	
	/**
	 * Adds a new QUIC frame that is ready to be put in a flight.
	 * 
	 * @param frame the QUIC frame
	 */
	public void add(IFrame frame) {
		frames.add(frame);
	}
	
	/**
	 * Retrieves, but not removes, the first QUIC frame that is ready to be put in a
	 * flight.
	 * 
	 * @return the QUIC frame, or {@code null} if no frame is ready to be put in a
	 *         flight
	 */
	public IFrame peek() {
		return frames.peek();
	}
	
	/**
	 * Tells if there are no frames ready to be put in a flight.
	 * 
	 * @return {@code true} if there are no frames ready to be put in a flight
	 */
	public boolean isEmpty() {
		return frames.isEmpty();
	}
	
	/**
	 * Removes all of the frames ready to be put in a flight.
	 */
	public void clear() {
		frames.clear();
	}
	
	/**
	 * Marks the given QUIC frame as put in a flight. If the frame is already
	 * waiting to be put in a flight it will never be retrieved by the
	 * {@link #peek()} method.
	 * 
	 * @param frame the QUIC frame
	 * @param pn    the number of the packet carrying the frame
	 */
	public void fly(IFrame frame, long pn) {
		FlyingFrames fframes = flying.get(pn);

		if (fframes == null) {
			fframes = new FlyingFrames(pn);
			flying.put(pn, fframes);
		}
		
		if (largest < pn) {
			largest = pn;
		}
		
		for (Iterator<IFrame> i = frames.iterator(); i.hasNext();) {
			if (i.next() == frame) {
				i.remove();
				break;
			}
		}
		
		fframes.getFrames().add(frame);
	}
		
	/**
	 * Acknowledges the given packet number.
	 * 
	 * @param pn the packet number to acknowledge
	 * @throws QuicException if an unexpected packet number was passed
	 * @return the frames that were acknowledged, or {@code null} if there were no
	 *         frames to acknowledged (were already acknowledged)
	 */
	public FlyingFrames ack(long pn) throws QuicException {
		if (pn > largest) {
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Unexpected acked packet number");
		}
		return flying.remove(pn);
	}

	/**
	 * Marks frames carried in a packet with the given packet number as lost.
	 * 
	 * @param pn the packet number
	 * @return the frames that where marked as lost , or {@code null} if there were
	 *         no frames to mark as lost
	 */
	public FlyingFrames lost(long pn) {
		FlyingFrames fframes = flying.remove(pn);
		
		if (fframes != null) {
			lost.addAll(fframes.getFrames());
		}
		return fframes;
	}

	/**
	 * Returns frames from all packets currently marked as lost.
	 * <p>
	 * NOTE: The return collection is not a copy so to mark a lost frame as
	 * recovered/retransmitted just remove it form it.
	 * 
	 * @return a collection of the frames
	 */
	public Collection<IFrame> getLost() {
		return lost;
	}
	
	/**
	 * Tells if there are still frames marked as put in a flight.
	 * 
	 * @return {@code true} if there are frames marked as put in a flight
	 */
	public boolean hasFlying() {
		return !flying.isEmpty();
	}
	
	/**
	 * Tells if a packet with the given packet number is marked as put in a flight.
	 * 
	 * @param pn the packet number
	 * @return {@code true} if the packet is marked as put in a flight
	 */
	public boolean isFlying(long pn) {
		return flying.containsKey(pn);
	}
	
	/**
	 * Clears frames from all packets currently marked as put in a flight.
	 */
	public void clearFlying() {
		flying.clear();
	}
	
	/**
	 * Returns frames carried in a packet with the given packet number that is
	 * marked as put in a flight.
	 * 
	 * @param pn the packet number
	 * @return the frames, or {@code null} if the packet is not marked as put in a
	 *         flight
	 */
	public FlyingFrames getFlying(long pn) {
		return flying.get(pn);
	}
		
	/**
	 * Returns frames from all packets currently marked as put in a flight.
	 * 
	 * @return a collection of the frames
	 */
	public Collection<FlyingFrames> getFlying() {
		return flying.values();
	}
	
}
