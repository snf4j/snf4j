/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2024 SNF4J contributors
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
package org.snf4j.core;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.engine.IEngineTimerTask;
import org.snf4j.core.engine.Status;
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionTimer;

public class TestEngine implements IEngine {

	int appBufferSize = 1024;
	int netBufferSize = 1024;
	int maxAppBufferSize = 1024;
	int maxNetBufferSize = 1024;
	
	EngineResult unwrapResult;
	EngineResult wrapResult;
	
	boolean beginHandshakeException;
	
	final List<Record> records = Collections.synchronizedList(new ArrayList<Record>());

	final List<Runnable> tasks = Collections.synchronizedList(new ArrayList<Runnable>());

	final List<Runnable> timerTasks = Collections.synchronizedList(new ArrayList<Runnable>());
	
	long timeTaskDelays = -1;
	
	Exception timerException;
	
	volatile Runnable pendingTimerTask;
	
	volatile Runnable awakeningTask;
	
	HandshakeStatus status = HandshakeStatus.NOT_HANDSHAKING;
	
	boolean outboundDone;

	boolean inboundDone;
	
	TraceBuilder trace;
	
	boolean traceAllWrapMethods;
	
	ISession session;
	
	int needWrap;
	
	int needUnwrap;
	
	public TestEngine(HandshakeStatus status) {
		this.status = status;
	}

	public TestEngine() {
	}
	
	public void setTrace(TraceBuilder trace) {
		this.trace = trace;
	}
	
	public void addTask(Runnable task) {
		tasks.add(task);
	}
	
	public void addTask() {
		addTask(new Runnable() {

			@Override
			public void run() {
				trace("TASK");
			}
			
		});
	}

	public void addTimerTask(Runnable task) {
		timerTasks.add(task);
	}
	
	public void addTimerTask() {
		addTimerTask(new TimerTask());
	}

	public void pendingTimerTask() {
		pendingTimerTask = new TimerTask();
	}
	
	void trace(String s) {
		if (trace != null) {
			trace.append(s);
		}
	}
	
	public void addRecord(String record) {
		synchronized (records) {
			records.add(new Record(record));
		}
	}
	
	public Record firstRecord(boolean remove) {
		Record r = null;
		
		synchronized (records) {
			if (!records.isEmpty()) {
				r = remove ? records.remove(0) : records.get(0);
			}
		}
		return r;
	}
	
	@Override
	public void link(ISession session) {
		this.session = session;
	}
	
	@Override
	public boolean needWrap() { 
		if (needWrap > 0) {
			--needWrap;
			return true;
		}
		return false; 
	}
	
	@Override
	public boolean needUnwrap() { 
		if (needUnwrap > 0) {
			--needUnwrap;
			return true;
		}
		return false; 
	}
	
	@Override
	public void init() {
		trace("INI");
	}
	
	@Override
	public void cleanup() {
		trace("FIN");
	}
	
	@Override
	public void beginHandshake() throws Exception {
		if (beginHandshakeException) {
			throw new Exception();
		}
		trace("HAND");
	}
	
	@Override
	public Object getSession() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isOutboundDone() {
		return outboundDone;
	}

	@Override
	public boolean isInboundDone() {
		return inboundDone;
	}
	
	@Override
	public void closeOutbound() {
		trace("CO");
	}

	@Override
	public void closeInbound() throws SessionIncidentException {
		trace("CI");
	}

	@Override
	public int getMinApplicationBufferSize() {
		return appBufferSize;
	}

	@Override
	public int getMinNetworkBufferSize() {
		return netBufferSize;
	}

	@Override
	public int getMaxApplicationBufferSize() {
		return maxAppBufferSize;
	}

	@Override
	public int getMaxNetworkBufferSize() {
		return maxNetBufferSize;
	}
	
	@Override
	public HandshakeStatus getHandshakeStatus() {
		if (!tasks.isEmpty()) {
			return HandshakeStatus.NEED_TASK;
		}
		if (!timerTasks.isEmpty()) {
			return HandshakeStatus.NEED_TIMER;
		}
		return status;
	}

	@Override
	public Runnable getDelegatedTask() {
		trace("GET_TASK");
		if (tasks.isEmpty()) {
			return null;
		}
		
		Runnable task = tasks.get(0);
		tasks.remove(0);
		return task;
	}

	int remaining(ByteBuffer[] srcs) {
		int i = 0;
		
		for (ByteBuffer src: srcs) {
			i += src.remaining();
		}
		return i;
	}
	
	@Override
	public void timer(ISessionTimer timer, Runnable awakeningTask) throws Exception {
		if (timerException != null) {
			throw timerException;
		}
		this.awakeningTask = awakeningTask;
		for (Iterator<Runnable> i = timerTasks.iterator(); i.hasNext();) {
			Runnable task = i.next();
			
			i.remove();
			if (timeTaskDelays >= 0) {
				timer.scheduleTask(task, timeTaskDelays, true);
			}
		}
	}
	
