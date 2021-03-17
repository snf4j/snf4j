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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

/**
 * The default implementation of the {@link ICodecExecutor}.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DefaultCodecExecutor implements ICodecExecutor {

	private final static int EVENT_COUNT = SessionEvent.values().length;
	
	private int decodersVersion;
	
	private final Deque<DecoderContext> decoders = new LinkedList<DecoderContext>();

	private IBaseDecoder<?> baseDecoder;
	
	private boolean hasDecoders;
	
	private int encodersVersion;
	
	/** first encoder that produce an output */
	private EncoderContext firstEncoder;
	
	private final Deque<EncoderContext> encoders = new LinkedList<EncoderContext>();
	
	private final InternalCodecPipeline pipeline = new InternalCodecPipeline();
	
	private int eventCodecsVersion;
	
	private IEventDrivenCodec[] eventCodecs;
	
	private SessionEvent[] events;
	
	private List<ICodecExecutor> children;
	
	@Override
	public final void syncDecoders() {
		if (pipeline.getCodecsVersion() != decodersVersion) {
			decoders.clear();
			baseDecoder = null;
			hasDecoders = false;
			synchronized (pipeline.getLock()) {
				CodecContext ctx = pipeline.getFirstDecoder();
				
				while (ctx != null) {
					decoders.add((DecoderContext) ctx);
					if (!ctx.isClogged()) {
						if (!hasDecoders) {
							if (((DecoderContext) ctx).getDecoder() instanceof IBaseDecoder<?>) {
								baseDecoder = (IBaseDecoder<?>) ((DecoderContext) ctx).getDecoder();
							}
							hasDecoders = true;
						}
					}
					ctx = ctx.next;
				}
				decodersVersion = pipeline.getCodecsVersion();
			}
		}
	}
	
	@Override
	public final void syncEncoders() {
		if (pipeline.getCodecsVersion() != encodersVersion) {
			encoders.clear();
			firstEncoder = null;
			synchronized (pipeline.getLock()) {
				CodecContext ctx = pipeline.getFirstEncoder();
				
				while (ctx != null) {
					encoders.add((EncoderContext) ctx);
					if (firstEncoder == null &&!ctx.isClogged()) {
						firstEncoder = (EncoderContext) ctx;
					}
					ctx = ctx.next;
				}
				encodersVersion = pipeline.getCodecsVersion();
			}
		}		
	}
	
	private static boolean remove(Object o, Object[] objects) {
		boolean removed = false;
		
		if (objects != null) {
			for (int i=0; i<objects.length; ++i) {
				if (objects[i] == o) {
					objects[i] = null;
					removed = true;
				}
			}
		}
		return removed;
	}
	
	private static IEventDrivenCodec[] toArrayWithNoDuplicates(List<IEventDrivenCodec> codecs) {
		if (codecs.isEmpty()) {
			return null;
		}
		Object[] objects = codecs.toArray();
		Iterator<IEventDrivenCodec> i = codecs.iterator();
		
		while (i.hasNext()) {
			if (!remove(i.next(), objects)) {
				i.remove();
			}
		}
		return codecs.toArray(new IEventDrivenCodec[codecs.size()]);
	}
	
	@Override
	public void syncEventDrivenCodecs(ISession session) {
		if (pipeline.getCodecsVersion() != eventCodecsVersion) {
			List<IEventDrivenCodec> newCodecs = new ArrayList<IEventDrivenCodec>();
			
			synchronized (pipeline.getLock()) {
				CodecContext ctx = pipeline.getFirstDecoder();
				ICodec<?,?> codec;
				
				while (ctx != null) {
					codec = ((DecoderContext)ctx).getDecoder();
					if (codec instanceof IEventDrivenCodec) {
						newCodecs.add((IEventDrivenCodec) codec);
					}
					ctx = ctx.next;
				}
				ctx = pipeline.getFirstEncoder();
				while (ctx != null) {
					codec = ((EncoderContext)ctx).getEncoder();
					if (codec instanceof IEventDrivenCodec) {
						newCodecs.add((IEventDrivenCodec) codec);
					}
					ctx = ctx.next;
				}
				eventCodecsVersion = pipeline.getCodecsVersion();
			}
			
			IEventDrivenCodec[] removed = eventCodecs;
			
			eventCodecs = toArrayWithNoDuplicates(newCodecs);
			if (eventCodecs != null) {
				for (IEventDrivenCodec c: eventCodecs) {
					if (!remove(c, removed)) {
						c.added(session);
					}
				}
			}
			if (removed != null) {
				for (IEventDrivenCodec c: removed) {
					if (c != null) {
						c.removed(session);
					}
				}
			}
		}
	}

	@Override
	public final ICodecPipeline getPipeline() {
		return pipeline;
	}
	
	@Override
	public boolean hasDecoders() {
		return hasDecoders;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Object> encode(ISession session, ByteBuffer data) throws Exception {
		Iterator<EncoderContext> i = encoders.descendingIterator();
		
		if (i.hasNext()) {
			EncoderContext ctx;
			
			do {
				ctx = i.next();
				if (ctx.isInboundByte()) {
					if (ctx.isInboundByteArray()) {
						byte[] dataArray = new byte[data.remaining()];
						
						if (ctx.isClogged()) {
							data.duplicate().get(dataArray);
							ctx.getEncoder().encode(session, dataArray, null);
						}
						else {
							data.get(dataArray);
							session.release(data);
							return encode(session, dataArray, ctx, i);
						}
					}
					else {
						if (ctx.isClogged()) {
							ctx.getEncoder().encode(session, data, null);
						}
						else {
							return encode(session, data, ctx, i);
						}
					}
				}
			} while (i.hasNext());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object> encode(ISession session, byte[] data) throws Exception {
		Iterator<EncoderContext> i = encoders.descendingIterator();
		
		if (i.hasNext()) {
			EncoderContext ctx;
			
			do {
				ctx = i.next();
				if (ctx.isInboundByte()) {
					if (ctx.isInboundByteArray()) {
						if (ctx.isClogged()) {
							ctx.getEncoder().encode(session, data, null);
						}
						else {
							return encode(session, data, ctx, i);
						}
					}
					else {
						ByteBuffer buffer = ByteBuffer.wrap(data);
						
						if (ctx.isClogged()) {
							ctx.getEncoder().encode(session, buffer, null);
						}
						else {
							return encode(session, buffer, ctx, i);
						}
					}
				}
			} while (i.hasNext());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object> encode(ISession session, Object msg) throws Exception {
		Iterator<EncoderContext> i = encoders.descendingIterator();
		
		if (i.hasNext()) {
			EncoderContext ctx;
			
			do {
				ctx = i.next();
				if (ctx.getEncoder().getInboundType().isAssignableFrom(msg.getClass())) {
					if (ctx.isClogged()) {
						ctx.getEncoder().encode(session, msg, null);
					}
					else {
						return encode(session, msg, ctx, i);
					}
				}
			} while (i.hasNext());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private final List<Object> encode(ISession session, Object data, EncoderContext ctx, Iterator<EncoderContext> i) throws Exception {
		List<Object> out = new ArrayList<Object>();

		ctx.getEncoder().encode(session, data, out);
		if (out.isEmpty()) {
			return out;
		}

		List<Object> in = new ArrayList<Object>();
		List<Object> tmp;
		boolean onlyClogged = firstEncoder == ctx; 
		
		while (i.hasNext()) {
			ctx = i.next();
			if (ctx.isClogged()) {
				for (Object o: out) {
					if (onlyClogged) {
						
						//There are only clogged encoders left and they can support
						//both byte or byte buffer as the inbound object
						boolean bytes = o.getClass() == byte[].class;
						
						if (bytes) {
							if (!ctx.isInboundByteArray()) {
								o = ByteBuffer.wrap((byte[])o);
							}
						}
						else {
							if (ctx.isInboundByteArray()) {
								ByteBuffer dup = ((ByteBuffer)o).duplicate();
								byte[] dataArray = new byte[dup.remaining()];
								
								((ByteBuffer)o).duplicate().get(dataArray);
								o = dataArray;
							}
						}
					}
					ctx.getEncoder().encode(session, o, null);
				}
				continue;
			}
			onlyClogged |= firstEncoder == ctx; 
			tmp = in;
			in = out;
			out = tmp;
			for (Object o: in) {
				ctx.getEncoder().encode(session, o, out);
			}
			in.clear();
		}
		return out;
	}
	
	@SuppressWarnings("unchecked")
	private final void decode(ISession session, DecoderContext ctx, byte[] data, List<Object> out) throws Exception {
		if (ctx.isInboundByteArray()) {
			ctx.getDecoder().decode(session, data, out);
		}
		else {
			ctx.getDecoder().decode(session, ByteBuffer.wrap(data), out);
		}
	}
	
	@Override
	public List<Object> decode(ISession session, byte[] data) throws Exception {
		Iterator<DecoderContext> i = decoders.iterator();
		
		if (!i.hasNext()) {
			return null;
		}

		DecoderContext ctx = i.next();
			
		while (ctx.isClogged()) {
			decode(session, ctx, data, null);
			if (i.hasNext()) {
				ctx = i.next();
			}
			else {
				return null;
			}
		}

		List<Object> out = new ArrayList<Object>();

		decode(session, ctx, data, out);
		return decode(session, i, out);
	}
	
	@Override
	public List<Object> decode(ISession session, ByteBuffer data) throws Exception {
		Iterator<DecoderContext> i = decoders.iterator();
		
		if (!i.hasNext()) {
			return null;
		}
		
		DecoderContext ctx = i.next();

		while (ctx.isClogged()) {
			decode(session, ctx, data);
			if (i.hasNext()) {
				ctx = i.next();
			}
			else {
				return null;
			}
		}

		List<Object> out = new ArrayList<Object>();

		decode(session, ctx, data, out);
		return decode(session, i, out);
	}
	
	@SuppressWarnings("unchecked")
	private final void decode(ISession session, DecoderContext ctx, ByteBuffer data, List<Object> out) throws Exception {
		if (ctx.isInboundByteArray()) {
			byte[] array = new byte[data.remaining()];
			
			data.get(array);
			session.release(data);
			ctx.getDecoder().decode(session, array, out);
		}
		else {
			ctx.getDecoder().decode(session, data, out);
		}
	}
	
	@SuppressWarnings("unchecked")
	private final void decode(ISession session, DecoderContext ctx, ByteBuffer data) throws Exception {
		if (ctx.isInboundByteArray()) {
			byte[] array = new byte[data.remaining()];
			
			data.duplicate().get(array);
			ctx.getDecoder().decode(session, array, null);
		}
		else {
			ctx.getDecoder().decode(session, data, null);
		}
	}
	
	@SuppressWarnings("unchecked")
	private final List<Object> decode(ISession session, Iterator<DecoderContext> i, List<Object> out) throws Exception {
		if (out.isEmpty()) {
			return out;
		}
		
		List<Object> in = new ArrayList<Object>();
		List<Object> tmp;
		DecoderContext ctx;
		
		while (i.hasNext()) {
			ctx = i.next();
			if (ctx.isClogged()) {
				for (Object o: out) {
					ctx.getDecoder().decode(session, o, null);
				}
				continue;
			}
			tmp = in;
			in = out;
			out = tmp;
			for (Object o: in) {
				ctx.getDecoder().decode(session, o, out);
			}
			in.clear();
		}
		return out;
	}
	
	@Override
	public final IBaseDecoder<?> getBaseDecoder() {
		return baseDecoder;
	}

	@Override
	public void event(ISession session, SessionEvent event) {
		if (events != null) {
			if (event == SessionEvent.CREATED) {
				for (int i=0; i<EVENT_COUNT; ++i) {
					events[i] = null;
				}
			}
		}
		else {
			events = new SessionEvent[EVENT_COUNT];
		}
		events[event.ordinal()] = event;
		
		if (eventCodecs != null) {
			for (IEventDrivenCodec codec: eventCodecs) {
				codec.event(session, event);
			}
		}
		
		if (children != null) {
			for (ICodecExecutor executor: children) {
				executor.syncEventDrivenCodecs(session);
				executor.event(session, event);
			}
		}
	}
	
	@Override
	public void addChild(ISession session, ICodecExecutor executor) {
		if (events != null) {
			for (int i=0; i<EVENT_COUNT; ++i) {
				SessionEvent event = events[i];

				if (event != null) {
					executor.syncEventDrivenCodecs(session);
					executor.event(session, event);
				}
			}
		}
		if (children == null) {
			children = new ArrayList<ICodecExecutor>();
		}
		children.add(executor);
	}
	
}
