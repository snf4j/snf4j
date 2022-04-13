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

	private final StringBuilder path = new StringBuilder(256);
	
	private IFuture<Void> writeFuture;
	
	FileServerHandler(SSLEngineBuilder builder) {
		super(builder);
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
					inf("Uploading " + path);
					startTime = System.currentTimeMillis();
					continueWriting();
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
				inf(String.format("Uploading of %,d bytes completed in %,d msec (%,d bytes/sec)", 
						fileLength, 
						time, 
						(long)(fileLength*1000/time)));
				inf(String.format("Allocator statistics: phisical allocations: %d (total %,d bytes), total allocations (from cache): %,d", 
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
			if (writeFuture != null && writeFuture.isSuccessful()) {
				writeFuture = null;
				continueWriting();
			}
		}
	}
	
	private void continueWriting() {
		int count = FileServer.BUFFER_COUNT;
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
			}
			else {
				writeFuture = getSession().write(holder);
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
	
	void inf(String msg) {
		Logger.info("["+remoteAddress+"] " + msg);
	}
}
