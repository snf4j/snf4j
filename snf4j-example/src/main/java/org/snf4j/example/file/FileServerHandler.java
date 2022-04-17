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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.snf4j.core.ByteBufferHolder;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ssl.SSLEngineBuilder;

class FileServerHandler extends AbstractFileHandler {

	private final static int DEFAULT_HALF2_SIZE = FileServer.BUFFER_COUNT / 2;

	private final static int DEFAULT_HALF1_SIZE = FileServer.BUFFER_COUNT - DEFAULT_HALF2_SIZE;
	
	private final StringBuilder path = new StringBuilder(256);
	
	private int half1Size = DEFAULT_HALF1_SIZE;
	
	private int half2Size = DEFAULT_HALF2_SIZE;
	
	private IFuture<Void> fullProgress;
	
	private IFuture<Void> halfProgress;
	
	FileServerHandler(SSLEngineBuilder builder) {
		super(builder);
		config.setMinInBufferCapacity(FileServer.BUFFER_SIZE)
			.setMinOutBufferCapacity(FileServer.BUFFER_SIZE);
	}
	
	@Override
	public void read(Object msg) {
		if (fileChannel != null) {
			getSession().release((ByteBuffer)msg);
			return;
		}
		
		byte[] data = new byte[((ByteBuffer)msg).remaining()];
		
		((ByteBuffer)msg).get(data);
		getSession().release((ByteBuffer)msg);
		
		for (int i=0; i<data.length; ++i) {
			if (data[i] == 0) {
				path.append(new String(data, 0, i, StandardCharsets.UTF_8));
				
				try {
					file = new RandomAccessFile(path.toString(), "r");
					fileChannel = file.getChannel();
					info("Uploading " + path);
					startTime = System.currentTimeMillis();
					progress(true);
				} catch (FileNotFoundException e) {
					err(e);
					getSession().close();
				}
				return;
			}
		}
		path.append(new String(data, StandardCharsets.UTF_8));
	}
	
	@Override
	public void event(SessionEvent event) {
		if (event == SessionEvent.CLOSED) {
			if (file != null) {
				long time = System.currentTimeMillis()-startTime;
				info(String.format("Uploading of %,d bytes completed in %,d msec (%,d bytes/sec)", 
						fileLength, 
						time, 
						(long)(fileLength*1000/time)));
				info(String.format("Allocator statistics: phisical allocations: %d (total %,d bytes), total allocations (from cache): %,d", 
						METRIC.getAllocatedCount(),
						METRIC.getAllocatedSize(),
						METRIC.getAllocatingCount()));
			}
		}
		super.event(event);
	}
	
	@Override
	public void event(DataEvent event, long length) {
		if (event == DataEvent.SENT) {
			if (halfProgress != null && halfProgress.isSuccessful()) {
				if (fullProgress.isSuccessful()) {
					half1Size = DEFAULT_HALF1_SIZE;
					half2Size = DEFAULT_HALF2_SIZE;
					halfProgress = fullProgress = null;
					progress(true);
				}
				else {
					int tmpCount = half1Size;

					half1Size = half2Size;
					half2Size = tmpCount;
					halfProgress = fullProgress;
					progress(false);
				}
			}
		}
	}
	
	private void progress(boolean full) {
		int count = full ? FileServer.BUFFER_COUNT : half2Size;
		ByteBufferHolder holder = new ByteBufferHolder(count);
		boolean eof = false;
		
		try {
			for (int i=0; i<count; ++i) {
				ByteBuffer buf = getSession().allocate(BUFFER_SIZE);
				int bytes = fileChannel.read(buf);
				
				if (bytes > 0) {
					fileLength += bytes;
					buf.flip();
					holder.add(buf);
				}
				else {
					getSession().release(buf);
					if (bytes == -1) {
						eof = true;
						break;
					}
				}
			}
			if (eof) {
				getSession().writenf(holder);
				getSession().close();
				fullProgress = halfProgress = null;
			}
			else {
				if (full) {
					int size = holder.size();
					
					if (size <= half1Size) {
						fullProgress = halfProgress = getSession().write(holder);	
					}
					else {
						ByteBufferHolder holder2 = new ByteBufferHolder();
						
						for (int i=half1Size; i<size; ++i) {
							holder2.add(holder.remove(half1Size));
						}
						halfProgress = getSession().write(holder);
						fullProgress = getSession().write(holder2);
					}
				}
				else {
					fullProgress = getSession().write(holder);
				}
			}
		}
		catch (IOException e) {
			err(e);
			for (ByteBuffer buf: holder) {
				getSession().release(buf);
			}
			getSession().close();
		}
	}

	@Override
	public void exception(Throwable t) {
		err(t);
	}
	
	void err(String msg) {
		Logger.error("["+remoteAddress+"] " + msg);
	}

	void err(Throwable t) {
		Logger.error("["+remoteAddress+"] " + t);
	}
	
	void info(String msg) {
		Logger.info("["+remoteAddress+"] " + msg);
	}
}
