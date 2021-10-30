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

class HandshakeFactory {
	
	static final HandshakeFactory DEFAULT = new HandshakeFactory();
	
	static HandshakeFactory getDefault() {		
		return DEFAULT;
	}
	
	int available(ByteBuffer data, boolean flipped, int[] lines) {
		return HttpUtils.available(data, flipped, lines);
	}
	
	int available(byte[] data, int off, int len, int[] lines) {
		return HttpUtils.available(data, off, len, lines);
	}
	
	void parseFields(HandshakeFrame frame, byte[] data, int[] lines, int beginLine) throws InvalidHandshakeException {
		int[] out = new int[4];
		
		for (int i=beginLine*2; i<lines.length; i += 2) {
			int i1, i0 = lines[i];
			
			if (i0 == -1 || i+1 >= lines.length) {
				break;
			}
			i1 = lines[i+1];
			switch (HttpUtils.splitHeaderField(data, i0, i1, out)) {
			case 4:
				if (frame.pendingName()) {
					throw new InvalidHandshakeException("No value in header field");
				}
				frame.addValue(HttpUtils.ascii(data, out, 0), HttpUtils.rtrimAscii(data, out, 1));
				break;
				
			case 2:
				if (frame.pendingName()) {
					throw new InvalidHandshakeException("No value in header field");
				}
				frame.pendingName(HttpUtils.ascii(data, out, 0));
				break;
				
			case -4:
				if (frame.pendingName()) {
					frame.addValue(HttpUtils.ascii(data, out, 0), HttpUtils.rtrimAscii(data, out, 1));
				}
				else {
					frame.appendValue(" ");
					frame.appendValue(HttpUtils.rtrimAscii(data, out[0], out[3]));
				}
				break;
				
			case -2:
				if (frame.pendingName()) {
					frame.pendingName(HttpUtils.ascii(data, out, 0));
				}
				else {
					frame.appendValue(" ");
					frame.appendValue(HttpUtils.rtrimAscii(data, out, 0));
				}
				break;
			}
		}
	}
	
	HandshakeFrame parse(byte[] data, int[] lines, boolean request) throws InvalidHandshakeException {
		int[] out = new int[10];
		HandshakeFrame frame;
		
		if (request) {
			if (HttpUtils.splitRequestLine(data, lines, 0, out) != 3) {
				throw new InvalidHandshakeException("Invalid http request");
			}
			if (!HttpUtils.equals(data, out, 2, HttpUtils.HTTP_VERSION)) {
				throw new InvalidHandshakeException("Invalid http request version");
			}
			if (!HttpUtils.equals(data, out, 0, HttpUtils.GET)) {
				throw new InvalidHandshakeRequestException("Forbidden http request command", HttpStatus.FORBIDDEN);
			}
			frame = new HandshakeRequest(HttpUtils.ascii(data, out, 1));
		}
		else {
			if (HttpUtils.splitResponseLine(data, lines, 0, out) < 3) {
				throw new InvalidHandshakeException("Invalid http response");
			}
			if (!HttpUtils.equals(data, out, 0, HttpUtils.HTTP_VERSION)) {
				throw new InvalidHandshakeException("Invalid http response version");
			}
			
			Integer status = HttpUtils.digits(data, out, 1);
			
			if (status == null || (out[3] - out[2]) != HttpUtils.STATUS_CODE_LENGTH) {
				throw new InvalidHandshakeException("Invalid http response status");
			}
			frame = new HandshakeResponse(status, HttpUtils.ascii(data, out[4], lines[1]));
		}
		parseFields(frame, data, lines, 1);
		return frame;
	}
	
	void format(HandshakeFrame frame, ByteBuffer out, boolean request) throws InvalidHandshakeException {
		if (request) {
			if (!(frame instanceof HandshakeRequest)) {
				throw new InvalidHandshakeException();
			}
			out.put(HttpUtils.GET);
			out.put(HttpUtils.SP);
			out.put(HttpUtils.bytes(((HandshakeRequest)frame).getUri()));
			out.put(HttpUtils.SP);
			out.put(HttpUtils.HTTP_VERSION);
			out.put(HttpUtils.CRLF);
		}
		else {
			if (!(frame instanceof HandshakeResponse)) {
				throw new InvalidHandshakeException();
			}
			out.put(HttpUtils.HTTP_VERSION);
			out.put(HttpUtils.SP);
			out.put(HttpUtils.statusCode(((HandshakeResponse)frame).getStatus()));
			out.put(HttpUtils.SP);
			out.put(HttpUtils.bytes(((HandshakeResponse)frame).getReason()));
			out.put(HttpUtils.CRLF);
		}
		for (String name: frame.getNames()) {
			out.put(HttpUtils.bytes(name));
			out.put(HttpUtils.FSSP);
			out.put(HttpUtils.bytes(frame.getValue(name)));
			out.put(HttpUtils.CRLF);
		}
		out.put(HttpUtils.CRLF);
	}
}
