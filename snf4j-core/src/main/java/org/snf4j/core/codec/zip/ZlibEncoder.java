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
import java.util.List;
import java.util.zip.Deflater;

import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.codec.IEventDrivenCodec;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

/**
 * Compresses an array of bytes using the 
 * <a href="http://en.wikipedia.org/wiki/Zlib">zlib</a> compression.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ZlibEncoder extends ZlibCodec implements IEncoder<byte[], ByteBuffer>, IEventDrivenCodec {
    
	/**
	 * The compressor used by this class to compress data.
	 */
	protected Deflater deflater;

	private volatile boolean finish;
	
	private volatile boolean finished;
	
	/**
	 * Creates a new zlib encoder with the default compression level ({@code 6}) and
	 * the default mode ({@link ZlibCodec.Mode#ZLIB ZLIB}).
	 */
	public ZlibEncoder() {
		this(6);
	}

	/**
	 * Creates a new zlib encoder with the specified compression level and the
	 * default mode ({@link ZlibCodec.Mode#ZLIB ZLIB}).
	 * 
	 * @param level the compression level ({@code 0} - {@code 9)}. {@code 1} yields
	 *              the fastest compression and {@code 9} yields the best
	 *              compression. {@code 0} means no compression. The default
	 *              compression level is {@code 6}.
	 * @throws IllegalArgumentException if the compression level is out of range
	 */
	public ZlibEncoder(int level) {
		this(level, Mode.ZLIB);
	}

	/**
	 * Creates a new zlib encoder with a preset dictionary for compression, the
	 * default compression level ({@code 6}) and the default mode
	 * ({@link ZlibCodec.Mode#ZLIB ZLIB}).
	 * 
	 * @param dictionary the preset dictionary or {@code null} if no preset
	 *                   dictionary should be used
	 */
	public ZlibEncoder(byte[] dictionary) {
		this(6, dictionary);
	}
	
	/**
	 * Creates a new zlib encoder with the specified compression level and the mode.
	 * 
	 * @param level the compression level ({@code 0} - {@code 9)}. {@code 1} yields
	 *              the fastest compression and {@code 9} yields the best
	 *              compression. {@code 0} means no compression. The default
	 *              compression level is {@code 6}.
	 * @param mode  the mode for the zlib encoder ({@link ZlibCodec.Mode#AUTO AUTO} means
	 *              {@link ZlibCodec.Mode#ZLIB ZLIB})
	 * @throws IllegalArgumentException if the compression level is out of range
	 * @throws NullPointerException     if the mode is null
	 */
	public ZlibEncoder(int level, Mode mode) {
		if (level < 0 || level > 9) {
			throw new IllegalArgumentException("compression level is out of range");
		}
		if (mode == null) {
			throw new NullPointerException("mode is null");
		}
		
		boolean nowrap = mode == Mode.RAW;
		
		deflater = new Deflater(level, nowrap);
	}
	
	/**
	 * Creates a new zlib encoder with the specified compression level and a preset
	 * dictionary.
	 * 
	 * @param level      the compression level ({@code 0} - {@code 9)}. {@code 1}
	 *                   yields the fastest compression and {@code 9} yields the
	 *                   best compression. {@code 0} means no compression. The
	 *                   default compression level is {@code 6}.
	 * @param dictionary the preset dictionary or {@code null} if no preset
	 *                   dictionary should be used
	 * @throws IllegalArgumentException if the compression level is out of range
	 */
	public ZlibEncoder(int level, byte[] dictionary) {
		this(level, dictionary, Mode.ZLIB);
	}
	
	/**
	 * Creates a new zlib encoder with the specified compression level, a preset
	 * dictionary and the mode.
	 * 
	 * @param level      the compression level ({@code 0} - {@code 9)}. {@code 1}
	 *                   yields the fastest compression and {@code 9} yields the
	 *                   best compression. {@code 0} means no compression. The
	 *                   default compression level is {@code 6}.
	 * @param dictionary the preset dictionary or {@code null} if no preset
	 *                   dictionary should be used
	 * @param mode       the mode for the zlib encoder ({@link ZlibCodec.Mode#AUTO AUTO} means
	 *                   {@link ZlibCodec.Mode#ZLIB ZLIB})
	 * @throws IllegalArgumentException if the compression level is out of range
	 * @throws NullPointerException     if the mode is null
	 */
	protected ZlibEncoder(int level, byte[] dictionary, Mode mode) {
		this(level, mode);
		if (dictionary != null) {
			deflater.setDictionary(dictionary);
		}
	}
	
	/**
	 * Returns upper bound on the compressed size.
	 * 
	 * @param len the length of the uncompressed data.
	 * @return the upper bound
	 */
	protected int deflateBound(int len) {

		/*
		 * Conservative upper bound for compressed data based on the source code
		 * deflate.c by Jean-loup Gailly and Mark Adler
		 */
		return len + ((len + 7) >> 3) + ((len + 63) >> 6) + 5 + 10;
	}
	
	/**
	 * Called right before compression of a portion of uncompressed data. It can be
	 * used, for example, to generate a customized header.
	 * 
	 * @param session the session object the encoder is associated with
	 * @param in      the uncompressed input data
	 * @param out     the output buffer
	 * @throws Exception if an error has occurred
	 */
	protected void preDeflate(ISession session, byte[] in, ByteBuffer out) throws Exception {
	}
	
	/**
	 * Called right before finishing of the compression. It can be used, for
	 * example, to generate a customized header if the compression was finished
	 * without compressing any data.
	 * 
	 * @param session the session object the encoder is associated with
	 * @param out     the output buffer
	 * @throws Exception if an error has occurred
	 */
	protected void preFinish(ISession session, ByteBuffer out) throws Exception {
	}
	
	/**
	 * Called right after the compression has been finished. It can be used, for
	 * example, to generate a customized footer.
	 * 
	 * @param session the session object the encoder is associated with
	 * @param out     the output buffer
	 * @throws Exception if an error has occurred
	 */
	protected void postFinish(ISession session, ByteBuffer out) throws Exception {
	}

	/**
	 * Requests finishing of the compression. Any data processed by this encoder
	 * after calling this method will not be compressed (will be passed to the
	 * output list without any change).
	 */
	public void finish() {
		finish = true;
	}
	
	/**
	 * Tells if the compression has been finished.
	 * 
	 * @return {@code true} if the compression has been finished.
	 */
	public boolean isFinished() {
		return finished;
	}
	
	/**
	 * Compresses the input data.
	 */
	@Override
	public void encode(ISession session, byte[] data, List<ByteBuffer> out) throws Exception {
		int len, dataLen = data.length;
		
		if (finished) {
			if (dataLen > 0) {
				out.add(ByteBuffer.wrap(data));
			}
			return;
		}
		
		if (finish) {
			ByteBuffer footer = ByteBuffer.allocate(deflateBound(0));
			
			preFinish(session, footer);
			deflater.finish();
			while (!deflater.finished()) {
				for(;;) {
					len = deflater.deflate(footer.array(), footer.position(), footer.remaining(), Deflater.SYNC_FLUSH);
					footer.position(footer.position() + len);
					if (!footer.hasRemaining()) {
						footer.flip();
						out.add(footer);
						footer = ByteBuffer.allocate(deflateBound(0));
						continue;
					}
					break;
				}
			}
			postFinish(session, footer);
			deflater.end();
			deflater = null;
			finished = true;
			footer.flip();
			out.add(footer);
			encode(session, data, out);
			return;
		}
		
		
		if (dataLen == 0) {
			return;
		}
		
		ByteBuffer deflated = ByteBuffer.allocate(deflateBound(dataLen));
		
		preDeflate(session, data, deflated);
		
		deflater.setInput(data);
		while (!deflater.needsInput()) {
			for(;;) {
				len = deflater.deflate(deflated.array(), deflated.position(), deflated.remaining(), Deflater.SYNC_FLUSH);
				deflated.position(deflated.position() + len);
				if (!deflated.hasRemaining()) {
					deflated.flip();
					out.add(deflated);
					deflated = ByteBuffer.allocate(deflateBound(0));
					continue;
				}
				deflated.flip();
				out.add(deflated);
				break;
			}
		}
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void added(ISession session) {
	}

	/**
	 * Finishes the compression when the associated session is ending
	 * ({@link SessionEvent#ENDING}).
	 */
	@Override
	public void event(ISession session, SessionEvent event) {
		if (event == SessionEvent.ENDING && !finished) {
			deflater.end();
			deflater = null;
			finished = true;
		}
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void removed(ISession session) {
	}

}
