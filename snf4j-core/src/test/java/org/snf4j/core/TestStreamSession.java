/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2021 SNF4J contributors
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
package org.snf4j.core;

import java.nio.ByteBuffer;

import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IStreamHandler;

public class TestStreamSession extends StreamSession {

	public volatile boolean getOutBuffersException;

	public volatile int getOutBuffersExceptionDelay;
	
	public volatile boolean getInBufferException;

	public volatile boolean copyInBufferException;
	
	public volatile StoppingType closeType;
	
	public TestStreamSession(String name, IStreamHandler handler) {
		super(name, handler);
	}

	public TestStreamSession(IStreamHandler handler) {
		super(handler);
	}
	
	@Override
	ByteBuffer[] getOutBuffers() {
		if (getOutBuffersException) {
			if (getOutBuffersExceptionDelay == 0) {
				throw new IllegalStateException();
			}
			else {
				getOutBuffersExceptionDelay--;
			}
		}
		return super.getOutBuffers();
	}

	@Override
	ByteBuffer getInBuffer() {
		if (getInBufferException) {
			throw new IllegalStateException();
		}
		return super.getInBuffer();
	}
	
	@Override
	int copyInBuffer(InternalSession oldSession) {
		if (copyInBufferException) {
			throw new IllegalStateException();
		}
		return super.copyInBuffer(oldSession);
	}
	
	public IFuture<Void> execute(Runnable task) {
		return super.execute(task);
	}
	
	public void executenf(Runnable task) {
		super.executenf(task);
	}
	
	@Override
	public void close() {
		closeType = StoppingType.GENTLE;
		super.close();
	}	

	@Override
	public void quickClose() {
		closeType = StoppingType.QUICK;
		super.quickClose();
	}	

	@Override
	public void dirtyClose() {
		closeType = StoppingType.DIRTY;
		super.dirtyClose();
	}	
}
