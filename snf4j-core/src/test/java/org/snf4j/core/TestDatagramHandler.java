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
package org.snf4j.core;

import java.net.SocketAddress;

import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.handler.SessionEvent;

public class TestDatagramHandler extends AbstractDatagramHandler {

	public RuntimeException createException;
	
	StringBuilder sb = new StringBuilder();

	volatile TestAllocator allocator;
	
	public TestDatagramHandler() {
	}
	
	public TestDatagramHandler(String name) {
		super(name);
	}
	
	String getEventLog() {
		String s = sb.toString();
		sb.setLength(0);
		return s;
	}
	
	class StructureFactory extends DefaultSessionStructureFactory {
		@Override
		public IByteBufferAllocator getAllocator() {
			if (allocator != null) {
				return allocator;
			}
			return new DefaultAllocator(false);
		}
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new StructureFactory();
	}
	
	@Override
	public void read(Object msg) {
	}

	@Override
	public void read(SocketAddress remoteAddress, Object msg) {
	}

	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case CREATED:
			sb.append("CR|");
			if (createException != null) {
				throw createException; 
			}
			break;
			
		case OPENED:
			sb.append("OP|");
			break;

		case READY:
			sb.append("RD|");
			break;
			
		case CLOSED:
			sb.append("CL|");
			break;
			
		case ENDING:
			sb.append("EN|");
			break;
			
		default:
		}
	}
}
