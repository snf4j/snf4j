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
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import org.snf4j.core.session.ISession;

/**
 * Compresses an array of bytes using the
 * <a href="http://en.wikipedia.org/wiki/Gzip">gzip</a> compression as
 * specified in <a href="http://tools.ietf.org/html/rfc1952">RFC 1952</a>.
 * It generates the simplest gzip header with FLG=0, MTIME=0, XFL=0 and OS=255.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class GzipEncoder extends ZlibEncoder {
	
	final static byte ID1 = 0x1f;
	
	final static byte ID2 = (byte)0x8b;
	
	final static byte[] HEADER = new byte[] {
		/* ID1   */ ID1,				 
		/* ID2   */ ID2, 
		/* CM    */ Deflater.DEFLATED,
		/* FLG   */ 0,
		/* MTIME */ 0, 0, 0, 0,
		/* XFL   */	0,
		/* OS    */ -1
	};
	
	private final CRC32 crc = new CRC32();	
	
	private boolean header;
	
	/**
	 * Creates a new gzip encoder with the default compression level ({@code 6}).
	 */
	public GzipEncoder() {
		this(6);
	}
	
	/**
	 * Creates a new gzip encoder with the specified compression level.
	 * 
	 * @param level the compression level ({@code 0} - {@code 9)}. {@code 1} yields
	 *              the fastest compression and {@code 9} yields the best
	 *              compression. {@code 0} means no compression. The default
	 *              compression level is {@code 6}.
	 * @throws IllegalArgumentException if the compression level is out of range
	 */
	public GzipEncoder(int level) {
		super(level, ZlibCodec.Mode.RAW);
	}

	@Override
	protected int deflateBound(int len) {
		
		/*
		 * Conservative upper bound for compressed data based on the source code
		 * deflate.c by Jean-loup Gailly and Mark Adler
		 */
		return super.deflateBound(len) + 8;
	}

	/**
	 * Generates the gzip header.
	 */
	@Override
	protected void preDeflate(ISession session, byte[] in, ByteBuffer out) {
		if (!header) {
			out.put(HEADER);
			header = true;
		}
		crc.update(in);
	}

	/**
	 * Generates the gzip header if the compression was finished
	 * without compressing any data.
	 */
	protected void preFinish(ISession session, ByteBuffer out) {
		if (!header) {
			out.put(HEADER);
			header = true;
		}
	}
	
	/**
	 * Generates the gzip footer.
	 */
	protected void postFinish(ISession session, ByteBuffer out) {
		long crcValue = crc.getValue();
		long bytes = deflater.getBytesRead();
		
        out.put((byte) crcValue);
        out.put((byte)(crcValue >>> 8));
        out.put((byte)(crcValue >>> 16));
        out.put((byte)(crcValue >>> 24));
        out.put((byte)bytes);
        out.put((byte)(bytes >>> 8));
        out.put((byte)(bytes >>> 16));
        out.put((byte)(bytes >>> 24));
	}
	
}
