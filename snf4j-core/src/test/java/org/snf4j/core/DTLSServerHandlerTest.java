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
package org.snf4j.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.junit.Test;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimer;

public class DTLSServerHandlerTest {
	
	@Test
	public void testConstructor() {
		IDatagramHandlerFactory hf = new IDatagramHandlerFactory() {

			@Override
			public IDatagramHandler create(SocketAddress remoteAddress) {
				return null;
			}
			
		};
		ISessionStructureFactory f = new ISessionStructureFactory() {

			@Override
			public IByteBufferAllocator getAllocator() {
				return null;
			}

			@Override
			public ConcurrentMap<Object, Object> getAttributes() {
				return null;
			}

			@Override
			public Executor getExecutor() {
				return null;
			}
			
			@Override
			public ITimer getTimer() {
				return null;
			}

			@Override
			public ITimeoutModel getTimeoutModel() {
				return null;
			}
			
		};
		DefaultSessionConfig c = new DefaultSessionConfig();
		
		DTLSServerHandler h = new DTLSServerHandler(hf);
		assertTrue(h.getFactory() == DefaultSessionStructureFactory.DEFAULT);
		assertTrue(h.getConfig().getClass() == DefaultSessionConfig.class);
		assertFalse(h.getConfig() == c);
		
		h = new DTLSServerHandler(hf, c);
		assertTrue(h.getFactory() == DefaultSessionStructureFactory.DEFAULT);
		assertTrue(h.getConfig() == c);

		h = new DTLSServerHandler(hf, c, f);
		assertTrue(h.getFactory() == f);
		assertTrue(h.getConfig() == c);
	
	}

}
