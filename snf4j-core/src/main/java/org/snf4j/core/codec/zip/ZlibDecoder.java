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
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEventDrivenCodec;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

/**
 * Decompresses an array of bytes using the 
 * <a href="http://en.wikipedia.org/wiki/Zlib">zlib</a> compression.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ZlibDecoder extends ZlibCodec implements IDecoder<byte[],ByteBuffer>, IEventDrivenCodec {

	/**
	 * The decompressor used by this class to decompress data.
	 */
	protected Inflater inflater;
	
	private boolean finishing;
	
	private volatile boolean finished;
	
	private final byte[] dictionary;
	
	private ByteBuffer in;
	
	/**
	 * Creates a new zlib decoder with the default mode ({@link ZlibCodec.Mode#ZLIB
	 * ZLIB}).
	 */
	public ZlibDecoder() {
		this(null, Mode.ZLIB);
	}
	
	/**
	 * Creates a new zlib decoder with a preset dictionary for decompression and the
	 * default mode ({@link ZlibCodec.Mode#ZLIB ZLIB}).
	 * 
	 * @param dictionary the preset dictionary or {@code null} if no preset
	 *                   dictionary should be used
	 */
	public ZlibDecoder(byte[] dictionary) {
		this(dictionary, Mode.ZLIB);
	}
	
	/**
	 * Creates a new zlib decoder with the specified mode.
	 * 
	 * @param mode the mode for the zlib decoder
	 * @throws NullPointerException if the mode is null
	 */
	public ZlibDecoder(Mode mode) {
		this(null, mode);
	}
	
	/**
	 * Creates a new zlib decoder with a preset dictionary for decompression and the
	 * mode.
	 * 
	 * @param dictionary the preset dictionary or {@code null} if no preset
	 *                   dictionary should be used
	 * @param mode       the mode for the zlib decoder
	 */
	protected ZlibDecoder(byte[] dictionary, Mode mode) {
		if (mode == null) {
			throw new NullPointerException("mode is null");
		}
		if (mode != Mode.AUTO) {
			boolean nowrap = mode == Mode.RAW;
			
			inflater = new Inflater(nowrap);
		}
		this.dictionary = dictionary;
	}
	
	private static boolean nowrap(short cmfflg) {

		/*
		 * CMF: CINFO=0x7 (32K window size)
		 *         CM=0x8 (deflate compression) 
		 * ((CMF << 8) + FLG) is a multiple of 31
		 */
		return (cmfflg & 0xFF00) != 0x7800 || cmfflg % 31 != 0;
	}
	
	/**
	 * Returns upper bound on the decompressed size.
	 * 
	 * @param len the length of the compressed data.
	 * @return the upper bound
	 */
	protected int inflateBound(int len) {
		return len << 1; 
	}
	
	/**
	 * Called right before decompression of a portion of compressed data. It can be
	 * used, for example, to parse a customized header. All data being part of the
	 * header have to be consumed from the input buffer.
	 * 
	 * @param session the session object the decoder is associated with
	 * @param in      the compressed input data
	 * @throws Exception if an error has occurred
	 */
	protected void preInflate(ISession session, ByteBuffer in) throws Exception {
	}
	
	/**
	 * Called right after decompression of a portion of compressed data. It can be
	 * used, for example, to compute the CRC-32 of a decompressed data stream.
	 * 
	 * @param session the session object the decoder is associated with
	 * @param data    an array with decompressed data
	 * @param off     the start offset in the array at which the data should
	 *                be read
	 * @param len     the number of bytes to read
	 * @throws Exception if an error has occurred
	 */
	protected void postInflate(ISession session, byte[] data, int off, int len) throws Exception {
	}
	
	/**
	 * Called right after the decompression has been finished. It can be used, for
	 * example, to parse a customized footer. All data being part of the footer have
	 * to be consumed from the input buffer.
	 * 
	 * @param session the session object the decoder is associated with
	 * @param in      the compressed input data
	 * @return {@code true} if all bytes forming the footer has been already
	 *         consumed
	 * @throws Exception if an error has occurred
	 */
	protected boolean postFinish(ISession session, ByteBuffer in) throws Exception {
		return true;
	}
	
	/**
	 * Tells if the decompression has been finished.
	 * 
	 * @return {@code true} if the decompression has been finished.
	 */
	public boolean isFinished() {
		return finished;
	}
	
	/**
	 * Decompresses the input data.
	 */
	@Override
	public void decode(ISession session, byte[] data, List<ByteBuffer> out) throws Exception {
		int dataLen = data.length;

		if (finished) {
			if (dataLen > 0) {
				out.add(ByteBuffer.wrap(data));
			}
			return;
		}
		
		if (dataLen == 0) {
			return;
		}
		
		if (in == null) {
			in = ByteBuffer.wrap(data);
		}
		else {
			ByteBuffer tmp = ByteBuffer.allocate(dataLen + in.remaining());
			
			tmp.put(in);
			tmp.put(data);
			tmp.flip();
			in = tmp;
		}
		
		if (inflater == null) {
			if (in.remaining() < 2) {
				return;
			}
			inflater = new Inflater(nowrap(in.getShort())); 
			in.position(in.position()-2);
		}
		
		if (!finishing) {
			preInflate(session, in);

			if (in.hasRemaining()) {
				ByteBuffer inflated = ByteBuffer.allocate(inflateBound(in.remaining()));
				int len;

				inflater.setInput(in.array(), in.position(), in.remaining());
				try {
					while (!inflater.needsInput()) {
						len = inflater.inflate(inflated.array(), inflated.position(), inflated.remaining());
						if (len > 0) {
							postInflate(session, inflated.array(), inflated.position(), len);
							inflated.position(inflated.position() + len);
							if (!inflated.hasRemaining()) {
								inflated.flip();
								out.add(inflated);
								inflated = ByteBuffer.allocate(inflateBound(inflater.getRemaining()));
							}
						}
						else if (inflater.needsDictionary()) {
							if (dictionary == null) {
								throw new DecompressionException("decompression failure: no dictionary specifed");
							}
							inflater.setDictionary(dictionary);
						}

						if (inflater.finished()) {
							finishing = true;
							break;
						}
					}
				}
				catch (DataFormatException e) {
					throw new DecompressionException("decompression failure: invalid compressed data format", e);
				}
				if (inflated.position() > 0) {
					inflated.flip();
					out.add(inflated);
				}
				in.position(in.position() + (in.remaining() - inflater.getRemaining()));
			}
		}
		
		if (finishing) {
			if (finished = postFinish(session, in)) {
				inflater.end();
				inflater = null;
				if (in.hasRemaining()) {
					out.add(in);
				}
			}
		}
		
		if (!in.hasRemaining()) {
			in = null;
		}
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void added(ISession session) {
	}

	/**
	 * Finishes the decompression when the associated session is ending
	 * ({@link SessionEvent#ENDING}).
	 */
	@Override
	public void event(ISession session, SessionEvent event) {
		if (event == SessionEvent.ENDING && !finished) {
			inflater.end();
			inflater = null;
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
