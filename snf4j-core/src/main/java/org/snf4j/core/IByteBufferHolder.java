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
package org.snf4j.core;

import java.nio.ByteBuffer;

/**
 * A byte buffer holder that represents a portion of bytes to be written in
 * one chunk for stream-oriented sessions or a datagram for datagram-oriented
 * sessions.
 * <p>
 * For sessions configured for optimized data copying (see
 * {@link org.snf4j.core.session.ISessionConfig#optimizeDataCopying()
 * ISessionConfig.optimizeDataCopying}) this holder when used as a parameter in
 * session's write methods can eliminate a need for copying data during creation
 * of a data chunk or a datagram to write. In such case the buffers stored in this
 * holder will be passed directly to the I/O channel's write(ByteBuffer[]) or
 * the SSLEngine's wrap(ByteBuffer[], ByteBuffer) methods.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IByteBufferHolder {
	
	/**
	 * Tells whether there are any bytes remaining in byte buffers stored in this
	 * holder.
	 * 
	 * @return {@code true} if, and only if, there is at least one byte remaining in
	 *         byte buffers stored in this holder
	 */
	boolean hasRemaining();
	
	/**
	 * Returns the total number of bytes remaining in all byte buffers stored in
	 * this holder.
	 * 
	 * @return The number of total bytes remaining in all byte buffers stored in
	 *         this holder
	 */
	int remaining();
	
	/**
	 * Returns an array containing all of byte buffers stored in this holder in
	 * proper sequence (from first to last buffer buffer to send).
	 * <p>
	 * The returned array should be "safe" in that no references to it are
	 * maintained by this holder. (In other words, this method must allocate a new
	 * array). The caller is thus free to modify the returned array.
	 * 
	 * @return an array containing all of the byte buffers in this holder in proper
	 *         sequence
	 */
	ByteBuffer[] toArray();
	
	/**
	 * Tells if this byte buffer holder should be treated as an message or raw bytes
	 * when it is processed thru a encoding pipeline.
	 * <p>
	 * When {@code true} is returned the processing will start from the last encoder
	 * in the pipeline with inbound type matching a class implementing this
	 * interface or this interface itself.
	 * <p>
	 * In other case, when {@code false} is returned, the processing will start from
	 * the last encoder with inbound type that is either {@code byte[]},
	 * {@link ByteBuffer} or this interface itself.
	 * 
	 * @return {@code true} if this holder should treated as a message
	 */
	boolean isMessage();
}
