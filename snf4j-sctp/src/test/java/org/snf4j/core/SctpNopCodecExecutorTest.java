/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021-2022 SNF4J contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * -----------------------------------------------------------------------------
 */
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
		e.syncDecoders(null);
		e.syncEncoders(null);
		e.syncEventDrivenCodecs(null);
		assertNull(e.getBaseDecoder());
		assertFalse(e.hasDecoders());
		assertNull(e.encode(null, (ByteBuffer)null));
		assertNull(e.encode(null, (IByteBufferHolder)null));
		assertNull(e.encode(null, (byte[])null));
		assertNull(e.encode(null, (Object)null));
		assertNull(e.decode(null, (byte[])null));
		assertNull(e.decode(null, (ByteBuffer)null));
		e.event(null, null);
		e.addChild(null, null);
	}
}
