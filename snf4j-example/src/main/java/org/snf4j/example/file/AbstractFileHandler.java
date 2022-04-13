/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.example.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;

import org.snf4j.core.allocator.CachingAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.ssl.SSLEngineBuilder;

public abstract class AbstractFileHandler extends AbstractStreamHandler {

	protected static final int BUFFER_SIZE = 4096;

	protected static final AllocatorMetric METRIC = new AllocatorMetric();
	
	private static final IByteBufferAllocator ALLOCATOR = new CachingAllocator(true, METRIC);

	protected final DefaultSessionConfig config;

	protected RandomAccessFile file;
	
	protected FileChannel fileChannel;
	
	protected long fileLength;

	protected SocketAddress remoteAddress;

	protected long startTime;
		
	AbstractFileHandler(SSLEngineBuilder builder) {
		config = new DefaultSessionConfig()
				.setOptimizeDataCopying(true)
				.setMinInBufferCapacity(BUFFER_SIZE)
				.setMinOutBufferCapacity(BUFFER_SIZE);
		if (builder != null) {
			config.addSSLEngineBuilder(builder);
		}
	}
	
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		
		case OPENED:
			remoteAddress = getSession().getRemoteAddress();
			break;
			
		case CLOSED:
			if (fileChannel != null) {
				try {
					fileChannel.close();
					file.close();
				} catch (IOException e) {
				}
			}
			break;
			
		default:
		}
	}
	
	@Override
	public ISessionConfig getConfig() {
		return config;
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
