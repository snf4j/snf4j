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
package org.snf4j.core.factory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimer;

/**
 * Factory used to configure the internal structure of the created session
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 *
 */
public interface ISessionStructureFactory {
	/**
	 * Gets a byte buffer allocator that will used by the created session.
	 * 
	 * @return a byte buffer allocator
	 */
	IByteBufferAllocator getAllocator();
	
	/**
	 * Gets an attributes that will be used by the created session.
	 * 
	 * @return an attributes, or <code>null</code> to configure the session
	 *         with its own empty copy of attributes
	 */
	ConcurrentMap<Object,Object> getAttributes();
	
	/**
	 * Returns an executor that will be used by engine-driven sessions to execute
	 * delegated tasks required by the sessions to complete operations that block, or
	 * may take an extended period of time to complete.
	 * 
	 * @return an executor, or {@code null} if the session should use the default
	 *         executor
	 */
	Executor getExecutor();
	
	/**
	 * Returns a timer implementation that will be used by the session timer to
	 * schedule events and tasks.
	 * 
	 * @return a timer, or {@code null} if the session time shouldn't be
	 *         supported by the session.
	 */
	ITimer getTimer();
	
	/**
	 * Returns a timeout model that will be used by engine-driven datagram sessions
	 * to retransmit lost packets.
	 * 
	 * @return the timeout model, or {@code null} if the session should use the default
	 *         timeout model.
	 */
	ITimeoutModel getTimeoutModel();
}
