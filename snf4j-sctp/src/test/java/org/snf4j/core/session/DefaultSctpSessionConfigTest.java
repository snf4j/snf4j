/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DefaultSctpSessionConfigTest {
	
	@Test
	public void testAll() throws Exception {
		DefaultSctpSessionConfig config = new DefaultSctpSessionConfig();
		
		assertEquals(0, config.getMinSctpStreamNumber());
		assertEquals(65536, config.getMaxSctpStreamNumber());
		assertEquals(Integer.MIN_VALUE, config.getMinSctpPayloadProtocolID());
		assertEquals(Integer.MAX_VALUE, config.getMaxSctpPayloadProtocolID());
		assertEquals(0, config.getDefaultSctpStreamNumber());
		assertEquals(0, config.getDefaultSctpPayloadProtocolID());
		assertFalse(config.getDefaultSctpUnorderedFlag());
		assertNull(config.createCodecExecutor(null));
		
		config.setMinSctpStreamNumber(40)
			.setMaxSctpStreamNumber(100)
			.setMinSctpPayloadProtocolID(77)
			.setMaxSctpPayloadProtocolID(777)
			.setDefaultSctpStreamNumber(103)
			.setDefaultSctpPayloadProtocolID(44)
			.setDefaultSctpUnorderedFlag(true);
		
		assertEquals(40, config.getMinSctpStreamNumber());
		assertEquals(100, config.getMaxSctpStreamNumber());
		assertEquals(77, config.getMinSctpPayloadProtocolID());
		assertEquals(777, config.getMaxSctpPayloadProtocolID());
		assertEquals(103, config.getDefaultSctpStreamNumber());
		assertEquals(44, config.getDefaultSctpPayloadProtocolID());
		assertTrue(config.getDefaultSctpUnorderedFlag());
		
	}
}
