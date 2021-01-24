package org.snf4j.core;

import java.nio.ByteBuffer;

import org.snf4j.core.future.IFuture;

import com.sun.nio.sctp.MessageInfo;

interface ISctpEncodeTaskWriter extends IEncodeTaskWriter {
	
	IFuture<Void> write(MessageInfo msgInfo, ByteBuffer buffer, boolean withFuture);

	IFuture<Void> write(MessageInfo msgInfo, byte[] bytes, boolean withFuture);
}
