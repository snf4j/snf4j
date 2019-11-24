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

public class EncoderContextTest {

	final EncoderContext get(IEncoder<?,?>... encoder) {
		int len = encoder.length;
		EncoderContext d = new EncoderContext("",encoder[len-1]);
		EncoderContext c = d;
		for (int i=len-2; i>=0; --i) {
			EncoderContext t = new EncoderContext("",encoder[i]);
		    c.prev = t;
		    c = t;
		}
		return d;
	}
	
	@Test
	public void testAll() throws Exception {
		AB ab = new AB();
		EncoderContext ctx = new EncoderContext("Name1", ab);
		assertEquals("Name1", ctx.getKey());
		assertFalse(ctx.isOutboundByte());
		assertFalse(ctx.isOutboundByteArray());
		assertFalse(ctx.isValid(null));
		assertTrue(ctx.isValid(get(new BA())));
		assertFalse(ctx.isValid(get(new BBA())));
		assertTrue(ctx.isValid(get(new B0A())));
		assertFalse(ctx.isValid(get(new AB())));
		assertTrue(ab == ctx.getEncoder());
		
		ctx = get(new B_ba());
		assertTrue(ctx.isOutboundByte());
		assertTrue(ctx.isOutboundByteArray());
		assertTrue(ctx.isValid(null));

		ctx = get(new B_bb());
		assertTrue(ctx.isOutboundByte());
		assertFalse(ctx.isOutboundByteArray());
		assertTrue(ctx.isValid(null));

		ctx = get(new B_bbb());
		assertTrue(ctx.isOutboundByte());
		assertFalse(ctx.isOutboundByteArray());
		assertTrue(ctx.isValid(null));

		ctx = get(new B_b0());
		assertFalse(ctx.isOutboundByte());
		assertFalse(ctx.isOutboundByteArray());
		assertFalse(ctx.isValid(null));
		assertFalse(ctx.isClogged());
		
		ctx = get(new BV());
		assertFalse(ctx.isInboundByte());
		assertFalse(ctx.isInboundByteArray());
		assertFalse(ctx.isValid(null));
		assertTrue(ctx.isClogged());
		
		ctx = get(new AB());
		assertFalse(ctx.isValid(new DecoderContext("",new ABD())));
	}
	
	@Test
	public void testIsValidForEmptyOrWithOnlyClogged() {
		EncoderContext ctx = get(new baV());
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
		
		ctx = get(new B_ba());
		assertTrue(ctx.isValid(null));
		assertTrue(ctx.isValid(get(new bbV())));
		assertTrue(ctx.isValid(get(new baV())));
		assertTrue(ctx.isValid(get(new baV(), new bbV())));

		ctx = get(new B_bb());
		assertTrue(ctx.isValid(null));
		assertTrue(ctx.isValid(get(new bbV())));
		assertTrue(ctx.isValid(get(new baV())));
		assertTrue(ctx.isValid(get(new baV(), new bbV())));
		
		ctx = get(new B_bbb());
		assertTrue(ctx.isValid(null));
		assertTrue(ctx.isValid(get(new bbV())));
		assertTrue(ctx.isValid(get(new baV())));
		assertTrue(ctx.isValid(get(new baV(), new bbV())));
		
		ctx = get(new B_b0());
		assertFalse(ctx.isValid(null));
		assertFalse(ctx.isValid(get(new bbV())));
		assertFalse(ctx.isValid(get(new baV())));
		assertFalse(ctx.isValid(get(new baV(), new bbV())));
	
	}
	
