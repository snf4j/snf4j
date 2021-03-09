/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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

import com.sun.nio.sctp.MessageInfo;

/**
 * A reader that reads data directly from an SCTP channel.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISctpReader {
	
	/**
	 * Called when a new message was received from the remote end.
	 * <p>
	 * The passed array can be safely stored or modified by this method as it will
	 * not be used by the caller.
	 * 
	 * @param msg     the message received from the remote end.
	 * @param msgInfo additional ancillary information about the received message.
	 */
	void read(byte[] msg, MessageInfo msgInfo);
	
	/**
	 * Called when a new message was received from the remote end. This method is
	 * only called when the associated session is configured to optimize data
	 * copying and uses an allocator supporting the releasing of no longer used
	 * buffers.
	 * <p>
	 * The passed buffer can be safely stored or modified by this method as it will
	 * not be used by the caller.
	 * 
	 * @param msg     the message received from the remote end.
	 * @param msgInfo additional ancillary information about the received message.
	 */
	void read(ByteBuffer msg, MessageInfo msgInfo);
}
