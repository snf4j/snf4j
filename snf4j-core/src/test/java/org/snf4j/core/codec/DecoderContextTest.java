/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.core.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.session.ISession;

public class DecoderContextTest {

	final DecoderContext get(IDecoder<?,?>... decoder) {
		int len = decoder.length;
		DecoderContext d = new DecoderContext("",decoder[len-1]);
		DecoderContext c = d;
		for (int i=len-2; i>=0; --i) {
			DecoderContext t = new DecoderContext("",decoder[i]);
		    c.prev = t;
		    c = t;
		}
		return d;
	}
	
	@Test
	public void testAll() throws Exception {
		AB ab = new AB();
		DecoderContext ctx = new DecoderContext("Name1",ab);
		assertEquals("Name1", ctx.getKey());
		assertFalse(ctx.isInboundByte());
		assertFalse(ctx.isInboundByteArray());
		assertFalse(ctx.isValid(null));
		assertTrue(ctx.isValid(new DecoderContext("",new BA())));
		assertTrue(ctx.isValid(new DecoderContext("",new BAA())));
		assertFalse(ctx.isValid(new DecoderContext("",new BA0())));
		assertFalse(ctx.isValid(new DecoderContext("",new AB())));
		assertTrue(ab == ctx.getDecoder());
		
		ctx = get(new baB());
		assertTrue(ctx.isInboundByte());
		assertTrue(ctx.isInboundByteArray());
		assertTrue(ctx.isValid(null));

		ctx = get(new bbB());
		assertTrue(ctx.isInboundByte());
		assertFalse(ctx.isInboundByteArray());
		assertTrue(ctx.isValid(null));

		ctx = get(new bbbB());
		assertFalse(ctx.isInboundByte());
		assertFalse(ctx.isInboundByteArray());
		assertFalse(ctx.isValid(null));

		ctx = get(new bb0B());
		assertTrue(ctx.isInboundByte());
		assertFalse(ctx.isInboundByteArray());
		assertTrue(ctx.isValid(null));
		assertFalse(ctx.isClogged());
		
		ctx = get(new BV());
		assertFalse(ctx.isInboundByte());
		assertFalse(ctx.isInboundByteArray());
		assertFalse(ctx.isValid(null));
		assertTrue(ctx.isClogged());

		ctx = get(new AB());
		assertFalse(ctx.isValid(new EncoderContext("",new ABE())));
	}
	
	@Test
	public void testIsValidForEmptyOrWithOnlyClogged() {
		DecoderContext ctx = get(new baV());
		assertTrue(ctx.isValid(null));
		assertTrue(ctx.isValid(get(new bbV())));
		assertTrue(ctx.isValid(get(new baV())));
		assertTrue(ctx.isValid(get(new baV(), new bbV())));
		
		ctx = get(new bbV());
		assertTrue(ctx.isValid(null));
		assertTrue(ctx.isValid(get(new bbV())));
		assertTrue(ctx.isValid(get(new baV())));
		assertTrue(ctx.isValid(get(new baV(), new bbV())));
		
		ctx = get(new BV());
		assertFalse(ctx.isValid(null));
		assertFalse(ctx.isValid(get(new bbV())));
		assertFalse(ctx.isValid(get(new baV())));
		assertFalse(ctx.isValid(get(new baV(), new bbV())));
		
		ctx = get(new baB());
		assertTrue(ctx.isValid(null));
		assertTrue(ctx.isValid(get(new bbV())));
		assertTrue(ctx.isValid(get(new baV())));
		assertTrue(ctx.isValid(get(new baV(), new bbV())));
		
		ctx = get(new bbB());
		assertTrue(ctx.isValid(null));
		assertTrue(ctx.isValid(get(new bbV())));
		assertTrue(ctx.isValid(get(new baV())));
		assertTrue(ctx.isValid(get(new baV(), new bbV())));
		
		ctx = get(new bb0B());
		assertTrue(ctx.isValid(null));
		assertTrue(ctx.isValid(get(new bbV())));
		assertTrue(ctx.isValid(get(new baV())));
		assertTrue(ctx.isValid(get(new baV(), new bbV())));
		
		ctx = get(new bbbB());
		assertFalse(ctx.isValid(null));
		assertFalse(ctx.isValid(get(new bbV())));
		assertFalse(ctx.isValid(get(new baV())));
		assertFalse(ctx.isValid(get(new baV(), new bbV())));
	}
	
