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
package org.snf4j.quic.engine.crypto;

import java.nio.ByteBuffer;

import org.snf4j.quic.QuicException;

/**
 * An engine responsible for consuming/producing cryptographic message
 * data during the QUIC handshake.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ICryptoEngine {
	
	/**
	 * Tells if this engine has some buffered data that has not been consumed yet.
	 * 
	 * @return {@code true} if there is some data that has not been consumed yet
	 */
	boolean needConsume();
	
	/**
	 * Consumes cryptographic message data from a CRYPTO frame.
	 * 
	 * @param src    the cryptographic message data
	 * @param offset the byte offset in the stream for the cryptographic message
	 *               data
	 * @param length the length of the cryptographic message data
	 * @throws CryptoException if a handshake related error occurred
	 * @throws QuicException   if an error occurred
	 */
	void consume(ByteBuffer src, int offset, int length) throws CryptoException, QuicException;
	
	/**
	 * Tells if this engine is ready to produce some cryptographic message data. If
	 * yes, calling the {@link #produce()} method should return some cryptographic
	 * message data.
	 * 
	 * @return {@code true} if some cryptographic message data is ready to be
	 *         produced
	 */
	boolean needProduce();
	
	/**
	 * Tries to produce some cryptographic message data.
	 * 
	 * @return the produced cryptographic message data or an empty array if no
	 *         cryptographic data was ready to be produced
	 * @throws CryptoException if a handshake related error occurred
	 * @throws QuicException   if an error occurred
	 */
	ProducedCrypto[] produce() throws CryptoException, QuicException;
	
	/**
	 * Starts this engine.
	 * 
	 * @throws CryptoException if a handshake related error occurred
	 * @throws QuicException   if an error occurred
	 */
	void start() throws CryptoException, QuicException;
	
	/**
	 * Cleans up the state of this engine.
	 */
	void cleanup();
	
	/**
	 * Returns a delegated {@code Runnable} task for this engine.
	 * <p>
	 * Some operations performed by this engine may require the results of
	 * operations that block, or may take an extended period of time to complete.
	 * This method is used to obtain a delegated task for performing such
	 * operations. Each task must be assigned a thread (possibly the current) to
	 * perform the run operation. Once the run method returns, the Runnable object
	 * is no longer needed and may be discarded.
	 * 
	 * @return a delegated task, or {@code null} if no task is available.
	 */
	Runnable getTask();
	
	/**
	 * Updates this engine after completion of one or more delegated tasks.
	 * 
	 * @return {@code true} if there are still uncompleted tasks, or {@code false}
	 *         if all tasks have been completed.
	 * @throws CryptoException if a handshake related error occurred
	 * @throws QuicException   if an error occurred
	 */
	boolean updateTasks() throws CryptoException, QuicException;
	
	/**
	 * Tells if a delegated {@code Runnable} task is ready to be returned by the
	 * {@link #getTask()} method.
	 * 
	 * @return {@code true} a delegated task is ready
	 */
	boolean hasTask();
	
	/**
	 * Tells if one or more delegated {@code Runnable} tasks that were returned by
	 * the {@link #getTask()} method are not finished yet.
	 * 
	 * @param onlyUndone {@code true} to only take into account tasks that haven't
	 *                   been run yet, or {@code false} for all tasks (i.e. whose
	 *                   result did not result in an update of the engine state).
	 * @return {@code true} if one or more task are not finished yet.
	 */
	boolean hasRunningTask(boolean onlyUndone);
	
	/**
	 * Tells if the handshake being processed by this engine is done.
	 * 
	 * @return {@code true} if the handshake is done
	 */
	boolean isHandshakeDone();
}
