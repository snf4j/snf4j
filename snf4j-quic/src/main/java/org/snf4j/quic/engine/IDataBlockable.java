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
 * An object that provides data blocking functionality.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IDataBlockable {
		
	/**
	 * Tells if blocked. In the blocked state no data should be sent to peer.
	 * 
	 * @return {@code true} if blocked
	 */
	boolean isBlocked();
	
	/**
	 * Locks the given amount of data. If called when there is still some locked
	 * data this data should be unlocked automatically.
	 * <p>
	 * The lock data indicate the minimum amount of data that should be available
	 * for this object to be unblocked (i.e. to make {@link #isBlocked()} return
	 * false).
	 * 
	 * @param amount the amount of data to lock
	 */
	void lock(int amount);
	
	/**
	 * Tells if some amount of data has not been unlocked yet.
	 * 
	 * @return {@code true} if some amount of data has not been unlocked yet
	 */
	boolean needUnlock();
	
	/**
	 * Unlocks the data, or does nothing if no data needs to be unlocked.
	 */
	void unlock();
	
	/**
	 * Returns the identifying name.
	 * 
	 * @return the identifying name
	 */
	default String name() {
		return "unknown";
	}
}
