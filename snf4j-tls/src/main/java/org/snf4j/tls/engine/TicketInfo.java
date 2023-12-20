/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.engine;

import org.snf4j.tls.Args;

public class TicketInfo {

	public final static TicketInfo NO_MAX_EARLY_DATA_SIZE = new TicketInfo();

	private final long maxEarlyDataSize;

	public TicketInfo() {
		this.maxEarlyDataSize = -1;
	}

	public TicketInfo(long maxEarlyDataSize) {
		Args.checkRange(maxEarlyDataSize, 0L, 0xffff_ffffL, "maxEarlyDataSize");
		this.maxEarlyDataSize = maxEarlyDataSize;
	}

	/**
	 * Gets the maximum size of the early data that can be sent when using a ticket
	 * created from this ticket info. For creating tickets not supporting the early
	 * data this method should return {@code -1}.
	 * 
	 * @return the maximum early data size or {@code -1}
	 */
	public long getMaxEarlyDataSize() {
		return maxEarlyDataSize;
	}

}
