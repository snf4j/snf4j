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

public final class NetworkUtil {

	private NetworkUtil() {}

    private static boolean isHexDigit(char c) {
        return c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f';
    }
 
	public static boolean ipv6ToBytes(String ip, byte[] bytes, int off) {
		int len = ip.length();
		int i,j;
		
		if (len < 2) {
			return false;
		}
		if (ip.charAt(0) == '[') {
			if (ip.charAt(len - 1) != ']') {
				return false;
			}
			i = 1;
			--len; 
		}
		else {
			i = 0;
		}
		
		if ((j = ip.indexOf('%')) >= 0) {
			len = j;
		}
		if (len-i < 2) {
			return false;
		}

		boolean doubleColon0;
		
		if (ip.charAt(i) == ':') {
			if (ip.charAt(i+1) == ':') {
				doubleColon0 = true;
			}
			else {
				return false;
			}
		}
		else {
			doubleColon0 = false;
		}
		
		int word = -1, periods = 0, colons;
		boolean doubleColon;
		int lj = 0, rj = 0;
		byte[] left, right, buf;
		char c = 0, prevc;
		
		if (doubleColon0) {
			left = null;
			right = new byte[16];
			buf = right;
			i += 2;
			doubleColon = true;
			colons = 2;
		}
		else {
			left = new byte[16];
			right = null;
			buf = left;
			doubleColon = false;
			colons = 0;
		}
		
		j = 0;
		for (; i<len; ++i) {
			prevc = c;
			c = ip.charAt(i);	
			switch (c) {
			case '.':
				if (periods > 2 || word < 0 || word > 255) {
					return false;
				}
				if (!doubleColon && colons != 6) {
					return false;
				}
				if (!doubleColon0 && colons == 7) {
					return false;
				}
				buf[j++] = (byte)word;
				word = -1;
				++periods;
				break;
				
			case ':':
				++colons;
				if (periods > 0 || colons > 7) {
					return false;
				}
				if (prevc == ':') {
					if (doubleColon) {
						return false;
					}
					doubleColon = true;
					lj = j;
					j = 0;
					right = new byte[16];
					buf = right;
				}
				else if (word < 0 || word > 0xffff) {
					return false;
				}
				else {
					buf[j++] = (byte)(word >> 8);
					buf[j++] = (byte)word;
					word = -1;
				}
				break;
				
			default:
				if (!isHexDigit(c)) {
					return false;
				}
				if (word == -1) {
					word = Character.digit(c, 16);
				}
				else {
					word <<= 4;
					word += Character.digit(c, 16);
				}
			}
		}
		
		if (periods > 0) {
			if (periods != 3 || word < 0 || word > 255) {
				return false;
			}
			buf[j++] = (byte)word;
		}
		else if (word == -1) {
			if (ip.charAt(len-2) != ':') {
				return false;
			}
		}
		else if (word >= 0 || word <= 0xffff) {
			buf[j++] = (byte)(word >> 8);
			buf[j++] = (byte)word;
		}
		else {
			return false;
		}
		
		if (right != null) {
			rj = j;
		}
		else {
			lj = j;
		}
		
		i = off;
		j = off+16;
		if (lj > 0) {
			System.arraycopy(left, 0, bytes, i, lj);
			i += lj;
		}
		if (rj > 0) {
			j -= rj;
			System.arraycopy(right, 0, bytes, j , rj);
		}
		for (; i<j; ++i) {
			bytes[i] = 0;
		}
		return true;
	}
	
	public static boolean ipv4ToBytes(String ip, byte[] bytes, int off) {
		int len = ip.length();
		
		if (len >= 7 && len <= 15) {
			byte[] buf = new byte[3];
			int periods = 0;
			int octet = -1;
			char c;

			for (int i=0; i<len; ++i) {
				c = ip.charAt(i);
				if (c == '.') {
					if (periods > 2 || octet == -1 || octet > 255) {
						return false;
					}
					buf[periods] = (byte)octet;
					octet = -1;
					++periods;
				}
				else if (!Character.isDigit(c)) {
					return false;
				}
				else if (octet == -1) {
					octet = Character.digit(c, 10);
				}
				else {
					octet *= 10;
					octet += Character.digit(c, 10);
				}
			}
			if (periods == 3 && octet != -1 && octet <= 255) {
				bytes[off] = buf[0];
				bytes[off+1] = buf[1];
				bytes[off+2] = buf[2];
				bytes[off+3] = (byte)octet;
				return true;
			}
		}		
		return false;
	}

	public static byte[] ipv4ToBytes(String ip) {
		byte[] buf = new byte[4];
		
		if (ipv4ToBytes(ip,buf,0)) {
			return buf;
		}
		return null;
	}
	
	public static String ipv4ToString(byte[] data, int off) {
		StringBuilder ip = new StringBuilder(15);
		
		ip.append((int)data[off] & 0xff);
		ip.append('.');
		ip.append((int)data[off+1] & 0xff);
		ip.append('.');
		ip.append((int)data[off+2] & 0xff);
		ip.append('.');
		ip.append((int)data[off+3] & 0xff);
		return ip.toString();
	}

	public static String ipv4ToString(byte[] data) {
		return ipv4ToString(data, 0);
	}
	
	public static short toShort(byte[] data, int off) {
		return (short) (((int)data[off] << 8) | ((int)data[off+1] & 0xff)); 
	}

	public static short toShort(byte[] data) {
		return toShort(data, 0); 
	}

	public static int toPort(byte[] data, int off) {
		return (int)toShort(data, off) & 0xffff; 
	}

	public static int toPort(byte[] data) {
		return toPort(data, 0); 
	}
}
