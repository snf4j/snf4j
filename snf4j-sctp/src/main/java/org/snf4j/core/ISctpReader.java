package org.snf4j.core;

import java.nio.ByteBuffer;

import com.sun.nio.sctp.MessageInfo;

public interface ISctpReader {
	
	void read(byte[] msg, MessageInfo msgInfo);
	
	void read(ByteBuffer msg, MessageInfo msgInfo);
}
