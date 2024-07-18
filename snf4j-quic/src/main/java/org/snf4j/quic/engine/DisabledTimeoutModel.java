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

import org.snf4j.core.timer.ITimeoutModel;

/**
 * A timeout model disabling the default retransmission performed by the engine
 * datagram handler. This model should be used by QUIC engines as they provide
 * their own retransmission mechanisms.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DisabledTimeoutModel implements ITimeoutModel {

	/** A stateless instance of this timeout model. */
	public static final ITimeoutModel INSTANCE = new DisabledTimeoutModel();
	
	private DisabledTimeoutModel() {}
	
	@Override
	public long next() {
		return Long.MAX_VALUE;
	}

	@Override
	public void reset() {
	}

	@Override
	public boolean isEnabled() { 
		return false; 
	}
}
