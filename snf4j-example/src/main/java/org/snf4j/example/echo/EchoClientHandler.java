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
package org.snf4j.example.echo;

import java.nio.ByteBuffer;

import org.snf4j.core.EndingAction;
import org.snf4j.core.allocator.CachingAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.ISessionConfig;

public class EchoClientHandler extends AbstractStreamHandler {

	private static final IByteBufferAllocator ALLOCATOR = new CachingAllocator(true);
	
	@Override
	public void read(Object msg) {
		getSession().writenf(msg);
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case READY:
			ByteBuffer msg = getSession().allocate(EchoClient.SIZE);
			
			for (int i=0; i<msg.capacity(); ++i) {
				msg.put((byte)i);
			}
			msg.flip();
			getSession().writenf(msg);
			break;
		}
	}
	
	@Override
	public void exception(Throwable e) {
		Logger.err(e.toString());
	}
	
	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		Logger.err(incident + ": " + t.toString());
		return true;
	}

	@Override
	public ISessionConfig getConfig() {
		return new SessionConfig(EchoClient.PIPELINE_SIZE)
				.setEndingAction(EndingAction.STOP)
				.setOptimizeDataCopying(true)
				.setMinOutBufferCapacity(EchoClient.SIZE << 1);
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new DefaultSessionStructureFactory() {
			
			@Override
			public IByteBufferAllocator getAllocator() {
				return ALLOCATOR;
			}
		};
	}
}
