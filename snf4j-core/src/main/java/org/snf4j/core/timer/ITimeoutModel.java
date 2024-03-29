/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020-2024 SNF4J contributors
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
 * Interface used to implement models of timing out operations.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 * 
 */
public interface ITimeoutModel {
	
	/**
	 * Returns the next timeout value.
	 * 
	 * @return the timeout value in milliseconds.
	 */
	long next();
	
	/**
	 * Resets the model. After calling this method the {@link #next()} method 
	 * should return the initial value.
	 */
	void reset();
	
	/**
	 * Tells if this timeout model is enabled or not. When the model is disabled
	 * all associated operation should never be timed out.
	 * <p>
	 * The default value is {@code true}
	 * 
	 * @return {@code true} to enable this timeout model
	 * @since 1.12
	 */
	default boolean isEnabled() { return true; }
}
