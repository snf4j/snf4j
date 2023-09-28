/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.engine;

import java.nio.ByteBuffer;

import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.engine.Status;

public class FlightController {
	
	private final StringBuilder trace = new StringBuilder();
	
	public byte[] earlyData;
	
	void trace(String s) {
		trace.append(s).append('|');
	}
	
	String trace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	void trace(TLSEngine engine, IEngineResult r) {
		String h = r == null ? null : toString(r.getHandshakeStatus());
		String s = r == null ? null : toString(r.getStatus());
		String hs = toString(engine.getHandshakeStatus());
		
		if (h != null) {
			trace(s+":"+h.toLowerCase()+hs.toLowerCase());
		}
		else {
			trace(hs.toLowerCase());
		}
	}
	
	String toString(Status s) {
		switch (s) {
		case BUFFER_OVERFLOW:
			return "O";
		case BUFFER_UNDERFLOW:
			return "U";
		case CLOSED:
			return "C";
		case OK:
			return "OK";
		default:
			return "?";
		}
	}
	
	String toString(HandshakeStatus s) {
		switch (s) {
		case NEED_WRAP:
			return "W";
		case FINISHED:
			return "F";
		case NEED_TASK:
			return "T";
		case NEED_UNWRAP:
			return "U";
		case NEED_UNWRAP_AGAIN:
			return "UA";
		case NOT_HANDSHAKING:
			return "NH";
		default:
			return "?";
		}
	}
	
	void fly(TLSEngine engine, ByteBuffer in, ByteBuffer out) throws Exception {
		IEngineResult r;
		int pos;
		int earlyData;
		
		switch (engine.getHandshakeStatus()) {
		case NEED_WRAP:
			trace("W");
			r = engine.wrap(in, out);
			trace(engine, r);
			break;
			
		case NEED_UNWRAP:
			pos = out.position();
			r = engine.unwrap(in, out);
			earlyData = out.position()-pos;
			if (earlyData > 0) {
				ByteBuffer dup = out.duplicate();
				
				dup.flip();
				dup.position(pos);
				this.earlyData = new byte[dup.remaining()];
				dup.get(this.earlyData);
			}
			out.position(pos);
			if (r.getStatus() == Status.BUFFER_UNDERFLOW) {
				return;
			}
			else {
				if (earlyData > 0) {
					trace("Ued(" + earlyData +")");
				}
				else {
					trace("U");
				}
				trace(engine, r);
			}
			break;
			
		case NEED_TASK:
			trace("T");
			engine.getDelegatedTask().run();
			trace(engine, null);
			break;
			
		case NOT_HANDSHAKING:
			if (in.hasRemaining()) {
				trace("U");
				r = engine.unwrap(in, out);
				trace(engine, r);
				if ((in.hasRemaining() && r.bytesConsumed() > 0) || r.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) {
					break;
				}
			}
			else {
				trace("NH");
			}
			return;
			
		default:
			trace("?");
		}
		fly(engine, in, out);
	}
}
