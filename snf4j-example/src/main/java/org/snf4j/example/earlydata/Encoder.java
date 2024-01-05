package org.snf4j.example.earlydata;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.session.ISession;

public class Encoder implements IEncoder<String, ByteBuffer> {

	private final int shift;
	
	Encoder(int shift) {
		this.shift = shift;
	}
	
	@Override
	public void encode(ISession session, String data, List<ByteBuffer> out) throws Exception {
		byte[] bytes = data.getBytes(StandardCharsets.US_ASCII);
		
		for (int i=0; i<bytes.length; ++i) {
			bytes[i] += shift;
		}
		ByteBuffer buffer = session.allocate(bytes.length + 2);
		buffer.putShort((short) bytes.length);
		buffer.put(bytes).flip();
		out.add(buffer);
	}
	
	@Override
	public Class<String> getInboundType() {
		return String.class;
	}

	@Override
	public Class<ByteBuffer> getOutboundType() {
		return ByteBuffer.class;
	}

}
