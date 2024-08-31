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
package org.snf4j.quic.engine;

/**
 * An anti-amplificator allowing to limit the amount of data an endpoint sends
 * to an unvalidated address.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IAntiAmplificator extends IDataBlockable {

	/**
	 * Tells if this anti-amplificator is armed. When armed the protection against
	 * exceeding the maximum aplification limit is enabled.
	 * 
	 * @return {@code true} if this anti-amplificator is armed
	 */
	boolean isArmed();
	
	/**
	 * Disarms this anti-amplificator.
	 */
	void disarm();
	
	/**
	 * Increases the received data by the given amount.
	 * 
	 * @param amount the amount of data
	 */
	void incReceived(int amount);

	/**
	 * Increases the sent data by the given amount.
	 * 
	 * @param amount the amount of data
	 */
	void incSent(int amount);
	
	/**
	 * Returns the maximum number of bytes that can be sent without exceeding the
	 * maximum amplification limit.
	 * 
	 * @return the maximum number of bytes that can be sent without exceeding the
	 *         maximum amplification limit
	 */
	int available();
	
}