	@Test
	public void testIsValid() {
		EncoderContext ctx = get(new BV());
		assertTrue(ctx.isValid(get(new B_bb())));
		assertTrue(ctx.isValid(get(new B_ba())));
		assertTrue(ctx.isValid(get(new baV(), new B_bb())));
		assertTrue(ctx.isValid(get(new bbV(), new B_bb())));
		assertTrue(ctx.isValid(get(new B_bb(), new BV())));
		assertTrue(ctx.isValid(get(new B_ba(), new BV())));
		assertTrue(ctx.isValid(get(new baV(), new B_bb(), new BV())));
		assertTrue(ctx.isValid(get(new bbV(), new B_bb(), new BV())));
		
		ctx = get(new AV());
		assertFalse(ctx.isValid(get(new B_bb())));
		assertFalse(ctx.isValid(get(new B_ba())));
		assertFalse(ctx.isValid(get(new baV(), new B_bb())));
		assertFalse(ctx.isValid(get(new bbV(), new B_bb())));
		assertFalse(ctx.isValid(get(new B_bb(), new BV())));
		assertFalse(ctx.isValid(get(new B_ba(), new BV())));
		assertFalse(ctx.isValid(get(new baV(), new B_bb(), new BV())));
		assertFalse(ctx.isValid(get(new bbV(), new B_bb(), new BV())));

		ctx = get(new B0V());
		assertTrue(ctx.isValid(get(new B_bb())));
		assertTrue(ctx.isValid(get(new B_ba())));
		assertTrue(ctx.isValid(get(new baV(), new B_bb())));
		assertTrue(ctx.isValid(get(new bbV(), new B_bb())));
		assertTrue(ctx.isValid(get(new B_bb(), new BV())));
		assertTrue(ctx.isValid(get(new B_ba(), new BV())));
		assertTrue(ctx.isValid(get(new baV(), new B_bb(), new BV())));
		assertTrue(ctx.isValid(get(new bbV(), new B_bb(), new BV())));

		ctx = get(new BB_V());
		assertFalse(ctx.isValid(get(new B_bb())));
		assertFalse(ctx.isValid(get(new B_ba())));
		assertFalse(ctx.isValid(get(new baV(), new B_bb())));
		assertFalse(ctx.isValid(get(new bbV(), new B_bb())));
		assertFalse(ctx.isValid(get(new B_bb(), new BV())));
		assertFalse(ctx.isValid(get(new B_ba(), new BV())));
		assertFalse(ctx.isValid(get(new baV(), new B_bb(), new BV())));
		assertFalse(ctx.isValid(get(new bbV(), new B_bb(), new BV())));
		
		ctx = get(new AB());
		assertTrue(ctx.isValid(get(new B_bb())));
		assertTrue(ctx.isValid(get(new B_ba())));
		assertTrue(ctx.isValid(get(new baV(), new B_bb())));
		assertTrue(ctx.isValid(get(new bbV(), new B_bb())));
		assertTrue(ctx.isValid(get(new B_bb(), new BV())));
		assertTrue(ctx.isValid(get(new B_ba(), new BV())));
		assertTrue(ctx.isValid(get(new baV(), new B_bb(), new BV())));
		assertTrue(ctx.isValid(get(new bbV(), new B_bb(), new BV())));
		
		ctx = get(new BA());
		assertFalse(ctx.isValid(get(new B_bb())));
		assertFalse(ctx.isValid(get(new B_ba())));
		assertFalse(ctx.isValid(get(new baV(), new B_bb())));
		assertFalse(ctx.isValid(get(new bbV(), new B_bb())));
		assertFalse(ctx.isValid(get(new B_bb(), new BV())));
		assertFalse(ctx.isValid(get(new B_ba(), new BV())));
		assertFalse(ctx.isValid(get(new baV(), new B_bb(), new BV())));
		assertFalse(ctx.isValid(get(new bbV(), new B_bb(), new BV())));
		
		ctx = get(new ABB());
		assertTrue(ctx.isValid(get(new B_bb())));
		assertTrue(ctx.isValid(get(new B_ba())));
		assertTrue(ctx.isValid(get(new baV(), new B_bb())));
		assertTrue(ctx.isValid(get(new bbV(), new B_bb())));
		assertTrue(ctx.isValid(get(new B_bb(), new BV())));
		assertTrue(ctx.isValid(get(new B_ba(), new BV())));
		assertTrue(ctx.isValid(get(new baV(), new B_bb(), new BV())));
		assertTrue(ctx.isValid(get(new bbV(), new B_bb(), new BV())));

		ctx = get(new AB0());
		assertFalse(ctx.isValid(get(new B_bb())));
		assertFalse(ctx.isValid(get(new B_ba())));
		assertFalse(ctx.isValid(get(new baV(), new B_bb())));
		assertFalse(ctx.isValid(get(new bbV(), new B_bb())));
		assertFalse(ctx.isValid(get(new B_bb(), new BV())));
		assertFalse(ctx.isValid(get(new B_ba(), new BV())));
		assertFalse(ctx.isValid(get(new baV(), new B_bb(), new BV())));
		assertFalse(ctx.isValid(get(new bbV(), new B_bb(), new BV())));
		
	}
	
