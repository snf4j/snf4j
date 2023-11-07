/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.engine;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;

public class HandshakeController {

	final Context[] ctxs = new Context[2];
	
	final StringBuilder[] traces = new StringBuilder[] {new StringBuilder(), new StringBuilder()};
	
	public HandshakeController(HandshakeEngine cli, HandshakeEngine srv) {
		ctxs[0] = new Context(cli);
		ctxs[1] = new Context(srv);
	}
	
	int idx(boolean client) {
		return client ? 0 : 1;
	}
	
	int other(int idx) {
		return idx == 0 ? 1 : 0;
	}
	
	public void run(boolean client, HandshakeType type) throws Exception {
		for (int i=0; i<ctxs.length; ++i) {
			Context ctx = ctxs[i];
			
			if (!ctx.produced.isEmpty()) {
				consume(i, client, type);
				return;
			}
		}
		
		for (int i=0; i<ctxs.length; ++i) {
			Context ctx = ctxs[i];
			
			ctx.produce();
			if (!ctx.produced.isEmpty()) {
				run(client, type);
				break;
			}
		}		
	}
	
	public String trace(boolean client) {
		int i = idx(client);
		StringBuilder sb = traces[i];
		
		String s = sb.toString();
		sb.setLength(0);
		return s;
	}
	
	void consume(int idx, boolean client, HandshakeType type) throws Exception {
		int findIdx = idx(!client);
		int consumeIdx = other(idx);
		Context consumeCtx = ctxs[consumeIdx];
		
		for (Iterator<IHandshake> i = ctxs[idx].produced.iterator(); i.hasNext();) {
			IHandshake h = i.next();
			
			if (h.getType() == null) {
				traces[consumeIdx].append(h.getClass().getSimpleName()).append('|');
				i.remove();
				continue;
			}
			else {
				traces[consumeIdx].append(h.getType().name()).append('|');
			}
			
			if (consumeIdx == findIdx && h.getType().equals(type)) {
				return;
			}
			ByteBuffer buf = ByteBuffer.allocate(1000);
			
			h.getBytes(buf);
			buf.flip();
			consumeCtx.consume(new ByteBuffer[] {buf}, buf.remaining());
			i.remove();
		}
		consumeCtx.produce();
		if (!consumeCtx.produced.isEmpty()) {
			run(client, type);
		}
	}
	
	IHandshake get(boolean client) {
		Context ctx = ctxs[other(idx(!client))];
		
		if (ctx.produced.isEmpty()) {
			return null;
		}
		return ctx.produced.get(0);
	}

	void remove(boolean client) {
		Context ctx = ctxs[other(idx(!client))];

		ctx.produced.remove(0);
	}
	
	void clear(boolean client) {
		Context ctx = ctxs[other(idx(!client))];

		ctx.produced.clear();
	}
	
	void add(boolean client, IHandshake h) {
		Context ctx = ctxs[other(idx(!client))];

		ctx.produced.add(h);
	}
	
	void set(boolean client, IHandshake h) {
		Context ctx = ctxs[other(idx(!client))];
		
		if (!ctx.produced.isEmpty()) {
			ctx.produced.set(0, h);
		}
	}
	
	void start(boolean client) throws Exception {
		Context ctx = ctxs[other(idx(!client))];

		ctx.start();
	}
	
	static class Context {
		
		final HandshakeEngine engine;
		
		final List<IHandshake> produced = new ArrayList<IHandshake>();
		
		boolean started;
		
		Context(HandshakeEngine engine) {
			this.engine = engine;
		}
		
		void start() throws Exception {
			if (!started) {
				engine.start();
				started = true;
			}
		}
		
		void produce() throws Exception {
			start();
			for (ProducedHandshake produced: engine.produce()) {
				this.produced.add(produced.getHandshake());
			}
		}
		
		void consume(ByteBuffer[] srcs, int remaining) throws Exception {
			if (!started) {
				engine.start();
				started = true;
			}
			engine.consume(srcs, remaining);
		}
	}
}
