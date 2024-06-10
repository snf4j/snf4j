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

import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.snf4j.quic.Version;

public class PacketInfoTest {

	@Test
	public void testLongHeaderType() {
		PacketInfo info = PacketInfo.of(Version.V1);
		
		assertSame(PacketType.INITIAL, info.longHeaderType(0b11001111));
		assertSame(PacketType.ZERO_RTT, info.longHeaderType(0b11011111));
		assertSame(PacketType.HANDSHAKE, info.longHeaderType(0b11101111));
		assertSame(PacketType.RETRY, info.longHeaderType(0b11111111));
		assertSame(PacketType.INITIAL, info.longHeaderType(0b00000000));
		assertSame(PacketType.ZERO_RTT, info.longHeaderType(0b00010000));
		assertSame(PacketType.HANDSHAKE, info.longHeaderType(0b00100000));
		assertSame(PacketType.RETRY, info.longHeaderType(0b00110000));
	}
}
