/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.handshake;

public class ServerHelloRandom {

	private final static byte[] RANDOM = new byte[] {
			(byte)0xCF,0x21,(byte)0xAD,0x74,(byte)0xE5,(byte)0x9A,0x61,0x11,
			(byte)0xBE,0x1D,(byte)0x8C,0x02,0x1E,0x65,(byte)0xB8,(byte)0x91,
			(byte)0xC2,(byte)0xA2,0x11,0x16,0x7A,(byte)0xBB,(byte)0x8C,0x5E,
			0x07,(byte)0x9E,0x09,(byte)0xE2,(byte)0xC8,(byte)0xA8,0x33,
			(byte)0x9C
	};
	
	private ServerHelloRandom() {
	}
	
	public static byte[] getHelloRetryRequestRandom() {
		return RANDOM.clone();
	}

	public static boolean isHelloRetryRequest(byte[] random) {
		if (random.length != 32 || random[0] != (byte)0xCF) {
			return false;
		}
		for (int i=1; i<32; ++i) {
			if (RANDOM[i] != random[i]) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean isHelloRetryRequest(IServerHello serverHello) {
		return isHelloRetryRequest(serverHello.getRandom());
	}
}
