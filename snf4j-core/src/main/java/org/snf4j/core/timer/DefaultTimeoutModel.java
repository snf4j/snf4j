/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.core.timer;

/**
 * The default implementation of the {@link ITimeoutModel}. It uses a simple
 * model that starts (the first execution of the {@link #next()}) from the
 * initial value and double the value at each consecutive execution of the
 * {@link #next()} method. After reaching the max value the {@link #next()}
 * method keeps returning the max value until the {@link #reset()} is called.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 *
 */
public class DefaultTimeoutModel implements ITimeoutModel {

	private final int initial;
	
	private final long max;
	
	private long current;
	
	/**
	 * Constructs the model with given initial value and max value.
	 * 
	 * @param initial the initial value in milliseconds
	 * @param max     the max value in milliseconds
	 */
	public DefaultTimeoutModel(int initial, long max) {
		this.initial = initial;
		this.max = max;
		current = initial;
	}
	
	/**
	 * Constructs the model with default values: initial = 1000 and max = 60000 
	 */
	public DefaultTimeoutModel() {
		this(1000, 60000);
	}
	
	@Override
	public long next() {
		long n = current;
		
		if (n < max) {
			current <<= 1;
			if (current > max) {
				current = max;
			}
		}
		return n;
	}
	
	@Override
	public void reset() {
		current = initial;
	}


}