	@Test
	public void testIsValid() {
		DecoderContext ctx = get(new BV());
		assertTrue(ctx.isValid(get(new bbB())));
		assertTrue(ctx.isValid(get(new baB())));
		assertTrue(ctx.isValid(get(new baV(), new bbB())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB())));
		assertTrue(ctx.isValid(get(new bbB(), new BV())));
		assertTrue(ctx.isValid(get(new baB(), new BV())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BV())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB(), new BV())));

		ctx = get(new AV());
		assertTrue(ctx.isValid(get(new bbB(), new BA())));
		assertTrue(ctx.isValid(get(new baB(), new BA())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BA())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB() ,new BA())));
		assertTrue(ctx.isValid(get(new bbB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new baB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB() ,new BA(), new AV())));

		ctx = get(new A0V());
		assertTrue(ctx.isValid(get(new bbB(), new BA())));
		assertTrue(ctx.isValid(get(new baB(), new BA())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BA())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB() ,new BA())));
		assertTrue(ctx.isValid(get(new bbB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new baB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB() ,new BA(), new AV())));

		ctx = get(new AAV());
		assertFalse(ctx.isValid(get(new bbB(), new BA())));
		assertFalse(ctx.isValid(get(new baB(), new BA())));
		assertFalse(ctx.isValid(get(new baV(), new bbB(), new BA())));
		assertFalse(ctx.isValid(get(new bbV(), new bbB() ,new BA())));
		assertFalse(ctx.isValid(get(new bbB(), new BA(), new AV())));
		assertFalse(ctx.isValid(get(new baB(), new BA(), new AV())));
		assertFalse(ctx.isValid(get(new baV(), new bbB(), new BA(), new AV())));
		assertFalse(ctx.isValid(get(new bbV(), new bbB() ,new BA(), new AV())));
		
		ctx = get(new AB());
		assertTrue(ctx.isValid(get(new bbB(), new BA())));
		assertTrue(ctx.isValid(get(new baB(), new BA())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BA())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB() ,new BA())));
		assertTrue(ctx.isValid(get(new bbB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new baB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB() ,new BA(), new AV())));

		ctx = get(new BA());
		assertTrue(ctx.isValid(get(new bbB())));
		assertTrue(ctx.isValid(get(new baB())));
		assertTrue(ctx.isValid(get(new baV(), new bbB())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB())));
		assertTrue(ctx.isValid(get(new bbB(), new BV())));
		assertTrue(ctx.isValid(get(new baB(), new BV())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BV())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB(), new BV())));

		ctx = get(new A0B());
		assertTrue(ctx.isValid(get(new bbB(), new BA())));
		assertTrue(ctx.isValid(get(new baB(), new BA())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BA())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB() ,new BA())));
		assertTrue(ctx.isValid(get(new bbB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new baB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new baV(), new bbB(), new BA(), new AV())));
		assertTrue(ctx.isValid(get(new bbV(), new bbB() ,new BA(), new AV())));
		
		ctx = get(new AAB());
		assertFalse(ctx.isValid(get(new bbB(), new BA())));
		assertFalse(ctx.isValid(get(new baB(), new BA())));
		assertFalse(ctx.isValid(get(new baV(), new bbB(), new BA())));
		assertFalse(ctx.isValid(get(new bbV(), new bbB() ,new BA())));
		assertFalse(ctx.isValid(get(new bbB(), new BA(), new AV())));
		assertFalse(ctx.isValid(get(new baB(), new BA(), new AV())));
		assertFalse(ctx.isValid(get(new baV(), new bbB(), new BA(), new AV())));
		assertFalse(ctx.isValid(get(new bbV(), new bbB() ,new BA(), new AV())));

	}
	
	static class AB implements IDecoder<A,B> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void decode(ISession session, A data, List<B> out) {}
	}

	static class A0B implements IDecoder<A0,B> {
		@Override public Class<A0> getInboundType() {return A0.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void decode(ISession session, A0 data, List<B> out) {}
	}
	
	static class AAB implements IDecoder<AA,B> {
		@Override public Class<AA> getInboundType() {return AA.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void decode(ISession session, AA data, List<B> out) {}
	}
	
	static class BA implements IDecoder<B,A> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<A> getOutboundType() {return A.class;}
		@Override public void decode(ISession session, B data, List<A> out) {}
	}

	static class BAA implements IDecoder<B,AA> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<AA> getOutboundType() {return AA.class;}
		@Override public void decode(ISession session, B data, List<AA> out) {}
	}

	static class BA0 implements IDecoder<B,A0> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<A0> getOutboundType() {return A0.class;}
		@Override public void decode(ISession session, B data, List<A0> out) {}
	}
	
	static class baV implements IDecoder<byte[],Void> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, byte[] data, List<Void> out) {}
	}

	static class bbV implements IDecoder<ByteBuffer,Void> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, ByteBuffer data, List<Void> out) {}
	}
	
	static class BV implements IDecoder<B,Void> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, B data, List<Void> out) {}
	}
	
	static class AV implements IDecoder<A,Void> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, A data, List<Void> out) {}
	}
	
	static class AAV implements IDecoder<AA,Void> {
		@Override public Class<AA> getInboundType() {return AA.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, AA data, List<Void> out) {}
	}

	static class A0V implements IDecoder<A0,Void> {
		@Override public Class<A0> getInboundType() {return A0.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, A0 data, List<Void> out) {}
	}
	
	static class baB implements IDecoder<byte[],B> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void decode(ISession session, byte[] data, List<B> out) {}
	}

	static class bbB implements IDecoder<ByteBuffer,B> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void decode(ISession session, ByteBuffer data, List<B> out) {}
	}

	static class bbbB implements IDecoder<MappedByteBuffer,B> {
		@Override public Class<MappedByteBuffer> getInboundType() {return MappedByteBuffer.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void decode(ISession session, MappedByteBuffer data, List<B> out) {}
	}

	static class bb0B implements IDecoder<Buffer,B> {
		@Override public Class<Buffer> getInboundType() {return Buffer.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void decode(ISession session, Buffer data, List<B> out) {}
	}
	
	static class ABE implements IEncoder<B,A> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<A> getOutboundType() {return A.class;}
		@Override public void encode(ISession session, B data, List<A> out) {}
	}
	
	static class A0 {}
	static class A extends A0 {}
	static class AA extends A {}
	static class B {}
}
