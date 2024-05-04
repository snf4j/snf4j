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
package org.snf4j.quic.packet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.Version;

public class HeaderInfoTest extends CommonTest {
	
	@Test
	public void testUnprotect() {
		HeaderInfo i = new HeaderInfo(0xff, Version.V1, bytes("00"), bytes("01"), null, 100);
		
		assertFalse(i.isUnprotected());
		assertEquals(0xff, i.getBits());
		assertEquals(100, i.getLength());
		i.unprotect(0x03, 16);
		assertTrue(i.isUnprotected());
		assertEquals(0xfc, i.getBits());
		assertEquals(84, i.getLength());
		i.unprotect(0xff, 16);
		assertEquals(0xfc, i.getBits());
		assertEquals(84, i.getLength());
	}
	
	@Test
	public void testConstructors() {
		HeaderInfo i = new HeaderInfo(0xaf, Version.V1, bytes("00"), bytes("01"), bytes("03"), 100);
		assertEquals(0xaf, i.getBits());
		assertSame(Version.V1, i.getVersion());
		assertArrayEquals(bytes("00"), i.getDestinationId());
		assertArrayEquals(bytes("01"), i.getSourceId());
		assertArrayEquals(bytes("03"), i.getToken());
		assertEquals(100, i.getLength());

		i = new HeaderInfo(0xaf, Version.V1, bytes("00"), bytes("01"), 101);
		assertEquals(0xaf, i.getBits());
		assertSame(Version.V1, i.getVersion());
		assertArrayEquals(bytes("00"), i.getDestinationId());
		assertArrayEquals(bytes("01"), i.getSourceId());
		assertNull(i.getToken());
		assertEquals(101, i.getLength());

		i = new HeaderInfo(0xaf, bytes("00"), 102);
		assertEquals(0xaf, i.getBits());
		assertNull(i.getVersion());
		assertArrayEquals(bytes("00"), i.getDestinationId());
		assertNull(i.getSourceId());
		assertNull(i.getToken());
		assertEquals(102, i.getLength());
	}

}
