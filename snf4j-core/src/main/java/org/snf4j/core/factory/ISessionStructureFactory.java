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
package org.snf4j.core.factory;

import java.util.concurrent.ConcurrentMap;

import org.snf4j.core.allocator.IByteBufferAllocator;

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
}
