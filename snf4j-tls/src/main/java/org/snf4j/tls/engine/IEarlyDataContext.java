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

public interface IEarlyDataContext {
	
	/**
	 * Gets the status of an early data processing.
	 * 
	 * @return the status
	 */
	EarlyDataState getState();
	
	/**
	 * Marks the processing of an early data as completed successfully.
	 */
	void complete();

	/**
	 * Marks the processing of an early data as rejected.
	 */
	void reject();
		
	/**
	 * Increments the number of already processed early-data bytes.
	 * 
	 * @param amount the amount of bytes to increment
	 */
	void incProcessedBytes(int amount);
	
	/**
	 * Tells if the maximum size limit of processed bytes has been exceeded.
	 * 
	 * @return {@code if the maximum size limit has been exceeded
	 */
	boolean isSizeLimitExceeded();
}
