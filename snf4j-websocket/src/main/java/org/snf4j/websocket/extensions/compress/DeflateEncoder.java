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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.codec.IEventDrivenCodec;
import org.snf4j.core.codec.zip.ZlibCodec;
import org.snf4j.core.codec.zip.ZlibEncoder;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;
import org.snf4j.websocket.frame.Frame;

abstract class DeflateEncoder extends DeflateCodec implements IEncoder<Frame,Frame>, IEventDrivenCodec {

    private final int compressionLevel;

    private final boolean noContext;
    
    private ZlibEncoder encoder;
    
    public DeflateEncoder(int compressionLevel, boolean noContext) {
    	this.compressionLevel = compressionLevel;
    	this.noContext = noContext;
    }
    
	abstract boolean allowEncoding(Frame frame);

	abstract boolean removeTail(Frame frame);
	
	@Override
	IEventDrivenCodec codec() {
		return encoder;
	}
	
	@Override
	public void encode(ISession session, Frame frame, List<Frame> out) throws Exception {
		if (allowEncoding(frame)) {
			if (encoder == null) {
				encoder = new ZlibEncoder(compressionLevel, ZlibCodec.Mode.RAW);
			}
			
			List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
			
			encoder.encode(session, frame.getPayload(), bufs);
			
			if (frame.isFinalFragment() && noContext) {
				encoder.event(session, SessionEvent.ENDING);
				encoder = null;
			}
			
			byte[] payload;
			ByteBuffer buf;
			
			switch (bufs.size()) {
			case 1:
				buf = bufs.get(0);
				payload = new byte[removeTail(frame) ? buf.remaining()-TAIL.length : buf.remaining()];
				buf.get(payload);
				break;
				
			case 0:
				if (frame.getPayloadLength() == 0) {
					payload = new byte[1];
				}
				else {
					throw new IllegalStateException("Deflating of input data produced no data");
				}
				break;
				
			default:
				payload = bytes(bufs, removeTail(frame));
			}
			
			frame = createFrame(frame, payload);
		}
		out.add(frame);
	}
	
}
