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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

class InternalCodecPipeline implements ICodecPipeline {

	private final static int FIRST_DECODER = 0;
	private final static int LAST_DECODER = 1;
	private final static int FIRST_ENCODER = 2;
	private final static int LAST_ENCODER = 3;

	private volatile int codecsVersion;

	private CodecContext[] codecs = new CodecContext[4];
	
	private Map<Object,CodecContext> map = new HashMap<Object,CodecContext>();
	
	final int getCodecsVersion() {
		return codecsVersion;
	}
	
	final Object getLock() {
		return codecs;
	}
	
	final CodecContext getFirstDecoder() {
		return codecs[FIRST_DECODER];
	}
	
	final CodecContext getFirstEncoder() {
		return codecs[FIRST_ENCODER];
	}
	
	private final void validate(CodecContext previous, CodecContext ctx, CodecContext next) {
		if (!ctx.isValid(previous)) {
			StringBuilder msg = new StringBuilder();
			
			if (previous != null && previous.isDecoder() != ctx.isDecoder()) {
				msg.append("incompatible codec type");
			}
			else {
				msg.append(ctx.isDecoder() ? "decoder '" : "encoder '");
				msg.append(ctx.key);
				msg.append("' has incompatible ");
				msg.append(ctx.isDecoder() ? "inbound " : "outbound ");
				msg.append("type");
			}
			throw new IllegalArgumentException(msg.toString());
		}
		if (next != null) {
			ctx.prev = previous;
			if (!next.isValid(ctx)) {
				StringBuilder msg = new StringBuilder();
				
				if (next.isDecoder() != ctx.isDecoder()) {
					msg.append("incompatible codec type");
				}
				else {
					msg.append(ctx.isDecoder() ? "decoder '" : "encoder '");
					msg.append(ctx.key);
					msg.append("' has incompatible ");
					msg.append(!ctx.isDecoder() ? "inbound " : "outbound ");
					msg.append("type");
				}
				throw new IllegalArgumentException(msg.toString());
			}
		}
	}

	private final void storeAndRemove(CodecContext ctx, CodecContext oldCtx) {
		if (oldCtx != null) {
			map.remove(oldCtx.key);
		}
		if (ctx != null) {
			if (map.containsKey(ctx.key)) {
				if (oldCtx != null) {
					map.put(oldCtx.key, oldCtx);
				}
				throw new IllegalArgumentException("key '" + ctx.key +"' already exists");
			}
			map.put(ctx.key, ctx);
		}
	}
	
	private final CodecContext find(Object key) {
		CodecContext ctx = map.get(key);
		
		if (ctx == null) {
			throw new NoSuchElementException("key '" + key +"' does not exist");
		}
		return ctx;
	}

	private final CodecContext ctx(Object key, ICodec<?,?> codec) {
		if (key == null) {
			throw new NullPointerException("key is null");
		}
		if (codec instanceof IDecoder) {
			return new DecoderContext(key, (IDecoder<?, ?>) codec);
		}
		return new EncoderContext(key, (IEncoder<?, ?>) codec);
	}
	
	private final void notNull(Object key, String argName) {
		if (key == null) {
			throw new NullPointerException(argName + " is null");
		}
	}
	
	private final void after(Object baseKey, Object key, ICodec<?,?> codec) {
		CodecContext ctx = ctx(key, codec);
		int lastCodec = ctx.isDecoder() ? LAST_DECODER : LAST_ENCODER;
		
		synchronized (codecs) {
			CodecContext prevCtx = baseKey != null ? find(baseKey) : codecs[lastCodec];
			validate(prevCtx, ctx, prevCtx != null ? prevCtx.next : null);
			storeAndRemove(ctx, null);
			if (prevCtx == null) {
				codecs[lastCodec-1] = ctx;
				codecs[lastCodec] = ctx;
			}
			else {
				ctx.prev = prevCtx;
				ctx.next = prevCtx.next;
				prevCtx.next = ctx;
				if (ctx.next != null) {
					ctx.next.prev = ctx;
				}
				else {
					codecs[lastCodec] = ctx;
				}
			}
			++codecsVersion;
		}
	}
	
	@Override
	public void add(Object key, IDecoder<?,?> decoder) {
		notNull(decoder, "decoder");
		after(null, key, decoder);
	}
	
	@Override
	public void add(Object key, IEncoder<?,?> encoder) {
		notNull(encoder, "encoder");
		after(null, key, encoder);
	}
	
	@Override
	public void addAfter(Object baseKey, Object key, IDecoder<?, ?> decoder) {
		notNull(baseKey, "baseKey");
		notNull(decoder, "decoder");
		after(baseKey, key, decoder);
	}

	@Override
	public void addAfter(Object baseKey, Object key, IEncoder<?, ?> encoder) {
		notNull(baseKey, "baseKey");
		notNull(encoder, "encoder");
		after(baseKey, key, encoder);
	}
	