	int consume(ByteBuffer[] srcs, byte[] expected) {
		byte[] consumed = new byte[expected.length];

		if (remaining(srcs) < expected.length) {
			return -1;
		}
		
		int i = 0;
		for (ByteBuffer src: srcs) {
			while (i < expected.length && src.hasRemaining()) {
				consumed[i] = src.get();
				if (consumed[i] != expected[i]) {
					throw new IllegalArgumentException();
				}
				++i;
			}
		}
		return i;
	}
	
	@Override
	public IEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws Exception {
		trace("W" + remaining(srcs));
		if (pendingTimerTask != null) {
			addTimerTask(pendingTimerTask);
			pendingTimerTask = null;
			return new EngineResult(Status.OK, getHandshakeStatus(), 0, 0);
		}
		if (isOutboundDone()) {
			return new EngineResult(Status.CLOSED, getHandshakeStatus(), 0, 0);
		}
		Record record = firstRecord(false);
		if (record == null) {
			return new EngineResult(Status.OK, getHandshakeStatus(), 0, 0);
		}
		
		if (!record.wrap) {
			return new EngineResult(Status.OK, getHandshakeStatus(), 0, 0);
		}
		
		int consumed = 0;
		int produced = 0;
		
		if (record.src != null) {
			if (consume(srcs, record.src) == -1) {
				throw new IllegalArgumentException();
			}
			consumed += record.src.length;
		}
		
		if (record.dst != null) {
			dst.put(record.dst);
			produced += record.dst.length;
		}

		status = record.status;
		firstRecord(true);

		if (record.result == Status.CLOSED) {
			outboundDone = true;
		}
		
		return new EngineResult(record.result, record.resultStatus, consumed, produced);
	}

	@Override
	public IEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws Exception {
		if (wrapResult == null) {
			if (traceAllWrapMethods) {
				trace("w");
			}
			return wrap(new ByteBuffer[] {src}, dst);
		}
		return wrapResult;
	}

	@Override
	public IEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws Exception {
		if (unwrapResult == null) {
			trace("U" + src.remaining());
			if (pendingTimerTask != null) {
				addTimerTask(pendingTimerTask);
				pendingTimerTask = null;
				return new EngineResult(Status.OK, getHandshakeStatus(), 0, 0);
			}
			if (isInboundDone()) {
				return new EngineResult(Status.CLOSED, getHandshakeStatus(), 0, 0);
			}
			Record record = firstRecord(false);
			if (record == null) {
				return new EngineResult(Status.OK, getHandshakeStatus(), 0, 0);
			}
			
			if (record.wrap) {
				return new EngineResult(Status.OK, getHandshakeStatus(), 0, 0);
			}
			
			int consumed = 0;
			int produced = 0;
			
			if (record.src != null) {
				if (consume(new ByteBuffer[] {src}, record.src) == -1) {
					return new EngineResult(Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
				}
				consumed += record.src.length;
			}
			
			if (record.dst != null) {
				dst.put(record.dst);
				produced += record.dst.length;
			}
			
			status = record.status;
			firstRecord(true);

			if (record.result == Status.CLOSED) {
				inboundDone = true;
			}
			
			return new EngineResult(record.result, record.resultStatus, consumed, produced);
			
		}
		return unwrapResult;
	}

	static Map<String, Object> map = new HashMap<String, Object>();
	
	static {
		map.put("F", HandshakeStatus.FINISHED);
		map.put("NH", HandshakeStatus.NOT_HANDSHAKING);
		map.put("NW", HandshakeStatus.NEED_WRAP);
		map.put("NU", HandshakeStatus.NEED_UNWRAP);
		map.put("NA", HandshakeStatus.NEED_UNWRAP_AGAIN);
		map.put("NT", HandshakeStatus.NEED_TASK);
		map.put("Nt", HandshakeStatus.NEED_TIMER);
		map.put("OK", Status.OK);
		map.put("BO", Status.BUFFER_OVERFLOW);
		map.put("BU", Status.BUFFER_UNDERFLOW);
		map.put("C", Status.CLOSED);
		map.put("W", Boolean.TRUE);
		map.put("U", Boolean.FALSE);
		map.put("-", null);
	}
	
	static class Record {
		boolean wrap;
		HandshakeStatus status;
		byte[] src;
		byte[] dst;
		HandshakeStatus resultStatus;
		Status result;
		
		Record(String data) {
			String[] items = data.split("\\|");
			
			if (items.length != 6) {
				throw new IllegalArgumentException();
			}
			
			wrap = (Boolean) map.get(items[0]);
			status = (HandshakeStatus) map.get(items[1]);
			src = items[2].equals("-") ? null : items[2].getBytes();
			dst = items[3].equals("-") ? null : items[3].getBytes();
			result = (Status) map.get(items[4]);
			resultStatus = (HandshakeStatus) map.get(items[5]);
			
			if (resultStatus == null) {
				resultStatus = status;
			}
		}
	}
	
	class TimerTask implements Runnable, IEngineTimerTask {

		@Override
		public void run() {
			trace("TIMER_TASK");
		}
	}
}
