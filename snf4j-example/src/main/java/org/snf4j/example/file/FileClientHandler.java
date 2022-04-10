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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.snf4j.core.EndingAction;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ssl.SSLEngineBuilder;

public class FileClientHandler extends AbstractFileHandler {

	private final String path;
	
	FileClientHandler(SSLEngineBuilder builder, String path) {
		super(builder);
		config.setEndingAction(EndingAction.STOP);
		this.path = path;
	}
	
	@Override
	public void read(Object msg) {
		try {
			if (fileChannel != null) {
				fileChannel.write((ByteBuffer)msg);
			}
		} catch (IOException e) {
			Logger.err(e.toString());
			getSession().close();
		}
		finally {
			getSession().release((ByteBuffer)msg);
		}
	}
	
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case OPENED:
			break;
			
		case READY:
			try {
				File filePath = new File(FileClient.DOWNLOAD_DIR + File.separator + new File(path).getName());
				
				if (filePath.exists()) {
					if (FileClient.DELETE_IF_EXISTS) {
						filePath.delete();
					}
					else {
						Logger.err("File " + filePath.getAbsolutePath() + " already exists");
						getSession().close();
						return;
					}
				}
				file = new RandomAccessFile(filePath, "rw");
				fileChannel = file.getChannel();
				Logger.inf("Downloading " + filePath.getAbsolutePath());
			} catch (Exception e) {
				Logger.err(e.toString());
				getSession().close();
				return;
			}
			
			ByteBuffer buf = getSession().allocate(BUFFER_SIZE);
			
			buf.put(path.getBytes(StandardCharsets.UTF_8));
			buf.put((byte)0);
			buf.flip();
			getSession().writenf(buf);
			break;
			
		case CLOSED:
			if (file != null) {
				Logger.inf(String.format("Downloading of %,d bytes completed (%,d bytes/sec)", 
						getSession().getReadBytes(), 
						(long)getSession().getReadBytesThroughput()));
			}
			break;
			
		default:
		}
		super.event(event);
	}
	
}
