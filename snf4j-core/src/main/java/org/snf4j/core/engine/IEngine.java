/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2020 SNF4J contributors
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
package org.snf4j.core.engine;

import java.nio.ByteBuffer;

import org.snf4j.core.handler.SessionIncidentException;

/**
 * An <code>interface</code> allowing implementation of customizable protocol
 * engines that can be used to drive {@link org.snf4j.core.EngineStreamSession
 * EngineStreamSession}.
 * <p>
 * The idea behind a protocol engine is to capture all the complex details of a
 * protocol implementation in a way that is decoupled from I/O operations and
 * threading models so the implementation can only concentrate on processing of
 * inbound and outbound byte streams.
 * <p>
 * There are following distinct phases in an engine driven session life cycle.
 * <ol>
 * <li>Session Creation - The session object is being created and associated
 * with an engine implementation.
 * 
 * <li>Engine Initialization - The engine is being initialized. Once the
 * initialization is completed the session is ready for opening and the
 * {@link org.snf4j.core.handler.SessionEvent#CREATED SessionEvent.CREATED}
 * event is sent to the session's handler.
 * 
 * <li>Session Opening - The session is opening. Once the session is opened
 * (i.e. the connection is established with a peer) the
 * {@link org.snf4j.core.handler.SessionEvent#OPENED SessionEvent.OPENED}
 * event is sent to the session's handler and engine can begin the
 * initial handshaking.
 * 
 * <li>Engine Handshaking - The engine starts or skips the handshaking. Once the
 * handshake finishes the session is ready for transferring application data and
 * the {@link org.snf4j.core.handler.SessionEvent#READY SessionEvent.READY}
 * event is sent to the session's handler.
 * 
 * <li>Session Ready - The session is ready for transferring application data.
 * 
 * <li>Engine Closing - The engine is closing. The closing can be initiated by
 * both calling the {@link #closeOutbound()} method or by unwrapping a closing
 * message that was send by the peer.
 * 
 * <li>Session Closed - The session has been closed and the
 * {@link org.snf4j.core.handler.SessionEvent#CLOSED SessionEvent.CLOSED} event
 * is sent to the session's handler.
 * 
 * <li>Session Ending - The seesion is about to end and the
 * {@link org.snf4j.core.handler.SessionEvent#ENDING SessionEvent.ENDING} event
 * is sent to the session's handler.
 * 
 * <li>Engine Cleanup - The engine is being cleaned up. 
 * </ol>
 * <p>
 * <b>Concurrency Notes</b>: Considering the way the interface is used by the
 * framework it is not required, in general, for an implementation to be thread
 * safe however there are two concurrency issues to be aware of:
 * <ol>
 * <li>The methods returning minimum and maximum buffer sizes are the only ones
 * that are not fully controlled by the SNF4J framework. However, each of them is
 * called only once by the constructor of the
 * {@link org.snf4j.core.EngineStreamSession EngineStreamSession} class. All
 * other methods from this interface are always executed in the same thread.
 * <li>Tasks returned by the <code>getDelegatedTask()</code> will be executed in
 * a separate thread what may enforce a thread-safe architecture.
 * </ol>
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IEngine {
	
	/**
	 * Signals that an {@link IEngine} implementation can initialize, if
	 * required, its internal state (e.g. allocate required resources). It is
	 * called during pre-creation phase of an engine driven session (i.e. 
	 * before the event {@link org.snf4j.core.handler.SessionEvent#CREATED 
	 * SessionEvent.CREATED} is signaled to the session handler).
	 */
	void init();
	
	/**
	 * Signals that an {@link IEngine} implementation can cleanup, if
	 * required, its internal state (e.g. release allocated resources). It is
	 * called during post-ending phase of an engine driven session (i.e. 
	 * after the event {@link org.snf4j.core.handler.SessionEvent#ENDING 
	 * SessionEvent.ENDING} is fully processed by the session handler).
	 */
	void cleanup();
	
	/**
	 * Initiates handshaking on this {@link IEngine} implementation. This method
	 * is not called by the SNF4J framework for a initial handshake, as the
	 * initial handshake should be initiated by the <code>wrap()</code> and
	 * <code>unwrap()</code> methods when they are called for the first time.
	 * <p>
	 * This method is never called by the SNF4J framework when another handshake
	 * is still in progress on this engine. Any try to begin a new handshake
	 * from an engine stream session will be silently ignored unless previously
	 * started handshake is finished. The finishing is signaled by the
	 * {@link org.snf4j.core.engine.HandshakeStatus#FINISHED
	 * HandshakeStatus.FINISHED} status returned by the <code>wrap</code> or
	 * <code>unwrap</code> method.
	 * 
	 * @throws Exception
	 *             if a problem was encountered while signaling the beginning of
	 *             a new handshake.
	 */
	void beginHandshake() throws Exception;
	
	/**
	 * Returns whether {@link #wrap(ByteBuffer, ByteBuffer)} and
	 * {@link #wrap(ByteBuffer[], ByteBuffer)} methods will produce 
	 * any more outbound network data.
	 * 
	 * @return <code>true</code> if an {@link IEngine} implementation
	 *         will not produce any more network data
	 */
	boolean isOutboundDone();
	
	/**
	 * Returns whether {@link #unwrap(ByteBuffer, ByteBuffer)} method 
	 * will accept any more inbound network data.
	 * 
	 * @return <code>true</code> if an {@link IEngine} implementation will not
	 *         consume anymore network data
	 * @see closeInbound
	 */
	boolean isInboundDone();
	
	/**
	 * Signals that no more outbound application data will be sent to an
	 * {@link IEngine} implementation.
	 * <p>
	 * This method should be idempotent: if the outbound side has already been
	 * closed, this method should not do anything.
	 * 
	 * @see isOutboundDone
	 */
	void closeOutbound();
	
	/**
	 * Signals that no more inbound network data will be sent to an
	 * {@link IEngine} implementation.
	 * <p>
	 * This method should be idempotent: if the inbound side has already been
	 * closed, this method should not do anything.
	 * 
	 * @throws SessionIncidentException
	 *             if this engine implementation detected an incident that
	 *             should be reported to the associated session's handler (e.g.
	 *             when an engine has not received a proper close message from
	 *             the peer).
	 * @see isInboundDone
	 */
	void closeInbound() throws SessionIncidentException;
	
	/**
	 * Gets the current minimum size of the buffer holding application data. An
	 * {@link IEngine} implementation may use application data (i.e. the
	 * application data wrapped in one network packet) of any size up to and
	 * including the value returned by this method.
	 * 
	 * @return the minimum buffer size
	 */
	int getMinApplicationBufferSize();
	
	/**
	 * Gets the current minimum size of the buffer holding network data. An {@link IEngine} 
	 * implementation may generate network packets of any size up to and including the value
	 * returned by this method.  
	 * 
	 * @return the minimum buffer size
	 */
	int getMinNetworkBufferSize();

	/**
	 * Gets the current maximum size of the buffer holding application data
	 * <p>
	 * This method is only used by the {@link org.snf4j.core.EngineStreamSession
	 * EngineStreamSession} class.
	 * 
	 * @return the maximum buffer size
	 */
	int getMaxApplicationBufferSize();
	
	/**
	 * Gets the current maximum size of the buffer holding network data
	 * <p>
	 * This method is only used by the {@link org.snf4j.core.EngineStreamSession
	 * EngineStreamSession} class.
	 * 
	 * @return the maximum buffer size
	 */
	int getMaxNetworkBufferSize();
	
	/**
	 * Returns the current handshake status for an {@link IEngine}
	 * implementation.
	 * <p>
	 * It should never return the {@link HandshakeStatus#FINISHED FINISHED} status.
	 * 
	 * @return the current handshake status
	 */
	HandshakeStatus getHandshakeStatus();

	/**
	 * Returns a delegated <code>Runnable</code> task for an {@link IEngine}
	 * implementation.
	 * <p>
	 * <code>IEngine</code> operations may require the results of operations
	 * that block, or may take an extended period of time to complete. This
	 * method should be used to obtain a pending <code>Runnable</code> operation
	 * (task). Each task will be assigned a thread to perform the run operation.
	 * The assigned thread will be created by a thread factory configured in the 
	 * selector loop that will handle the session associated with this {@link IEngine}
	 * implementation.  
	 * <p>
	 * A call to this method should return each pending task exactly once.
	 * <p>
	 * Multiple tasks can be run in parallel.
	 * 
	 * @return a pending <code>Runnable,</code> task, or null if none are
	 *         available.
	 */
	Runnable getDelegatedTask();
	
	/**
	 * Attempts to encode outbound application data from a subsequence of data buffers
	 * into outbound network data.
	 * <p>
	 * Depending on the state of an {@link IEngine} implementation, this method
	 * can produce network data without consuming any application data (for
	 * example, it may generate handshake data.)
	 * <p>
	 * If an {@link IEngine} implementation has not yet started its initial
	 * handshake, this method should automatically start the handshake.
	 * 
	 * @param srcs
	 *            an array of <code>ByteBuffers</code> containing the outbound
	 *            application data
	 * @param dst
	 *            a <code>ByteBuffer</code> to hold outbound network data
	 * @return an {@link EngineResult} describing the result of this operation.
	 * @throws Exception
	 *             when a problem occurred. Once it is thrown the associated
	 *             session will be quickly closed
	 */
	IEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws Exception;	

	/**
	 * Attempts to encode outbound application data from a data buffer into outbound 
	 * network data.
	 * <p>
	 * Depending on the state of an {@link IEngine} implementation, this method
	 * can produce network data without consuming any application data (for
	 * example, it may generate handshake data.)
	 * <p>
	 * If an {@link IEngine} implementation has not yet started its initial
	 * handshake, this method should automatically start the handshake.
	 * 
	 * @param src
	 *            a <code>ByteBuffer</code> containing the outbound
	 *            application data
	 * @param dst
	 *            a <code>ByteBuffer</code> to hold outbound network data
	 * @return an {@link EngineResult} describing the result of this operation.
	 * @throws Exception
	 *             when a problem occurred. Once it is thrown the associated
	 *             session will be quickly closed
	 */
	IEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws Exception;

	/**
	 * Attempts to encode inbound network data from a data buffer into inbound 
	 * application data.
	 * <p>
	 * Depending on the state of an {@link IEngine} implementation, this method
	 * can consume network data without producing any application data (for
	 * example, it may generate handshake data.)
	 * <p>
	 * If an {@link IEngine} implementation has not yet started its initial
	 * handshake, this method should automatically start the handshake.
	 * 
	 * @param src
	 *            a <code>ByteBuffer</code> containing the inbound
	 *            application data
	 * @param dst
	 *            a <code>ByteBuffer</code> to hold inbound application data
	 * @return an {@link EngineResult} describing the result of this operation.
	 * @throws Exception
	 *             when a problem occurred. Once it is thrown the associated
	 *             session will be quickly closed
	 */
	IEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws Exception;	
}
