/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
package org.snf4j.core.session;

import org.snf4j.core.ClosingAction;

/**
 * Default configuration for the session.
 */
public class DefaultSessionConfig implements ISessionConfig {

	/** The minimum size of the input buffer used to read incoming data */
	private int minInBufferCapacity = 2048;
	
	/** The maximum size of the input buffer used to read incoming data */
	private int maxInBufferCapacity = 8192;
	
	/** The minimum size of the output buffer used to write outgoing data */
	private int minOutBufferCapacity = 2048;
	
	/** The interval between each throughput calculation */
	private long throughputCalculationInterval = 3000;
	
	/** Determines if possibly incomplete datagrams should be ignored */
	private boolean ignorePossibleIncompleteDatagrams = true;
	
	private ClosingAction closingAction = ClosingAction.DEFAULT;
	/**
	 * Sets the minimum capacity for the session's input buffer.
	 * 
	 * @param capacity the minimum capacity in bytes
	 */
	public void setMinInBufferCapacity(int capacity) {
		minInBufferCapacity = capacity;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is 2048 
	 */
	@Override
	public int getMinInBufferCapacity() {
		return minInBufferCapacity;
	}

	/**
	 * Sets the maximum capacity for the session's input buffer.
	 * 
	 * @param capacity the maximum capacity in bytes
	 */
	public void setMaxInBufferCapacity(int capacity) {
		maxInBufferCapacity = capacity;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is 8192 
	 */
	@Override
	public int getMaxInBufferCapacity() {
		return maxInBufferCapacity;
	}

	/**
	 * Sets the minimum capacity for the session's output buffer.
	 * 
	 * @param capacity the minimum capacity in bytes
	 */
	public void setMinOutBufferCapacity(int capacity) {
		minOutBufferCapacity = capacity;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is 2048 
	 */
	@Override
	public int getMinOutBufferCapacity() {
		return minOutBufferCapacity;
	}

	/**
	 * Sets the interval in milliseconds between each throughput calculation.
	 * 
	 * @param interval the interval in milliseconds.
	 */
	public void setThroughputCalculationInterval(long interval) {
		throughputCalculationInterval = interval;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is 3000 
	 */
	@Override
	public long getThroughputCalculationInterval() {
		return throughputCalculationInterval;
	}

	/**
	 * Configures the behavior after receiving possibly incomplete datagrams.
	 *  
	 * @param ignore <code>true</code> if possibly incomplete datagrams should 
	 * be ignored.
	 */
	public void setIgnorePossiblyIncompleteDatagrams(boolean ignore) {
		ignorePossibleIncompleteDatagrams = ignore;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is <code>true</code>
	 */
	@Override
	public boolean ignorePossiblyIncompleteDatagrams() {
		return ignorePossibleIncompleteDatagrams;
	}

	/**
	 * Sets the closing action that should be performed by the selector
	 * loop after closing of the associated session.
	 * 
	 * @param action the closing action
	 */
	public void setClosingAction(ClosingAction action) {
		closingAction = action;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is {@link org.snf4j.core.ClosingAction#DEFAULT DEFAUL}
	 */
	@Override
	public ClosingAction getClosingAction() {
		return closingAction;
	}

}
