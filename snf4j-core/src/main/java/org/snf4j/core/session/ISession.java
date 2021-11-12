/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2021 SNF4J contributors
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
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;

import org.snf4j.core.SelectorLoopStoppingException;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IHandler;

/**
 * A session which represents connection between end-points regardless of transport type. 
 * <p>
 * The implementations provided by the API are thread-safe.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISession {
	
	/**
	 * Gets a unique identifier for this session
	 * 
	 * @return a unique identifier
	 */
	long getId();
	
	/**
	 * Gets the name for this session
	 * 
	 * @return the name
	 */
	String getName();
	
	/**
	 * Gets the handler associated with this session
	 * 
	 * @return the handler
	 */
	IHandler getHandler();
	
	/**
	 * Gets the configuration of this session.
	 * 
	 * @return the configuration
	 */
	ISessionConfig getConfig();
	
	/**
	 * Gets the codec pipeline that is associated with this session.
	 * 
	 * @return the codec pipeline or {@code null} if the session does not
	 *         support it
	 */
	ICodecPipeline getCodecPipeline();
	
	/**
	 * Gets the current state of this session.
	 * 
	 * @return the current state
	 */
	SessionState getState();
	
	/**
	 * Tells if this session is open. It means that the session is able to communicate with the remote end.
	 * <p>
	 * It is equal to:
	 * <pre>
	 * return getState() == SessionState.OPEN
	 * </pre>
	 *  
	 * @return <code>true</code> if the session is open. 
	 */
	boolean isOpen();
	
	/**
	 * Gently closes this session after all pending data waiting for writing are fully flushed. This operation is
	 * asynchronous.
	 * <p>
	 * After returning from this method any consecutive writes will be simply discarded.
	 */
	void close();
	
	/**
	 * Quickly closes this session without flushing any pending data. This operation is
	 * asynchronous.
	 * <p>
	 * After returning from this method any consecutive writes will be simply discarded.
	 */
	void quickClose();
	
	/**
	 * Quickly closes this session without flushing any pending data and without following 
	 * close procedure of an application layer (e.g. SSL/TLS).
	 */
	void dirtyClose();
	
	/**
	 * Returns the thread safe map of the user-defined attributes associated with this session.
	 * 
	 * @return the map of the attributes
	 */
	ConcurrentMap<Object, Object> getAttributes();
	
	/**
	 * Return the local address this session is bound to.
	 * 
	 * @return the local address or <code>null</code> if this session is not
	 *         bound yet
	 * @see java.net.Socket
	 */
	SocketAddress getLocalAddress();

	/**
	 * Returns the remote address to which this session is connected.
	 * 
	 * @return the remote address or <code>null</code> if this session is not
	 *         connected yet
	 * @see java.net.Socket
	 */
	SocketAddress getRemoteAddress();
	
	/**
	 * Suspends read operations for this session.
	 * 
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void suspendRead();

	/**
	 * Suspends write operations for this session. 

	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void suspendWrite();

	/**
	 * Resumes read operations for this session.

	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void resumeRead();

	/**
	 * Resumes write operations for this session.

	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void resumeWrite();
	
	/**
	 * Tells if read operations are suspended for this session.
	 * 
	 * @return <code>true</code> if suspended
	 */
	boolean isReadSuspended();
	
	/**
	 * Tells if write operations are suspended for this session.
	 * 
	 * @return <code>true</code> if suspended
	 */
	boolean isWriteSuspended();
	
	/**
	 * Gets the total number of bytes which were read from this session.
	 * 
	 * @return the total number of bytes
	 */
	long getReadBytes();
	
	/**
	 * Gets the total number of bytes which were written to this session.
	 * 
	 * @return the total number of bytes
	 */
	long getWrittenBytes();
	
	/**
	 * Gets the number of read bytes per second.
	 * 
	 * @return the number of read bytes per second
	 */
	double getReadBytesThroughput();
	
	/**
	 * Gets the number of written bytes per second.
	 * 
	 * @return the number of written bytes per second
	 */
	double getWrittenBytesThroughput();
	
	/**
	 * Gets the session's creation time in milliseconds.
	 * 
	 * @return the session's creation time
	 */
	long getCreationTime();
	
	/**
	 * Gets the time in milliseconds when I/O operation occurred lastly.
	 *  
	 * @return the time of the last I/O operation
	 */
	long getLastIoTime();
	
	/**
	 * Gets the time in milliseconds when read operation occurred lastly.
	 *  
	 * @return the time of the last read operation
	 */
	long getLastReadTime();
	
	/**
	 * Gets the time in milliseconds when write operation occurred lastly.
	 *  
	 * @return the time of the last write operation
	 */
	long getLastWriteTime();
	
	/**
	 * Gets the future that can be use to wait for the completion of the
	 * creation phase.
	 * 
	 * @return the future associated with the creation phase of this session
	 */
	IFuture<Void> getCreateFuture();

	/**
	 * Gets the future that can be use to wait for the completion of the opening
	 * phase.
	 * 
	 * @return the future associated with the opening phase of this session
	 */
	IFuture<Void> getOpenFuture();

	/**
	 * Gets the future that can be use to wait for the completion of the ready
	 * phase.
	 * 
	 * @return the future associated with the opening phase of this session
	 */
	IFuture<Void> getReadyFuture();
	
	/**
	 * Gets the future that can be use to wait for the completion of the closing
	 * phase.
	 * 
	 * @return the future associated with the closing phase of this session
	 */
	IFuture<Void> getCloseFuture();

	/**
	 * Gets the future that can be use to wait for the completion of the ending
	 * phase.
	 * 
	 * @return the future associated with the ending phase of this session
	 */
	IFuture<Void> getEndFuture();
	
	/**
	 * Gets the parent session.
	 * <p>
	 * Some sessions may not implement its own I/O functionalities and use their parent's 
	 * ones instead.
	 * 
	 * @return the parent session or {@code null} if it does not exist.
	 */
	ISession getParent();
	
	/**
	 * Gets the session timer associated with this session.
	 * 
	 * @return the session timer
	 */
	ISessionTimer getTimer();
	
	/**
	 * Tells if the processing of data is optimized to reduce data copying between
	 * byte buffers.
	 * <p>
	 * Checking this flag can be helpful in determining if a buffer allocated by the
	 * {@link #allocate} method need to be released after passing it to the
	 * session's write/send methods. For example, if the returned value is
	 * {@code true} the buffer will be released automatically and so it should not
	 * be released by a user's implementation.
	 * 
	 * @return {@code true} if the processing of data is optimized
	 * @see ISessionConfig#optimizeDataCopying()
	 */
	boolean isDataCopyingOptimized();
	
	/**
	 * Allocates a byte buffer by using the allocator associated with this session.
	 * 
	 * @param capacity minimal required capacity
	 * @return the allocated byte buffer
	 */
	ByteBuffer allocate(int capacity);
	
	/**
	 * Release given byte buffer by the allocator associated with this session.
	 * 
	 * @param buffer the byte buffer being released
	 */
	void release(ByteBuffer buffer);

	/**
	 * Executes a task in the selector-loop's thread this session is registered
	 * with. If this method is called in the selector-loop's thread the task is
	 * executed synchronously.
	 * 
	 * @param task task to be executed in the selector-loop's thread
	 * @throws SelectorLoopStoppingException if selector loop is in the process of
	 *                                       stopping
	 * @throws IllegalArgumentException      if {@code task} is null
	 * @throws IllegalStateException         if the session is not associated with
	 *                                       a selector loop
	 * @return the future associated with the specified task
	 */
	IFuture<Void> execute(Runnable task);
	
	/**
	 * Executes a task in the selector-loop's thread this session is registered
	 * with. If this method is called in the selector-loop's thread the task is
	 * executed synchronously.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize
	 * on a future associated with the specified task. This will save some
	 * resources and may improve performance.
	 * 
	 * @param task task to be executed in the selector-loop's thread
	 * @throws SelectorLoopStoppingException if selector loop is in the process of
	 *                                       stopping
	 * @throws IllegalArgumentException      if {@code task} is null
	 * @throws IllegalStateException         if the session is not associated with
	 *                                       a selector loop
	 */
	void executenf(Runnable task);
}
