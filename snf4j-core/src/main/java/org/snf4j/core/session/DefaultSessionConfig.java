/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
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

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.snf4j.core.EndingAction;

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

	/** Determines if the session object can own the data passed to 
	 * the write methods */
	private boolean canOwnDataPassedToWriteAndSendMethods;

	/** Determines the action that should be performed by the selector 
	 * loop after ending of the associated session. */
	private EndingAction endingAction = EndingAction.DEFAULT;

	private int maxSSLApplicationBufferSizeRatio = 1;
	
	private int maxSSLNetworkBufferSizeRatio = 1;
	
	/**
	 * Sets the minimum capacity for the session's input buffer.
	 * 
	 * @param capacity the minimum capacity in bytes
	 * @return this session config object
	 */
	public DefaultSessionConfig setMinInBufferCapacity(int capacity) {
		minInBufferCapacity = capacity;
		return this;
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
	 * @return this session config object
	 */
	public DefaultSessionConfig setMaxInBufferCapacity(int capacity) {
		maxInBufferCapacity = capacity;
		return this;
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
	 * @return this session config object
	 */
	public DefaultSessionConfig setMinOutBufferCapacity(int capacity) {
		minOutBufferCapacity = capacity;
		return this;
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
	 * @return this session config object
	 */
	public DefaultSessionConfig setThroughputCalculationInterval(long interval) {
		throughputCalculationInterval = interval;
		return this;
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
	 * @param ignore <code>true</code> if possibly incomplete datagrams should be
	 *               ignored.
	 * @return this session config object
	 */
	public DefaultSessionConfig setIgnorePossiblyIncompleteDatagrams(boolean ignore) {
		ignorePossibleIncompleteDatagrams = ignore;
		return this;
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
	 * 
	 * Configures if the session object can own the data (i.e. byte arrays or byte
	 * buffers) passed to the write and send methods.
	 * 
	 * @param canOwnData <code>true</code> if the session object can own the data
	 *                   passed in the write methods.
	 * @return this session config object
	 * @see #canOwnDataPassedToWriteAndSendMethods()
	 */
	public DefaultSessionConfig setCanOwnDataPassedToWriteAndSendMethods(boolean canOwnData) {
		canOwnDataPassedToWriteAndSendMethods = canOwnData;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is <code>false</code>
	 */
	@Override
	public boolean canOwnDataPassedToWriteAndSendMethods() {
		return canOwnDataPassedToWriteAndSendMethods;
	}

	/**
	 * Sets an action that should be performed by the selector loop after ending of
	 * the associated session.
	 * 
	 * @param action en ending action
	 * @return this session config object
	 */
	public DefaultSessionConfig setEndingAction(EndingAction action) {
		endingAction = action;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is {@link org.snf4j.core.EndingAction#DEFAULT DEFAUL}
	 */
	@Override
	public EndingAction getEndingAction() {
		return endingAction;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * By default it returns value returned by <code>SSLContext.getDefault().createSSLEngine()</code>
	 * @throws NoSuchAlgorithmException 
	 */
	public SSLEngine createSSLEngine() throws NoSuchAlgorithmException {
		return SSLContext.getDefault().createSSLEngine();
	}
	
	/**
	 * Sets the ratio that is used to calculate the maximum size of the SSL
	 * application buffers.
	 * 
	 * @param ratio
	 *            the ratio
	 * @return this session config object
	 * @see #getMaxSSLApplicationBufferSizeRatio()
	 */
	public DefaultSessionConfig setMaxSSLApplicationBufferSizeRatio(int ratio) {
		maxSSLApplicationBufferSizeRatio = ratio;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is 1
	 */
	@Override
	public int getMaxSSLApplicationBufferSizeRatio() {
		return maxSSLApplicationBufferSizeRatio;
	}

	/**
	 * Sets the ratio that is used to calculate the maximum size of the SSL
	 * network buffers.
	 * 
	 * @param ratio
	 *            the ratio
	 * @return this session config object
	 * @see #getMaxSSLNetworkBufferSizeRatio()
	 */
	public DefaultSessionConfig setMaxSSLNetworkBufferSizeRatio(int ratio) {
		maxSSLNetworkBufferSizeRatio = ratio;
		return this;
	}
	
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is 1
	 */
	@Override
	public int getMaxSSLNetworkBufferSizeRatio() {
		return maxSSLNetworkBufferSizeRatio;
	}

}
