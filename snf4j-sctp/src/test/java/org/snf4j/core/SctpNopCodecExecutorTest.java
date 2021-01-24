package org.snf4j.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.codec.ICodecExecutor;

public class SctpNopCodecExecutorTest {

	@Test
	public void testAll() throws Exception {
		ICodecExecutor e = SctpNopCodecExecutor.INSTANCE;
		
		assertNull(e.getPipeline());
		e.syncDecoders();
		e.syncEncoders();
		e.syncEventDrivenCodecs(null);
		assertNull(e.getBaseDecoder());
		assertFalse(e.hasDecoders());
		assertNull(e.encode(null, (ByteBuffer)null));
		assertNull(e.encode(null, (byte[])null));
		assertNull(e.encode(null, (Object)null));
		assertNull(e.decode(null, (byte[])null));
		assertNull(e.decode(null, (ByteBuffer)null));
		e.event(null, null);
	}
}
