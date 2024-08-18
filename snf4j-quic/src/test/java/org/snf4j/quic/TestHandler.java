/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic;

import java.net.SocketAddress;

import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimer;
import org.snf4j.quic.engine.DisabledTimeoutModel;

public class TestHandler  extends AbstractDatagramHandler {
	
	final DefaultSessionConfig config = new DefaultSessionConfig();

	final ITimer timer;
	
	volatile boolean proxyAction;
	
	public TestHandler(ITimer timer) {
		this.timer = timer;
	}

	@Override
	public void read(SocketAddress remoteAddress, Object msg) {
	}

	@Override
	public void read(Object msg) {
	}
	
	@Override
	public ISessionConfig getConfig() {
		return config;
	}

	@Override
	public ISessionStructureFactory getFactory() {
		return new DefaultSessionStructureFactory() {
			
			@Override
			public ITimer getTimer() {
				return timer;
			}
			
			@Override
			public ITimeoutModel getTimeoutModel() {
				return DisabledTimeoutModel.INSTANCE;
			}
		};
	}

}
