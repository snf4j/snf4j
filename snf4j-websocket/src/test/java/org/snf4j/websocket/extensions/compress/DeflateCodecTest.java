/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.websocket.extensions.compress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.TestSession;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.codec.IEventDrivenCodec;
import org.snf4j.core.codec.zip.ZlibDecoder;
import org.snf4j.core.codec.zip.ZlibEncoder;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.PingFrame;

public class DeflateCodecTest {
	
	List<ByteBuffer> buffers(String... values) {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		
		for (String value: values) {
			bufs.add(ByteBuffer.wrap(value.getBytes()));
		}
		return bufs;
	}
	
	public static ZlibDecoder getZlibDecoder(Object deflateDecoder) throws Exception {
		Field f = DeflateDecoder.class.getDeclaredField("decoder");
		
		f.setAccessible(true);
		return (ZlibDecoder) f.get(deflateDecoder);
	}

	public static ZlibEncoder getZlibEncoder(Object deflateEncoder) throws Exception {
		Field f = DeflateEncoder.class.getDeclaredField("encoder");
		
		f.setAccessible(true);
		return (ZlibEncoder) f.get(deflateEncoder);
	}
	
	@Test
	public void testBytes() {
		List<ByteBuffer> bufs;
		
		bufs = buffers("-AAAAAAAAA+",";BBBBBB=","CCCCXXXX");
		assertEquals("-AAAAAAAAA+;BBBBBB=CCCCXXXX", new String(DeflateCodec.bytes(bufs, false)));
		bufs = buffers("-AAAAAAAAA+",";BBBBBB=","CCCCXXXX");
		assertEquals("-AAAAAAAAA+;BBBBBB=CCCC", new String(DeflateCodec.bytes(bufs, true)));
		bufs = buffers(";BBBBBB=","CCCCXXXX");
		assertEquals(";BBBBBB=CCCCXXXX", new String(DeflateCodec.bytes(bufs, false)));
		bufs = buffers(";BBBBBB=","CCCCXXXX");
		assertEquals(";BBBBBB=CCCC", new String(DeflateCodec.bytes(bufs, true)));
		bufs = buffers("CCCCXXXX");
		assertEquals("CCCCXXXX", new String(DeflateCodec.bytes(bufs, false)));
		bufs = buffers("CCCCXXXX");
		assertEquals("CCCC", new String(DeflateCodec.bytes(bufs, true)));
		bufs = buffers();
		assertEquals("", new String(DeflateCodec.bytes(bufs, false)));
		bufs = buffers("");
		assertEquals("", new String(DeflateCodec.bytes(bufs, true)));
	}
	
	@Test
	public void testMisc() {
		final DrivenCodec drivenCodec = new DrivenCodec();
		
		DeflateCodec codec = new DeflateCodec() {

			@Override
			protected int rsvBits(Frame frame) {
				return 4;
			}

			@Override
			protected IEventDrivenCodec codec() {
				return drivenCodec;
			}
			
		};
		
		assertTrue(codec.getInboundType() == Frame.class);
		assertTrue(codec.getOutboundType() == Frame.class);
		Frame f = new PingFrame();
		assertTrue(f == codec.createFrame(f, "ABCD".getBytes()));
		
		TestSession session = new TestSession("s1");
		
		codec.added(session, null);
		assertEquals("As1", drivenCodec.getTrace());
		codec.event(session, SessionEvent.ENDING);
		assertEquals("Es1ENDING", drivenCodec.getTrace());
		codec.removed(session, null);
		assertEquals("Rs1", drivenCodec.getTrace());
		
		codec = new DeflateCodec() {

			@Override
			protected int rsvBits(Frame frame) {
				return 4;
			}

			@Override
			protected IEventDrivenCodec codec() {
				return null;
			}
			
		};
		codec.added(session, null);
		codec.event(session, SessionEvent.ENDING);
		codec.removed(session, null);
	
	}
	
	static class DrivenCodec implements IEventDrivenCodec {

		StringBuilder trace = new StringBuilder();
		
		String getTrace() {
			String s = trace.toString();
			trace.setLength(0);
			return s;
		}
		
		@Override
		public void added(ISession session, ICodecPipeline pipeline) {
			trace.append("A"+session.getName());
		}

		@Override
		public void event(ISession session, SessionEvent event) {
			trace.append("E"+session.getName()+event);
		}

		@Override
		public void removed(ISession session, ICodecPipeline pipeline) {
			trace.append("R"+session.getName());;
		}
		
	}
}
