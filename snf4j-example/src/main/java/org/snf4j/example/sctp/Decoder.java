package org.snf4j.example.sctp;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.session.ISession;

public class Decoder implements IDecoder<ByteBuffer,ByteBuffer> {

	@Override
	public Class<ByteBuffer> getInboundType() {
		return ByteBuffer.class;
	}

	@Override
	public Class<ByteBuffer> getOutboundType() {
		return ByteBuffer.class;
	}

	@Override
	public void decode(ISession session, ByteBuffer data, List<ByteBuffer> out) throws Exception {
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