	static class AB implements IEncoder<A,B> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void encode(ISession session, A data, List<B> out) {}
	}

	static class ABB implements IEncoder<A,BB> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<BB> getOutboundType() {return BB.class;}
		@Override public void encode(ISession session, A data, List<BB> out) {}
	}

	static class AB0 implements IEncoder<A,B0> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<B0> getOutboundType() {return B0.class;}
		@Override public void encode(ISession session, A data, List<B0> out) {}
	}

	static class BA implements IEncoder<B,A> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<A> getOutboundType() {return A.class;}
		@Override public void encode(ISession session, B data, List<A> out) {}
	}

	static class BBA implements IEncoder<BB,A> {
		@Override public Class<BB> getInboundType() {return BB.class;}
		@Override public Class<A> getOutboundType() {return A.class;}
		@Override public void encode(ISession session, BB data, List<A> out) {}
	}

	static class AV implements IEncoder<A,Void> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, A data, List<Void> out) {}
	}

	static class baV implements IEncoder<byte[],Void> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, byte[] data, List<Void> out) {}
	}

	static class bbV implements IEncoder<ByteBuffer,Void> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, ByteBuffer data, List<Void> out) {}
	}
	
	static class BV implements IEncoder<B,Void> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, B data, List<Void> out) {}
	}
	
	static class B0V implements IEncoder<B0,Void> {
		@Override public Class<B0> getInboundType() {return B0.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, B0 data, List<Void> out) {}
	}
	
	static class BB_V implements IEncoder<BB,Void> {
		@Override public Class<BB> getInboundType() {return BB.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, BB data, List<Void> out) {}
	}

	static class B0A implements IEncoder<B0,A> {
		@Override public Class<B0> getInboundType() {return B0.class;}
		@Override public Class<A> getOutboundType() {return A.class;}
		@Override public void encode(ISession session, B0 data, List<A> out) {}
	}
	
	static class B_ba implements IEncoder<B,byte[]> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void encode(ISession session, B data, List<byte[]> out) {}
	}

	static class B_bb implements IEncoder<B,ByteBuffer> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<ByteBuffer> getOutboundType() {return ByteBuffer.class;}
		@Override public void encode(ISession session, B data, List<ByteBuffer> out) {}
	}

	static class B_bbb implements IEncoder<B,MappedByteBuffer> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<MappedByteBuffer> getOutboundType() {return MappedByteBuffer.class;}
		@Override public void encode(ISession session, B data, List<MappedByteBuffer> out) {}
	}

	static class B_b0 implements IEncoder<B,Buffer> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<Buffer> getOutboundType() {return Buffer.class;}
		@Override public void encode(ISession session, B data, List<Buffer> out) {}
	}

	static class ABD implements IDecoder<A,B> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void decode(ISession session, A data, List<B> out) {}
	}
	
	static class A {}
	static class B0 {}
	static class B extends B0 {}
	static class BB extends B {}
}
