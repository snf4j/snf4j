/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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
package org.snf4j.core.future;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.snf4j.core.TestSession;

public class AbstractFutureTest {
	
	@Test
	public void testToString() {
		TestSession session = new TestSession("Session-1");
		SessionFuturesController futures = new SessionFuturesController(session);
		Exception cause = new Exception("Ex1");
		
		assertEquals("CancelledFuture[canceled]", new SessionFuturesController(null).getCancelledFuture().toString());
		assertEquals("Session-1-CancelledFuture[canceled]", futures.getCancelledFuture().toString());
		assertEquals("Session-1-SuccessfulFuture[successful]", futures.getSuccessfulFuture().toString());
		assertEquals("Session-1-FailedFuture[failed:"+cause+"]", futures.getFailedFuture(cause).toString());
		assertEquals("Session-1-EventFuture[incomplete,event=CREATED]", futures.getCreateFuture().toString());
		assertEquals("Session-1-ThresholdFuture[incomplete,threshold=100]", futures.getWriteFuture(100).toString());

		ITwoThresholdFuture<Void> f = futures.getEngineWriteFuture(100);
		assertEquals("Session-1-TwoThresholdFuture[incomplete,firstThreshold=100,secondThreshold=-1]", f.toString());
		f.setSecondThreshold(99);
		assertEquals("Session-1-TwoThresholdFuture[incomplete,firstThreshold=100,secondThreshold=99]", f.toString());	
		
	}
}
