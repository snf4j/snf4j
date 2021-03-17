/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2021 SNF4J contributors
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.snf4j.core.TestSession;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

public class CodecPipelineTest {

	private StringBuilder trace = new StringBuilder();
	
	private void trace(String s) {
		trace.append(s);
	}
	
	private String getTrace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}

	@Test
	public void testGetPipeline() {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		
		assertTrue(p.getPipeline() instanceof InternalCodecPipeline);
	}
	
	@Test
	public void testAddDecoder() {
		
		//add first base decoder
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		BaseS bd = new BaseS();
		assertNull(p.getBaseDecoder());
		p.getPipeline().add("1", bd);
		p.syncDecoders();
		assertTrue(bd == p.getBaseDecoder());
		
		//add first byte decoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		p.syncDecoders();
		assertNull(p.getBaseDecoder());
		
		//add first byte buffer decoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BBSD());
		p.syncDecoders();
		assertNull(p.getBaseDecoder());
		
		//add first incompatible decoder
		p = new DefaultCodecExecutor();
		try {
			p.getPipeline().add("1", new SBD());
			fail("should be thrown");
		}
		catch (IllegalArgumentException e) {}
		
		//add second compatible decoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		p.getPipeline().add("2", new SBD());
		
		//add second incompatible decoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		try {
			p.getPipeline().add("2", new BSD());
			fail("should be thrown");
		}
		catch (IllegalArgumentException e) {}
		
		//add third base decoder
		bd = new BaseS();
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		p.getPipeline().add("2", new SBD());
		p.getPipeline().add("3", bd);
		p.syncDecoders();
		assertNull(p.getBaseDecoder());
	}

	@Test
	public void testAddEncoder() {

		//add first byte encoder
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());

		//add first byte buffer encoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBBE());

		//add first incompatible encoder
		p = new DefaultCodecExecutor();
		try {
			p.getPipeline().add("1", new BSE());
			fail("should be thrown");
		}
		catch (IllegalArgumentException e) {}

		//add second compatible encoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.getPipeline().add("2", new BSE());
		
		//add second incompatible encoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		try {
			p.getPipeline().add("2", new SBE());
			fail("should be thrown");
		}
		catch (IllegalArgumentException e) {}
	}	
	
	DefaultCodecExecutor pipeline(String encoders, String decoders) {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		
		if (encoders != null) {
			int len = encoders.length();
			for (int i=0; i<len; ++i) {
				p.getPipeline().add(encoders.charAt(i), new E(""+encoders.charAt(i)));
			}
		}
		if (decoders != null) {
			int len = decoders.length();
			for (int i=0; i<len; ++i) {
				p.getPipeline().add(decoders.charAt(i), new D(""+decoders.charAt(i)));
			}
		}
		return p;
	}
	
	void assertEncoders(DefaultCodecExecutor p, String expected) throws Exception {
		assertEncoders(p, expected, expected);
	}

	void assertEncoders(DefaultCodecExecutor p, String expected, String expectedKeys) throws Exception {
		p.syncEncoders();
		getTrace();
		List<Object> out = p.encode(null, new byte[0]);
		if (expected == null) {
			assertNull(out);
		}
		else {
			int len = expectedKeys.length();
			for (int i=0; i<len; i+=2) {
				assertNotNull(p.getPipeline().get(expectedKeys.charAt(i)));
			}
			assertEquals(expected, getTrace());
		}
	}

	void assertDecoders(DefaultCodecExecutor p, String expected) throws Exception {
		assertDecoders(p, expected, expected);
	}
	
	void assertDecoders(DefaultCodecExecutor p, String expected, String expectedKeys) throws Exception {
		p.syncDecoders();
		getTrace();
		List<Object> out = p.decode(null, new byte[0]);
		if (expected == null) {
			assertNull(out);
		}
		else {
			int len = expectedKeys.length();
			for (int i=0; i<len; i+=2) {
				assertNotNull(p.getPipeline().get(expectedKeys.charAt(i)));
			}
			assertEquals(expected, getTrace());
		}
	}
	
	void assertFirstLast(DefaultCodecExecutor p, String expEncoders, String expDecoders) throws Exception {
		p.getPipeline().add('D', new E("D"));
		p.getPipeline().add('d', new D("d"));
		assertEncoders(p, "D|" + expEncoders);
		assertDecoders(p, expDecoders + "d|");
		p.getPipeline().addFirst('E', new E("E"));
		p.getPipeline().addFirst('e', new D("e"));
		assertEncoders(p, "D|" + expEncoders + "E|");
		assertDecoders(p, "e|" + expDecoders + "d|");
	}
	
	@Test
	public void testAdd() throws Exception {
		DefaultCodecExecutor p = pipeline(null,null);
		try {
			p.getPipeline().add(null, new D("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		try {
			p.getPipeline().add(null, new E("a")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		try {
			p.getPipeline().add('A', (IDecoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("decoder is null", e.getMessage());
		}
		try {
			p.getPipeline().add('a', (IEncoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("encoder is null", e.getMessage());
		}
		
		//add to empty
		p = pipeline(null, null);
		assertEncoders(p, "");
		assertDecoders(p, "");
		p.getPipeline().add('A', new E("A"));
		assertEncoders(p, "A|");
		assertDecoders(p, "");
		p.getPipeline().add('a', new D("a"));
		assertEncoders(p, "A|");
		assertDecoders(p, "a|");
		assertFirstLast(p, "A|", "a|"); 

		//add to one
		p = pipeline("A", "a");
		assertEncoders(p, "A|");
		assertDecoders(p, "a|");
		p.getPipeline().add('B', new E("B"));
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|");
		p.getPipeline().add('b', new D("b"));
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		assertFirstLast(p, "B|A|", "a|b|"); 
	
		//add to three
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().add('X', new E("X"));
		assertEncoders(p, "X|C|B|A|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().add('x', new D("x"));
		assertEncoders(p, "X|C|B|A|");
		assertDecoders(p, "a|b|c|x|");
		assertFirstLast(p, "X|C|B|A|", "a|b|c|x|"); 
		
		//add existing
		p = pipeline("ABC", "abc");
		try {
		p.getPipeline().add('A', new E("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("key 'A' already exists", e.getMessage());
		}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		try {
		p.getPipeline().add('a', new D("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("key 'a' already exists", e.getMessage());
		}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		
	}

	@Test
	public void testAddAfter() throws Exception {
		DefaultCodecExecutor p = pipeline(null,null);
		try {
			p.getPipeline().addAfter(null, 'A', new D("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("baseKey is null", e.getMessage());
		}
		try {
			p.getPipeline().addAfter(null, 'A', new E("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("baseKey is null", e.getMessage());
		}
		try {
			p.getPipeline().addAfter('A', null, new D("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		try {
			p.getPipeline().addAfter('A', null, new E("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		try {
			p.getPipeline().addAfter('A', 'B', (IDecoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("decoder is null", e.getMessage());
		}
		try {
			p.getPipeline().addAfter('A', 'B', (IEncoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("encoder is null", e.getMessage());
		}

		//add to empty
		p = pipeline(null, null);
		try {
			p.getPipeline().addAfter('X', 'A', new E("A"));
		}
		catch (NoSuchElementException e) {}
		try {
			p.getPipeline().addAfter('X', 'a', new D("a"));
		}
		catch (NoSuchElementException e) {}
		assertEncoders(p, "");
		assertDecoders(p, "");

		//add after only one
		p = pipeline("A", "a");
		p.getPipeline().addAfter('A', 'B', new E("B"));
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|");
		p.getPipeline().addAfter('a', 'b', new D("b"));
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		assertFirstLast(p, "B|A|", "a|b|"); 
		
		//add after first
		p = pipeline("ABC", "abc");
		p.getPipeline().addAfter('A', 'X', new E("X"));
		assertEncoders(p, "C|B|X|A|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().addAfter('a', 'x', new D("x"));
		assertEncoders(p, "C|B|X|A|");
		assertDecoders(p, "a|x|b|c|");
		assertFirstLast(p, "C|B|X|A|", "a|x|b|c|"); 

		//add after middle
		p = pipeline("ABC", "abc");
		p.getPipeline().addAfter('B', 'X', new E("X"));
		assertEncoders(p, "C|X|B|A|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().addAfter('b', 'x', new D("x"));
		assertEncoders(p, "C|X|B|A|");
		assertDecoders(p, "a|b|x|c|");
		assertFirstLast(p, "C|X|B|A|", "a|b|x|c|"); 
	
		//add after last
		p = pipeline("ABC", "abc");
		p.getPipeline().addAfter('C', 'X', new E("X"));
		assertEncoders(p, "X|C|B|A|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().addAfter('c', 'x', new D("x"));
		assertEncoders(p, "X|C|B|A|");
		assertDecoders(p, "a|b|c|x|");
		assertFirstLast(p, "X|C|B|A|", "a|b|c|x|"); 
	
		//add that exists
		p = pipeline("ABC", "abc");
		try {
			p.getPipeline().addAfter('C', 'A', new E("X"));
		}
		catch (Exception e) {
			assertEquals("key 'A' already exists", e.getMessage());
		}
		try {
			p.getPipeline().addAfter('c', 'a', new D("x"));
		}
		catch (Exception e) {
			assertEquals("key 'a' already exists", e.getMessage());
		}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertFirstLast(p, "C|B|A|", "a|b|c|"); 
		
		//mixed codecs
		p = pipeline("ABC", "abc");
		try {
			p.getPipeline().addAfter('a', 'x', new E("X"));
		}
		catch (Exception e) {
			assertEquals("incompatible codec type", e.getMessage());
		}
		try {
			p.getPipeline().addAfter('C', 'x', new D("x"));
		}
		catch (Exception e) {
			assertEquals("incompatible codec type", e.getMessage());
		}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertFirstLast(p, "C|B|A|", "a|b|c|"); 
	
	}
	
	@Test
	public void testAddBefore() throws Exception {
		DefaultCodecExecutor p = pipeline(null,null);
		try {
			p.getPipeline().addBefore(null, 'A', new D("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("baseKey is null", e.getMessage());
		}
		try {
			p.getPipeline().addBefore(null, 'A', new E("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("baseKey is null", e.getMessage());
		}
		try {
			p.getPipeline().addBefore('A', null, new D("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		try {
			p.getPipeline().addBefore('A', null, new E("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		try {
			p.getPipeline().addBefore('A', 'B', (IDecoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("decoder is null", e.getMessage());
		}
		try {
			p.getPipeline().addBefore('A', 'B', (IEncoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("encoder is null", e.getMessage());
		}
		
		//add to empty
		p = pipeline(null, null);
		try {
			p.getPipeline().addBefore('X', 'A', new E("A"));
		}
		catch (NoSuchElementException e) {}
		try {
			p.getPipeline().addBefore('X', 'a', new D("a"));
		}
		catch (NoSuchElementException e) {}
		assertEncoders(p, "");
		assertDecoders(p, "");

		//add before only one
		p = pipeline("A", "a");
		p.getPipeline().addBefore('A', 'B', new E("B"));
		assertEncoders(p, "A|B|");
		assertDecoders(p, "a|");
		p.getPipeline().addBefore('a', 'b', new D("b"));
		assertEncoders(p, "A|B|");
		assertDecoders(p, "b|a|");
		assertFirstLast(p, "A|B|", "b|a|"); 
		
		//add before first
		p = pipeline("ABC", "abc");
		p.getPipeline().addBefore('A', 'X', new E("X"));
		assertEncoders(p, "C|B|A|X|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().addBefore('a', 'x', new D("x"));
		assertEncoders(p, "C|B|A|X|");
		assertDecoders(p, "x|a|b|c|");
		assertFirstLast(p, "C|B|A|X|", "x|a|b|c|"); 

		//add before middle
		p = pipeline("ABC", "abc");
		p.getPipeline().addBefore('B', 'X', new E("X"));
		assertEncoders(p, "C|B|X|A|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().addBefore('b', 'x', new D("x"));
		assertEncoders(p, "C|B|X|A|");
		assertDecoders(p, "a|x|b|c|");
		assertFirstLast(p, "C|B|X|A|", "a|x|b|c|"); 
	
		//add before last
		p = pipeline("ABC", "abc");
		p.getPipeline().addBefore('C', 'X', new E("X"));
		assertEncoders(p, "C|X|B|A|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().addBefore('c', 'x', new D("x"));
		assertEncoders(p, "C|X|B|A|");
		assertDecoders(p, "a|b|x|c|");
		assertFirstLast(p, "C|X|B|A|", "a|b|x|c|"); 
	
		//add that exists
		p = pipeline("ABC", "abc");
		try {
			p.getPipeline().addBefore('C', 'A', new E("X"));
		}
		catch (Exception e) {
			assertEquals("key 'A' already exists", e.getMessage());
		}
		try {
			p.getPipeline().addBefore('c', 'a', new D("x"));
		}
		catch (Exception e) {
			assertEquals("key 'a' already exists", e.getMessage());
		}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertFirstLast(p, "C|B|A|", "a|b|c|"); 
		
		//mixed codecs
		p = pipeline("ABC", "abc");
		try {
			p.getPipeline().addBefore('a', 'x', new E("X"));
		}
		catch (Exception e) {
			assertEquals("incompatible codec type", e.getMessage());
		}
		try {
			p.getPipeline().addBefore('C', 'x', new D("x"));
		}
		catch (Exception e) {
			assertEquals("incompatible codec type", e.getMessage());
		}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertFirstLast(p, "C|B|A|", "a|b|c|"); 
	}
	
	@Test
	public void testAddFirst() throws Exception {
		DefaultCodecExecutor p = pipeline(null,null);
		try {
			p.getPipeline().addFirst(null, new D("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		try {
			p.getPipeline().addFirst(null, new E("a")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		try {
			p.getPipeline().addFirst('A', (IDecoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("decoder is null", e.getMessage());
		}
		try {
			p.getPipeline().addFirst('a', (IEncoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("encoder is null", e.getMessage());
		}
		
		//add to empty
		p = pipeline(null, null);
		assertEncoders(p, "");
		assertDecoders(p, "");
		p.getPipeline().addFirst('A', new E("A"));
		assertEncoders(p, "A|");
		assertDecoders(p, "");
		p.getPipeline().addFirst('a', new D("a"));
		assertEncoders(p, "A|");
		assertDecoders(p, "a|");
		assertFirstLast(p, "A|", "a|"); 

		//add to one
		p = pipeline("A", "a");
		assertEncoders(p, "A|");
		assertDecoders(p, "a|");
		p.getPipeline().addFirst('B', new E("B"));
		assertEncoders(p, "A|B|");
		assertDecoders(p, "a|");
		p.getPipeline().addFirst('b', new D("b"));
		assertEncoders(p, "A|B|");
		assertDecoders(p, "b|a|");
		assertFirstLast(p, "A|B|", "b|a|"); 
	
		//add to three
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().addFirst('X', new E("X"));
		assertEncoders(p, "C|B|A|X|");
		assertDecoders(p, "a|b|c|");
		p.getPipeline().addFirst('x', new D("x"));
		assertEncoders(p, "C|B|A|X|");
		assertDecoders(p, "x|a|b|c|");
		assertFirstLast(p, "C|B|A|X|", "x|a|b|c|"); 
		
		//add existing
		p = pipeline("ABC", "abc");
		try {
		p.getPipeline().addFirst('A', new E("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("key 'A' already exists", e.getMessage());
		}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		try {
		p.getPipeline().addFirst('a', new D("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("key 'a' already exists", e.getMessage());
		}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		
	}
	
	@Test
	public void testReplace() throws Exception {
		DefaultCodecExecutor p = pipeline(null,null);
		try {
			p.getPipeline().replace(null, 'A', new D("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("oldKey is null", e.getMessage());
		}
		try {
			p.getPipeline().replace('B', null, new D("A")); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		try {
			p.getPipeline().replace('B', 'A', (IDecoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("decoder is null", e.getMessage());
		}
		try {
			p.getPipeline().replace('B', 'A', (IEncoder<?,?>)null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("encoder is null", e.getMessage());
		}
		
		//replace from empty pipeline
		assertEncoders(p, null);
		assertDecoders(p, null);
		try {
			p.getPipeline().replace("K", 'W', new D("W")); fail();
		}
		catch (NoSuchElementException e) {}
		try {
			p.getPipeline().replace("K", 'w', new E("w")); fail();
		}
		catch (NoSuchElementException e) {}
		assertEncoders(p, null);
		assertDecoders(p, null);
		
		//remove not existing 
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		try {
			p.getPipeline().replace("K", 'W', new D("W")); fail();
		}
		catch (NoSuchElementException e) {}
		try {
			p.getPipeline().replace("K", 'w', new E("w")); fail();
		}
		catch (NoSuchElementException e) {}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");

		//remove first 
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertEquals("A", ((E)p.getPipeline().replace('A','X',new E("X"))).name);
		assertEncoders(p, "C|B|X|");
		assertDecoders(p, "a|b|c|");
		assertNull(p.getPipeline().get('A'));
		assertEquals("a", ((D)p.getPipeline().replace('a','x',new D("x"))).name);
		assertEncoders(p, "C|B|X|");
		assertDecoders(p, "x|b|c|");
		assertNull(p.getPipeline().get('a'));
		assertFirstLast(p, "C|B|X|", "x|b|c|"); 
	
		//remove middle 
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertEquals("B", ((E)p.getPipeline().replace('B','X',new E("X"))).name);
		assertEncoders(p, "C|X|A|");
		assertDecoders(p, "a|b|c|");
		assertNull(p.getPipeline().get('B'));
		assertEquals("b", ((D)p.getPipeline().replace('b','x',new D("x"))).name);
		assertEncoders(p, "C|X|A|");
		assertDecoders(p, "a|x|c|");
		assertNull(p.getPipeline().get('b'));
		assertFirstLast(p, "C|X|A|", "a|x|c|"); 

		//remove last 
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertEquals("C", ((E)p.getPipeline().replace('C','X',new E("X"))).name);
		assertEncoders(p, "X|B|A|");
		assertDecoders(p, "a|b|c|");
		assertNull(p.getPipeline().get('C'));
		assertEquals("c", ((D)p.getPipeline().replace('c','x',new D("x"))).name);
		assertEncoders(p, "X|B|A|");
		assertDecoders(p, "a|b|x|");
		assertNull(p.getPipeline().get('c'));
		assertFirstLast(p, "X|B|A|", "a|b|x|"); 
		
		//replace the last one
		p = pipeline("A", "a");
		assertEncoders(p, "A|");
		assertDecoders(p, "a|");
		assertEquals("A", ((E)p.getPipeline().replace('A','X',new E("X"))).name);
		assertEncoders(p, "X|");
		assertDecoders(p, "a|");
		assertNull(p.getPipeline().get('A'));
		assertEquals("a", ((D)p.getPipeline().replace('a','x',new D("x"))).name);
		assertEncoders(p, "X|");
		assertDecoders(p, "x|");
		assertNull(p.getPipeline().get('a'));
		assertFirstLast(p, "X|", "x|"); 
	
		//replace first from two
		p = pipeline("AB", "ab");
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		assertEquals("A", ((E)p.getPipeline().replace('A','X',new E("X"))).name);
		assertEncoders(p, "B|X|");
		assertDecoders(p, "a|b|");
		assertNull(p.getPipeline().get('A'));
		assertEquals("a", ((D)p.getPipeline().replace('a','x',new D("x"))).name);
		assertEncoders(p, "B|X|");
		assertDecoders(p, "x|b|");
		assertNull(p.getPipeline().get('a'));
		assertFirstLast(p, "B|X|", "x|b|"); 

		//replace last from two
		p = pipeline("AB", "ab");
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		assertEquals("B", ((E)p.getPipeline().replace('B','X',new E("X"))).name);
		assertEncoders(p, "X|A|");
		assertDecoders(p, "a|b|");
		assertNull(p.getPipeline().get('B'));
		assertEquals("b", ((D)p.getPipeline().replace('b','x',new D("x"))).name);
		assertEncoders(p, "X|A|");
		assertDecoders(p, "a|x|");
		assertNull(p.getPipeline().get('b'));
		assertFirstLast(p, "X|A|", "a|x|"); 
		
		//replace with existing replaced key
		p = pipeline("AB", "ab");
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		assertEquals("B", ((E)p.getPipeline().replace('B','B',new E("X"))).name);
		assertEncoders(p, "X|A|", "B|A|");
		assertDecoders(p, "a|b|");
		assertEquals("b", ((D)p.getPipeline().replace('b','b',new D("x"))).name);
		assertEncoders(p, "X|A|", "B|A|");
		assertDecoders(p, "a|x|", "a|b|");
		
		//replace with existing key 
		p = pipeline("AB", "ab");
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		try {
			p.getPipeline().replace('B','A',new E("X"));
		}
		catch (IllegalArgumentException e) {
			assertEquals("key 'A' already exists", e.getMessage());
		}
		try {
			p.getPipeline().replace('b','a',new D("X"));
		}
		catch (IllegalArgumentException e) {
			assertEquals("key 'a' already exists", e.getMessage());
		}
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		
		//mixed types
		p = pipeline("AB", "ab");
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		try {
			p.getPipeline().replace('b','X',new E("X"));
		}
		catch (IllegalArgumentException e) {
			assertEquals("incompatible codec type", e.getMessage());
		}
		try {
			p.getPipeline().replace('A','x',new D("x"));
		}
		catch (IllegalArgumentException e) {
			assertEquals("incompatible codec type", e.getMessage());
		}
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		
	}
	
	@Test
	public void testRemove() throws Exception {
		DefaultCodecExecutor p = pipeline(null,null);
		try {
			p.getPipeline().remove(null); fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}

		//remove from empty pipeline
		assertEncoders(p, null);
		assertDecoders(p, null);
		try {
			p.getPipeline().remove("K"); fail();
		}
		catch (NoSuchElementException e) {}
		assertEncoders(p, null);
		assertDecoders(p, null);
		
		//remove not existing 
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		try {
			p.getPipeline().remove("K"); fail();
		}
		catch (NoSuchElementException e) {}
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");

		//remove first 
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertEquals("A", ((E)p.getPipeline().remove('A')).name);
		assertNull(p.getPipeline().get('A'));
		assertEncoders(p, "C|B|");
		assertDecoders(p, "a|b|c|");
		assertEquals("a", ((D)p.getPipeline().remove('a')).name);
		assertNull(p.getPipeline().get('a'));
		assertEncoders(p, "C|B|");
		assertDecoders(p, "b|c|");
		assertFirstLast(p, "C|B|", "b|c|"); 
		
		//remove in middle 
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertEquals("B", ((E)p.getPipeline().remove('B')).name);
		assertNull(p.getPipeline().get('B'));
		assertEncoders(p, "C|A|");
		assertDecoders(p, "a|b|c|");
		assertEquals("b", ((D)p.getPipeline().remove('b')).name);
		assertNull(p.getPipeline().get('b'));
		assertEncoders(p, "C|A|");
		assertDecoders(p, "a|c|");
		assertFirstLast(p, "C|A|", "a|c|"); 

		//remove last 
		p = pipeline("ABC", "abc");
		assertEncoders(p, "C|B|A|");
		assertDecoders(p, "a|b|c|");
		assertEquals("C", ((E)p.getPipeline().remove('C')).name);
		assertNull(p.getPipeline().get('C'));
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|c|");
		assertEquals("c", ((D)p.getPipeline().remove('c')).name);
		assertNull(p.getPipeline().get('c'));
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		assertFirstLast(p, "B|A|", "a|b|"); 

		//remove the last one
		p = pipeline("A", "a");
		assertEncoders(p, "A|");
		assertDecoders(p, "a|");
		assertEquals("A", ((E)p.getPipeline().remove('A')).name);
		assertNull(p.getPipeline().get('A'));
		assertEncoders(p, "");
		assertDecoders(p, "a|");
		assertEquals("a", ((D)p.getPipeline().remove('a')).name);
		assertNull(p.getPipeline().get('a'));
		assertEncoders(p, "");
		assertDecoders(p, "");
		assertFirstLast(p, "", ""); 

		//remove first from two
		p = pipeline("AB", "ab");
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		assertEquals("A", ((E)p.getPipeline().remove('A')).name);
		assertNull(p.getPipeline().get('A'));
		assertEncoders(p, "B|");
		assertDecoders(p, "a|b|");
		assertEquals("a", ((D)p.getPipeline().remove('a')).name);
		assertNull(p.getPipeline().get('a'));
		assertEncoders(p, "B|");
		assertDecoders(p, "b|");
		assertFirstLast(p, "B|", "b|"); 

		//remove last from two
		p = pipeline("AB", "ab");
		assertEncoders(p, "B|A|");
		assertDecoders(p, "a|b|");
		assertEquals("B", ((E)p.getPipeline().remove('B')).name);
		assertNull(p.getPipeline().get('B'));
		assertEncoders(p, "A|");
		assertDecoders(p, "a|b|");
		assertEquals("b", ((D)p.getPipeline().remove('b')).name);
		assertNull(p.getPipeline().get('b'));
		assertEncoders(p, "A|");
		assertDecoders(p, "a|");
		assertFirstLast(p, "A|", "a|"); 
	}
	
	private void assertKeys(String expected, List<Object> keys) {
		int len = expected.length();
		
		assertEquals(len, keys.size());
		for (int i=0; i<len; ++i) {
			assertEquals(expected.charAt(i), keys.get(i));
		}
	}
	
	@Test
	public void testKeys() {
		DefaultCodecExecutor p = pipeline(null, null);
		
		assertKeys("", p.getPipeline().decoderKeys());
		assertKeys("", p.getPipeline().encoderKeys());

		p = pipeline("A", "a");
		assertKeys("a", p.getPipeline().decoderKeys());
		assertKeys("A", p.getPipeline().encoderKeys());

		p = pipeline("ABC", "abcde");
		assertKeys("abcde", p.getPipeline().decoderKeys());
		assertKeys("ABC", p.getPipeline().encoderKeys());
	}
	
	@Test
	public void testDecoder() throws Exception {
		
		//empty pipeline
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		List<Object> out = p.decode(null, "ABC".getBytes());
		assertNull(out);
		
		//pipeline with base decoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BaseS());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == String.class);
		assertEquals("ABC", out.get(0));
		assertEquals("BaseS|", getTrace());

		//pipeline with one decoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == String.class);
		assertEquals("ABC", out.get(0));
		assertEquals("BSD|", getTrace());
		
		//pipeline with two decoders
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		p.getPipeline().add("2", new SBD());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BSD|SBD|", getTrace());

		//pipeline with two decoders
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		p.getPipeline().add("2", new SBD());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BSD|SBD|", getTrace());

		//pipeline with tree decoders
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BBSD());
		p.getPipeline().add("2", new SBD());
		p.getPipeline().add("3", new BSD());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == String.class);
		assertEquals("ABC", out.get(0));
		assertEquals("BBSD|SBD|BSD|", getTrace());
		
		//pipeline with discarding decoder (first)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSDDiscard());
		p.getPipeline().add("2", new SBD());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertEquals(0, out.size());
		assertEquals("BSDDiscard|", getTrace());

		//pipeline with discarding decoder (last)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		p.getPipeline().add("2", new SBD());
		p.getPipeline().add("3", new BSDDiscard());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertEquals(0, out.size());
		assertEquals("BSD|SBD|BSDDiscard|", getTrace());

		//pipeline with discarding decoder (middle)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		p.getPipeline().add("2", new SBD());
		p.getPipeline().add("3", new BSDDiscard());
		p.getPipeline().add("4", new SBD());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertEquals(0, out.size());
		assertEquals("BSD|SBD|BSDDiscard|", getTrace());

		//pipeline with duplicating decoder (first)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSDDuplicate());
		p.getPipeline().add("2", new SBD());
		p.getPipeline().add("3", new BSD());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertEquals(2, out.size());
		assertEquals("BSDDuplicate|SBD|SBD|BSD|BSD|", getTrace());
		assertEquals("ABC", out.get(0));
		assertEquals("ABC", out.get(1));

		//pipeline with duplicating decoder (middle)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());
		p.getPipeline().add("2", new SBD());
		p.getPipeline().add("3", new BSDDuplicate());
		p.getPipeline().add("4", new SBD());
		p.getPipeline().add("fifth", new BSD());
		p.syncDecoders();
		assertEquals("", getTrace());
		out = p.decode(null, "ABC".getBytes());
		assertEquals(2, out.size());
		assertEquals("BSD|SBD|BSDDuplicate|SBD|SBD|BSD|BSD|", getTrace());
		assertEquals("ABC", out.get(0));
		assertEquals("ABC", out.get(1));
	}

	@Test
	public void testEncoder() throws Exception {
		TestSession session = new TestSession();
		
		//empty pipeline
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		List<Object> out = p.encode(null, "ABC".getBytes());
		assertNull(out);
		out = p.encode(null, "ABC");
		assertNull(out);
		out = p.encode(null, ByteBuffer.wrap("ABC".getBytes()));
		assertNull(out);
		
		//pipeline with one encoder (byte in)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BBE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC".getBytes());
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BBE|", getTrace());
		assertNull(p.encode(null, "ABC"));
		out = p.encode(session, ByteBuffer.wrap("ABC".getBytes()));
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BBE|", getTrace());

		//pipeline with one encoder (byte buffer in)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BB_BE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, ByteBuffer.wrap("ABC".getBytes()));
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BB_BE|", getTrace());
		assertNull(p.encode(null, "ABC"));
		out = p.encode(null, "ABC".getBytes());
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BB_BE|", getTrace());
		
		//pipeline with one encoder (String in)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC");
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("SBE|", getTrace());
		assertNull(p.encode(null, "ABC".getBytes()));
		assertNull(p.encode(null, new Integer(0)));
		assertNull(p.encode(null, ByteBuffer.wrap("ABC".getBytes())));
		
		//pipeline with two encoders (byte in)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.getPipeline().add("2", new BSE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC".getBytes());
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BSE|SBE|", getTrace());
		out = p.encode(null, "ABC");
		assertEquals("SBE|", getTrace());
		assertEquals("ABC", new String((byte[])out.get(0)));
		out = p.encode(session, ByteBuffer.wrap("ABC".getBytes()));
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BSE|SBE|", getTrace());
		
		//pipeline with two encoders (byte buffer in)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.getPipeline().add("2", new BBSE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, ByteBuffer.wrap("ABC".getBytes()));
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BBSE|SBE|", getTrace());
		out = p.encode(null, "ABC");
		assertEquals("SBE|", getTrace());
		assertEquals("ABC", new String((byte[])out.get(0)));
		out = p.encode(null, "ABC".getBytes());
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BBSE|SBE|", getTrace());

		//pipeline with two encoders (string in)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BBE());
		p.getPipeline().add("2", new SBE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC");
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("SBE|BBE|", getTrace());
		out = p.encode(null, "ABC".getBytes());
		assertEquals("BBE|", getTrace());
		assertEquals("ABC", new String((byte[])out.get(0)));
		out = p.encode(session, ByteBuffer.wrap("ABC".getBytes()));
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BBE|", getTrace());
		
		//pipeline with three encoders (byte in)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.getPipeline().add("2", new BSE());
		p.getPipeline().add("3", new BBE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC".getBytes());
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BBE|BSE|SBE|", getTrace());
		out = p.encode(null, "ABC");
		assertEquals("SBE|", getTrace());
		assertEquals("ABC", new String((byte[])out.get(0)));
		out = p.encode(session, ByteBuffer.wrap("ABC".getBytes()));
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BBE|BSE|SBE|", getTrace());
		
		//pipeline with three encoders (byte buffer in)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.getPipeline().add("2", new BSE());
		p.getPipeline().add("3", new BB_BE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, ByteBuffer.wrap("ABC".getBytes()));
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BB_BE|BSE|SBE|", getTrace());
		out = p.encode(null, "ABC");
		assertEquals("SBE|", getTrace());
		assertEquals("ABC", new String((byte[])out.get(0)));
		out = p.encode(null, "ABC".getBytes());
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BB_BE|BSE|SBE|", getTrace());
		
		//pipeline with three encoders (string in)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.getPipeline().add("2", new BSE());
		p.getPipeline().add("3", new SBE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC");
		assertNotNull(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getClass() == byte[].class);
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("SBE|BSE|SBE|", getTrace());
		out = p.encode(null, "ABC".getBytes());
		assertEquals("BSE|SBE|", getTrace());
		assertEquals("ABC", new String((byte[])out.get(0)));
		out = p.encode(session, ByteBuffer.wrap("ABC".getBytes()));
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("BSE|SBE|", getTrace());

		//pipeline with discarding encoder (first)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBEDiscard());
		p.getPipeline().add("2", new BSE());
		p.getPipeline().add("3", new SBE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC");
		assertEquals(0, out.size());
		assertEquals("SBE|BSE|SBEDiscard|", getTrace());

		//pipeline with discarding encoder (last)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.getPipeline().add("2", new BSE());
		p.getPipeline().add("3", new SBEDiscard());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC");
		assertEquals(0, out.size());
		assertEquals("SBEDiscard|", getTrace());

		//pipeline with discarding encoder (middle)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.getPipeline().add("2", new BSE());
		p.getPipeline().add("3", new SBEDiscard());
		p.getPipeline().add("4", new BSE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC".getBytes());
		assertEquals(0, out.size());
		assertEquals("BSE|SBEDiscard|", getTrace());

		//pipeline with duplicating encoder (first)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBEDuplicate());
		p.getPipeline().add("2", new BSE());
		p.getPipeline().add("3", new SBE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC");
		assertEquals(2, out.size());
		assertEquals("SBE|BSE|SBEDuplicate|", getTrace());
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("ABC", new String((byte[])out.get(1)));

		//pipeline with duplicating decoder (middle)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new SBE());
		p.getPipeline().add("2", new BSE());
		p.getPipeline().add("3", new SBEDuplicate());
		p.getPipeline().add("4", new BSE());
		p.syncEncoders();
		assertEquals("", getTrace());
		out = p.encode(null, "ABC".getBytes());
		assertEquals(2, out.size());
		assertEquals("BSE|SBEDuplicate|BSE|BSE|SBE|SBE|", getTrace());
		assertEquals("ABC", new String((byte[])out.get(0)));
		assertEquals("ABC", new String((byte[])out.get(1)));
	}	
	
	void assertValidation(IllegalArgumentException e, Object key, boolean inbound, boolean decoder) {
		StringBuilder s = new StringBuilder();
		
		s.append(decoder ? "decoder '" : "encoder '");
		s.append(key);
		s.append("' has incompatible ");
		s.append(inbound ? "inbound " : "outbound ");
		s.append("type");
		assertEquals(s.toString(), e.getMessage());
	}
	
	@Test
	public void testAddValidation() {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		
		try {
			p.getPipeline().add("1", new SBD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "1", true, true);
		}
		p.getPipeline().add("1", new BSD());
		try {
			p.getPipeline().add("2", new BSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "2", true, true);
		}	
		
		try {
			p.getPipeline().add("_1", new BSE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_1", false, false);
		}
		p.getPipeline().add("_1", new SBE());
		try {
			p.getPipeline().add("_2", new SBE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_2", false, false);
		}	

	}
	
	@Test
	public void testAddAfterValidation() {
		DefaultCodecExecutor p = new DefaultCodecExecutor();

		p.getPipeline().add("1", new BSD());
		try {
			p.getPipeline().addAfter("1", "2", new BSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "2", true, true);
		}
		p.getPipeline().addAfter("1", "2", new SBD());
		try {
			p.getPipeline().addAfter("1", "3", new D("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "3", true, true);
		}
		p.getPipeline().add("3", new BSD());
		try {
			p.getPipeline().addAfter("2", "4", new BSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "4", false, true);
		}
		try {
			p.getPipeline().addAfter("2", "4", new SSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "4", true, true);
		}

		p.getPipeline().add("_1", new SBE());
		try {
			p.getPipeline().addAfter("_1", "_2", new SBE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_2", false, false);
		}
		p.getPipeline().addAfter("_1", "_2", new BSE());
		try {
			p.getPipeline().addAfter("_1", "_3", new E("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_3", false, false);
		}
		p.getPipeline().add("_3", new SBE());
		try {
			p.getPipeline().addAfter("_2", "_4", new SBE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_4", true, false);
		}
		try {
			p.getPipeline().addAfter("_2", "_4", new SSE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_4", false, false);
		}
	}
	
	@Test
	public void testAddFirstValidation() {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		
		try {
			p.getPipeline().addFirst("1", new SBD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "1", true, true);
		}
		p.getPipeline().addFirst("1", new BSD());
		try {
			p.getPipeline().addFirst("2", new BSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "2", false, true);
		}	
		try {
			p.getPipeline().addFirst("2", new SSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "2", true, true);
		}	
	
		try {
			p.getPipeline().addFirst("_1", new BSE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_1", false, false);
		}
		p.getPipeline().addFirst("_1", new SBE());
		try {
			p.getPipeline().addFirst("_2", new SBE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_2", true, false);
		}	
		try {
			p.getPipeline().addFirst("_2", new SSE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_2", false, false);
		}	
		
	}
	
	@Test
	public void testAddBeforeValidation() {
		DefaultCodecExecutor p = new DefaultCodecExecutor();

		p.getPipeline().add("1", new BSD());
		try {
			p.getPipeline().addBefore("1", "2", new BSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "2", false, true);
		}
		try {
			p.getPipeline().addBefore("1", "2", new SBD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "2", true, true);
		}
		p.getPipeline().addAfter("1", "2", new SBD());
		try {
			p.getPipeline().addBefore("2", "3", new BSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "3", true, true);
		}
		try {
			p.getPipeline().addBefore("2", "3", new SBD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "3", false, true);
		}
		try {
			p.getPipeline().addBefore("2", "3", new D("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "3", true, true);
		}

		p.getPipeline().add("_1", new SBE());
		try {
			p.getPipeline().addBefore("_1", "_2", new SBE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_2", true, false);
		}
		try {
			p.getPipeline().addBefore("_1", "_2", new BSE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_2", false, false);
		}
		p.getPipeline().addAfter("_1", "_2", new BSE());

		try {
			p.getPipeline().addBefore("_2", "_3", new SBE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_3", false, false);
		}
		try {
			p.getPipeline().addBefore("_2", "_3", new BSE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_3", true, false);
		}
		try {
			p.getPipeline().addBefore("_2", "_3", new E("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_3", false, false);
		}		
	}
	
	@Test
	public void testReplaceValidation() {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BSD());

		try {
			p.getPipeline().replace("1", "2", new SSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "2", true, true);
		}		
		p.getPipeline().add("2", new SBD());
		try {
			p.getPipeline().replace("1", "3", new SSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "3", true, true);
		}		
		try {
			p.getPipeline().replace("1", "3", new D("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "3", false, true);
		}	
		try {
			p.getPipeline().replace("2", "3", new D("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "3", true, true);
		}	
		p.getPipeline().add("3", new BSD());
		try {
			p.getPipeline().replace("2", "4", new D("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "4", true, true);
		}		
		try {
			p.getPipeline().replace("2", "4", new SSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "4", false, true);
		}		
		try {
			p.getPipeline().replace("2", "4", new BSD()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "4", true, true);
		}		

		p.getPipeline().add("_1", new SBE());
		try {
			p.getPipeline().replace("_1", "_2", new SSE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_2", false, false);
		}		
		p.getPipeline().add("_2", new BSE());
		try {
			p.getPipeline().replace("_1", "_3", new SSE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_3", false, false);
		}		
		try {
			p.getPipeline().replace("_1", "_3", new E("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_3", true, false);
		}	
		try {
			p.getPipeline().replace("_2", "_3", new E("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_3", false, false);
		}	
		p.getPipeline().add("_3", new SBE());
		try {
			p.getPipeline().replace("_2", "_4", new E("X")); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_4", false, false);
		}		
		try {
			p.getPipeline().replace("_2", "_4", new SSE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_4", true, false);
		}		
		try {
			p.getPipeline().replace("_2", "_4", new SBE()); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_4", false, false);
		}		
	}

	@Test
	public void testRemoveValidation() {
		DefaultCodecExecutor p = new DefaultCodecExecutor();

		p.getPipeline().add("1", new BSD());
		p.getPipeline().add("2", new SBD());
		try {
			p.getPipeline().remove("1"); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "2", true, true);
		}		
		p.getPipeline().add("3", new BSD());
		try {
			p.getPipeline().remove("2"); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "3", true, true);
		}		

		p.getPipeline().add("_1", new SBE());
		p.getPipeline().add("_2", new BSE());
		try {
			p.getPipeline().remove("_1"); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_2", false, false);
		}		
		p.getPipeline().add("_3", new SBE());
		try {
			p.getPipeline().remove("_2"); fail();
		}
		catch (IllegalArgumentException e) {
			assertValidation(e, "_3", false, false);
		}		
	}
	
	@Test
	public void testGetBaseDecoder() {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		BaseS base = new BaseS();
		assertNull(p.getBaseDecoder());
		p.getPipeline().add(1, base);
		assertNull(p.getBaseDecoder());
		p.syncDecoders();
		assertTrue(base == p.getBaseDecoder());
		p.getPipeline().addFirst(2, new D("x"));
		assertTrue(base == p.getBaseDecoder());
		p.syncDecoders();
		assertNull(p.getBaseDecoder());
	}

	@Test
	public void testHasDecoders() {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		assertFalse(p.hasDecoders());
		p.getPipeline().add(1, new BaseS());
		assertFalse(p.hasDecoders());
		p.syncDecoders();
		assertTrue(p.hasDecoders());
		p.getPipeline().remove(1);
		assertTrue(p.hasDecoders());
		p.syncDecoders();
		assertFalse(p.hasDecoders());
		p.getPipeline().add(1, new BSD());
		assertFalse(p.hasDecoders());
		p.syncDecoders();
		assertTrue(p.hasDecoders());
		p.getPipeline().addFirst(2, new BVD("V"));
		assertTrue(p.hasDecoders());
		p.syncDecoders();
		assertTrue(p.hasDecoders());
		p.getPipeline().remove(1);
		assertTrue(p.hasDecoders());
		p.syncDecoders();
		assertFalse(p.hasDecoders());
		p.getPipeline().remove(2);
	}
	
	@Test
	public void testSyncEncode() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add(1, new SBE());
		p.encode(null, "ABC");
		assertEquals("", getTrace());
		p.syncEncoders();
		p.encode(null, "ABC");
		assertEquals("SBE|", getTrace());
		p.getPipeline().add(2, new BSE());
		p.encode(null, "ABC".getBytes());
		assertEquals("", getTrace());
		p.syncEncoders();
		p.encode(null, "ABC".getBytes());
		assertEquals("BSE|SBE|", getTrace());
		p.getPipeline().add(3, new SBE());
		p.encode(null, (Object)"ABC");
		assertEquals("SBE|", getTrace());
		p.syncEncoders();
		p.encode(null, (Object)"ABC");
		assertEquals("SBE|BSE|SBE|", getTrace());
	}

	@Test
	public void testSyncDecode() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add(1, new BaseS());
		p.decode(null, "ABC".getBytes());
		assertEquals("", getTrace());
		p.syncDecoders();
		p.decode(null, "ABC".getBytes());
		assertEquals("BaseS|", getTrace());
		p.getPipeline().add(2, new SBD());
		p.decode(null, "ABC".getBytes());
		assertEquals("BaseS|", getTrace());
		p.syncDecoders();
		p.decode(null, "ABC".getBytes());
		assertEquals("BaseS|SBD|", getTrace());
	}
	
	private String stringFromBuffer(Object buffer) {
		ByteBuffer bb = (ByteBuffer)buffer;
		byte[] b = new byte[bb.remaining()];
		bb.get(b);
		return new String(b);
	}
	
	@Test
	public void testDecodeWithVoidDecoders() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add(1, new BVD("V1",true));
		p.getPipeline().add(2, new BBVD("V2",true));
		p.syncDecoders();
		assertNull(p.decode(null, "ABC".getBytes()));
		assertEquals("V1(ABC)|V2(ABC)|", getTrace());
		p.getPipeline().add(3, new BSD());
		p.getPipeline().add(4, new SBD());
		p.getPipeline().add(5, new BVD("V4",true));
		p.getPipeline().add(6, new B_BBD());
		p.getPipeline().add(7, new BBVD("V5",true));
		p.syncDecoders();
		List<Object> out = p.decode(null, "ABC".getBytes());
		assertNotNull(out);
		assertEquals(1, out.size());
		assertEquals("ABC", stringFromBuffer(out.get(0)));
		assertEquals("V1(ABC)|V2(ABC)|BSD|SBD|V4(ABC)|B_BBD|V5(ABC)|", getTrace());
	}
	
	@Test
	public void testEncodeWithVoidEncoders() throws Exception {
		TestSession session = new TestSession();
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add(1, new BVE("V1",true));
		p.getPipeline().add(2, new BBVE("V2",true));
		p.syncEncoders();
		assertNull(p.encode(null, "ABC".getBytes()));
		assertEquals("V2(ABC)|V1(ABC)|", getTrace());
		assertNull(p.encode(null, ByteBuffer.wrap("ABC".getBytes())));
		assertEquals("V2(ABC)|V1(ABC)|", getTrace());
		assertNull(p.encode(null, "ABC"));
		assertEquals("", getTrace());
		p.getPipeline().add(3, new SBE());
		p.getPipeline().add(4, new SVE("V3",true));
		p.getPipeline().add(5, new BSE());
		p.getPipeline().add(6, new BVE("V5",true));
		p.getPipeline().add(7, new BB_BE());
		p.getPipeline().add(8, new BBVE("V6",true));
		p.syncEncoders();
		List<Object> o = p.encode(null, "ABC".getBytes());
		assertNotNull(o);
		assertEquals(1, o.size());
		assertEquals("ABC", new String((byte[])o.get(0)));
		assertEquals("V6(ABC)|BB_BE|V5(ABC)|BSE|V3(ABC)|SBE|V2(ABC)|V1(ABC)|", getTrace());
		
		p = new DefaultCodecExecutor();
		p.getPipeline().add(1, new SBE());
		p.getPipeline().add(2, new SVE("V2",true));
		p.getPipeline().add(3, new SVE("V3",true));
		p.syncEncoders();
		o = p.encode(null, "ABC");
		assertNotNull(o);
		assertEquals(1, o.size());
		assertEquals("ABC", new String((byte[])o.get(0)));
		assertEquals("V3(ABC)|V2(ABC)|SBE|", getTrace());
		
		p = new DefaultCodecExecutor();
		p.getPipeline().add(1, new BVE("V1",true));
		p.getPipeline().add(2, new BBVE("V2",true));
		p.getPipeline().add(3, new SBBE());
		p.getPipeline().add(4, new BSE());
		p.syncEncoders();
		o = p.encode(null, "ABC".getBytes());
		assertNotNull(o);
		assertEquals(1, o.size());
		assertEquals("ABC", stringFromBuffer(o.get(0)));
		assertEquals("BSE|SBBE|V2(ABC)|V1(ABC)|", getTrace());
		o = p.encode(session, ByteBuffer.wrap("ABC".getBytes()));
		assertNotNull(o);
		assertEquals(1, o.size());
		assertEquals("ABC", stringFromBuffer(o.get(0)));
		assertEquals("BSE|SBBE|V2(ABC)|V1(ABC)|", getTrace());
		p.getPipeline().add(5, new SBE());
		p.getPipeline().add(6, new BBSE());
		p.syncEncoders();
		o = p.encode(null, ByteBuffer.wrap("ABC".getBytes()));
		assertNotNull(o);
		assertEquals(1, o.size());
		assertEquals("ABC", stringFromBuffer(o.get(0)));
		assertEquals("BBSE|SBE|BSE|SBBE|V2(ABC)|V1(ABC)|", getTrace());
		
	}
	
	@Test 
	public void testSessionEventOrdinals() {
		assertEquals(5, SessionEvent.values().length);
		assertEquals(0, SessionEvent.CREATED.ordinal());
		assertEquals(1, SessionEvent.OPENED.ordinal());
		assertEquals(2, SessionEvent.READY.ordinal());
		assertEquals(3, SessionEvent.CLOSED.ordinal());
		assertEquals(4, SessionEvent.ENDING.ordinal());
	}
	
	DefaultCodecExecutor createExecutor(String id) {
		DefaultCodecExecutor e = new DefaultCodecExecutor();
		ICodecPipeline p = e.getPipeline();
		
		p.add(1, new SBE());
		p.add(2, new SSEEv(id));
		return e;
	}
	
	@Test
	public void testEventDrivenChildCodecs() {
		DefaultCodecExecutor e0 = createExecutor("0");
		DefaultCodecExecutor e1 = createExecutor("1");
	
		e0.addChild(null, e1);
		e0.syncEventDrivenCodecs(null);
		e0.event(null, SessionEvent.CREATED);
		assertEquals("ADD(0)|CREATED(0)|ADD(1)|CREATED(1)|", getTrace());
		e0.event(null,  SessionEvent.OPENED);
		assertEquals("OPENED(0)|OPENED(1)|", getTrace());

		DefaultCodecExecutor e2 = createExecutor("2");
		e0.addChild(null, e2);
		assertEquals("ADD(2)|CREATED(2)|OPENED(2)|", getTrace());
		e0.event(null, SessionEvent.READY);
		assertEquals("READY(0)|READY(1)|READY(2)|", getTrace());
		
		e2.getPipeline().remove(2);
		e0.event(null, SessionEvent.CLOSED);
		assertEquals("CLOSED(0)|CLOSED(1)|REM(2)|", getTrace());
		e0.event(null, SessionEvent.ENDING);
		assertEquals("ENDING(0)|ENDING(1)|", getTrace());
		
		DefaultCodecExecutor e3 = createExecutor("3");
		e0.addChild(null, e3);
		assertEquals("ADD(3)|CREATED(3)|OPENED(3)|READY(3)|CLOSED(3)|ENDING(3)|", getTrace());
		e3.getPipeline().remove(2);
		
		e0.event(null, SessionEvent.OPENED);
		assertEquals("OPENED(0)|OPENED(1)|REM(3)|", getTrace());
		
		DefaultCodecExecutor e4 = createExecutor("4");
		e0.addChild(null, e4);
		assertEquals("ADD(4)|CREATED(4)|OPENED(4)|READY(4)|CLOSED(4)|ENDING(4)|", getTrace());
		
		e0.event(null, SessionEvent.CREATED);
		assertEquals("CREATED(0)|CREATED(1)|CREATED(4)|", getTrace());
		
		DefaultCodecExecutor e5 = createExecutor("5");
		e0.addChild(null, e5);
		assertEquals("ADD(5)|CREATED(5)|", getTrace());
		e0.event(null, SessionEvent.ENDING);
		assertEquals("ENDING(0)|ENDING(1)|ENDING(4)|ENDING(5)|", getTrace());

		DefaultCodecExecutor e6 = createExecutor("6");
		e0.addChild(null, e6);
		assertEquals("ADD(6)|CREATED(6)|ENDING(6)|", getTrace());
	}
	
	@Test
	public void testEventDrivenCodecs() {
		DefaultCodecExecutor e = new DefaultCodecExecutor();
		ICodecPipeline p = e.getPipeline();
		
		e.event(null, SessionEvent.CREATED);
		e.syncEventDrivenCodecs(null);
		e.event(null, SessionEvent.CREATED);
		p.add(1, new SBE());
		p.add(2, new BSD());
		e.syncEventDrivenCodecs(null);
		e.event(null, SessionEvent.CREATED);
		SSEEv e3 = new SSEEv("3");
		SSDEv d4 = new SSDEv("4");
		p.add(3, e3);
		p.add(4, d4);
		e.syncEventDrivenCodecs(null);
		e.event(null, SessionEvent.CREATED);
		assertEquals("ADD(4)|ADD(3)|CREATED(4)|CREATED(3)|", getTrace());
		SSEEv e5 = new SSEEv("5");
		p.add(5, e5);
		e.event(null, SessionEvent.READY);
		assertEquals("READY(4)|READY(3)|", getTrace());
		e.syncEventDrivenCodecs(null);
		e.event(null, SessionEvent.READY);
		assertEquals("ADD(5)|READY(4)|READY(3)|READY(5)|", getTrace());
		p.remove(3);
		e.syncEventDrivenCodecs(null);
		e.event(null, SessionEvent.READY);
		assertEquals("REM(3)|READY(4)|READY(5)|", getTrace());
		p.add(31, e3);
		p.add(32, e3);
		e.syncEventDrivenCodecs(null);
		e.event(null, SessionEvent.READY);
		assertEquals("ADD(3)|READY(4)|READY(5)|READY(3)|", getTrace());
		p.remove(31);
		p.remove(32);
		e.syncEventDrivenCodecs(null);
		e.event(null, SessionEvent.READY);
		assertEquals("REM(3)|READY(4)|READY(5)|", getTrace());
	}
	
	class BVD implements IDecoder<byte[],Void> {
		final String name; final boolean read;
		BVD(String name) {this.name = name; read = false;}
		BVD(String name, boolean read) {this.name = name; this.read = read;}
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, byte[] data, List<Void> out) {
			String s = name;
			if (read) {
				s += "("+new String(data)+")";
			}
			trace(s+"|");
		}
	}

	class BBVD implements IDecoder<ByteBuffer,Void> {
		final String name; final boolean read;
		BBVD(String name) {this.name = name; read = false;}
		BBVD(String name, boolean read) {this.name = name; this.read = read;}
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, ByteBuffer data, List<Void> out) {
			String s = name;
			if (read) {
				byte[] b = new byte[data.remaining()];
				
				data.duplicate().get(b);
				s += "("+new String(b)+")";
			}
			trace(s+"|");
		}
	}
	
	class B_BBD implements IDecoder<byte[],ByteBuffer> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<ByteBuffer> getOutboundType() {return ByteBuffer.class;}
		@Override public void decode(ISession session, byte[] data, List<ByteBuffer> out) {
			out.add(ByteBuffer.wrap(data)); trace("B_BBD|");
		}
	}
	
	class D implements IDecoder<byte[],byte[]> {
		final String name;
		D(String name) {this.name = name;}
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void decode(ISession session, byte[] data, List<byte[]> out) {
			out.add(data); trace(name+"|");
		}
	}

	class E implements IEncoder<byte[],byte[]> {
		final String name;
		E(String name) {this.name = name;}
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void encode(ISession session, byte[] data, List<byte[]> out) {
			out.add(data); trace(name+"|");
		}
	}
	
	class BaseS implements IBaseDecoder<String> {
		@Override public int available(ISession session, ByteBuffer buffer, boolean flipped) {return 0;}
		@Override public int available(ISession session, byte[] buffer, int off, int len) {return 0;}
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void decode(ISession session, byte[] data, List<String> out) {
			out.add(new String(data)); trace("BaseS|");
		}
	}
	
	class BSD implements IDecoder<byte[],String> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void decode(ISession session, byte[] data, List<String> out) {
			out.add(new String(data)); trace("BSD|");
		}
	}

	class BSDDiscard implements IDecoder<byte[],String> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void decode(ISession session, byte[] data, List<String> out) {
			trace("BSDDiscard|");
		}
	}

	class BSDDuplicate implements IDecoder<byte[],String> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void decode(ISession session, byte[] data, List<String> out) {
			out.add(new String(data));
			out.add(new String(data));
			trace("BSDDuplicate|");
		}
	}
	
	class SBD implements IDecoder<String,byte[]> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void decode(ISession session, String data, List<byte[]> out) {
			out.add(data.getBytes()); trace("SBD|");
		}
	}
	
	class SSD implements IDecoder<String,String> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void decode(ISession session, String data, List<String> out) {
			out.add(data); trace("SSD|");
		}
	}
	
	class SSDEv extends SSD implements IEventDrivenCodec {
		String id;
		
		SSDEv(String id) {
			this.id = id;
		}
		
		@Override
		public void added(ISession session) {
			trace("ADD("+id+")|");
		}

		@Override
		public void event(ISession session, SessionEvent event) {
			trace(event.toString() + "("+id+")|");
		}

		@Override
		public void removed(ISession session) {
			trace("REM("+id+")|");
		}
	}
	
	class BBSD implements IDecoder<ByteBuffer,String> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void decode(ISession session, ByteBuffer data, List<String> out) {
			byte[] b = new byte[data.remaining()];
			data.get(b);
			out.add(new String(b));
			trace("BBSD|");
		}
	}

	class BVE implements IEncoder<byte[],Void> {
		final String name; final boolean read;
		BVE(String name) {this.name = name; read = false;}
		BVE(String name, boolean read) {this.name = name; this.read = read;}
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, byte[] data, List<Void> out) {
			String s = name;
			if (read) {
				s += "("+new String(data)+")";
			}
			trace(s+"|");
		}
	}
	
	class BBVE implements IEncoder<ByteBuffer,Void> {
		final String name; final boolean read;
		BBVE(String name) {this.name = name; read = false;}
		BBVE(String name, boolean read) {this.name = name; this.read = read;}
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, ByteBuffer data, List<Void> out) {
			String s = name;
			if (read) {
				byte[] b = new byte[data.remaining()];
				
				data.duplicate().get(b);
				s += "("+new String(b)+")";
			}
			trace(s+"|");
		}
	}

	class SVE implements IEncoder<String,Void> {
		final String name; final boolean read;
		SVE(String name) {this.name = name; read = false;}
		SVE(String name, boolean read) {this.name = name; this.read = read;}
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, String data, List<Void> out) {
			String s = name;
			if (read) {
				s += "("+data+")";
			}
			trace(s+"|");
		}
	}
	
	class BBE implements IEncoder<byte[],byte[]> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void encode(ISession session, byte[] data, List<byte[]> out) {
			out.add(data); trace("BBE|");}
	}
	
	class BB_BE implements IEncoder<ByteBuffer,byte[]> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void encode(ISession session, ByteBuffer data, List<byte[]> out) {
			byte[] b = new byte[data.remaining()];
			data.get(b);
			out.add(b);
			trace("BB_BE|");
		}
	}
	
	class SBE implements IEncoder<String,byte[]> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void encode(ISession session, String data, List<byte[]> out) {
			out.add(data.getBytes()); trace("SBE|");
		}
	}

	class SBEDiscard implements IEncoder<String,byte[]> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void encode(ISession session, String data, List<byte[]> out) {
			trace("SBEDiscard|");
		}
	}

	class SBEDuplicate implements IEncoder<String,byte[]> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void encode(ISession session, String data, List<byte[]> out) {
			out.add(data.getBytes());
			out.add(data.getBytes());
			trace("SBEDuplicate|");
		}
	}
	
    class BSE implements IEncoder<byte[],String> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void encode(ISession session, byte[] data, List<String> out) {
			out.add(new String(data)); trace("BSE|");
		}
	}
	
	class SBBE implements IEncoder<String,ByteBuffer> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<ByteBuffer> getOutboundType() {return ByteBuffer.class;}
		@Override public void encode(ISession session, String data, List<ByteBuffer> out) {
			out.add(ByteBuffer.wrap(data.getBytes()));
			trace("SBBE|");
		}
	}
	
	class BBSE implements IEncoder<ByteBuffer,String> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void encode(ISession session, ByteBuffer data, List<String> out) {
			byte[] b = new byte[data.remaining()];
			data.get(b);
			out.add(new String(b));
			trace("BBSE|");
		}
	}

	class SSE implements IEncoder<String,String> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void encode(ISession session, String data, List<String> out) {
			out.add(data);
			trace("SSE|");
		}
	}
	
	class SSEEv extends SSE  implements IEventDrivenCodec {
		String id;
		
		SSEEv(String id) {
			this.id = id;
		}
		
		@Override
		public void added(ISession session) {
			trace("ADD("+id+")|");
		}

		@Override
		public void event(ISession session, SessionEvent event) {
			trace(event.toString() + "("+id+")|");
		}

		@Override
		public void removed(ISession session) {
			trace("REM("+id+")|");
		}
	}
}
