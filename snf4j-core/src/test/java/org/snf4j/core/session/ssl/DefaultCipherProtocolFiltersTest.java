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
package org.snf4j.core.session.ssl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DefaultCipherProtocolFiltersTest {

	@Test
	public void testFilter() {
		String[] items = new String[] {"it1","it2"};
		String[] defaultItems = new String[] {"it3","it4"};
		String[] array;
		
		array = DefaultCipherProtocolFilters.INSATNCE.filterCiphers(items, defaultItems, null);
		assertArrayEquals(new String[] {"it1","it2"}, array);
		assertTrue(items == array);

		array = DefaultCipherProtocolFilters.INSATNCE.filterProtocols(items, defaultItems, null);
		assertArrayEquals(new String[] {"it1","it2"}, array);
		assertTrue(items == array);

		array = DefaultCipherProtocolFilters.INSATNCE.filterCiphers(null, defaultItems, null);
		assertArrayEquals(new String[] {"it3","it4"}, array);
		assertTrue(defaultItems == array);

		array = DefaultCipherProtocolFilters.INSATNCE.filterProtocols(null, defaultItems, null);
		assertArrayEquals(new String[] {"it3","it4"}, array);
		assertTrue(defaultItems == array);
	}
}
