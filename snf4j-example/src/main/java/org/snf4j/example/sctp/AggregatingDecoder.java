package org.snf4j.example.sctp;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.session.ISession;

public class AggregatingDecoder implements IDecoder<ByteBuffer,ByteBuffer> {
	
	private final int size;
	
	private ByteBuffer buffer;
	
	AggregatingDecoder(int size) {
		this.size = size;
	}
	
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
		if (buffer == null) {
			if (data.remaining() == size) {
				out.add(data);
				return;
			}
			buffer = session.allocate(size);
		}
		
		if (data.remaining() <= buffer.remaining()) {
			buffer.put(data);
		}
		else {
			ByteBuffer dup = data.duplicate();
			dup.limit(dup.position()+buffer.remaining());
			buffer.put(dup);
			data.position(dup.position());
		}
		
		if (!buffer.hasRemaining()) {
			buffer.flip();
			out.add(buffer);
			buffer = null;
			decode(session, data, out);
		}
	}

}
