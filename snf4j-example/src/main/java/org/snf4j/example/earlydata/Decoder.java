package org.snf4j.example.earlydata;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.session.ISession;

public class Decoder implements IBaseDecoder<ByteBuffer, String> {
	
	private final int shift;
	
	Decoder(int shift) {
		this.shift = shift;
	}

	@Override
	public void decode(ISession session, ByteBuffer data, List<String> out) throws Exception {
		byte[] bytes = new byte[data.remaining()-2];
		
		data.position(2);
		data.get(bytes);
		session.release(data);
		for (int i=0; i<bytes.length; ++i) {
			bytes[i] -= shift;
		}
		out.add(new String(bytes, StandardCharsets.US_ASCII));
	}

	@Override
	public Class<ByteBuffer> getInboundType() {
		return ByteBuffer.class;
	}

	@Override
	public Class<String> getOutboundType() {
		return String.class;
	}

	@Override
	public int available(ISession session, ByteBuffer buffer, boolean flipped) {
		if (flipped) {
			if (buffer.remaining() >= 2) {
				int length = buffer.getShort(buffer.position()) + 2;
				
				if (buffer.remaining() >= length) {
					return length;
				}
			}
		}
		else if (buffer.position() >= 2) {
			int length = buffer.getShort(0) + 2;
			
			if (buffer.position() >= length) {
				return length;
			}
		}
		return 0;
	}

	@Override
	public int available(ISession session, byte[] buffer, int off, int len) {
		if (buffer.length >= 2) {
			int length = ByteBuffer.wrap(buffer).getShort() + 2;
					
			if (buffer.length >= length) {
				return length;
			}
		}
		return 0;
	}

}
