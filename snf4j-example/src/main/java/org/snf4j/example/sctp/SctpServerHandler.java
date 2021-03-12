package org.snf4j.example.sctp;

import java.nio.ByteBuffer;
import java.util.Random;

import org.snf4j.core.ImmutableSctpMessageInfo;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.allocator.ThreadLocalCachingAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractSctpHandler;
import org.snf4j.core.session.ISctpSessionConfig;

import com.sun.nio.sctp.MessageInfo;

public class SctpServerHandler extends AbstractSctpHandler {

	final static IByteBufferAllocator ALLOCATOR = new ThreadLocalCachingAllocator(true);

	final static ImmutableSctpMessageInfo COMPRESSING = ImmutableSctpMessageInfo.create(1);
	
	final static ImmutableSctpMessageInfo CODING = ImmutableSctpMessageInfo.create(1,0,true);
	
	final static ImmutableSctpMessageInfo DEFAULT = ImmutableSctpMessageInfo.create(0);
	
	final static ImmutableSctpMessageInfo[] STREAMS = new ImmutableSctpMessageInfo[] {
			COMPRESSING, CODING, DEFAULT
	};
	
	final static Random RANDOM = new Random(System.currentTimeMillis());
	
	ImmutableSctpMessageInfo randomStream() {
		return STREAMS[RANDOM.nextInt(STREAMS.length)];
	}
	
	ByteBuffer msg() {
		int size = SctpClient.SIZE;
		ByteBuffer data = getSession().allocate(size);
		
		for (int i=0; i<size; i++) {
			data.put((byte)i);
		}
		data.flip();
		return data;
	}
	
	void check(Object msg) {
		ByteBuffer buf = (ByteBuffer) msg;
		int size = SctpClient.SIZE;
		
		if (buf.remaining() != size) {
			throw new IllegalArgumentException("incorrect msg size");
		}
		ByteBuffer dup = buf.duplicate();
		byte[] bytes = new byte[size];
		
		dup.get(bytes);
		for (int i=0; i<size; ++i) {
			if (bytes[i] != (byte)i) {
				throw new IllegalArgumentException("incorrect msg data");
			}
		}
	}
	
	@Override
	public void read(Object msg, MessageInfo msgInfo) {
		check(msg);
		getSession().writenf(msg, randomStream());
	}

	@Override
	public void exception(Throwable t) {
		System.err.println("ERR: " + t);
	}
	
	@Override
	public ISctpSessionConfig getConfig() {
		return (ISctpSessionConfig) new SessionConfig()
				.setOptimizeDataCopying(true)
				.setMinOutBufferCapacity(SctpClient.SIZE << 1);
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new DefaultSessionStructureFactory() {
			
			@Override
			public IByteBufferAllocator getAllocator() {
				return ALLOCATOR;
			}
		};
	}
}
