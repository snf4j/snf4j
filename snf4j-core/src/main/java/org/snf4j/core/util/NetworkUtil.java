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

/**
 * A class with network-related utility functions.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public final class NetworkUtil {

	private NetworkUtil() {}

	static int digit(char c) {
		switch (c) {
		case '0': return 0;
		case '1': return 1;
		case '2': return 2;
		case '3': return 3;
		case '4': return 4;
		case '5': return 5;
		case '6': return 6;
		case '7': return 7;
		case '8': return 8;
		case '9': return 9;
		}
		return -1;
	}

	static int hexDigit(char c) {
		switch (c) {
		case '0': return 0;
		case '1': return 1;
		case '2': return 2;
		case '3': return 3;
		case '4': return 4;
		case '5': return 5;
		case '6': return 6;
		case '7': return 7;
		case '8': return 8;
		case '9': return 9;
		}
		
		c = Character.toLowerCase(c);
		switch (c) {
		case 'a': return 10;
		case 'b': return 11;
		case 'c': return 12;
		case 'd': return 13;
		case 'e': return 14;
		case 'f': return 15;
		}
		return -1;
	}
	
	/**
	 * Converts a text representation of an IPv6 address into an array of bytes.
	 * 
	 * @param ip     the text representation of an IPv6 address
	 * @param output an array for the output bytes
	 * @param off    starting position for the output bytes in the array
	 * @return {@code true} if the text representation was a valid IPv6 address. If
	 *         the returned value is {@code false} the initial content in the output
	 *         array is not changed
	 */
	public static boolean ipv6ToBytes(CharSequence ip, byte[] output, int off) {
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
		
		for (j=i; j<len; ++j) {
			if (ip.charAt(j) == '%') {
				len = j;
				break;
			}
		}
		if (len-i < 2) {
			return false;
		}

		boolean doubleColon;
		char c;
		int colons;
		
		if (ip.charAt(i) == ':') {
			if (ip.charAt(i+1) == ':') {
				i += 2;
				doubleColon = true;
				c = ':';
				colons = 2;
			}
			else {
				return false;
			}
		}
		else {
			doubleColon = false;
			c = 0;
			colons = 0;
		}
		
		int word = -1, maxWord = 0xffff, periods = 0, radix = 16;
		int j0 = 0, d;
		byte[] buf = new byte[16];
		char prevc;
		
		j = 0;
		for (; i<len; ++i) {
			prevc = c;
			c = ip.charAt(i);	
			switch (c) {
			case '.':
				if (periods > 2 || word == -1 || colons == 0) {
					return false;
				}
				if (radix == 16) { 
					maxWord = 255;
					radix = 10;
					if (word > 9) {
						--i;
						for (; ip.charAt(i) != ':'; --i);
						c = ':';
						word = -1;
						break;
					}
				}
				if (j > 15) {
					return false;
				}
				else {
					buf[j++] = (byte)word;
					word = -1;
					++periods;
				}
				break;
				
			case ':':
				++colons;
				if (periods > 0) {
					return false;
				}
				if (prevc == ':') {
					if (doubleColon) {
						return false;
					}
					doubleColon = true;
					j0 = j;
				}
				else if (j > 14) {
					return false;
				}
				else {
					buf[j++] = (byte)(word >> 8);
					buf[j++] = (byte)word;
					word = -1;
				}
				break;
				
			default:
				if (radix == 16) {
					d = hexDigit(c);
					if (d == -1) {
						return false;
					}
				}
				else {
					d = digit(c);
					if (d == -1) {
						return false;
					}
				}
				if (word == -1) {
					word = d;
				}
				else {
					word *= radix;
					word += d;
					if (word > maxWord) {
						return false;
					}
				}
			}
		}
		
		if (periods > 0) {
			if (periods != 3 || word == -1) {
				return false;
			}
			buf[j++] = (byte)word;
		}
		else if (word == -1) {
			if (ip.charAt(len-2) != ':') {
				return false;
			}
		}
		else if (j > 14) {
			return false;
		}
		else {
			buf[j++] = (byte)(word >> 8);
			buf[j++] = (byte)word;
		}
		
		if (j == 16) {
			if (doubleColon) {
				return false;
			}
			System.arraycopy(buf, 0, output, off, 16);
		}
		else if (doubleColon) {
			System.arraycopy(buf, 0, output, off, j0);
			len = 16-j+j0;
			for (i=j0; i<len; ++i) {
				output[off+i] = 0;
			}
			System.arraycopy(buf, j0, output, off+i, j-j0);
		}
		else {
			return false;
		}
		return true;
	}

	/**
	 * Converts a text representation of an IPv6 address into an array of bytes.
	 * 
	 * @param ip the text representation of an IPv6 address
	 * @return an array of bytes, or {@code null} if the text representation was not
	 *         a valid IPv6 address
	 */
	public static byte[] ipv6ToBytes(CharSequence ip) {
		byte[] buf = new byte[16];
		
		if (ipv6ToBytes(ip,buf,0)) {
			return buf;
		}
		return null;
	}

	/**
	 * Converts an array of bytes into a compressed text representation of an IPv6
	 * address.
	 * 
	 * @param data      the array of bytes
	 * @param off       starting position in the array of bytes
	 * @param embedIpv4 {@code true} if the IPv4 notation should should be embedded
	 *                  in the returned text representation of the IPv6 address.
	 * @return the text representation of the IPv6 address
	 */
	public static String ipv6ToString(byte[] data, int off, boolean embedIpv4) {
		return ipv6ToString(data, off, embedIpv4, true);
	}
	
	/**
	 * Converts an array of bytes into a text representation of an IPv6 address.
	 * 
	 * @param data      the array of bytes
	 * @param off       starting position in the array of bytes
	 * @param embedIpv4 {@code true} if the IPv4 notation should should be embedded
	 *                  in the returned text representation of the IPv6 address
	 * @param compress  {@code true} to used compressed text representation of the
	 *                  IPv6 address
	 * 
	 * @return the text representation of the IPv6 address
	 */
	public static String ipv6ToString(byte[] data, int off, boolean embedIpv4, boolean compress) {
		int[] words = new int[8];
		int i,j,len = off+16;
		
		for (i=off,j=0; i<len; i+=2, ++j) {
			words[j] = (int)toShort(data, i) & 0xffff;
		}
		len = embedIpv4 ? 6 : 8;
		
		int p = -1, plen = 0, maxp = -1, maxplen = 0;
		
		if (compress) {
			// Find longest run of 0s
			for (i=0; i<len; ++i) {
				if (words[i] == 0) {
					if (p == -1) {
						p = i;
					}
				}
				else if (p != -1){
					plen = i - p;
					if (plen > maxplen) {
						maxp = p;
						maxplen = plen;
					}
					p = -1;
				}
			}
			if (p != -1) {
				plen = i - p;
				if (plen > maxplen) {
					maxp = p;
					maxplen = plen;
				}
			}
			if (maxplen == 1) {
				maxplen = 0;
				maxp = -1;
			}
			else if (maxp != -1) {
				j = maxp +maxplen;
				for (i=maxp; i<j; ++i) {
					words[i] = -1;
				}
			}
		}
		
		//Longest xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:ddd.ddd.ddd.ddd => 6*5+4*4-1
		StringBuilder sb = new StringBuilder(45);
		boolean doubleColon = false;
		
		for (i=0; i<len; ++i) {
			j = words[i];
			if (j == -1) { 
				if (!doubleColon) {
					sb.append(i == 0 ? "::" : ":");
					doubleColon = true;
				}
			}
			else {
				sb.append(Integer.toHexString(j));
				if (i < 7) {
					sb.append(':');
				}
			}	
		}
		if (embedIpv4) {
			sb.append((int)data[off+12] & 0xff);
			sb.append('.');
			sb.append((int)data[off+13] & 0xff);
			sb.append('.');
			sb.append((int)data[off+14] & 0xff);
			sb.append('.');
			sb.append((int)data[off+15] & 0xff);
		}
		return sb.toString();
	}
	
	/**
	 * Converts a text representation of an IPv4 address into an array of bytes.
	 * 
	 * @param ip     the text representation of an IPv4 address
	 * @param output an array for the output bytes
	 * @param off    starting position for the output bytes in the array
	 * @return {@code true} if the text representation was a valid IPv4 address. If
	 *         the returned value is {@code false} the initial content in the output
	 *         array is not changed
	 */
	public static boolean ipv4ToBytes(CharSequence ip, byte[] output, int off) {
		int len = ip.length();
		
		if (len >= 7 && len <= 15) {
			byte[] buf = new byte[3];
			int periods = 0, octet = -1, d;
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
				else if ((d = digit(c)) == -1) {
					return false;
				}
				else if (octet == -1) {
					octet = d;
				}
				else {
					octet *= 10;
					octet += d;
				}
			}
			if (periods == 3 && octet != -1 && octet <= 255) {
				output[off] = buf[0];
				output[off+1] = buf[1];
				output[off+2] = buf[2];
				output[off+3] = (byte)octet;
				return true;
			}
		}		
		return false;
	}

	/**
	 * Converts a text representation of an IPv4 address into an array of bytes.
	 * 
	 * @param ip the text representation of an IPv4 address
	 * @return an array of bytes, or {@code null} if the text representation was not
	 *         a valid IPv4 address
	 */
	public static byte[] ipv4ToBytes(CharSequence ip) {
		byte[] buf = new byte[4];
		
		if (ipv4ToBytes(ip,buf,0)) {
			return buf;
		}
		return null;
	}
	
	/**
	 * Converts an array of bytes into a text representation of an IPv4 address.
	 * 
	 * @param data the array of bytes
	 * @param off  starting position in the array of bytes
	 * @return the text representation of the IPv4 address
	 */
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

	/**
	 * Converts an array of bytes into a text representation of an IPv4 address.
	 * 
	 * @param data the array of bytes
	 * @return the text representation of the IPv4 address
	 */
	public static String ipv4ToString(byte[] data) {
		return ipv4ToString(data, 0);
	}

	/**
	 * Converts an {@code int} value into a text representation of an IPv4 address.
	 * 
	 * @param value the {@code int} value
	 * @return the text representation of the IPv4 address
	 */
	public static String ipv4ToString(int value) {
		return ipv4ToString(toBytes(value));
	}
	
	/**
	 * Converts a text representation of an IPv4 address into an {@code int} value.
	 * 
	 * @param ip the text representation of an IPv4 address
	 * @return an {@code int} value
	 */
	public static int ipv4ToInt(CharSequence ip) {
		byte[] buf = ipv4ToBytes(ip);
		
		if (buf != null) {
			return toInt(buf);
		}
		throw new IllegalArgumentException("ip is invalid IPv4 address");
	}
	
	/**
	 * Converts an array of bytes into a port number.
	 * 
	 * @param data the array of bytes
	 * @param off  starting position in the array of bytes
	 * @return the port number
	 */
	public static int toPort(byte[] data, int off) {
		return (int)toShort(data, off) & 0xffff; 
	}

	/**
	 * Converts an array of bytes into a port number.
	 * 
	 * @param data the array of bytes
	 * @return the port number
	 */
	public static int toPort(byte[] data) {
		return toPort(data, 0); 
	}

	/**
	 * Converts a port number into an array of bytes.
	 * 
	 * @param port  the port number
	 * @param output an array for the output bytes
	 * @param off    starting position for the output bytes in the array
	 */
	public static void portToBytes(int port, byte[] output, int off) {
		output[off] = (byte)(port >> 8);
		output[off+1] = (byte)port;
	}

	/**
	 * Converts a port number into an array of bytes.
	 * 
	 * @param port  the port number
	 * @return an array of bytes
	 */
	public static byte[] portToBytes(int port) {
		byte[] buf = new byte[2];
		
		portToBytes(port, buf, 0);
		return buf;
	}
	
	/**
	 * Converts an array of bytes into a {@code short} value.
	 * 
	 * @param data the array of bytes
	 * @param off  starting position in the array of bytes
	 * @return the {@code short} value
	 */
	public static short toShort(byte[] data, int off) {
		return (short) (((int)data[off] << 8) | 
				((int)data[off+1] & 0xff)); 
	}

	/**
	 * Converts an array of bytes into a {@code short} value.
	 * 
	 * @param data the array of bytes
	 * @return the {@code short} value
	 */
	public static short toShort(byte[] data) {
		return toShort(data, 0); 
	}
	
	/**
	 * Converts a {@code short} value into an array of bytes.
	 * 
	 * @param value  the {@code short} value
	 * @param output an array for the output bytes
	 * @param off    starting position for the output bytes in the array
	 */
	public static void toBytes(short value, byte[] output, int off) {
		output[off  ] = (byte)(value >> 8);
		output[off+1] = (byte)value;
	}

	/**
	 * Converts a {@code short} value into an array of bytes.
	 * 
	 * @param value the short value
	 * @return an array of bytes
	 */
	public static byte[] toBytes(short value) {
		byte[] buf = new byte[2];
		
		toBytes(value, buf, 0);
		return buf;
	}
	
	/**
	 * Converts an array of bytes into an {@code int} value.
	 * 
	 * @param data the array of bytes
	 * @param off  starting position in the array of bytes
	 * @return the {@code int} value
	 */
	public static int toInt(byte[] data, int off) {
		return ((int)data[off] << 24) |
				((int)data[off+1] << 16) & 0xff0000 | 
				((int)data[off+2] <<  8) & 0xff00 | 
				((int)data[off+3]        & 0xff); 
	}

	/**
	 * Converts an array of bytes into an {@code int} value.
	 * 
	 * @param data the array of bytes
	 * @return the {@code int} value
	 */
	public static int toInt(byte[] data) {
		return toInt(data, 0); 
	}
	
	/**
	 * Converts an {@code int} value into an array of bytes.
	 * 
	 * @param value  the short value
	 * @param output an array for the output bytes
	 * @param off    starting position for the output bytes in the array
	 */
	public static void toBytes(int value, byte[] output, int off) {
		output[off  ] = (byte)(value >> 24);
		output[off+1] = (byte)(value >> 16);
		output[off+2] = (byte)(value >> 8);
		output[off+3] = (byte)value;
	}

	/**
	 * Converts an {@code int} value into an array of bytes.
	 * 
	 * @param value the short value
	 * @return an array of bytes
	 */
	public static byte[] toBytes(int value) {
		byte[] buf = new byte[4];
		
		toBytes(value, buf, 0);
		return buf;
	}

	/**
	 * Converts an array of bytes into a {@code long} value.
	 * 
	 * @param data the array of bytes
	 * @param off  starting position in the array of bytes
	 * @return the {@code long} value
	 */
	public static long toLong(byte[] data, int off) {
		return ((long)data[off] << 56) |
				((long)data[off+1] << 48) & 0xff000000000000L |
				((long)data[off+2] << 40) & 0xff0000000000L |
				((long)data[off+3] << 32) & 0xff00000000L |
				((long)data[off+4] << 24) & 0xff000000L |
				((long)data[off+5] << 16) & 0xff0000 | 
				((long)data[off+6] <<  8) & 0xff00 | 
				((long)data[off+7]        & 0xff); 
	}

	/**
	 * Converts an array of bytes into a {@code long} value.
	 * 
	 * @param data the array of bytes
	 * @return the {@code long} value
	 */
	public static long toLong(byte[] data) {
		return toLong(data, 0); 
	}
	
	/**
	 * Converts a {@code long} value into an array of bytes.
	 * 
	 * @param value  the short value
	 * @param output an array for the output bytes
	 * @param off    starting position for the output bytes in the array
	 */
	public static void toBytes(long value, byte[] output, int off) {
		output[off  ] = (byte)(value >> 56);
		output[off+1] = (byte)(value >> 48);
		output[off+2] = (byte)(value >> 40);
		output[off+3] = (byte)(value >> 32);
		output[off+4] = (byte)(value >> 24);
		output[off+5] = (byte)(value >> 16);
		output[off+6] = (byte)(value >> 8);
		output[off+7] = (byte)value;
	}

	/**
	 * Converts a {@code long} value into an array of bytes.
	 * 
	 * @param value the short value
	 * @return an array of bytes
	 */
	public static byte[] toBytes(long value) {
		byte[] buf = new byte[8];
		
		toBytes(value, buf, 0);
		return buf;
	}
	
}
