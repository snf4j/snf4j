/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
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
import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;

public class TestHandler extends AbstractStreamHandler {
	
	StringBuilder events;
	
	Boolean exceptionResult;
	
	List<ByteBuffer> released = new ArrayList<ByteBuffer>();
	
	IByteBufferAllocator allocator = new DefaultAllocator(false) {
		public void release(ByteBuffer buffer) {
			released.add(buffer);
		}
	};
	
	ISessionStructureFactory factory = new DefaultSessionStructureFactory() {
		@Override
		public IByteBufferAllocator getAllocator() {
			return allocator;
		}
	};
	
	TestHandler(String name) {
		super(name);
	}

	public List<ByteBuffer> getReleasedBuffers() {
		return released;
	}
	
	@Override
	public int toRead(ByteBuffer buffer, boolean flipped) {
		return 0;
	}

	@Override
	public void read(byte[] data) {
	}

	@Override
	public int toRead(byte[] buffer, int off, int len) {
		return 0;
	}

	@Override
	public ISessionConfig getConfig() {
		DefaultSessionConfig config = new DefaultSessionConfig();
		
		config.setMinInBufferCapacity(1024);
		config.setMinOutBufferCapacity(1024);
		config.setThroughputCalculationInterval(1000);
		return config;
	}
	
	@Override
	public void event(SessionEvent event) {
		if (events != null) {
			events.append(event.type());
			events.append('|');
		}
		super.event(event);
	}
	
	@Override
	public void event(DataEvent event, long length) {
		if (events != null) {
			events.append(event.type());
			events.append('|');
		}
		super.event(event, length);
	}

	@Override
	public boolean exception(Throwable t) {
		if (events != null) {
			events.append(EventType.EXCEPTION_CAUGHT);
			events.append('|');
		}
		return exceptionResult == null ? super.exception(t) : exceptionResult;
	}
	
	public String getEvents() {
		String s = events.toString();
		events.setLength(0);
		return s;
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return factory;
	}
	
}