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

import javax.net.ssl.SSLEngine;

import org.snf4j.core.EndingAction;
import org.snf4j.core.codec.ICodecExecutor;

/**
 * A configuration for associated session.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISessionConfig {

	/**
	 * Gets the minimum capacity for the session's input buffer. The current
	 * capacity is adjusted automatically and will never be decreased below
	 * this value.
	 * <p>
	 * This value also determines the initial capacity of the input buffer.
	 * <p>
	 * Changing it once an associated session is created has no effect for 
	 * the session.
	 * 
	 * @return the minimum capacity in bytes.
	 */
	int getMinInBufferCapacity();
	
	/**
	 * Gets the maximum capacity for the session's input buffer. The current
	 * capacity is adjusted automatically and will never be increased above
	 * this value. 
	 * <p>
	 * Changing it once an associated session is created has no effect for 
	 * the session.
	 * 
	 * @return the maximum capacity in bytes.
	 */
	int getMaxInBufferCapacity();
	
	/**
	 * Gets the minimum capacity for the session's output buffer. The current
	 * capacity is adjusted automatically and will never be decreased below
	 * this value.
	 * <p>
	 * This configuration parameter is only valid for stream-oriented 
	 * transmissions.
	 * <p>
	 * Changing it once an associated session is created has no effect for 
	 * the session.
	 *  
	 * @return the minimum capacity in bytes.
	 */
	int getMinOutBufferCapacity();
	
	/**
	 * Gets the interval in milliseconds between each throughput calculation.
	 * 
	 * @return the interval in milliseconds.
	 */
	long getThroughputCalculationInterval();
	
	/**
	 * Determines if possibly incomplete datagrams should be ignored.
	 * <p>
	 * If the size of received datagram is bigger than the size of session's
	 * input buffer the remainder of the datagram is silently discarded by the
	 * NIO API. In some protocols (e.g. TFTP) such situations would lead to
	 * erroneous processing of the received datagram.
	 * <p>
	 * To solve such situations we can ignore each datagram that fully filled up
	 * the session's input buffer. Considering that after detecting such
	 * datagram the size of the input buffer will be extended up to the max
	 * limit there is a big chance that there will be enough room in the buffer
	 * when the ignored (i.e. lost) datagram will be retransmitted by the remote
	 * end-point.
	 * <p>
	 * Changing it once an associated session is created has no effect for 
	 * the session.
	 * 
	 * @return <code>true</code> if possibly incomplete datagrams should be
	 *         ignored
	 * @see java.nio.channels.DatagramChannel#read
	 */
	boolean ignorePossiblyIncompleteDatagrams();
	
	/**
	 * Determines if the processing of data should be optimized to reduce data
	 * copying between byte buffers.
	 * <p>
	 * Setting this parameter to <code>true</code> instructs the write and send
	 * methods that the passed data will be no longer used by the caller. In such
	 * situation the write and send methods may, if possible, eliminate not needed
	 * copy operations what can improve the performance.
	 * <p>
	 * In addition, setting this parameter to <code>true</code> when the allocator
	 * associated with the session supports releasing of no longer used buffers will
	 * also cause data copy optimization between session's internal byte buffers. In
	 * such situation user's implementation is responsible for releasing of no
	 * longer used buffers that was passed to session handlers or codecs by calling
	 * {@link ISession#release}.
	 * 
	 * @return <code>true</code> if the data copy optimization should be enabled
	 */
	boolean optimizeDataCopying();
	
	/**
	 * Gets the action that should be performed by the selector
	 * loop after ending of the associated session.
	 * 
	 * @return the ending action
	 */
	EndingAction getEndingAction();
	
	/**
	 * Creates a new SSLEngine for the SSL/DTLS session.
	 * 
	 * @param clientMode
	 *            <code>true</code> if the engine should start its handshaking
	 *            in client mode
	 * @return the SSLEngine object
	 * @throws SSLEngineCreateException
	 *             when the SSL engine could not be created
	 */
	SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException;
	
	/**
	 * Creates a new SSLEngine for the SSL/DTLS session with known remote peer. It 
	 * is called when the remote address is known at the moment a session object is 
	 * created. 
	 * 
	 * @param remoteAddress the address of the remote peer
	 * @param clientMode
	 *            <code>true</code> if the engine should start its handshaking
	 *            in client mode
	 * @return the SSLEngine object
	 * @throws SSLEngineCreateException
	 *             when the SSL engine could not be created
	 */
	SSLEngine createSSLEngine(SocketAddress remoteAddress, boolean clientMode) throws SSLEngineCreateException;
	
	/**
	 * Gets the ratio that is used to calculate the maximum size of the SSL
	 * application buffers. The base for the calculation is the maximum SSL 
	 * application packet size that is determined by the <code>SSLEngine</code>. 
	 * <p>
	 * As the maximum size of the SSL application buffers cannot be less than
	 * the maximum SSL application packet size all values less than 100 will
	 * be silently ignored and treated as 100.
	 * 
	 * @return the ratio
	 * @see javax.net.ssl.SSLSession#getApplicationBufferSize()
	 */
	int getMaxSSLApplicationBufferSizeRatio();
	
	/**
	 * Gets the ratio that is used to calculate the maximum size of the SSL
	 * network buffers. The base for the calculation is the maximum SSL 
	 * network packet size that is determined by the <code>SSLEngine</code>.
	 * <p>
	 * As the maximum size of the SSL network buffers cannot be less than
	 * the maximum SSL network packet size all values less than 100 will
	 * be silently ignored and treated as 100.
	 * 
	 * @return the ratio
	 * @see javax.net.ssl.SSLSession#getPacketBufferSize()
	 */
	int getMaxSSLNetworkBufferSizeRatio();
	
	/**
	 * Determines if the SNF4J framework should wait for the peer's corresponding
	 * close message in situation when the closing was initiated by calling the
	 * {@link org.snf4j.core.EngineStreamSession#close
	 * EngineStreamSession.close()} or
	 * {@link org.snf4j.core.EngineStreamSession#quickClose
	 * EngineStreamSession.quickClose()} method.
	 * <p>
	 * NOTE: Setting this value to <code>true</code> may delay the closing of
	 * the connection for an indefinite period of time. Usually it is not
	 * required to wait for the close message (e.g. the TLS specification (RFC
	 * 2246))
	 * 
	 * @return <code>true</code> to wait for the peer's corresponding close
	 *         message.
	 */
	boolean waitForInboundCloseMessage();
	
	/**
	 * Creates a new codec executor that will be responsible for decoding and
	 * encoding data read/received and written/send in the associated session.
	 * 
	 * @return the codec executor, or <code>null</code> if decoding and encoding
	 *         is not required.
	 */
	ICodecExecutor createCodecExecutor();
	
	/**
	 * Determines how long the SNF4J framework should wait for completion of the
	 * handshake phase for engine-driven sessions. If the handshake does not
	 * complete within the timeout interval the
	 * {@link org.snf4j.core.handler.HandshakeTimeoutException
	 * HandshakeTimeoutException} will be thrown and the session will be quickly
	 * closed.
	 * 
	 * @return the timeout in milliseconds
	 */
	long getEngineHandshakeTimeout();
	
	/**
	 * Determines how long the {@link org.snf4j.core.DatagramServerHandler
	 * DatagramServerHandler} should block re-opening of a new session for the
	 * remote peer which session has just been closed. The purpose of it is to
	 * prevent opening of a new session as a result of receiving some delayed or
	 * retransmitted datagrams.
	 * 
	 * @return the period in milliseconds or zero if the re-opening should be
	 *         allowed immediately
	 */
	long getDatagramServerSessionNoReopenPeriod();
	
	/**
	 * Returns the maximum loop count for write operations performed by the selector
	 * loop before returning control to the NIO selector or to other channel ready
	 * for I/O operations. The write operations are performed in the loop until the
	 * channel's write method returns a non-zero value and there is still pending
	 * data to be written or the maximum loop count is reached.
	 * <p>
	 * It improves write throughput depending on the platform that JVM runs on.
	 * 
	 * @return the maximum loop count for write operations
	 */
	int getMaxWriteSpinCount();
}
