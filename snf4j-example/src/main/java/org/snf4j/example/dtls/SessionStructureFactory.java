/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.example.dtls;

import org.snf4j.core.allocator.DefaultDatagramSessionAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.core.timer.ITimer;

public class SessionStructureFactory extends DefaultSessionStructureFactory {
	
	private static final ITimer TIMER = new DefaultTimer(true);
	
	private static final IByteBufferAllocator ALLOCATOR = new DefaultDatagramSessionAllocator(
			false, 
			10,
			SessionConfig.MAX_APPLICATION_DATA_SIZE);
	
	static final SessionStructureFactory INSTANCE = new SessionStructureFactory();
	
	private SessionStructureFactory() {
	}
	
	@Override
	public ITimer getTimer() {
		return TIMER;
	}

	@Override
	public IByteBufferAllocator getAllocator() {
		return ALLOCATOR;
	}	
}
