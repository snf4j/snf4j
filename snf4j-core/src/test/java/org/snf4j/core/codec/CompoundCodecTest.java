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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.session.ISession;

public class CompoundCodecTest {

	StringBuilder trace = new StringBuilder();
	
	void trace(String s) {
		trace.append(s);
		trace.append('|');
	}
	
	String getTrace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	@Test
	public void testContructor() throws Exception {
		CompoundAB c = new CompoundAB();
		List<B> out = new ArrayList<B>();
		
		c.encode(null, new A(), out);
		assertEquals(0, out.size());
		assertEquals("", getTrace());
		assertEquals("encoder", c.type());

		CompoundABD cd = new CompoundABD();
		assertEquals("decoder", cd.type());
		
		//A0x
		
		try {
			c = new CompoundAB(new A0B0()); fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("last encoder has incompatible outbound type", e.getMessage());
		}
		
		out.clear();
		c = new CompoundAB(new A0B());
		c.encode(null, new A(), out);
		assertEquals(1, out.size());
		assertEquals("A0B|", getTrace());

		out.clear();
		c = new CompoundAB(new A0BB());
		c.encode(null, new A(), out);
		assertEquals(1, out.size());
		assertEquals("A0BB|", getTrace());
		
		//Ax
		
		try {
			c = new CompoundAB(new AB0()); fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("last encoder has incompatible outbound type", e.getMessage());
		}
		
		out.clear();
		c = new CompoundAB(new AB());
		c.encode(null, new A(), out);
		assertEquals(1, out.size());
		assertEquals("AB|", getTrace());
		
		out.clear();
		c = new CompoundAB(new ABB());
		c.encode(null, new A(), out);
		assertEquals(1, out.size());
		assertEquals("ABB|", getTrace());
		
		//AAx
		
		try {
			c = new CompoundAB(new AAB0()); fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("incompatible encoder(s)", e.getMessage());
		}
		try {
			c = new CompoundAB(new AAB()); fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("incompatible encoder(s)", e.getMessage());
		}
		try {
			c = new CompoundAB(new AABB()); fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("incompatible encoder(s)", e.getMessage());
		}

		
		//Void
		out.clear();
		c = new CompoundAB(new AV(), new AB(), new BV());
		c.encode(null, new A(), out);
		assertEquals(1, out.size());
		assertEquals("AV|AB|BV|", getTrace());
		
		out.clear();
		c = new CompoundAB(new AV(), new AB());
		c.encode(null, new A(), out);
		assertEquals(1, out.size());
		assertEquals("AV|AB|", getTrace());

		out.clear();
		c = new CompoundAB(new AB(), new BV());
		c.encode(null, new A(), out);
		assertEquals(1, out.size());
		assertEquals("AB|BV|", getTrace());
		
		try {
			new CompoundAB(new AV()); fail();
		} catch (IllegalArgumentException e) {
			assertEquals("no encoder produces output", e.getMessage());			
		}

		//more encoders
		out.clear();
		c = new CompoundAB(new AS(), new SA(), new AB());
		c.encode(null, new A(), out);
		assertEquals(1, out.size());
		assertEquals("AS|SA|AB|", getTrace());
		
		out.clear();
		c = new CompoundAB(new AV(), new AS(), new SA(), new AV(), new AB(), new BV());
		c.encode(null, new A(), out);
		assertEquals(1, out.size());
		assertEquals("AV|AS|SA|AV|AB|BV|", getTrace());

		try {
			new CompoundAB(new AS(), new AB()); fail();
		} catch (IllegalArgumentException e) {
			assertEquals("incompatible encoder(s)", e.getMessage());			
		}
		try {
			new CompoundAB(new BV(), new AS(), new SA(), new AB()); fail();
		} catch (IllegalArgumentException e) {
			assertEquals("incompatible encoder(s)", e.getMessage());			
		}
		try {
			new CompoundAB(new AS(), new SA(), new AB(), new AV()); fail();
		} catch (IllegalArgumentException e) {
			assertEquals("incompatible encoder(s)", e.getMessage());			
		}
		
		try {
			new CompoundAV(); fail();
		} catch (IllegalStateException e) {
			assertEquals("compound encoder with void output", e.getMessage());			
		}
	}
	
	class CompoundAB extends CompoundEncoder<A,B> {

		CompoundAB(IEncoder<?,?>... encoders) {
			super(encoders);
		}
		
