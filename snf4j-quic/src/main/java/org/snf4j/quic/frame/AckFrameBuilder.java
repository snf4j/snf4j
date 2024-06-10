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
	 * @param pn packet number to acknowledge
	 */
	public void add(long pn) {
		if (ranges.isEmpty()) {
			ranges.add(new Range(pn));
		}
		else {
			int size = ranges.size();
			int j = size;
			
			for (Iterator<Range> i = ranges.descendingIterator(); i.hasNext(); --j) {
				Range curr = i.next();
				
				if (pn > curr.max) {
					if (pn == curr.max+1) {
						curr.max = pn;
					}
					else if (j == size){
						add(-1, new Range(pn));
					}
					else {
						add(j, new Range(pn));
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
			add(0, new Range(pn));
		}
	}
		
	/**
	 * Builds an ACK frame with largest packet numbers stored in this builder
	 * <p>
	 * NOTE: The packet numbers in the returned ACK frame will not be removed from
	 * this builder.
	 * 
	 * @param limit the maximum number of ACK ranges that should be present in the
	 *              returned ACK frame
	 * @return the ACK frame
	 */
	public AckFrame build(int limit) {
		if (!ranges.isEmpty()) {
			AckRange[] acks = new AckRange[Math.min(limit, ranges.size())];
			int j = 0;
			
			for (Iterator<Range> i = ranges.descendingIterator(); i.hasNext() && j<acks.length;) {
				Range range = i.next();
				
				acks[j++] = new AckRange(range.max, range.min);
			}
			return new AckFrame(acks, 1000);
		}
		return null;
	}
	
	/**
	 * Removes from this builder all packets numbers with values equal or greater
	 * than the given packet number.
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
	
	static class Range {
		
		long min, max;
		
		Range(long val) {
			min = val;
			max = val;
		}
	}
}
