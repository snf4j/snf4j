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

import java.util.concurrent.atomic.AtomicBoolean;

import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.InternalErrorAlertException;

abstract public class AbstractEngineTask implements IEngineTask {

	private final AtomicBoolean started = new AtomicBoolean();
	
	private volatile Throwable cause;
	
	private volatile boolean done;
	
	@Override
	public void run() {
		if (!started.compareAndSet(false, true)) {
			throw new IllegalStateException("Task has already started");
		}
		try {
			execute();
		} catch (Exception e) {
			cause = e;
		}
		done = true;
	}

	public void run(EngineState state) throws AlertException {
		try {
			execute();
		} catch (Exception e) {
			throw new InternalErrorAlertException(name() + " task failed", e);
		}
		finish(state);
	}
	
	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean isSuccessful() {
		return cause == null;
	}
	
	@Override
	public Throwable cause() {
		return cause;
	}
	
	abstract void execute() throws Exception;
}
