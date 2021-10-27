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

import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEventDrivenCodec;
import org.snf4j.core.codec.zip.ZlibCodec;
import org.snf4j.core.codec.zip.ZlibDecoder;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.frame.CloseFrame;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.InvalidFrameException;

abstract class DeflateDecoder extends DeflateCodec implements IDecoder<Frame,Frame>, IEventDrivenCodec {

    private ZlibDecoder decoder;
    
    private final boolean noContext;
    
    private final int minInflateBound;
    
    public DeflateDecoder(boolean noContext, int minInflateBound) {
    	this.noContext = noContext;
    	this.minInflateBound = minInflateBound;
    }

    public DeflateDecoder(boolean noContext) {
    	this.noContext = noContext;
    	minInflateBound = 0;
    }
    
    abstract boolean allowDecoding(Frame frame);

	abstract boolean appendTail(Frame frame);
	
	private RuntimeException protocolError(ISession session, String message) throws InvalidFrameException {
		((IStreamSession)session).writenf(new CloseFrame(CloseFrame.PROTOCOL_ERROR));
		return new InvalidFrameException(message);
	}
	
	private RuntimeException protocolError(ISession session, Throwable cause) throws InvalidFrameException {
		((IStreamSession)session).writenf(new CloseFrame(CloseFrame.PROTOCOL_ERROR));
		return new InvalidFrameException(cause);
	}
	
	@Override
	protected IEventDrivenCodec codec() {
		return decoder;
	}
	
	@Override
	public void decode(ISession session, Frame frame, List<Frame> out) throws Exception {
		if (allowDecoding(frame)) {
			if (decoder == null) {
				decoder = new ZlibDecoder(ZlibCodec.Mode.RAW) {
					@Override
					protected int inflateBound(int len) {
						len = super.inflateBound(len);
						
						if (len < minInflateBound) {
							len = minInflateBound;
						}
						return len;
					}
				};
			}
			
			List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
			
			try {
				decoder.decode(session, frame.getPayload(), bufs);
				if (appendTail(frame)) {
					decoder.decode(session, TAIL, bufs);
				}
			}
			catch (Exception e) {
				throw protocolError(session, e);
			}
			
			if (frame.isFinalFragment() && noContext) {
				decoder.event(session, SessionEvent.ENDING);
				decoder = null;
			}
			
			byte[] payload;
			ByteBuffer buf;
			
			switch (bufs.size()) {
			case 1:
				buf = bufs.get(0);
				payload = new byte[buf.remaining()];
				buf.get(payload);
				break;
				
			case 0:
				payload = frame.getPayload();
				if (payload.length == 1 && payload[0] == 0) {
					payload = new byte[0];
				}
				else {
					throw protocolError(session, "Inflating of input data produced no data");
				}
				break;
				
			default:
				payload = bytes(bufs, false);
			}
			
			frame = createFrame(frame, payload);
		}
		out.add(frame);
	}

}
