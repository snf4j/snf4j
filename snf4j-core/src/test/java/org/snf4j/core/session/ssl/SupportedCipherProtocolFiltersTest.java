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

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class SupportedCipherProtocolFiltersTest {
	@Test
	public void testFilter() {
		String[] items = new String[] {"it1","it2"};
		String[] defaultItems = new String[] {"it3","it4"};
		Set<String> supportedItems = new HashSet<String>();
		String[] array;

		supportedItems.add("it2");
		supportedItems.add("it3");
		supportedItems.add("it4");
		
		array = SupportedCipherProtocolFilters.INSATNCE.filterCiphers(items, defaultItems, supportedItems);
		assertArrayEquals(new String[] {"it2"}, array);
		array = SupportedCipherProtocolFilters.INSATNCE.filterCiphers(null, defaultItems, supportedItems);
		assertArrayEquals(new String[] {"it3","it4"}, array);
		assertTrue(defaultItems == array);
		
		array = SupportedCipherProtocolFilters.INSATNCE.filterProtocols(items, defaultItems, supportedItems);
		assertArrayEquals(new String[] {"it2"}, array);
		array = SupportedCipherProtocolFilters.INSATNCE.filterProtocols(null, defaultItems, supportedItems);
		assertArrayEquals(new String[] {"it3","it4"}, array);
		assertTrue(defaultItems == array);
		
	
	}
}
