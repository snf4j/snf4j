/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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

import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;

public class TestSessionPipelineHandler extends AbstractStreamHandler {

	int count;
	
	int size;
	
	int exceptionCount;
	
	RuntimeException exception;
	
	StringBuilder trace = new StringBuilder();
	
	int closePipelineCount = -1;
	
	boolean closePipeline;
	
	Throwable closePipelineCause;
	
	void trace(String s) {
		synchronized (trace) {
			trace.append(s + "|");
		}
	}
	
	String getTrace() {
		String s;
		synchronized (trace) {
			s = trace.toString();
			trace.setLength(0);
		}
		return s;
	}
	
	TestSessionPipelineHandler(int count, int size) {
		this.count = count;
		this.size = size;
	}

	TestSessionPipelineHandler(int size) {
		this.count = -1;
		this.size = size;
	}
	
	TestSessionPipelineHandler exception(int count, RuntimeException e) {
		exceptionCount = count;
		exception = e;
		return this;
	}
	
	TestSessionPipelineHandler closePipeline(Throwable cause) {
		return closePipeline(-1, cause);
	}

	TestSessionPipelineHandler closePipeline(int count, Throwable cause) {
		closePipelineCount = count;
		closePipeline = true;
		closePipelineCause = cause;
		return this;
	}
	
	byte[] bytes() {
		byte[] bytes = new byte[size];
		
		bytes[0] = (byte) count;
		return bytes;
	}
	
	int count(Object msg) {
		byte[] b;
		
		if (msg instanceof ByteBuffer) {
			ByteBuffer bb = ((ByteBuffer) msg).duplicate();
			
			b = new byte[bb.remaining()];
			bb.get(b);
		}
		else {
			b = (byte[]) msg;
		}
		return b[0];
	}
	
	void close() {
		if (closePipeline) {
			trace("CP");
			if (closePipelineCause == null) {
				getSession().getPipeline().markClosed();
			}
			else {
				getSession().getPipeline().markClosed(closePipelineCause);
			}
			getSession().close();
		}
		else {
			trace("C");
			getSession().close();
		}
	}
	
	@Override
	public void read(Object msg) {
		int c = count(msg);

		trace("R" + c);
		if (exception != null && exceptionCount == c) {
			getSession().writenf(bytes());
			throw exception;
		}
		if (closePipelineCount != -1 && closePipelineCount == c) {
			close();
			return;
		}
		if (count == -1) {
			getSession().writenf(msg);
			if (c == 0) {
				close();
			}
		}
		else {
			if (c == 0) {
				close();
			}
			else {
				--count;
				getSession().writenf(bytes());
			}
		}
	}
	
	@Override
	public void event(SessionEvent event) {
		if (event == SessionEvent.READY) {
			if (count >= 0) {
				getSession().writenf(bytes());
			}
		}
	}
	
	@Override
	public void exception(Throwable t) {
		trace("E");
	}

	@Override
	public int available(ByteBuffer buffer, boolean flipped) {
		int len = flipped ? buffer.remaining() : buffer.position();
		
		return len >= size ? size : 0;
	}
	
	@Override
	public int available(byte[] buffer, int off, int len) {
		return len >= size ? size : 0;
	}
		
}
