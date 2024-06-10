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
	
	private Queue<IFrame> frames;
	
	private Map<Long,List<IFrame>> flights;
	
	private long largest = -1;
	
	/**
	 * Adds a new QUIC frame that is ready to be put in a flight.
	 * 
	 * @param frame the QUIC frame
	 */
	public void add(IFrame frame) {
		if (frames == null) {
			frames = new LinkedList<>();
		}
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
		if (frames != null) {
			return frames.peek();
		}
		return null;
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
		List<IFrame> list;
		
		if (flights == null) {
			flights = new HashMap<>();
			list = new LinkedList<>();
			flights.put(pn, list);
		}
		else {
			list = flights.get(pn);
			if (list == null) {
				list = new LinkedList<>();
				flights.put(pn, list);
			}
		}
		
		if (largest < pn) {
			largest = pn;
		}
		
		if (frames != null) {
			for (Iterator<IFrame> i = frames.iterator(); i.hasNext();) {
				if (i.next() == frame) {
					i.remove();
					break;
				}
			}
		}
		
		list.add(frame);
	}
	
	/**
	 * Tells if all frames marked as put in a flight have been already acknowledged.
	 * 
	 * @return {@code true} if all frames marked as put in a flight have been
	 *         acknowledged
	 */
	public boolean allAcked() {
		return flights == null || flights.isEmpty();
	}
	
	/**
	 * Acknowledges the given packet number.
	 * 
	 * @param pn the packet number to acknowledge
	 * @throws QuicException if an unexpected packet number was passed
	 */
	public void ack(long pn) throws QuicException {
		if (pn > largest) {
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Unexpected acked packet number");
		}
		flights.remove(pn);
	}
}
