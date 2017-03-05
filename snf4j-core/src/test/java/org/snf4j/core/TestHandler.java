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
package org.snf4j.core;

import java.nio.ByteBuffer;

import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;

public class TestHandler extends AbstractStreamHandler {
	
	StringBuilder events;
	
	Boolean exceptionResult;
	
	TestHandler(String name) {
		super(name);
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
}