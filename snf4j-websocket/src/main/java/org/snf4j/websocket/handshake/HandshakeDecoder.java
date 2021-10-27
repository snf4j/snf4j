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
package org.snf4j.websocket.handshake;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.logger.ExceptionLogger;
import org.snf4j.core.logger.IExceptionLogger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IStreamSession;

/**
 * Decodes a Web Socket handshake frame from bytes in the protocol version 13 format.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class HandshakeDecoder implements IDecoder<byte[],HandshakeFrame>, IBaseDecoder<byte[],HandshakeFrame> {

	private final static ILogger LOGGER = LoggerFactory.getLogger(HandshakeDecoder.class);
	
	private final static IExceptionLogger ELOGGER = ExceptionLogger.getInstance();
	
	private final static int DEFAULT_MAX_LINES_IN_CHUNK = 50;
	
	private final static int DEFAULT_MAX_LENGTH = 65536;
	
	private final int[] lines;
	
	private boolean linesReady;
	
	private final boolean clientMode;
	
	private final int maxLength;
	
	private HandshakeFactory factory = HandshakeFactory.getDefault();
	
	private HandshakeFrame frame;
	
	private long frameLength;
	
	private boolean fullFrame;
	
	/**
	 * Constructs a Web Socket handshake decoder.
	 * 
	 * @param clientMode      determines the mode (client/server) in which the
	 *                        decoder should work
	 * @param maxLength       maximum length of a handshake frame. Setting it to an
	 *                        appropriate value can prevent from denial of service
	 *                        attacks
	 * @param maxLinesInChunk maximum number of lines of a Web Socket handshake
	 *                        frame to be decoded in a single pass. Setting it to an
	 *                        appropriate value can improve performance of decoding
	 *                        frames with big number of lines.
	 */
	public HandshakeDecoder(boolean clientMode, int maxLength, int maxLinesInChunk) {
		if (maxLinesInChunk < 1) {
			throw new IllegalArgumentException("lineCount is less than 1");
		}
		this.clientMode = clientMode;
		this.maxLength = maxLength;
		lines = new int[maxLinesInChunk*2+1];
	}
	
	/**
	 * Constructs a Web Socket handshake decoder with the default maximum number
	 * (50) of lines of a handshake frame to be decoded in a single pass.
	 * 
	 * @param clientMode determines the mode (client/server) in which the decoder
	 *                   should work
	 * @param maxLength  maximum length of a handshake frame. Setting it to an
	 *                   appropriate value can prevent from denial of service
	 *                   attacks
	 */
	public HandshakeDecoder(boolean clientMode, int maxLength) {
		this(clientMode, maxLength, DEFAULT_MAX_LINES_IN_CHUNK);
	}
	
	/**
	 * Constructs a Web Socket handshake decoder with the default maximum length
	 * (65536) of a handshake frame and the default maximum number (50) of lines of
	 * a handshake frame to be decoded in a single pass.
	 * 
	 * @param clientMode determines the mode (client/server) in which the decoder
	 *                   should work
	 */
	public HandshakeDecoder(boolean clientMode) {
		this(clientMode, DEFAULT_MAX_LENGTH, DEFAULT_MAX_LINES_IN_CHUNK);
	}
	
	@Override
	public Class<byte[]> getInboundType() {
		return byte[].class;
	}

	@Override
	public Class<HandshakeFrame> getOutboundType() {
		return HandshakeFrame.class;
	}

	private void fireException(ISession session, Throwable t) {
		try {
			session.getHandler().exception(t);
		}
		catch (Throwable e) {	
			ELOGGER.error(LOGGER, "Exception handling failed for {}: {}", session, e);
		}
		session.close();
	}
	
	@Override
	public void decode(ISession session, byte[] data, List<HandshakeFrame> out) throws Exception {
		InvalidHandshakeException exception = null;
		HttpStatus status = null;
		byte[] pending = null;
		int pendingLength = 0;

		if (!linesReady) {
			int available = available(session, data, 0, data.length);
			
			linesReady = false;
			if (available == 0) {
				throw new IllegalArgumentException("decoded data does not end with CRLF");
			}
			else if (available < data.length) {
				pending = Arrays.copyOfRange(data, available, data.length);
				pendingLength = pending.length;
			}
		}
		
		if (frame == null) {
			frameLength = data.length;
		}
		else {
			frameLength += data.length;
		}
		frameLength -= pendingLength;
		
		if (frameLength > maxLength) {
			exception = new InvalidHandshakeException("Handshake frame too large");
			status = HttpStatus.REQUEST_ENTITY_TOO_LARGE;
		}
		else {
			try {
				if (frame == null) {
					frame = factory.parse(data, lines, !clientMode);
				} 
				else {
					factory.parseFields(frame, data, lines, 0);
				}
			}
			catch (InvalidHandshakeRequestException e) {
				exception = e;
				status = e.getStatus();
			}
			catch (InvalidHandshakeException e) {
				exception = e;
				status = HttpStatus.BAD_REQUEST;
			}
		}
		
		if (status != null) {
			if (clientMode) {
				throw exception;
			}
			else {
				((IStreamSession)session).writenf(new HandshakeResponse(status));
				fireException(session, exception);
				return;
			}
		}
		
		if (fullFrame) {
			out.add(frame);
			frame = null;
			fullFrame = false;
		}
		if (pending != null) {
			decode(session, pending, out);
		}
	}

	private int available0(int available) {
		linesReady = true;
		if (available > 0) {
			fullFrame = true;
			return available;
		}
		for (int i=0; i<lines.length; i+=2) {
			if (lines[i] == -1) {
				return i == 0 ? 0 : lines[i-1] + HttpUtils.CRLF.length;
			}
		}
		return 0;
	}
	
	@Override
	public int available(ISession session, ByteBuffer buffer, boolean flipped) {
		return available0(factory.available(buffer, flipped, lines));
	}

	@Override
	public int available(ISession session, byte[] buffer, int off, int len) {
		return available0(factory.available(buffer, off, len, lines));
	}

}
