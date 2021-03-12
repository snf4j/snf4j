package org.snf4j.example.sctp;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.session.ISession;

public class Encoder implements IEncoder<ByteBuffer,ByteBuffer>{

	@Override
	public Class<ByteBuffer> getInboundType() {
		return ByteBuffer.class;
	}

	@Override
	public Class<ByteBuffer> getOutboundType() {
		return ByteBuffer.class;
	}

	@Override
	public void encode(ISession session, ByteBuffer data, List<ByteBuffer> out) throws Exception {
		byte[] bytes = new byte[data.remaining()];
		
		data.get(bytes);
		for (int i=0; i<bytes.length; ++i) {
			bytes[i] ^= 0xaa;
		}
		data.clear();
		data.put(bytes).flip();
		out.add(data);
	}

}
