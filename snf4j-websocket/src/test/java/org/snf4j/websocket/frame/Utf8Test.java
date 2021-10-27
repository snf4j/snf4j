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
package org.snf4j.websocket.frame;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Utf8Test {
	
	String toString(byte[] data) {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<data.length; ++i) {
			sb.append(Integer.toHexString(Byte.toUnsignedInt(data[i])));
			sb.append(':');
		}
		return sb.toString();
	}
	
	byte[] oneByte(int b1, int value, byte[] out) {
		out[0] = (byte) ((b1 << 7) | value);
		return out;
	}
	
	byte[] twoByte(int b1, int b2, int value, byte[] out) {
		int v1 = (value >> 6) & 0x1f;
		int v2 = value & 0x3f;
		
		out[0] = (byte) (0x80 | (b1 << 5) | v1);
		out[1] = (byte) ((b2 << 6) | v2);
		return out;
	}
	
	byte[] threeByte(int b1, int b2, int b3, int value, byte[] out) {
		int v1 = (value >> 12) & 0x0f;
		int v2 = (value >> 6) & 0x3f;
		int v3 = value & 0x3f;
		
		out[0] = (byte) (0xc0 | (b1 << 4) | v1);
		out[1] = (byte) ((b2 << 6) | v2);
		out[2] = (byte) ((b3 << 6) | v3);
		return out;
	}

	byte[] fourByte(int b1, int b2, int b3, int b4, int value, byte[] out) {
		int v1 = (value >> 18) & 0x07;
		int v2 = (value >> 12) & 0x3f;
		int v3 = (value >> 6) & 0x3f;
		int v4 = value & 0x3f;
		
		out[0] = (byte) (0xe0 | (b1 << 3) | v1);
		out[1] = (byte) ((b2 << 6) | v2);
		out[2] = (byte) ((b3 << 6) | v3);
		out[3] = (byte) ((b4 << 6) | v4);
		return out;
	}
	
	
	byte[] out1 = new byte[1];
	byte[] out2 = new byte[2];
	byte[] out3 = new byte[3];
	
	void assertIsValidIncomplete(byte[] data, int len) {
		switch (len-1) {
		case 3:
			out3[2] = data[2];
			out3[1] = data[1];
			out3[0] = data[0];
			assertFalse(Utf8.isValid(out3));
			
		case 2:
			out2[1] = data[1];
			out2[0] = data[0];
			assertFalse(toString(out2),Utf8.isValid(out2));
			
		case 1:
			out1[0] = data[0];
			assertFalse(Utf8.isValid(out1));
		}
	}
	
	@Test
	public void testIsValid() {
		byte[] out = new byte[5];
		
		for (int i=0; i <= 0x7f; ++ i) {
			assertTrue(Utf8.isValid(oneByte(0, i, out)));
			assertFalse(Utf8.isValid(oneByte(1, i, out)));
		}

		for (int i=0; i < 4; ++i) {
			for (int j=0; j < 4; ++j) {
				if (i == 2 && j == 2) {
					for (int v=0; v<=0x7f; ++v) {
						assertFalse(Utf8.isValid(twoByte(i,j,v,out)));
						assertIsValidIncomplete(out, 2);
					}
					for (int v=0x80; v<0x7ff; ++v) {
						assertTrue(Utf8.isValid(twoByte(i,j,v,out)));
						assertIsValidIncomplete(out, 2);
					}
				}
				else {
					for (int v=0; v<0x7ff; ++v) {
						assertFalse(Utf8.isValid(twoByte(i,j,v,out)));
						assertIsValidIncomplete(out, 2);
					}
				}
			}
		}

		for (int i=0; i < 4; ++i) {
			for (int j=0; j < 4; ++j) {
				for (int k=0; k < 4; ++k) {
					if (i == 2 && j == 2 && k == 2) {
						for (int v=0; v<=0x7ff; ++v) {
							assertFalse(toString(out), Utf8.isValid(threeByte(i,j,k,v,out)));
							assertIsValidIncomplete(out, 3);
						}
						for (int v=0x800; v<0xd800; ++v) {
							assertTrue(toString(out), Utf8.isValid(threeByte(i,j,k,v,out)));
							assertIsValidIncomplete(out, 3);
						}
						for (int v=0xd800; v<=0xdfff; ++v) {
							assertFalse(toString(out), Utf8.isValid(threeByte(i,j,k,v,out)));
							assertIsValidIncomplete(out, 3);
						}
						for (int v=0xdfff+1; v<=0xffff; ++v) {
							assertTrue(toString(out), Utf8.isValid(threeByte(i,j,k,v,out)));
							assertIsValidIncomplete(out, 3);
						}
					}
					else if (i > 1) {
						for (int v=0; v<0xffff; ++v) {
							assertFalse(toString(out), Utf8.isValid(threeByte(i,j,k,v,out)));
							assertIsValidIncomplete(out, 3);
						}
					}
				}
			}
		}
		
		for (int i=0; i < 4; ++i) {
			for (int j=0; j < 4; ++j) {
				for (int k=0; k < 4; ++k) {
					for (int l=0; l < 4; ++l) {
						if (i == 2 && j == 2 && k == 2 && l == 2) {
							for (int v=0; v<=0xffff; ++v) {
								assertFalse(toString(out), Utf8.isValid(fourByte(i,j,k,l,v,out)));
								assertIsValidIncomplete(out, 4);
							}
							for (int v=0x10000; v<=0x10ffff; ++v) {
								assertTrue(toString(out), Utf8.isValid(fourByte(i,j,k,l,v,out)));
								assertIsValidIncomplete(out, 4);
							}
							for (int v=0x10ffff+1; v<=0x1fffff; ++v) {
								assertFalse(toString(out), Utf8.isValid(fourByte(i,j,k,l,v,out)));
								assertIsValidIncomplete(out, 4);
							}
						}
					}
				}
			}
		}
		
		out[0] = (byte) 0xdf;
		out[1] = (byte) 0xdf;
		out[2] = (byte) 0xbf;
		out[3] = (byte) 0xdf;
		out[4] = (byte) 0xdf;
		assertFalse(Utf8.isValid(out));
		for (int off=0; off<5; ++off) {
			for (int len=1; len<5-off; ++len) {
				if (len == 2 && off == 1) {
					assertTrue(Utf8.isValid(out, off, len));
				}
				else {
					assertFalse(Utf8.isValid(out, off, len));
				}
			}
		}
	}
}
