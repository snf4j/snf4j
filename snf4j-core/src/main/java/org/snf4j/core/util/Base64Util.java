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

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * A class with Base64 utility functions.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public final class Base64Util {

	private final static byte[] EMPTY = new byte[0];
	
	private final static byte PAD = (byte) '=';

	private final static int PAD_INDEX = 64;

	private final static char[] ALPHABET = new char[('Z' - 'A' + 1) * 2 + 10 + 2 + 1];

	private final static int[] DECODING = new int[256];

	static {
		int i = 0;

		for (char c = 'A'; c <= 'Z'; c++) {
			ALPHABET[i++] = c;
		}
		for (char c = 'a'; c <= 'z'; c++) {
			ALPHABET[i++] = c;
		}
		for (char c = '0'; c <= '9'; c++) {
			ALPHABET[i++] = c;
		}
		ALPHABET[i++] = '+';
		ALPHABET[i++] = '/';
		ALPHABET[i++] = PAD;

		Arrays.fill(DECODING, (byte) -1);
		for (i = 0; i < ALPHABET.length - 1; ++i) {
			DECODING[ALPHABET[i]] = (byte) i;
		}
	}

	private Base64Util() {
	}

	/**
	 * Encodes bytes from the specified byte array into a newly-allocated byte array
	 * using the Base64 encoding scheme.
	 * <p>
	 * It uses "The Base 64 Alphabet" as specified in Table 1 of RFC 4648.
	 * 
	 * @param data the byte array to encode
	 * @return A newly-allocated byte array containing the resulting encoded bytes
	 */
	public static byte[] encode(byte[] data) {
		return encode(data, 0, data.length);
	}

	/**
	 * Encodes the specified byte array into a String using the Base64 encoding
	 * scheme.
	 * <p>
	 * It first encodes input bytes into a base64 encoded byte array by calling the
	 * {@link #encode(byte[])} method and then constructs a new String by using the
	 * encoded byte array and the specified charset.
	 * 
	 * @param data    the byte array to encode
	 * @param charset the charset used to encode the resulting String
	 * @return A String containing the resulting Base64 encoded characters
	 */
	public static String encode(byte[] data, Charset charset) {
		return new String(encode(data, 0, data.length), charset);
	}

	/**
	 * Encodes bytes from the specified byte array into a newly-allocated byte array
	 * using the Base64 encoding scheme.
	 * <p>
	 * It uses "The Base 64 Alphabet" as specified in Table 1 of RFC 4648.
	 * 
	 * @param data   the byte array to encode
	 * @param offset offset within the array of the first byte to be encoded
	 * @param length number of bytes to be encoded
	 * @return A newly-allocated byte array containing the resulting encoded bytes
	 */
	public static byte[] encode(byte[] data, int offset, int length) {
		if (length == 0) {
			return EMPTY;
		}

		int end = (length / 3) * 3 + offset;
		byte[] encoded = new byte[(length / 3 + (length % 3 == 0 ? 0 : 1)) * 4];
		int c = 0, i = offset, v;

		while (i < end) {
            v = (data[i++] & 0xff) << 16 | (data[i++] & 0xff) <<  8 | (data[i++] & 0xff);
			encoded[c++] = (byte)ALPHABET[(v >>> 18) & 0x3f];
			encoded[c++] = (byte)ALPHABET[(v >>> 12) & 0x3f];
			encoded[c++] = (byte)ALPHABET[(v >>> 6)  & 0x3f];
			encoded[c++] = (byte)ALPHABET[v & 0x3f];
		}

		int i1, i2, i3, i4;
		
		switch (length-i+offset) {
		case 1:
			i1 = data[i] >>> 2 & 0x3f;
			i2 = data[i] << 4 & 0x3f;
			i3 = PAD_INDEX;
			i4 = PAD_INDEX;
			break;

		case 2:
			i1 = data[i] >>> 2 & 0x3f;
			i2 = (data[i] << 4 | data[i + 1] >>> 4 & 0x0f) & 0x3f;
			i3 = data[i + 1] << 2 & 0x3f;
			i4 = PAD_INDEX;
			break;

		default:
			return encoded;
		}

		encoded[c++] = (byte)ALPHABET[i1];
		encoded[c++] = (byte)ALPHABET[i2];
		encoded[c++] = (byte)ALPHABET[i3];
		encoded[c++] = (byte)ALPHABET[i4];
		return encoded;
	}

	/**
	 * Encodes the specified byte array into a String using the Base64 encoding
	 * scheme.
	 * <p>
	 * It first encodes input bytes into a base64 encoded byte array by calling the
	 * {@link #encode(byte[])} method and then constructs a new String by using the
	 * encoded byte array and the specified charset.
	 * 
	 * @param data    the byte array to encode
	 * @param offset  offset within the array of the first byte to be encoded
	 * @param length  number of bytes to be encoded
	 * @param charset the charset used to encode the resulting String
	 * @return A String containing the resulting Base64 encoded characters
	 */
	public static String encode(byte[] data, int offset, int length, Charset charset) {
		return new String(encode(data, offset, length), charset);
	}
	
	/**
	 * Decodes bytes from the specified byte array into a newly-allocated byte array
	 * using the Base64 encoding scheme.
	 * <p>
	 * It uses "The Base 64 Alphabet" as specified in Table 1 of RFC 4648.
	 * 
	 * @param data the byte array to decode
	 * @return A newly-allocated byte array containing the resulting decoded bytes
	 * @throws IllegalArgumentException - if the data is not in valid Base64 scheme
	 */
	public static byte[] decode(byte[] data) {
		return decode(data, 0, data.length, false);
	}

	/**
	 * Decodes bytes from the specified byte array into a newly-allocated byte array
	 * using the Base64 encoding scheme with an option for the MIME format.
	 * <p>
	 * It uses "The Base 64 Alphabet" as specified in Table 1 of RFC 4648.
	 * 
	 * @param data   the byte array to decode
	 * @param isMime {@code true} if the data is encoded in the MIME format
	 * @return A newly-allocated byte array containing the resulting decoded bytes,
	 *         or {@code null} if the data is not in valid Base64 scheme
	 */
	public static byte[] decode(byte[] data, boolean isMime) {
		return decode(data, 0, data.length, isMime);
	}
	
	/**
	 * Decodes a Base64 encoded String into a newly-allocated byte array using the
	 * Base64 encoding scheme.
	 * <p>
	 * It first decodes the Base64 encoded String into a sequence of bytes using the
	 * given charset and then decode the bytes by calling the
	 * {@link #decode(byte[])} method.
	 * 
	 * @param data    the string to decode
	 * @param charset The charset to be used to encode the String
	 * @return A newly-allocated byte array containing the resulting decoded bytes,
	 *         or {@code null} if the data is not in valid Base64 scheme
	 */
	public static byte[] decode(String data, Charset charset) {
		return decode(data.getBytes(charset), false);
	}

	/**
	 * Decodes a Base64 encoded String into a newly-allocated byte array using the
	 * Base64 encoding scheme with an option for the MIME format.
	 * <p>
	 * It first decodes the Base64 encoded String into a sequence of bytes using the
	 * given charset and then decode the bytes by calling the
	 * {@link #decode(byte[])} method.
	 * 
	 * @param data    the string to decode
	 * @param isMime  {@code true} if the data is encoded in the MIME format
	 * @param charset The charset to be used to encode the String
	 * @return A newly-allocated byte array containing the resulting decoded bytes,
	 *         or {@code null} if the data is not in valid Base64 scheme
	 */
	public static byte[] decode(String data, Charset charset, boolean isMime) {
		return decode(data.getBytes(charset), isMime);
	}

	/**
	 * Decodes bytes from the specified byte array into a newly-allocated byte array
	 * using the Base64 encoding scheme with an option for the MIME format.
	 * <p>
	 * It uses "The Base 64 Alphabet" as specified in Table 1 of RFC 4648.
	 * 
	 * @param data   the byte array to decode
	 * @param offset offset within the array of the first byte to be decoded
	 * @param length number of bytes to be encoded
	 * @param isMime {@code true} if the data is encoded in the MIME format
	 * @return A newly-allocated byte array containing the resulting decoded bytes,
	 *         or {@code null} if the data is not in valid Base64 scheme
	 */
	public static byte[] decode(byte[] data, int offset, int length, boolean isMime) {
		int end = offset+length;
		int origEnd = end;
		int ignored = 0;
		
		if (length == 0) {
			return EMPTY;
		} else if (length < 2) {
			return null;
		}

		if (isMime) {
			for (int i=offset; i<end; ++i) {
				byte b = data[i];
				
				if (b == PAD) {
					end = i;
					length = end - offset;
					break;
				}
				if (DECODING[b & 0xff] == -1) {
					++ignored;
				}
			}
		}
		else if (data[end - 1] == PAD) {
			--end;
			--length;
			if (data[end - 1] == PAD) {
				--end;
				--length;
			}
		}

		if (length == 0) {
			return EMPTY;
		}

		int calcLen = ((length-ignored) / 4) * 3;

		switch ((length-ignored) & 0x03) {
		case 1:
			return null;

		case 2:
			calcLen++;
			break;

		case 3:
			calcLen += 2;

		default:
		}

		byte[] decoded = new byte[calcLen];
		int d = 0;
		int v = 0, vcount = 0;
		
		for (int i=offset; i<end; ++i) {
			int c = DECODING[data[i] & 0xff];
			
			if (c == -1) {
				if (isMime) {
					continue;
				}
				return null;
			}
			v <<= 6;
			v |= c;
			if (vcount == 3) {
				decoded[d++] = (byte) (v >> 16);
				decoded[d++] = (byte) (v >> 8);
				decoded[d++] = (byte) v;
				vcount = 0;
				v = 0;
			}
			else {
				++vcount;
			}
		}
		
		if (vcount > 0) {
			int shift = (4 - vcount) * 6;

			decoded[d++] = (byte) (v >> (16 - shift));
			if (shift == 6) {
				decoded[d++] = (byte) (v >> (8 - shift));
			}
		}
		
		if (isMime && end < origEnd) {
			for (; end < origEnd; ++end) {
				byte b = data[end];
				
				if (b == PAD) {
					continue;
				}
				if (DECODING[b & 0xff] != -1) {
					return null;
				}
			}
		}
		return decoded;
	}

}
