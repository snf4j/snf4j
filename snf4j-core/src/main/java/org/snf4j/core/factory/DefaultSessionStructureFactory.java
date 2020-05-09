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

import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.timer.ITimer;

/**
 * Default factory used to configure the internal structure of the created session.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DefaultSessionStructureFactory implements ISessionStructureFactory {

	/**
	 * Default session's structure factory.
	 */
	public final static DefaultSessionStructureFactory DEFAULT = new DefaultSessionStructureFactory();
	
	/**
	 * Constructs default session's structure factory.
	 */
	protected DefaultSessionStructureFactory() {
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * By default it simply returns {@link DefaultAllocator#DEFAULT}
	 * 
	 * @return the default byte buffer allocator that will have a backing array.
	 */
	@Override
	public IByteBufferAllocator getAllocator() {
		return DefaultAllocator.DEFAULT;
	}

	/**
	 * {@inheritDoc}
	 * @return <code>null</code>, so the session will be responsible for creation of its own copy of attributes
	 */
	@Override
	public ConcurrentMap<Object, Object> getAttributes() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 * @return <code>null</code>, so the default executor should be used by the session
	 */
	@Override
	public Executor getExecutor() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 * @return <code>null</code>, so the session timer will not be supported by the session
	 */
	@Override
	public ITimer getTimer() {
		return null;
	}

}
