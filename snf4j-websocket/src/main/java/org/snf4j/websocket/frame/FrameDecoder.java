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
package org.snf4j.websocket.frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IStreamSession;

/**
 * Decodes a Web Socket frame from bytes in the protocol version 13 format.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class FrameDecoder implements IBaseDecoder<ByteBuffer,Frame>{

	private final boolean clientMode;
	
	private final long maxPayloadLen;
	
	private final boolean allowExtensions;
	
	private boolean fragmentation;
	
	private Opcode opcode;
	
	private boolean fin;
	
	private int rsv;
	
	private byte[] mask;
	
	private byte[] payload;
	
	private int payloadLen;
	
	private boolean closed;
	
	/**
	 * Constructs a Web Socket decoder.
	 * 
	 * @param clientMode      determines the mode (client/server) in which the
	 *                        decoder should work
	 * @param allowExtensions determines it the decoder should allow to use the
	 *                        reserved extension bits
	 * @param maxPayloadLen   maximum length of a frame's payload data. Setting it
	 *                        to an appropriate value can prevent from denial of
	 *                        service attacks
	 */
	public FrameDecoder(boolean clientMode, boolean allowExtensions, long maxPayloadLen) {
		this.clientMode = clientMode;
		this.maxPayloadLen = maxPayloadLen;
		this.allowExtensions = allowExtensions;
	}
	
	@Override
	public Class<ByteBuffer> getInboundType() {
		return ByteBuffer.class;
	}

	@Override
	public Class<Frame> getOutboundType() {
		return Frame.class;
	}

	private RuntimeException protocolError(ISession session, String message) throws InvalidFrameException {
		closed = true;
		((IStreamSession)session).writenf(new CloseFrame(CloseFrame.PROTOCOL_ERROR));
		return new InvalidFrameException(message);
	}
	
	private RuntimeException nonUtf8(ISession session, String message) throws InvalidFrameException {
		closed = true;
		((IStreamSession)session).writenf(new CloseFrame(CloseFrame.NON_UTF8));
		return new InvalidFrameException(message);
	}
	
	private Frame createFrame(ISession session, Opcode opcode, boolean fin, int rsv, byte[] payload) {
		Frame frame = null;
		int len;
		
		switch (opcode) {
		case CONTINUATION:
			frame = new ContinuationFrame(fin, rsv, payload);
			break;
			
		case BINARY:
			frame = new BinaryFrame(fin, rsv, payload);
			break;
			
		case TEXT:
			frame = new TextFrame(fin, rsv, payload);
			break;
			
		case CLOSE:
			len = payload.length;
			if (len > 0) {
				int status = len16(payload, 0);
				
			    if (status <= 999 || status > 4999) {
			    	throw protocolError(session, "Invalid close frame status code (" + status + ")");
			    }
			    if (len > 2) {
			    	if (!Utf8.isValid(payload, 2, len-2)) {
			    		throw nonUtf8(session, "Invalid close frame reason value: bytes are not UTF-8");
			    	}
			    }
			}
			frame = new CloseFrame(rsv, payload);
			break;
			
		case PING:
			frame = new PingFrame(rsv, payload);
			break;
			
		case PONG:
			frame = new PongFrame(rsv, payload);
			break;
		}
		
		if (frame.isFinalFragment()) {
			if (!frame.getOpcode().isControl()) {
				fragmentation = false;
			}
		}
		else {
			fragmentation = true;
		}
		
		return frame;
	}
	
	private void decodePayload(ISession session, ByteBuffer data, List<Frame> out) {
		int off = payloadLen;
		
		if (data.remaining() == payload.length - off) {
			data.get(payload, off, data.remaining());
			if (mask != null) {
				for (int i=0; i<payload.length; ++i) {
					payload[i] ^= mask[i % 4];
				}
				mask = null;
			}
			out.add(createFrame(session, opcode, fin, rsv, payload));
			opcode = null;
			payload = null;
		}
		else {
			payloadLen = off + data.remaining();
			data.get(payload, off, data.remaining());
		}
	}
	
	@Override
	public void decode(ISession session, ByteBuffer data, List<Frame> out) throws Exception {
		Opcode opcode;
		byte b;
		
		if (closed) {
			return;
		}
		
		try {
			data.order(ByteOrder.BIG_ENDIAN);

			if (this.opcode != null) {
				decodePayload(session, data, out);
				return;
			}
			
			b = data.get();
			opcode = Opcode.findByValue(b & Opcode.VALUE_MASK);
			if (opcode == null) {
				throw protocolError(session, "Unexpected opcode value (" + (b & Opcode.VALUE_MASK) + ")");
			}

			boolean fin = (b & 0x80) != 0;
			int rsv = (b >> 4) & 0x07;

			if (rsv != 0 && !allowExtensions) {
				throw protocolError(session, "Unexpected non-zero RSV bits (" + rsv + ")");
			}
			b = data.get();

			boolean masked = (b & 0x80) != 0;
			long payloadLen = b & 0x7f;

			if (masked == clientMode) {
				throw protocolError(session, "Unexpected payload masking");
			}

			if (opcode.isControl()) {
				if (!fin) {
					throw protocolError(session, "Fragmented control frame");
				}
				if (payloadLen > 125) {
					throw protocolError(session, "Invalid payload length (" + payloadLen + ") in control frame");
				}
				if (opcode == Opcode.CLOSE && payloadLen == 1) {
					throw protocolError(session, "Invalid payload length (" + payloadLen + ") in close frame");
				}
			}
			else if (opcode == Opcode.CONTINUATION) {
				if (!fragmentation) {
					throw protocolError(session, "Continuation frame outside fragmented message");
				}
			}
			else if (fragmentation) {
				throw protocolError(session, "Non-continuation frame while inside fragmented massage");
			}

			if (payloadLen == 126) {
				payloadLen = (long)data.getShort() & 0xffff;
				if (payloadLen < 126) {
					throw protocolError(session, "Invalid minimal payload length");
				}
			}
			else if (payloadLen == 127) {
				payloadLen = data.getLong();
				if (payloadLen < 0 || payloadLen > Integer.MAX_VALUE) {
					throw protocolError(session, "Invalid maximum payload length");
				}
				if (payloadLen <= 0xffff) {
					throw protocolError(session, "Invalid minimal payload length");
				}
			}

			if (payloadLen > maxPayloadLen) {
				throw protocolError(session, "Maximum frame length (" + maxPayloadLen +") has been exceeded");
			}

			if (masked) {
				mask = new byte[4];
				data.get(mask);
			}

			byte[] payload = new byte[(int) payloadLen];
			int remaining = data.remaining();
			
			if (payload.length == remaining) {
				data.get(payload);
				if (mask != null) {
					for (int i=0; i<remaining; ++i) {
						payload[i] ^= mask[i % 4];
					}
					mask = null;
				}
				out.add(createFrame(session, opcode, fin, rsv, payload));
			}
			else {
				data.get(payload, 0, remaining);
				this.opcode = opcode;
				this.fin = fin;
				this.rsv = rsv;
				this.payload = payload;
				this.payloadLen = remaining; 
			}
		}
		finally {
			session.release(data);
		}
	}

	@Override
	public int available(ISession session, ByteBuffer buffer, boolean flipped) {
		int pos, len;
		
		if (flipped) {
			pos = buffer.position();
			len = buffer.remaining();
		}
		else {
			pos = 0;
			len = buffer.position();
		}

		if (closed) {
			return len;
		}
		if (opcode != null) {
			return availablePayload(len);
		}
		
		if (buffer.hasArray()) {
			return available(session, buffer.array(), buffer.arrayOffset()+pos, len);
		}
		
		int need = 2;
		
		if (len < need) return 0;
		int plen = buffer.get(pos+1) & 0x7f;
		if (plen == 126) {
			need += 2;
		}
		else if (plen == 127) {
			need += 8;
		}
		byte[] bytes = new byte[need];
		ByteBuffer dup = buffer.duplicate();
		
		if (!flipped) {
			dup.flip();
		}
		dup.get(bytes);
		return available(session, bytes, 0, len);
	}

	static int len16(byte[] data, int off) {
		return ((int)data[off] << 8) & 0xff00 | ((int)data[off+1] & 0x00ff); 
	}
	
	static long len64(byte[] data, int off) {
		long len = 0;
		
		for (int i=0; i<8; ++i) {
			len <<= 8;
			len |= (long)data[off++] & 0xff;
		}
		return len;
	}
	
	private int availablePayload(int len) {
		int needed = payload.length - payloadLen;
		
		if (len >= needed) {
			return needed;
		}
		return len;
	}
	
	@Override
	public int available(ISession session, byte[] buffer, int off, int len) {
		if (closed) {
			return len;
		}
		if (opcode != null) {
			return availablePayload(len);
		}
		
		int need = 2;
		
		if (len < need) return 0;
		if ((buffer[off+1] & 0x80) != 0) {
			need += 4;
			if (len < need) return 0;
		}
		
		long plen = buffer[off+1] & 0x7f;
		if (plen < 126) {
			need += plen;
		}
		else if (plen == 126) {
			need += 2;
			if (len < need) return 0;
			plen = len16(buffer, off+2); 
			need += plen;
		}
		else {
			need += 8;
			if (len < need) return 0;
			off += 2;
			plen = len64(buffer, off);
			if (plen < 0L) {
				throw protocolError(session, "Negative payload length (" + plen + ")");
			}
			if (plen + need > (long)Integer.MAX_VALUE) {
				throw protocolError(session, "Extended payload length (" + plen + ") > " + (Integer.MAX_VALUE-need));
			}
			need += plen;
		}
		if (len > need) {
			return need;
		}
		return len;
	}

}
