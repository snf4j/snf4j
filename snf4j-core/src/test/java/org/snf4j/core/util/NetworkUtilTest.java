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
package org.snf4j.core.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class NetworkUtilTest {
	
	byte[] bytes(int... b) {
		byte[] bytes = new byte[b.length];
		for (int i=0; i<bytes.length; ++i) {
			bytes[i] = (byte)b[i];
		}
		return bytes;
	}
	
	void assertDigit(int i) {
		char c = Character.forDigit(i, 10);
		assertEquals(i, NetworkUtil.digit(c));
	}
	
	@Test
	public void testDigit() {
		for (int i=0; i<10; ++i) {
			assertDigit(i);
		}
		assertEquals(-1, NetworkUtil.digit('a'));
	}

	void assertHexDigit(int i, boolean upper) {
		char c = Character.forDigit(i, 16);
		assertEquals(i, NetworkUtil.hexDigit(upper ? Character.toUpperCase(c) : Character.toLowerCase(c)));
	}
	
	@Test
	public void testHexDigit() {
		for (int i=0; i<16; ++i) {
			assertHexDigit(i, false);
			assertHexDigit(i, true);
		}
		assertEquals(-1, NetworkUtil.digit('g'));
	}
	
	void assertIpv6ToBytes(byte[] expected, boolean expectedResult, String ip, int off) {
		byte[] bytes = new byte[16+off*2];
		Arrays.fill(bytes, (byte)6);
		assertEquals(expectedResult, NetworkUtil.ipv6ToBytes(ip, bytes, off));
		assertArrayEquals(expected, bytes);
	}
	
	@Test
	public void testIpv6ToBytes() {
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "[", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "[]", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "[[", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "]", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "]]", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "][", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "[:]", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "[:%8]", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1::2", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":::", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::::", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "[::2", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::2]", 1);

		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1:2", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1:2:3", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1:2:3:4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1:2:3:4:5", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1:2:3:4:5:6", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1:2:3:4:5:6:7", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8,6), true, "1:2:3:4:5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1:2:3:4:5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:8:", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:8:9", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:8:9:a", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1:2:3:4:5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5::6:7:8", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:8::", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1:2:3:4:5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:g:4:5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:G", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:10000:4:5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:10000", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:8:10000:9", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:ffffffff:4:5:6:7:8", 1);
		
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "::", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "1::", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,6), true, "::1", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,6), true, "1::2", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "::%eth0", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "1::%eth0", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,6), true, "::1%eth0", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,6), true, "1::2%eth0", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,6), true, "1::2%", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,6), true, "1::2%%", 1);

		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "[::]", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "[1::]", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,6), true, "[::1]", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,6), true, "[1::2]", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "[::%eth0]", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "[1::%eth0]", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,6), true, "[::1%eth0]", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,6), true, "[1::2%eth0]", 1);

		assertIpv6ToBytes(bytes(6,0,1,0,2,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "1:2::", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,0,0,0,0,0,0,0,0,0,6), true, "1:2:3::", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,0,0,0,0,0,0,0,6), true, "1:2:3:4::", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,5,0,0,0,0,0,0,6), true, "1:2:3:4:5::", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,5,0,6,0,0,0,0,6), true, "1:2:3:4:5:6::", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,0,6), true, "1:2:3:4:5:6:7::", 1);

		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,8,6), true, "::7:8", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,6,0,7,0,8,6), true, "::6:7:8", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,5,0,6,0,7,0,8,6), true, "::5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,4,0,5,0,6,0,7,0,8,6), true, "::4:5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,3,0,4,0,5,0,6,0,7,0,8,6), true, "::3:4:5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,2,0,3,0,4,0,5,0,6,0,7,0,8,6), true, "::2:3:4:5:6:7:8", 1);

		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,8,6), true, "1::8", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,0,0,0,0,0,0,0,0,0,0,8,6), true, "1:2::8", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,0,0,0,0,0,0,0,0,7,0,8,6), true, "1:2::7:8", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,0,0,0,0,0,0,7,0,8,6), true, "1:2:3::7:8", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,0,0,0,0,6,0,7,0,8,6), true, "1:2:3::6:7:8", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,0,0,6,0,7,0,8,6), true, "1:2:3:4::6:7:8", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8,6), true, "1:2:3:4:5:6:7:8", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6), true, "0:0:0:0:0:0:0:0", 1);
		assertIpv6ToBytes(bytes(6,0,10,0,11,0,12,0,13,0,14,0,15,0,7,0,8,6), true, "A:b:C:d:E:f:7:8", 1);
		assertIpv6ToBytes(bytes(6,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,6), true, "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", 1);
		assertIpv6ToBytes(bytes(6,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,6), true, "0102:0304:0506:0708:090a:0b0c:0d0e:0f10", 1);

		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ".4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ".3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ".2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "4:5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":4:5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "3:4:5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":3:4:5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "2:3:4:5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":2:3:4:5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,5,0,6,1,2,3,4,6), true, "1:2:3:4:5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, ":1:2:3:4:5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "0:1:2:3:4:5:6:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:1.2.3.4.5", 1);
		
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,1,2,3,4,6), true, "::1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,9,2,3,4,6), true, "::9.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,10,2,3,4,6), true, "::10.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,12,2,3,4,6), true, "::12.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,100,2,3,4,6), true, "::100.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,102,2,3,4,6), true, "::102.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,123,2,3,4,6), true, "::123.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,6), true, "::255.255.255.255", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,0,255,254,253,252,6), true, "::255.254.253.252", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,0,0,1,30,31,32,33,6), true, "::1:30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,0,0,2,0,1,30,31,32,33,6), true, "::2:1:30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,0,0,3,0,2,0,1,30,31,32,33,6), true, "::3:2:1:30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,0,0,4,0,3,0,2,0,1,30,31,32,33,6), true, "::4:3:2:1:30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,0,0,5,0,4,0,3,0,2,0,1,30,31,32,33,6), true, "::5:4:3:2:1:30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,0,30,31,32,33,6), true, "1::30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,0,0,0,0,0,0,0,30,31,32,33,6), true, "1:2::30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,0,0,0,0,0,30,31,32,33,6), true, "1:2:3::30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,0,0,0,30,31,32,33,6), true, "1:2:3:4::30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,2,0,3,0,4,0,5,0,0,30,31,32,33,6), true, "1:2:3:4:5::30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,0,0,0,0,0,0,0,0,2,30,31,32,33,6), true, "1::2:30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,3,0,0,0,0,0,0,0,2,30,31,32,33,6), true, "1:3::2:30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,3,0,0,0,0,0,4,0,2,30,31,32,33,6), true, "1:3::4:2:30.31.32.33", 1);
		assertIpv6ToBytes(bytes(6,0,1,0,3,0,5,0,0,0,4,0,2,30,31,32,33,6), true, "1:3:5::4:2:30.31.32.33", 1);
		
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::a.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1.b.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1.2.c.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1.2.3.D", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1.2.3.4:", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1.2.3.4:5", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::256.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1.256.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1.2.3.256", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1.2.3.", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1.2.", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1..3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1...4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "::1...", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:1.2.3.4", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:1.2.3", 1);
		assertIpv6ToBytes(bytes(6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6), false, "1:2:3:4:5:6:7:1.2", 1);

		assertArrayEquals(bytes(0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8), NetworkUtil.ipv6ToBytes("1:2:3:4:5:6:7:8"));
		assertNull(NetworkUtil.ipv6ToBytes("1:2:3:4:5:6:7:10000"));
		
	}
	
	@Test
	public void testIpv6ToString() {
		assertEquals("::", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0), 1, false));
		assertEquals("1::", NetworkUtil.ipv6ToString(bytes(0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0), 1, false));
		assertEquals("1:2::", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,0,0,0,0,0,0,0,0,0,0,0), 1, false));
		assertEquals("1:2:3::", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,0,0,0,0,0,0,0,0,0), 1, false));
		assertEquals("1:2:3:4::", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,0,0,0,0,0,0,0), 1, false));
		assertEquals("1:2:3:4:5::", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,5,0,0,0,0,0,0), 1, false));
		assertEquals("1:2:3:4:5:6::", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,5,0,6,0,0,0,0), 1, false));
		assertEquals("1:2:3:4:5:6:7:0", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,0), 1, false));
		assertEquals("1:2:3:4:5:6:7:8", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8), 1, false));
		assertEquals("::1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1), 1, false));
		assertEquals("::2:1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,1), 1, false));
		assertEquals("::3:2:1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,3,0,2,0,1), 1, false));
		assertEquals("::4:3:2:1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,4,0,3,0,2,0,1), 1, false));
		assertEquals("::5:4:3:2:1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,5,0,4,0,3,0,2,0,1), 1, false));
		assertEquals("::6:5:4:3:2:1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,6,0,5,0,4,0,3,0,2,0,1), 1, false));
		assertEquals("0:7:6:5:4:3:2:1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,7,0,6,0,5,0,4,0,3,0,2,0,1), 1, false));
		assertEquals("2::1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,1), 1, false));
		assertEquals("2::3:1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,0,0,0,0,0,0,0,0,0,0,3,0,1), 1, false));
		assertEquals("2:4::3:1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,4,0,0,0,0,0,0,0,0,0,3,0,1), 1, false));
		assertEquals("2:4::5:3:1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,4,0,0,0,0,0,0,0,5,0,3,0,1), 1, false));
		assertEquals("2:4:6::5:3:1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,4,0,6,0,0,0,0,0,5,0,3,0,1), 1, false));
		assertEquals("2:4:6:0:7:5:3:1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,4,0,6,0,0,0,7,0,5,0,3,0,1), 1, false));
		assertEquals("0:0:1::", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0), 1, false));
		assertEquals("::1:0:0:2:3:0", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,1,0,0,0,0,0,2,0,3,0,0), 1, false));

		assertEquals("::0.0.0.0", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0), 1, true));
		assertEquals("1::0.0.0.0", NetworkUtil.ipv6ToString(bytes(0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0), 1, true));
		assertEquals("1:2::0.0.0.0", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,0,0,0,0,0,0,0,0,0,0,0), 1, true));
		assertEquals("1:2:3::0.0.0.0", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,0,0,0,0,0,0,0,0,0), 1, true));
		assertEquals("1:2:3:4::0.0.0.0", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,0,0,0,0,0,0,0), 1, true));
		assertEquals("1:2:3:4:5:0:0.0.0.0", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,5,0,0,0,0,0,0), 1, true));
		assertEquals("1:2:3:4:5:6:0.0.0.0", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,5,0,6,0,0,0,0), 1, true));
		assertEquals("1:2:3:4:5:6:0.7.0.0", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,0), 1, true));
		assertEquals("1:2:3:4:5:6:0.7.0.8", NetworkUtil.ipv6ToString(bytes(0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8), 1, true));
		assertEquals("::0.0.0.1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1), 1, true));
		assertEquals("::0.2.0.1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,1), 1, true));
		assertEquals("::3:0.2.0.1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,3,0,2,0,1), 1, true));
		assertEquals("::4:3:0.2.0.1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,4,0,3,0,2,0,1), 1, true));
		assertEquals("::5:4:3:0.2.0.1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,5,0,4,0,3,0,2,0,1), 1, true));
		assertEquals("::6:5:4:3:0.2.0.1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,6,0,5,0,4,0,3,0,2,0,1), 1, true));
		assertEquals("0:7:6:5:4:3:0.2.0.1", NetworkUtil.ipv6ToString(bytes(0,0,0,0,7,0,6,0,5,0,4,0,3,0,2,0,1), 1, true));
		assertEquals("2::0.0.0.1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,1), 1, true));
		assertEquals("2::0.3.0.1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,0,0,0,0,0,0,0,0,0,0,3,0,1), 1, true));
		assertEquals("2::4:0.3.0.1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,0,0,0,0,0,0,0,0,4,0,3,0,1), 1, true));
		assertEquals("2:4::5:0.3.0.1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,4,0,0,0,0,0,0,0,5,0,3,0,1), 1, true));
		assertEquals("2:4:6::5:0.3.0.1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,4,0,6,0,0,0,0,0,5,0,3,0,1), 1, true));
		assertEquals("2:4:6:0:7:5:0.3.0.1", NetworkUtil.ipv6ToString(bytes(0,0,2,0,4,0,6,0,0,0,7,0,5,0,3,0,1), 1, true));
		assertEquals("0:0:1::0.0.0.0", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0), 1, true));
		assertEquals("1:2:3:4:5:6:7:8", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8), 4, false));
		
		assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:255.255.255.255", NetworkUtil.ipv6ToString(bytes(0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255), 1, true));
		assertEquals("fffe:fdfc:fbfa:f9f8:f7f6:f5f4:243.242.241.240", NetworkUtil.ipv6ToString(bytes(0,255,254,253,252,251,250,249,248,247,246,245,244,243,242,241,240), 1, true));

		assertEquals("0:0:0:0:0:0:0:0", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0), 1, false, false));
		assertEquals("0:0:0:0:0:0:0.0.0.0", NetworkUtil.ipv6ToString(bytes(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0), 1, true, false));
		assertEquals("100:0:0:0:0:300:200:0", NetworkUtil.ipv6ToString(bytes(0,1,0,0,0,0,0,0,0,0,0,3,0,2,0,0,0), 1, false, false));
		assertEquals("100:0:0:0:0:300:2.0.0.0", NetworkUtil.ipv6ToString(bytes(0,1,0,0,0,0,0,0,0,0,0,3,0,2,0,0,0), 1, true, false));
		
	}
	
	void assertIpv4ToBytes(byte[] expected, boolean expectedResult, String ip, int off) {
		byte[] bytes = new byte[4+off*2];
		assertEquals(expectedResult, NetworkUtil.ipv4ToBytes(ip, bytes, off));
		assertArrayEquals(expected, bytes);
	}
	
	@Test
	public void testIpv4ToBytes() {
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1.", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1.1", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1.1.", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1.1.1", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1.1.1.", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "x.x.x.", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, " 1.2.3.4 ", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1.2.3.4 ", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, " 1.2.3.4", 1);
		
		byte[] b = bytes(1,1,1,1,1,1);
		assertTrue(NetworkUtil.ipv4ToBytes("0.0.0.0", b, 1));
		assertArrayEquals(bytes(1,0,0,0,0,1), b);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), true, "0.0.0.0", 1);
		assertIpv4ToBytes(bytes(0,1,2,3,4,0), true, "1.2.3.4", 1);
		assertIpv4ToBytes(bytes(0,255,255,255,255,0), true, "255.255.255.255", 1);
		assertIpv4ToBytes(bytes(0,1,2,3,4,0), true, "001.002.003.004", 1);
		assertIpv4ToBytes(bytes(1,2,3,4), true, "1.2.3.4", 0);
		assertIpv4ToBytes(bytes(0,0,1,2,3,4,0,0), true, "1.2.3.4", 2);

		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "256.255.255.255", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "255.256.255.255", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "255.255.256.255", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "255.255.255.256", 1);

		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, ".201.202.203", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "200..202.203", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "200.201..203", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "200.201.202.", 1);

		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "201.202.203", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "201.202.203.0001", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "201.202.2.3.4", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "x.2.3.4", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1.2x.3.4", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1.2.x3.4", 1);
		assertIpv4ToBytes(bytes(0,0,0,0,0,0), false, "1.2.3.x", 1);
		
		assertArrayEquals(bytes(1,2,3,4), NetworkUtil.ipv4ToBytes("1.2.3.4"));
		assertNull(NetworkUtil.ipv4ToBytes("256.2.3.4"));
	}
	
	@Test
	public void testIpv4ToString() {
		assertEquals("0.0.0.0", NetworkUtil.ipv4ToString(bytes(1,0,0,0,0), 1));
		assertEquals("1.2.3.4", NetworkUtil.ipv4ToString(bytes(0,1,2,3,4), 1));
		assertEquals("255.254.253.252", NetworkUtil.ipv4ToString(bytes(0,255,254,253,252), 1));
		assertEquals("255.255.255.255", NetworkUtil.ipv4ToString(bytes(0,255,255,255,255), 1));
		assertEquals("1.2.3.4", NetworkUtil.ipv4ToString(bytes(1,2,3,4)));
		assertEquals("255.255.255.255", NetworkUtil.ipv4ToString(bytes(255,255,255,255)));
	}
	
	@Test
	public void toShort() {
		assertEquals(0, NetworkUtil.toShort(bytes(1,0,0), 1));
		assertEquals(1, NetworkUtil.toShort(bytes(0,0,1), 1));
		assertEquals(256, NetworkUtil.toShort(bytes(0,1,0), 1));
		assertEquals(255, NetworkUtil.toShort(bytes(0,0,255), 1));
		assertEquals(-1, NetworkUtil.toShort(bytes(0,255,255), 1));
		assertEquals(0x7fff, NetworkUtil.toShort(bytes(0,0x7f,255), 1));
		assertEquals(-1, NetworkUtil.toShort(bytes(255,255)));
		assertEquals(0x7fff, NetworkUtil.toShort(bytes(0x7f,255)));
	}
	
	@Test
	public void toPort() {
		assertEquals(0, NetworkUtil.toPort(bytes(1,0,0), 1));
		assertEquals(1, NetworkUtil.toPort(bytes(0,0,1), 1));
		assertEquals(256, NetworkUtil.toPort(bytes(0,1,0), 1));
		assertEquals(255, NetworkUtil.toPort(bytes(0,0,255), 1));
		assertEquals(0x0ffff, NetworkUtil.toPort(bytes(0,255,255), 1));
		assertEquals(0x07fff, NetworkUtil.toPort(bytes(0,0x7f,255), 1));
		assertEquals(0x0ffff, NetworkUtil.toPort(bytes(255,255)));
		assertEquals(0x07fff, NetworkUtil.toPort(bytes(0x7f,255)));
	}
}
