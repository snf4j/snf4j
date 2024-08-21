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
package org.snf4j.quic.frame;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * An ACK frame builder. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class AckFrameBuilder {

	private final LinkedList<Range> ranges = new LinkedList<>();
	
	private final int limit;
	
	/**
	 * Constructs an ACK builder with the given limit of stored ACK ranges. Ranges
	 * exceeding the given limit will be removed from this builder.
	 * 
	 * @param limit the limit of stored ACK ranges
	 */
	public AckFrameBuilder(int limit) {
		this.limit = limit;
	}
	
	private void add(int index, Range range) {
		if (index == -1) {
			ranges.add(range);
		}
		else {
			ranges.add(index, range);
		}
		if (ranges.size() > limit) {
			ranges.removeFirst();
		}
	}
	
	/**
	 * Adds a packet number to acknowledge.
	 * 
	 * @param pn          packet number to acknowledge
	 * @param receiveTime the time in nanoseconds the packet with the given packet
	 *                    number was received
	 */
	public void add(long pn, long receiveTime) {
		if (ranges.isEmpty()) {
			ranges.add(new Range(pn, receiveTime));
		}
		else {
			int size = ranges.size();
			int j = size;
			
			for (Iterator<Range> i = ranges.descendingIterator(); i.hasNext(); --j) {
				Range curr = i.next();
				
				if (pn > curr.max) {
					if (pn == curr.max+1) {
						curr.max = pn;
						curr.reciveTime = receiveTime;
					}
					else if (j == size){
						add(-1, new Range(pn, receiveTime));
					}
					else {
						add(j, new Range(pn, receiveTime));
					}
					return;
				}
				else if (pn < curr.min) {
					if (pn == curr.min-1) {
						curr.min = pn;		
						if (i.hasNext()) {
							Range next = i.next();
							
							if (curr.min-1 == next.max) {
								curr.min = next.min;
								i.remove();
							}
						}
						return;
					}
				}
				else {
					return;
				}
			}
			add(0, new Range(pn, receiveTime));
		}
	}
		
	/**
	 * Builds an ACK frame with largest packet numbers stored in this builder
	 * <p>
	 * NOTE: The packet numbers in the returned ACK frame will not be removed from
	 * this builder.
	 * 
	 * @param limit    the maximum number of ACK ranges that should be present in
	 *                 the returned ACK frame
	 * @param ackTime  the time in nanoseconds the acknowledgment is going to be
	 *                 sent
	 * @param exponent the acknowledgment delay exponent
	 * @return the ACK frame
	 */
	public AckFrame build(int limit, long ackTime, int exponent) {
		if (!ranges.isEmpty()) {
			AckRange[] acks = new AckRange[Math.min(limit, ranges.size())];
			Iterator<Range> i = ranges.descendingIterator();
			int j = 0;
			Range range;
			long receiveTime;
			
			range = i.next();
			acks[j++] = new AckRange(range.max, range.min);
			receiveTime = range.reciveTime;
			for (; i.hasNext() && j<acks.length;) {
				range = i.next();
				acks[j++] = new AckRange(range.max, range.min);
			}
			return new AckFrame(acks, (Math.max(ackTime - receiveTime, 0) / 1000) >> exponent);
		}
		return null;
	}
	
	/**
	 * Removes from this builder all packets numbers with values equal or greater
	 * than the given packet number.
	 * <p>
	 * NOTE: As the receive time is only tracked for the maximum packet number in a
	 * range keeping prior to a packet number that is in a range and is greater than
	 * than the minimum value in that range will cause not precise calculation of
	 * the acknowledge delay. To avoid it always use the lowest packet number
	 * acknowledged by previously built ACK frame.
	 * 
	 * @param pn the packet number that determines packet numbers to remove
	 */
	public void keepPriorTo(long pn) {
		for (Iterator<Range> i = ranges.descendingIterator(); i.hasNext();) {
			Range range = i.next();
			
			if (pn <= range.min) {
				i.remove();
				continue;
			}
			else if (pn <= range.max) {
				range.max = pn-1;
			}
			break;
		}
	}
	
	/**
	 * Tells if this builder has nothing to build.
	 * 
	 * @return {@code true} if there is nothing to build
	 */
	public boolean isEmpty() {
		return ranges.isEmpty();
	}
	
	/**
	 * Removes all packets numbers from this builder.
	 */
	public void clear() {
		ranges.clear();
	}
	
	private static class Range {
		
		long min, max, reciveTime;
		
		Range(long val, long reciveTime) {
			min = val;
			max = val;
			this.reciveTime = reciveTime;
		}
	}
}
