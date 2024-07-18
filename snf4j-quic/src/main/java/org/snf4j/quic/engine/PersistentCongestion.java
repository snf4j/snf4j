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

/**
 * A detector of the persistent congestion.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
class PersistentCongestion {

	private Item first;
	
	private Item last;
	
	/**
	 * Stores the sent time in nanoseconds of a lost packet.
	 * 
	 * @param sentTime the sent time
	 */
	void lost(long sentTime) {
		Item item = new Item(sentTime);
		
		if (first == null) {
			first = last = item;
		}
		else if (first.sentTime - sentTime > 0){
			item.next = first;
			first = item;
		}
		else if (last.sentTime - sentTime < 0) {
			last.next = item;
			last = item;
		}
		else {
			Item i = first;
			
			if (i.sentTime != sentTime) {
				Item prev;
				
				for (;;) {
					prev = i;
					i = i.next;
					if (i.sentTime == sentTime) {
						break;
					}
					else if (i.sentTime - sentTime > 0) {
						item.next = i;
						prev.next = item;	
						return;
					}
				}
			}
		}
	}

	/**
	 * Updates this detector after receiving of an acknowledgement. After calling it
	 * all stored times that equal or are before the given sent time will be
	 * removed.
	 * 
	 * @param sentTime the sent time in nanoseconds of the packet being acknowledged
	 */
	void acked(long sentTime) {
		if (first != null) {
			if (last.sentTime - sentTime <= 0) {
				first = last = null;
			}
			else {
				Item i = first;
				Item max = null;
				
				for (;;) {
					if (i.sentTime - sentTime <= 0) {
						max = i;
					}
					else if (max != null) {
						first = max.next;
						break;
					}
					else {
						break;
					}
					i = i.next;
				}
			}
		}
	}
	
	/**
	 * Tells if there is enough data stored to detect the persistent congestion.
	 * 
	 * @return {@code true} if detection is possible
	 */
	boolean isDetectable() {
		return first != last;
	}

	/**
	 * Tells if the persistent congestion occurred for the given persistent
	 * congestion duration in nanoseconds.
	 * 
	 * @param duration the persistent congestion duration
	 * @return {@code true} if the persistent congestion occurred
	 */
	boolean detect(long duration) {
		if (first != null) {
			if (last.sentTime - first.sentTime > duration) {
				first = last = null;
				return true;
			}
		}
		return false;
	}
	
	static class Item {
		
		final long sentTime;
		
		Item next;
		
		Item(long time) {
			this.sentTime = time;
		}
	}
}
