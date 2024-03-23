/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.engine.crypto;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.core.TraceBuilder;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.engine.IEngineHandler;
import org.snf4j.tls.engine.IEngineState;
import org.snf4j.tls.engine.IHandshakeEngine;
import org.snf4j.tls.engine.ProducedHandshake;
import org.snf4j.tls.handshake.HandshakeDecoder;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.IHandshakeDecoder;

public class TestHandshakeEngine implements IHandshakeEngine {

	final List<ProducedHandshake> produced = new ArrayList<>();
	
	final List<IHandshake> consumed = new ArrayList<>();
	
	final Queue<Runnable> tasks = new LinkedList<>();
	
	final TraceBuilder trace = new TraceBuilder();
	
	IHandshakeDecoder decoder = HandshakeDecoder.DEFAULT;
	
	public Alert produceAlert;

	public Alert consumeAlert;
	
	public Alert updateTasksAlert;
	
	public Alert startAlert;
	
	public void addProduced(ProducedHandshake handshake) {
		produced.add(handshake);
	}
	
	public void addTask(Runnable task) {
		tasks.add(task);
	}
	
	public IHandshake[] getConsumed(boolean clear) {
		IHandshake[] c = consumed.toArray(new IHandshake[consumed.size()]);
		if (clear) {
			consumed.clear();
		}
		return c;
	}
	
	@Override
	public IEngineHandler getHandler() {
		return null;
	}

	@Override
	public void consume(ByteBuffer[] srcs, int remaining) throws Alert {
		consume(ByteBufferArray.wrap(srcs), remaining);
	}

	@Override
	public void consume(ByteBufferArray srcs, int remaining) throws Alert {
		if (consumeAlert != null) {
			throw consumeAlert;
		}
		consumed.add(decoder.decode(srcs, remaining));
	}

	@Override
	public boolean needProduce() {
		return !produced.isEmpty();
	}

	@Override
	public ProducedHandshake[] produce() throws Alert {
		if (produceAlert != null) {
			throw produceAlert;
		}
		ProducedHandshake[] p = produced.toArray(new ProducedHandshake[produced.size()]);
		produced.clear();
		return p;
	}

	@Override
	public boolean updateTasks() throws Alert {
		if (updateTasksAlert != null) {
			throw updateTasksAlert;
		}
		return !tasks.isEmpty();
	}

	@Override
	public boolean hasProducingTask() {
		return false;
	}

	@Override
	public boolean hasRunningTask(boolean onlyUndone) {
		return false;
	}

	@Override
	public boolean hasTask() {
		return !tasks.isEmpty();
	}

	@Override
	public Runnable getTask() {
		return tasks.poll();
	}

	@Override
	public void start() throws Alert {
		if (startAlert != null) {
			throw startAlert;
		}
	}

	@Override
	public IEngineState getState() {
		return null;
	}

	@Override
	public void updateKeys() throws Alert {
	}

	@Override
	public void cleanup() {
		trace.append("CUP");
	}

}
