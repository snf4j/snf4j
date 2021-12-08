/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2021 SNF4J contributors
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
import static org.junit.Assert.fail;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.junit.Test;
import org.snf4j.core.EndingAction;
import org.snf4j.core.session.ssl.SSLEngineBuilder;

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
		assertEquals(false, c.optimizeDataCopying());
		assertEquals(100, c.getMaxSSLApplicationBufferSizeRatio());
		assertEquals(100, c.getMaxSSLNetworkBufferSizeRatio());
		assertFalse(c.waitForInboundCloseMessage());
		assertNull(c.createCodecExecutor());
		assertEquals(60000, c.getEngineHandshakeTimeout());
		assertEquals(60000, c.getDatagramServerSessionNoReopenPeriod());
		assertEquals(16, c.getMaxWriteSpinCount());
		assertFalse(c.alwaysNotifiedBeingInPipeline());

		c.setMinInBufferCapacity(10).setMaxInBufferCapacity(100).setMinOutBufferCapacity(1000)
			.setThroughputCalculationInterval(5000).setIgnorePossiblyIncompleteDatagrams(false)
			.setEndingAction(EndingAction.STOP).setOptimizeDataCopying(true)
			.setMaxSSLApplicationBufferSizeRatio(500).setMaxSSLNetworkBufferSizeRatio(600)
			.setWaitForInboundCloseMessage(true).setEngineHandshakeTimeout(1001)
			.setDatagramServerSessionNoReopenPeriod(1002)
			.setMaxWriteSpinCount(8)
			.setAlwaysNotifiedBeingInPipeline(true)
			.getClass();

		assertEquals(10, c.getMinInBufferCapacity());
		assertEquals(100, c.getMaxInBufferCapacity());
		assertEquals(1000, c.getMinOutBufferCapacity());
		assertEquals(5000, c.getThroughputCalculationInterval());
		assertEquals(false, c.ignorePossiblyIncompleteDatagrams());
		assertEquals(EndingAction.STOP, c.getEndingAction());
		assertEquals(true, c.optimizeDataCopying());
		assertEquals(500, c.getMaxSSLApplicationBufferSizeRatio());
		assertEquals(600, c.getMaxSSLNetworkBufferSizeRatio());
		assertTrue(c.waitForInboundCloseMessage());
		assertEquals(1001, c.getEngineHandshakeTimeout());
		assertEquals(1002, c.getDatagramServerSessionNoReopenPeriod());
		assertEquals(8, c.getMaxWriteSpinCount());
		assertTrue(c.alwaysNotifiedBeingInPipeline());
		
		SSLEngine engine = c.createSSLEngine(true);
		assertNotNull(engine);
		assertTrue(engine.getUseClientMode());
		engine = c.createSSLEngine(false);
		assertNotNull(engine);
		assertFalse(engine.getUseClientMode());
		
		c.setMaxSSLApplicationBufferSizeRatio(101);
		assertEquals(101, c.getMaxSSLApplicationBufferSizeRatio());
		c.setMaxSSLApplicationBufferSizeRatio(100);
		assertEquals(100, c.getMaxSSLApplicationBufferSizeRatio());
		try {
			c.setMaxSSLApplicationBufferSizeRatio(99);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		c.setMaxSSLNetworkBufferSizeRatio(101);
		assertEquals(101, c.getMaxSSLNetworkBufferSizeRatio());
		c.setMaxSSLNetworkBufferSizeRatio(100);
		assertEquals(100, c.getMaxSSLNetworkBufferSizeRatio());
		try {
			c.setMaxSSLNetworkBufferSizeRatio(99);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		
	}
	
	@Test
	public void testAddSSLEngineBuilder() throws Exception {
		DefaultSessionConfig c = new DefaultSessionConfig();
		SSLContext context = SSLContext.getDefault();
		SSLEngineBuilder cb1 = SSLEngineBuilder.forClient(context);
		SSLEngineBuilder cb2 = SSLEngineBuilder.forClient(context);
		SSLEngineBuilder sb1 = SSLEngineBuilder.forServer(context);
		SSLEngineBuilder sb2 = SSLEngineBuilder.forServer(context);
		
		assertNull(c.getSSLEngineBuilder(true));
		assertNull(c.getSSLEngineBuilder(false));
		assertTrue (c == c.addSSLEngineBuilder(cb1));
		assertTrue(cb1 == c.getSSLEngineBuilder(true));
		assertNull(c.getSSLEngineBuilder(false));
		c.addSSLEngineBuilder(sb1);
		assertTrue(cb1 == c.getSSLEngineBuilder(true));
		assertTrue(sb1 == c.getSSLEngineBuilder(false));
		c.addSSLEngineBuilder(cb2);
		assertTrue(cb2 == c.getSSLEngineBuilder(true));
		assertTrue(sb1 == c.getSSLEngineBuilder(false));
		c.addSSLEngineBuilder(sb2);
		assertTrue(cb2 == c.getSSLEngineBuilder(true));
		assertTrue(sb2 == c.getSSLEngineBuilder(false));
		assertTrue (c == c.removeSSLEngineBuilder(true));
		assertNull(c.getSSLEngineBuilder(true));
		assertTrue(sb2 == c.getSSLEngineBuilder(false));
		c.removeSSLEngineBuilder(false);
		assertNull(c.getSSLEngineBuilder(true));
		assertNull(c.getSSLEngineBuilder(false));
	}
	
	@Test
	public void testCreateSSLEngine() throws Exception {
		DefaultSessionConfig c = new DefaultSessionConfig();
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, null, null);
		SSLEngineBuilder cb = SSLEngineBuilder.forClient(context).protocols("TLSv1.2");
		SSLEngineBuilder sb = SSLEngineBuilder.forServer(context).protocols("TLSv1.2");
		SSLEngine e;
		
		e = c.createSSLEngine(true);
		assertTrue(e.getUseClientMode());
		assertTrue(e.getEnabledProtocols().length > 1);
		e = c.createSSLEngine(false);
		assertFalse(e.getUseClientMode());
		assertTrue(e.getEnabledProtocols().length > 1);
		
		c.addSSLEngineBuilder(cb);
		e = c.createSSLEngine(true);
		assertTrue(e.getUseClientMode());
		assertTrue(e.getEnabledProtocols().length == 1);
		e = c.createSSLEngine(false);
		assertFalse(e.getUseClientMode());
		assertTrue(e.getEnabledProtocols().length > 1);
		c.addSSLEngineBuilder(sb);
		e = c.createSSLEngine(true);
		assertTrue(e.getUseClientMode());
		assertTrue(e.getEnabledProtocols().length == 1);
		e = c.createSSLEngine(false);
		assertFalse(e.getUseClientMode());
		assertTrue(e.getEnabledProtocols().length == 1);	
	}
}
