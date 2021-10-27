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
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.Opcode;

public class PerMessageDeflateDecoder extends DeflateDecoder {

    private boolean compressing; 	
    
    public PerMessageDeflateDecoder(boolean noContext, int minInflateBound) {
		super(noContext, minInflateBound);
	}
    
    public PerMessageDeflateDecoder(boolean noContext) {
		super(noContext);
	}

	@Override
	boolean allowDecoding(Frame frame) {
		Opcode opcode = frame.getOpcode();
		
		return (opcode == Opcode.TEXT || opcode == Opcode.BINARY) 
				&& frame.isRsvBit1()
				|| (opcode == Opcode.CONTINUATION && compressing);
	}

	@Override
	boolean appendTail(Frame frame) {
        return frame.isFinalFragment();
	}

	@Override
	int rsvBits(Frame frame) {
		return frame.isRsvBit1() ? frame.getRsvBits() ^ IExtension.RSV1 : frame.getRsvBits();
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void decode(ISession session, Frame frame, List<Frame> out) throws Exception {
        super.decode(session, frame, out);
        
		Opcode opcode = frame.getOpcode();

		if (!opcode.isControl()) {
			if (frame.isFinalFragment()) {
				compressing = false;
			} else if (frame.isRsvBit1()) {
				switch (opcode) {
				case TEXT:
				case BINARY:
					compressing = true;
				}
			}
		}
  	}	

}
