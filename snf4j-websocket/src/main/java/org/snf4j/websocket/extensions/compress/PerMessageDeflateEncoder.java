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
package org.snf4j.websocket.extensions.compress;

import java.util.List;

import org.snf4j.core.session.ISession;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.Opcode;

/**
 * The per-message implementation of deflate encoder (compressor).
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class PerMessageDeflateEncoder extends DeflateEncoder {

    private boolean compressing; 	
    
	/**
	 * Constructs a per-message deflate encoder with specified compression level and
	 * the context takeover control.
	 * 
	 * @param compressionLevel the compression level (0-9)
	 * @param noContext        {@code true} to prevent from using the context
	 *                         takeover
	 */
	public PerMessageDeflateEncoder(int compressionLevel, boolean noContext) {
		super(compressionLevel, noContext);
	}

	@Override
	boolean allowEncoding(Frame frame) {
		Opcode opcode = frame.getOpcode();
		
		return (opcode == Opcode.TEXT || opcode == Opcode.BINARY)
				&& !frame.isRsvBit1()
				|| (opcode == Opcode.CONTINUATION && compressing);
	}

	@Override
	boolean removeTail(Frame frame) {
		return frame.isFinalFragment();
	}

	@Override
	int rsvBits(Frame frame) {
		switch (frame.getOpcode()) {
		case TEXT:
		case BINARY:
			return frame.getRsvBits() | Frame.RSV1;
		
		default:
			return frame.getRsvBits();
		}
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void encode(ISession session, Frame frame, List<Frame> out) throws Exception {
		super.encode(session, frame, out);

		Opcode opcode = frame.getOpcode();
		
		if (!opcode.isControl()) {
			if (frame.isFinalFragment()) {
				compressing = false;
			} else if (!frame.isRsvBit1()){
				switch (opcode) {
				case TEXT:
				case BINARY:
					compressing = true;
				}
			} 	
		}
	}	
}
