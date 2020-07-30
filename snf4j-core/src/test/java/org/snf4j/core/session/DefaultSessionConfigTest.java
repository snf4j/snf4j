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
package org.snf4j.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.net.ssl.SSLEngine;

import org.junit.Test;
import org.snf4j.core.EndingAction;

public class DefaultSessionConfigTest {

	@Test
	public void testAll() throws Exception {
		DefaultSessionConfig c = new DefaultSessionConfig();

		assertEquals(2048, c.getMinInBufferCapacity());
		assertEquals(8192, c.getMaxInBufferCapacity());
		assertEquals(2048, c.getMinOutBufferCapacity());
		assertEquals(3000, c.getThroughputCalculationInterval());
		assertEquals(true, c.ignorePossiblyIncompleteDatagrams());
		assertEquals(EndingAction.DEFAULT, c.getEndingAction());
		assertEquals(false, c.canOwnDataPassedToWriteAndSendMethods());
		assertEquals(1, c.getMaxSSLApplicationBufferSizeRatio());
		assertEquals(1, c.getMaxSSLNetworkBufferSizeRatio());
		assertFalse(c.waitForInboundCloseMessage());
		assertNull(c.createCodecExecutor());
		assertEquals(60000, c.getEngineHandshakeTimeout());
		assertEquals(60000, c.getDatagramServerSessionNoReopenPeriod());

		c.setMinInBufferCapacity(10).setMaxInBufferCapacity(100).setMinOutBufferCapacity(1000)
			.setThroughputCalculationInterval(5000).setIgnorePossiblyIncompleteDatagrams(false)
			.setEndingAction(EndingAction.STOP).setCanOwnDataPassedToWriteAndSendMethods(true)
			.setMaxSSLApplicationBufferSizeRatio(5).setMaxSSLNetworkBufferSizeRatio(6)
			.setWaitForInboundCloseMessage(true).setEngineHandshakeTimeout(1001)
			.setDatagramServerSessionNoReopenPeriod(1002)
			.getClass();

		assertEquals(10, c.getMinInBufferCapacity());
		assertEquals(100, c.getMaxInBufferCapacity());
		assertEquals(1000, c.getMinOutBufferCapacity());
		assertEquals(5000, c.getThroughputCalculationInterval());
		assertEquals(false, c.ignorePossiblyIncompleteDatagrams());
		assertEquals(EndingAction.STOP, c.getEndingAction());
		assertEquals(true, c.canOwnDataPassedToWriteAndSendMethods());
		assertEquals(5, c.getMaxSSLApplicationBufferSizeRatio());
		assertEquals(6, c.getMaxSSLNetworkBufferSizeRatio());
		assertTrue(c.waitForInboundCloseMessage());
		assertEquals(1001, c.getEngineHandshakeTimeout());
		assertEquals(1002, c.getDatagramServerSessionNoReopenPeriod());
		
		SSLEngine engine = c.createSSLEngine(true);
		assertNotNull(engine);
		assertTrue(engine.getUseClientMode());
		engine = c.createSSLEngine(false);
		assertNotNull(engine);
		assertFalse(engine.getUseClientMode());
		
	}
}
