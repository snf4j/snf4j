/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.core;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.session.ISession;

public class TestCodec {
	
	volatile Exception decodeException;
	
	volatile Exception encodeException;
	
	volatile boolean discardingDecode;
	
	volatile boolean duplicatingDecode;
	
	volatile boolean decodeClose;
	volatile boolean decodeQuickClose;
	volatile boolean decodeDirtyClose;

	volatile boolean encodeClose;
	volatile boolean encodeQuickClose;
	volatile boolean encodeDirtyClose;
	volatile boolean encodeFakeClosing;

	IDecoder<?,?> PPD() { return new PPD(); }
	IDecoder<?,?> PBD() { return new PBD(); }
	IBaseDecoder<?> BasePD() { return new BasePD(); }
	IDecoder<?,?> BPD() { return new BPD(); }
	IEncoder<?,?> PBE() { return new PBE(); }
	IEncoder<?,?> PBBE() { return new PBBE(); }
	IEncoder<?,?> BPE() { return new BPE(); }
	
	class PPD implements IDecoder<Packet, Packet> {
		@Override public Class<Packet> getInboundType() {return Packet.class;}
		@Override public Class<Packet> getOutboundType() {return Packet.class;}
		@Override public void decode(ISession session, Packet data, List<Packet> out)throws Exception {
			out.add(data);
			out.add(data);
		}
	}

	class PBD implements IDecoder<Packet, byte[]> {
		@Override public Class<Packet> getInboundType() {return Packet.class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void decode(ISession session, Packet data, List<byte[]> out)throws Exception {
			data.payload = data.payload + "d";
			if (!discardingDecode) {
				out.add(data.toBytes());
			}
			if (duplicatingDecode) {
				out.add(data.toBytes());
				out.add(data.toBytes());
			}
			if (decodeException != null) {
				throw decodeException;
			}
			if (decodeClose) {
				session.close();
			}
			else if (decodeQuickClose) {
				session.quickClose();
			}
			else if (decodeDirtyClose) {
				session.dirtyClose();
			}
		}
	}
	
	class BasePD implements IBaseDecoder<Packet> {
		@Override public void decode(ISession session, byte[] data, List<Packet> out) throws Exception {
			out.add(Packet.fromBytes(data));
		}
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<Packet> getOutboundType() {return Packet.class;}
		@Override public int available(ISession session, ByteBuffer buffer, boolean flipped) {
			int len = flipped ? buffer.remaining() : buffer.position();
			byte[] data = new byte[len];
			ByteBuffer dup = buffer.duplicate();
			
			if (!flipped) {
				dup.flip();
			}
			dup.get(data);
			return Packet.available(data, 0, len);
		}
		@Override public int available(ISession session, byte[] buffer, int off, int len) {
			return Packet.available(buffer, off, len);
		}
	}

	class BPD implements IDecoder<byte[],Packet> {
		@Override public void decode(ISession session, byte[] data, List<Packet> out) throws Exception {
			out.add(Packet.fromBytes(data));
		}
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<Packet> getOutboundType() {return Packet.class;}
	}

	class PBE implements IEncoder<Packet, byte[]> {
		@Override public Class<Packet> getInboundType() {return Packet.class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void encode(ISession session, Packet data, List<byte[]> out)throws Exception {
			data.payload = data.payload + "e";
			out.add(data.toBytes());
			if (encodeException != null) {
				throw encodeException;
			}
			if (encodeClose) {
				session.close();
			}
			else if (encodeQuickClose) {
				session.quickClose();
			}
			else if (encodeDirtyClose) {
				session.dirtyClose();
			}
			else if (encodeFakeClosing) {
				((InternalSession)session).closing = ClosingState.FINISHED;
			}
		}
	}

	class PBBE implements IEncoder<Packet, ByteBuffer> {
		@Override public Class<Packet> getInboundType() {return Packet.class;}
		@Override public Class<ByteBuffer> getOutboundType() {return ByteBuffer.class;}
		@Override public void encode(ISession session, Packet data, List<ByteBuffer> out)throws Exception {
			data.payload = data.payload + "e2";
			out.add(ByteBuffer.wrap(data.toBytes()));
			if (encodeException != null) {
				throw encodeException;
			}
		}
	}
	
	class BPE implements IEncoder<byte[],Packet> {
		@Override public void encode(ISession session, byte[] data, List<Packet> out) throws Exception {
			out.add(Packet.fromBytes(data));
		}
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<Packet> getOutboundType() {return Packet.class;}
	}	

}
