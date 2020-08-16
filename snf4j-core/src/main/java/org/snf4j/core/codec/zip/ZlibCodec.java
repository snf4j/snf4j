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
package org.snf4j.core.codec.zip;

import java.nio.ByteBuffer;

import org.snf4j.core.codec.ICodec;

/**
 * The base {@code class} for compressors and decompressors transforming an
 * array of bytes in the <a href="http://en.wikipedia.org/wiki/Zlib">zlib</a>
 * compression format into a {@link ByteBuffer}.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ZlibCodec implements ICodec<byte[],ByteBuffer> {

	/**
	 * Modes determining how the
	 * <a href="http://en.wikipedia.org/wiki/Zlib">zlib</a> compressors and
	 * decompressors should work.
	 */
	public enum Mode {
		
		/**
		 * The ZLIB mode as specified in
		 * <a href="http://tools.ietf.org/html/rfc1950">RFC 1950</a>.
		 */
		ZLIB,
		
	    /**
	     * Raw mode for DEFLATE stream only (no header and no footer).
	     */ 		
		RAW,
		
		/**
		 * The mode ({@link Mode#ZLIB ZLIB} or {@link Mode#RAW RAW}) should be determined
		 * automatically by decompressors. For compressors this mode always means
		 * {@link Mode#ZLIB ZLIB}.
		 */
		AUTO
	}
	
	@Override
	public Class<byte[]> getInboundType() {
		return byte[].class;
	}

	@Override
	public Class<ByteBuffer> getOutboundType() {
		return ByteBuffer.class;
	}

}
