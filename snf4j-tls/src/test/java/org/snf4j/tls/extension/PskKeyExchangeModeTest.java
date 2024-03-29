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
package org.snf4j.tls.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PskKeyExchangeModeTest {

	@Test
	public void testAll() {
		assertEquals(0, PskKeyExchangeMode.PSK_KE.value());
		assertEquals(1, PskKeyExchangeMode.PSK_DHE_KE.value());
		assertTrue(PskKeyExchangeMode.PSK_KE.isKnown());
		assertTrue(PskKeyExchangeMode.PSK_DHE_KE.isKnown());
		
		assertSame(PskKeyExchangeMode.PSK_KE, PskKeyExchangeMode.of(0));
		assertSame(PskKeyExchangeMode.PSK_DHE_KE, PskKeyExchangeMode.of(1));
		assertFalse(PskKeyExchangeMode.of(2).isKnown());
		assertFalse(PskKeyExchangeMode.of(255).isKnown());
	}

	@Test
	public void testImplemented() {
		PskKeyExchangeMode[] modes = new PskKeyExchangeMode[0];
		assertSame(modes, PskKeyExchangeMode.implemented(modes));
		modes = new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_KE};
		assertEquals(0, PskKeyExchangeMode.implemented(modes).length);
		modes = new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_DHE_KE};
		assertSame(modes, PskKeyExchangeMode.implemented(modes));
		modes = new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_DHE_KE,PskKeyExchangeMode.PSK_KE};
		modes = PskKeyExchangeMode.implemented(modes);
		assertEquals(1, modes.length);
		assertSame(PskKeyExchangeMode.PSK_DHE_KE, modes[0]);
	}
}
