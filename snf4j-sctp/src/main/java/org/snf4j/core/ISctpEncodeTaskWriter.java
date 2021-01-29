package org.snf4j.core;

import java.nio.ByteBuffer;

import org.snf4j.core.future.IFuture;

interface ISctpEncodeTaskWriter extends IEncodeTaskWriter {
	
	IFuture<Void> write(ImmutableSctpMessageInfo msgInfo, ByteBuffer buffer, boolean withFuture);

	IFuture<Void> write(ImmutableSctpMessageInfo msgInfo, byte[] bytes, boolean withFuture);
}
