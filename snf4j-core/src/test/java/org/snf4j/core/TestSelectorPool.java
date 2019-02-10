/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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

import java.nio.channels.SelectableChannel;

import org.snf4j.core.pool.ISelectorLoopPool;


public class TestSelectorPool implements ISelectorLoopPool {

	volatile boolean getException;
	
	volatile SelectorLoop loop;
	
	volatile StringBuilder sb = new StringBuilder();

	public String getUpdate() {
		String s = sb.toString();
		sb.setLength(0);
		return s;
	}
	
	@Override
	public SelectorLoop getLoop(SelectableChannel channel) {
		if (getException) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			throw new IllegalArgumentException("selector loop exception");
		}
		return loop;
	}

	@Override
	public void stop() {
	}

	@Override
	public void quickStop() {
	}

	@Override
	public void update(SelectorLoop loop, int newSize, int prevSize) {
		sb.append(loop.toString());
		sb.append('|');
		sb.append(newSize);
		sb.append('|');
		sb.append(prevSize);
		sb.append('|');
	}

	@Override
	public boolean join(long millis) throws InterruptedException {
		return true;
	}

	@Override
	public void join() throws InterruptedException {
	}

}