		@Override
		public Class<A> getInboundType() {
			return A.class;
		}

		@Override
		public Class<B> getOutboundType() {
			return B.class;
		}	
	}

	class CompoundAV extends CompoundEncoder<A,Void> {

		CompoundAV(IEncoder<?,?>... encoders) {
			super(encoders);
		}
		
		@Override
		public Class<A> getInboundType() {
			return A.class;
		}

		@Override
		public Class<Void> getOutboundType() {
			return Void.class;
		}	
	}

	class CompoundABD extends CompoundDecoder<A,B> {

		CompoundABD(IDecoder<?,?>... encoders) {
			super(encoders);
		}
		
		@Override
		public Class<A> getInboundType() {
			return A.class;
		}

		@Override
		public Class<B> getOutboundType() {
			return B.class;
		}	
	}
	
	class AS implements IEncoder<A,String> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void encode(ISession session, A data, List<String> out) throws Exception {
			trace("AS"); out.add("AS");
		}
	}
	
	class SA implements IEncoder<String,A> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<A> getOutboundType() {return A.class;}
		@Override public void encode(ISession session, String data, List<A> out) throws Exception {
			trace("SA"); out.add(new A());
		}
	}
	
	class AB0 implements IEncoder<A,B0> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<B0> getOutboundType() {return B0.class;}
		@Override public void encode(ISession session, A data, List<B0> out) throws Exception {
			trace("AB0"); out.add(new B0());
		}
	}
	
	class AB implements IEncoder<A,B> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void encode(ISession session, A data, List<B> out) throws Exception {
			trace("AB"); out.add(new B());
		}
	}
	
	class AV implements IEncoder<A,Void> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, A data, List<Void> out) throws Exception {
			trace("AV");
		}
	}

	class BV implements IEncoder<B,Void> {
		@Override public Class<B> getInboundType() {return B.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, B data, List<Void> out) throws Exception {
			trace("BV");
		}
	}
	
	class ABB implements IEncoder<A,BB> {
		@Override public Class<A> getInboundType() {return A.class;}
		@Override public Class<BB> getOutboundType() {return BB.class;}
		@Override public void encode(ISession session, A data, List<BB> out) throws Exception {
			trace("ABB"); out.add(new BB());
		}
	}
	
	class A0B0 implements IEncoder<A0,B0> {
		@Override public Class<A0> getInboundType() {return A0.class;}
		@Override public Class<B0> getOutboundType() {return B0.class;}
		@Override public void encode(ISession session, A0 data, List<B0> out) throws Exception {
			trace("A0B0"); out.add(new B0());
		}
	}

	class A0B implements IEncoder<A0,B> {
		@Override public Class<A0> getInboundType() {return A0.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void encode(ISession session, A0 data, List<B> out) throws Exception {
			trace("A0B"); out.add(new B());
		}
	}
	
	class A0BB implements IEncoder<A0,BB> {
		@Override public Class<A0> getInboundType() {return A0.class;}
		@Override public Class<BB> getOutboundType() {return BB.class;}
		@Override public void encode(ISession session, A0 data, List<BB> out) throws Exception {
			trace("A0BB"); out.add(new BB());
		}
	}
	
	class AAB0 implements IEncoder<AA,B0> {
		@Override public Class<AA> getInboundType() {return AA.class;}
		@Override public Class<B0> getOutboundType() {return B0.class;}
		@Override public void encode(ISession session, AA data, List<B0> out) throws Exception {
			trace("AAB0"); out.add(new B0());
		}
	}

	class AAB implements IEncoder<AA,B> {
		@Override public Class<AA> getInboundType() {return AA.class;}
		@Override public Class<B> getOutboundType() {return B.class;}
		@Override public void encode(ISession session, AA data, List<B> out) throws Exception {
			trace("AAB"); out.add(new B());
		}
	}
	
	class AABB implements IEncoder<AA,BB> {
		@Override public Class<AA> getInboundType() {return AA.class;}
		@Override public Class<BB> getOutboundType() {return BB.class;}
		@Override public void encode(ISession session, AA data, List<BB> out) throws Exception {
			trace("AABB"); out.add(new BB());
		}
	}
	
	static class A0 {}
	static class A extends A0 {}
	static class AA extends A {}
	
	static class B0 {}
	static class B extends B0 {}
	static class BB extends B {}
}

