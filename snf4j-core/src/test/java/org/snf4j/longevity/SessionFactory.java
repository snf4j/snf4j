/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.longevity;

import java.nio.channels.SocketChannel;

import org.snf4j.core.SSLSession;
import org.snf4j.core.StreamSession;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.handler.IStreamHandler;

public class SessionFactory extends AbstractSessionFactory {

	protected SessionFactory(boolean ssl) {
		super(ssl);
	}
	
	@Override
	public StreamSession create(SocketChannel channel) throws Exception {
		StreamSession s = super.create(channel);
		
		if (s instanceof SSLSession) {
			if (!Utils.randomBoolean(Config.DEFAULT_EXECUTOR_RATIO)) {
				((SSLSession)s).setExecutor(Utils.pool);
			}
		}
		return s;
	}
	
	@Override
	protected IStreamHandler createHandler(SocketChannel channel) {
		return new ServerHandler();
	}

}
