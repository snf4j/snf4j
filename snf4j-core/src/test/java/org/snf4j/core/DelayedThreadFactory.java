/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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

import java.util.concurrent.ThreadFactory;

/**
 * Factory creating a thread with delayed execution of the target's <code>run</code> method.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DelayedThreadFactory implements ThreadFactory {

	private long delay;
	
	/**
	 * Constructs a thread with given delay.
	 * 
	 * @param delay the delay in milliseconds
	 */
	public DelayedThreadFactory(long delay) {
		this.delay = delay;
	}
	
	@Override
	public Thread newThread(Runnable target) {
		return new Thread(new DelayedRunner(target, delay), target.toString());
	}
	
	static class DelayedRunner implements Runnable {

		private Runnable r;
		private long delay;
		
		DelayedRunner(Runnable r, long delay) {
			this.r = r;
			this.delay = delay;
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				//Ignore
			}
			r.run();
		}
		
	}

}
