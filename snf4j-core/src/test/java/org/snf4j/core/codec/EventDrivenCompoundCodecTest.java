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
package org.snf4j.core.codec;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.TestSession;
import org.snf4j.core.codec.bytes.ArrayToBufferDecoder;
import org.snf4j.core.codec.bytes.ArrayToBufferEncoder;
import org.snf4j.core.codec.bytes.BufferToArrayDecoder;
import org.snf4j.core.codec.bytes.BufferToArrayEncoder;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

public class EventDrivenCompoundCodecTest {
	
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
	public void testEncoding() {
		IEncoder<?,?> eBb = new BufferToArrayEncoder();
		IEncoder<?,?> ebB = new ArrayToBufferEncoder();
		Encoder e1Bb = new Encoder("1");
		Encoder e2Bb = new Encoder("2");
		TestSession s = new TestSession() {
			
			@Override
			public String toString() {
				return "S";
			}
		};
		InternalCodecPipeline p = new InternalCodecPipeline() {
			@Override
			public String toString() {
				return "P";
			}
		};
		
		CompoundEncoder ceBb = new CompoundEncoder(e1Bb);
		ceBb.added(s, p);
		assertEquals("ADD(1)[SP]|", getTrace());
		ceBb.removed(s, p);
		assertEquals("REM(1)[SP]|", getTrace());
		ceBb.event(s, SessionEvent.CREATED);
		assertEquals("CREATED(1)|", getTrace());
		
		ceBb = new CompoundEncoder(eBb,ebB,e1Bb,ebB,eBb);
		ceBb.added(s, p);
		assertEquals("ADD(1)[SP]|", getTrace());
		ceBb.removed(s, p);
		assertEquals("REM(1)[SP]|", getTrace());
		ceBb.event(s, SessionEvent.CREATED);
		assertEquals("CREATED(1)|", getTrace());
		
		ceBb = new CompoundEncoder(eBb,ebB,e1Bb,ebB,e2Bb);
		ceBb.added(s, p);
		assertEquals("ADD(1)[SP]|ADD(2)[SP]|", getTrace());
		ceBb.removed(s, p);
		assertEquals("REM(1)[SP]|REM(2)[SP]|", getTrace());
		ceBb.event(s, SessionEvent.CREATED);
		assertEquals("CREATED(1)|CREATED(2)|", getTrace());
	}
	
	@Test
	public void testDecoding() {
		IDecoder<?,?> dBb = new BufferToArrayDecoder();
		IDecoder<?,?> dbB = new ArrayToBufferDecoder();
		Decoder d1Bb = new Decoder("1");
		Decoder d2Bb = new Decoder("2");
		TestSession s = new TestSession() {
			
			@Override
			public String toString() {
				return "S";
			}
		};
		InternalCodecPipeline p = new InternalCodecPipeline() {
			@Override
			public String toString() {
				return "P";
			}
		};
		
		CompoundDecoder cdBb = new CompoundDecoder(d1Bb);
		cdBb.added(s, p);
		assertEquals("ADD(1)[SP]|", getTrace());
		cdBb.removed(s, p);
		assertEquals("REM(1)[SP]|", getTrace());
		cdBb.event(s, SessionEvent.CREATED);
		assertEquals("CREATED(1)|", getTrace());
		
		cdBb = new CompoundDecoder(dBb,dbB,d1Bb,dbB,dBb);
		cdBb.added(s, p);
		assertEquals("ADD(1)[SP]|", getTrace());
		cdBb.removed(s, p);
		assertEquals("REM(1)[SP]|", getTrace());
		cdBb.event(s, SessionEvent.CREATED);
		assertEquals("CREATED(1)|", getTrace());
		
		cdBb = new CompoundDecoder(dBb,dbB,d1Bb,dbB,d2Bb);
		cdBb.added(s, p);
		assertEquals("ADD(1)[SP]|ADD(2)[SP]|", getTrace());
		cdBb.removed(s, p);
		assertEquals("REM(1)[SP]|REM(2)[SP]|", getTrace());
		cdBb.event(s, SessionEvent.CREATED);
		assertEquals("CREATED(1)|CREATED(2)|", getTrace());
	}

	class Encoder extends BufferToArrayEncoder implements IEventDrivenCodec {
		
		String id;
		
		Encoder(String id) {
			this.id = id;
		}
		
		@Override
		public void added(ISession session, ICodecPipeline pipeline) {
			trace("ADD("+id+")["+session+pipeline+"]");
		}

		@Override
		public void event(ISession session, SessionEvent event) {
			trace(event.name() + "("+id+")");
		}

		@Override
		public void removed(ISession session, ICodecPipeline pipeline) {
			trace("REM("+id+")["+session+pipeline+"]");
		}
	}

	class Decoder extends BufferToArrayDecoder implements IEventDrivenCodec {
		
		String id;
		
		Decoder(String id) {
			this.id = id;
		}
		
		@Override
		public void added(ISession session, ICodecPipeline pipeline) {
			trace("ADD("+id+")["+session+pipeline+"]");
		}

		@Override
		public void event(ISession session, SessionEvent event) {
			trace(event.name() + "("+id+")");
		}

		@Override
		public void removed(ISession session, ICodecPipeline pipeline) {
			trace("REM("+id+")["+session+pipeline+"]");
		}
	}
	
	class CompoundEncoder extends EventDrivenCompoundEncoder<ByteBuffer,byte[]> {

		CompoundEncoder(IEncoder<?,?>... encoders) {
			super(encoders);
		}
		
		public Class<ByteBuffer> getInboundType() {
			return ByteBuffer.class;
		}

		@Override
		public Class<byte[]> getOutboundType() {
			return byte[].class;
		}
		
	}
	
	class CompoundDecoder extends EventDrivenCompoundDecoder<ByteBuffer,byte[]> {

		CompoundDecoder(IDecoder<?,?>... decoders) {
			super(decoders);
		}
		
		public Class<ByteBuffer> getInboundType() {
			return ByteBuffer.class;
		}

		@Override
		public Class<byte[]> getOutboundType() {
			return byte[].class;
		}
		
	}	
}
