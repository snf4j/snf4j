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
package org.snf4j.core.pool;

import java.nio.channels.SelectableChannel;

import org.snf4j.core.SelectorLoop;

/**
 * A base pool for the selector loops
 */
public interface ISelectorLoopPool {

	/**
	 * Gently stops all selector loops in this pool. This operation is
	 * asynchronous.
	 * 
	 * @see org.snf4j.core.SelectorLoop#stop
	 */
	void stop();
	
	/**
	 * Quickly stops all selector loops in this pool. This operation is
	 * asynchronous.
	 * 
	 * @see org.snf4j.core.SelectorLoop#quickStop
	 */
	void quickStop();
	
	/**
	 * Gets the best selector loop from this pool that should be used to process
	 * given channel.
	 * 
	 * @param channel
	 *            the channel that needs to be processed by a selector loop form
	 *            this pool
	 * @return the best selector loop, or <code>null</code> if none could be returned
	 */
	SelectorLoop getLoop(SelectableChannel channel);

	/**
	 * Notifies the pool that given selector loop is now processing different
	 * number of channels. This method can be used to reorganize the internal
	 * structure of the pool to optimize the way the best selector loop is
	 * chosen by the <code>getLoop</code> method.
	 * 
	 * @param loop
	 *            the selector loop
	 * @param newSize
	 *            the new number of channels processed now by the selector loop
	 * @param prevSize
	 *            the previous number of channels processed by the selector loop
	 */
	void update(SelectorLoop loop, int newSize, int prevSize);
	
	/**
	 * Waits at most <code>millis</code> milliseconds for all selector loop's
	 * threads to die.
	 * 
	 * @param millis
	 *            the time to wait in milliseconds
	 * @throws InterruptedException
	 *             if any thread has interrupted the current thread
	 * @throws IllegalArgumentException
	 *             if the value of <code>millis</code> is negative
	 * @return <code>true</code> if the selector loop's thread died
	 */
	boolean join(long millis) throws InterruptedException;
	
	/**
	 * Waits for all selector loop's threads to die.
	 * 
	 * @throws InterruptedException
	 *             if any thread has interrupted the current thread
	 */
	void join() throws InterruptedException;
	
}
