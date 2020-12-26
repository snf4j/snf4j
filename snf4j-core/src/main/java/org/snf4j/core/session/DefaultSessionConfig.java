/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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

import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.snf4j.core.EndingAction;
import org.snf4j.core.codec.ICodecExecutor;

/**
 * Default configuration for the session.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
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

	/** Determines if the data copy optimization should be enabled */
	private boolean optimizeDataCopying;

	/** Determines the action that should be performed by the selector 
	 * loop after ending of the associated session. */
	private EndingAction endingAction = EndingAction.DEFAULT;

	private int maxSSLApplicationBufferSizeRatio = 100;
	
	private int maxSSLNetworkBufferSizeRatio = 100;
	
	private boolean waitForInboundCloseMessage; 
	
	private long engineHandshakeTimeout = 60000;
	
	private long datagramServerSessionNoReopenPeriod = 60000;
	
	private int maxWriteSpinCount = 16;
	
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
	 * Configures if the processing of data should be optimized to reduce data
	 * copying between byte buffers.
	 * 
	 * @param optimize <code>true</code> if the processing of data should be
	 *                 optimized
	 * @return this session config object
	 * @see #optimizeDataCopying()
	 */
	public DefaultSessionConfig setOptimizeDataCopying(boolean optimize) {
		optimizeDataCopying = optimize;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is <code>false</code>
	 */
	@Override
	public boolean optimizeDataCopying() {
		return optimizeDataCopying;
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
	 * The default value is {@link org.snf4j.core.EndingAction#DEFAULT DEFAULT}
	 */
	@Override
	public EndingAction getEndingAction() {
		return endingAction;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * By default it returns value returned by <code>SSLContext.getDefault().createSSLEngine()</code>
	 */
	@Override
	public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
		SSLEngine engine;
		try {
			engine = SSLContext.getDefault().createSSLEngine();
		} catch (NoSuchAlgorithmException e) {
			throw new SSLEngineCreateException(e);
		}
		engine.setUseClientMode(clientMode);
		return engine;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * By default it returns value from the {@link #createSSLEngine(boolean)} method.
	 */
	@Override
	public SSLEngine createSSLEngine(SocketAddress remoteAddress, boolean clientMode) throws SSLEngineCreateException {
		return createSSLEngine(clientMode);
	}
	
	/**
	 * Sets the ratio that is used to calculate the maximum size of the SSL
	 * application buffers.
	 * 
	 * @param ratio
	 *            the ratio
	 * @return this session config object
	 * @throws IllegalArgumentException if the ratio is less than 100
	 * @see #getMaxSSLApplicationBufferSizeRatio()
	 */
	public DefaultSessionConfig setMaxSSLApplicationBufferSizeRatio(int ratio) {
		if (ratio < 100) {
			throw new IllegalArgumentException("ratio is less than 100");
		}
		maxSSLApplicationBufferSizeRatio = ratio;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is 100
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
	 * @throws IllegalArgumentException if the ratio is less than 100
	 * @see #getMaxSSLNetworkBufferSizeRatio()
	 */
	public DefaultSessionConfig setMaxSSLNetworkBufferSizeRatio(int ratio) {
		if (ratio < 100) {
			throw new IllegalArgumentException("ratio is less than 100");
		}
		maxSSLNetworkBufferSizeRatio = ratio;
		return this;
	}
	
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is 100
	 */
	@Override
	public int getMaxSSLNetworkBufferSizeRatio() {
		return maxSSLNetworkBufferSizeRatio;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is <code>false</code>
	 */
	@Override
	public boolean waitForInboundCloseMessage() {
		return waitForInboundCloseMessage;
	}

	/**
	 * Configures if the SNF4J framework should wait for the peer's
	 * corresponding close message in situation when the closing was initiated
	 * by calling the {@link org.snf4j.core.EngineStreamSession#close
	 * EngineStreamSession.close()} or
	 * {@link org.snf4j.core.EngineStreamSession#quickClose
	 * EngineStreamSession.quickClose()} method.
	 * 
	 * @param waitForCloseMessage
	 *            <code>true</code> to wait for the peer's corresponding close
	 *            message.
	 * @return this session config object
	 * @see #waitForInboundCloseMessage()
	 */
	public DefaultSessionConfig setWaitForInboundCloseMessage(boolean waitForCloseMessage) {
		waitForInboundCloseMessage = waitForCloseMessage;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is <code>null</code>
	 */
	@Override
	public ICodecExecutor createCodecExecutor() {
		return null;
	}
	
	/**
	 * Configures how long the SNF4J framework should wait for completion of the
	 * handshake phase for engine-driven sessions.
	 * 
	 * @param timeout the timeout in milliseconds
	 * @return this session config object
	 * @see #getEngineHandshakeTimeout()
	 */
	public DefaultSessionConfig setEngineHandshakeTimeout(long timeout) {
		engineHandshakeTimeout = timeout;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is <code>60000</code>
	 */
	@Override
	public long getEngineHandshakeTimeout() {
		return engineHandshakeTimeout;
	}
	
	/**
	 * Configures how long the {@link org.snf4j.core.DatagramServerHandler
	 * DatagramServerHandler} should block re-opening of a new session for the
	 * remote peer which session has just been closed. The purpose of it is to
	 * prevent opening of a new session as a result of receiving some delayed or
	 * retransmitted datagrams.
	 * 
	 * @param period the period in milliseconds or zero if the re-opening should
	 *                 be allowed immediately
	 * @return this session config object
	 * @see #getDatagramServerSessionNoReopenPeriod()
	 */
	public DefaultSessionConfig setDatagramServerSessionNoReopenPeriod(long period) {
		datagramServerSessionNoReopenPeriod = period;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is <code>60000</code>
	 */
	@Override
	public long getDatagramServerSessionNoReopenPeriod() {
		return datagramServerSessionNoReopenPeriod;
	}

	/**
	 * Configures the maximum loop count for write operations performed by the
	 * selector loop before returning control to the NIO selector or to other channel ready
	 * for I/O operations. The write operations are performed in the loop until the
	 * channel's write method returns a non-zero value and there is still pending
	 * data to be written or the maximum loop count is reached.
	 * <p>
	 * It improves write throughput depending on the platform that JVM runs on.
	 * 
	 * @param count the maximum loop count
	 * @return this session config object
	 * @see #getMaxWriteSpinCount()
	 */
	public DefaultSessionConfig setMaxWriteSpinCount(int count) {
		maxWriteSpinCount = count;
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default value is <code>16</code>
	 */
	public int getMaxWriteSpinCount() {
		return maxWriteSpinCount;
	}
	
}