	private final void before(Object baseKey, Object key, ICodec<?,?> codec) {
		CodecContext ctx = ctx(key, codec);
		int firstCodec = ctx.isDecoder() ? FIRST_DECODER : FIRST_ENCODER;
		
		synchronized (codecs) {
			CodecContext nextCtx = baseKey != null ? find(baseKey) : codecs[firstCodec];
			
			validate(nextCtx != null ? nextCtx.prev : null, ctx, nextCtx);
			storeAndRemove(ctx, null);
			if (nextCtx == null) {
				codecs[firstCodec] = ctx;
				codecs[firstCodec+1] = ctx;
			}
			else {
				ctx.next = nextCtx;
				ctx.prev = nextCtx.prev;
				nextCtx.prev = ctx;
				if (ctx.prev != null) {
					ctx.prev.next = ctx;
				}
				else {
					codecs[firstCodec] = ctx;
				}
			}
			++codecsVersion;
		}
	}
	
	@Override
	public void addFirst(Object key, IDecoder<?, ?> decoder) {
		notNull(decoder, "decoder");
		before(null, key, decoder);
	}

	@Override
	public void addFirst(Object key, IEncoder<?, ?> encoder) {
		notNull(encoder, "encoder");
		before(null, key, encoder);
	}
	
	@Override
	public void addBefore(Object baseKey, Object key, IDecoder<?, ?> decoder) {
		notNull(baseKey, "baseKey");
		notNull(decoder, "decoder");
		before(baseKey, key, decoder);
	}

	@Override
	public void addBefore(Object baseKey, Object key, IEncoder<?, ?> encoder) {
		notNull(baseKey, "baseKey");
		notNull(encoder, "encoder");
		before(baseKey, key, encoder);
	}
	
	private final CodecContext replace0(Object oldKey, Object key, ICodec<?,?> codec) {
		notNull(oldKey, "oldKey");
		
		CodecContext ctx = ctx(key, codec);
		int firstCodec = ctx.isDecoder() ? FIRST_DECODER : FIRST_ENCODER;
		CodecContext oldCtx;
		
		synchronized (codecs) {
			oldCtx = find(oldKey);
			
			validate(oldCtx.prev, ctx, oldCtx.next);
			storeAndRemove(ctx, oldCtx);
			ctx.prev = oldCtx.prev;
			ctx.next = oldCtx.next;
			if (ctx.prev == null) {
				codecs[firstCodec] = ctx;
			}
			else {
				ctx.prev.next = ctx;
			}
			if (ctx.next == null) {
				codecs[firstCodec+1] = ctx;
			}
			else {
				ctx.next.prev = ctx;
			}
			++codecsVersion;
		}
		return oldCtx;
	}
	
	@Override
	public IDecoder<?,?> replace(Object oldKey, Object key, IDecoder<?,?> decoder) {
		notNull(decoder, "decoder");
		return ((DecoderContext)replace0(oldKey, key, decoder)).getDecoder();
	}
	
	@Override
	public IEncoder<?,?> replace(Object oldKey, Object key, IEncoder<?,?> encoder) {
		notNull(encoder, "encoder");
		return ((EncoderContext)replace0(oldKey, key, encoder)).getEncoder();
	}
	
	public ICodec<?,?> remove(Object key) {
		notNull(key, "key");
		
		CodecContext ctx;
		
		synchronized (codecs) {
			ctx = find(key);
			int firstCodec = ctx.isDecoder() ? FIRST_DECODER : FIRST_ENCODER;
			
			if (ctx.next != null) {
				validate(ctx.prev, ctx.next, null);
				ctx.next.prev = ctx.prev;
			}
			else {
				codecs[firstCodec+1] = ctx.prev;
			}
			if (ctx.prev != null) {
				ctx.prev.next = ctx.next;
			}
			else {
				codecs[firstCodec] = ctx.next;
			}
			storeAndRemove(null, ctx);
			++codecsVersion;
		}
		return ctx.isDecoder() ? ((DecoderContext)ctx).getDecoder() : ((EncoderContext)ctx).getEncoder();
	}
	
	@Override
	public ICodec<?,?> get(Object key) {
		notNull(key, "key");
		
		CodecContext ctx;
		
		synchronized (codecs) {
			ctx = map.get(key);
		}
		if (ctx == null) {
			return null;
		}
		return ctx.isDecoder() ? ((DecoderContext)ctx).getDecoder() : ((EncoderContext)ctx).getEncoder();
		
	}
	
	private final List<Object> keys(int index) {
		List<Object> keys = new ArrayList<Object>();
		
		synchronized(codecs) {
			CodecContext ctx = codecs[index];
			
			while (ctx != null) {
				keys.add(ctx.getKey());
				ctx = ctx.next;
			}
		}
		return keys;
	}
	
	@Override
	public List<Object> encoderKeys() {
		return keys(FIRST_ENCODER);
	}

	@Override
	public List<Object> decoderKeys() {
		return keys(FIRST_DECODER);
	}
}
