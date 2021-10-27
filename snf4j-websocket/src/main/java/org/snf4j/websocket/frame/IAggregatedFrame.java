/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.websocket.frame;

import java.util.List;

/**
 * An aggregated Web Socket frame.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IAggregatedFrame {
	
	/**
	 * Adds a new payload data fragment for future aggregation.
	 * 
	 * @param payload the new payload data fragment
	 */
	void addFragment(byte[] payload);
	
	/**
	 * Returns all payload data fragments already stored in this frame.
	 * <p>
	 * NOTE: It is expected that, due to possible optimizations, the number of
	 * return fragments may change after executing of the {@link #getPayload()}
	 * method. However, in such a case, the aggregated payload data should
	 * still be the same.
	 * 
	 * @return the payload data fragments
	 */
	List<byte[]> getFragments();
	
	/**
	 * Returns the total length of all payload data fragments already stored in this frame.
	 * 
	 * @return the total length of all payload data fragments
	 */
	int getPayloadLength();
	
	/**
	 * Returns the aggregated payload data from all payload data fragments already
	 * stored in this frame.
	 * 
	 * @return the aggregated payload data
	 */
	byte[] getPayload();
}
