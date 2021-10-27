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
package org.snf4j.websocket.handshake;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class HttpUtils {
	
	final static byte CR = (byte)13;
	
	final static byte LF = (byte)10;
	
	final static byte SP = ' ';
	
	final static byte HT = '\t';
	
	final static byte[] CRLF = new byte[] {CR,LF};
	
	final static byte FS = ':';
	
	final static byte[] FSSP = new byte[] {FS,SP};
	
	final static byte[] GET = "GET".getBytes(StandardCharsets.US_ASCII);
	
	final static byte[] HTTP_VERSION = "HTTP/1.1".getBytes(StandardCharsets.US_ASCII);
	
	final static byte DIGIT_0 = '0';
	
	final static byte DIGIT_9 = '9';
	
	final static int STATUS_CODE_LENGTH = 3;
	
	final static String HOST = "Host";
	
	final static String UPGRADE = "Upgrade";
	
	final static String CONNECTION = "Connection";
	
	final static int HTTP_PORT = 80;
	
	final static int HTTPS_PORT = 443;
	
	final static String HTTP = "http";
	
	final static String HTTPS = "https";
	
	private HttpUtils() {	
	}
	
	public static int available(byte[] data, int off, int len, int[] out) {
		if (out.length == 0) {
			return 0;
		}
		
		int dataLen = off+len;
		int line0 = off, lineCount = 0, maxCount = out.length-3;
		byte prev, curr = 0; 
		boolean end = false;
		
		out[0] = -1;
		for (int i=off; i<dataLen; ++i) {
			prev = curr;
			curr = data[i];
			if (curr == LF) {
				if (prev == CR) {
					if (end) {
						return i+1-off;
					}
					if (lineCount > maxCount) {
						return 0;
					}
					end = true;
					out[lineCount++] = line0-off;
					out[lineCount++] = i-1-off;
					line0 = i+1;
					out[lineCount] = -1;
				}
			}
			else if (curr != CR) {
				end = false;
			}
		}
		return 0;
	}
	
	public static int available(ByteBuffer data, boolean flipped, int[] out) {
		if (data.hasArray()) {
			if (flipped) {
				return available(data.array(), data.arrayOffset() + data.position(), data.remaining(), out);
			}
			return available(data.array(), data.arrayOffset(), data.position(), out);
		}
		
		ByteBuffer dup = data.duplicate();
		byte[] array;
		
		if (!flipped) {
			dup.flip();
		}
		array = new byte[dup.remaining()];
		dup.get(array);
		return available(array, 0, array.length, out);
	}
	
	public static int splitRequestLine(byte[] data, int[] lines, int lineIndex, int[] out) {
		int i = lineIndex << 1;
		
		return splitRequestLine(data, lines[i], lines[i+1], out);
	}
	
	public static int splitResponseLine(byte[] data, int[] lines, int lineIndex, int[] out) {
		return splitRequestLine(data, lines, lineIndex, out);
	}
	
	public static int splitRequestLine(byte[] data, int beginIndex, int endIndex, int[] out) {
		int maxCount = out.length-2;
		
		if (maxCount < 0) {
			return 0;
		}
		
		int line0 = beginIndex, count = 0;
		byte prev, curr = 0;
		
		for (int i=beginIndex; i<endIndex; ++i) {
			prev = curr;
			curr = data[i];
			if (curr == SP) {
				if (prev != SP) {
					out[count++] = line0;
					out[count++] = i;
					if (count > maxCount) {
						return count / 2;
					}
				}
			}
			else if (prev == SP) {
				line0 = i;
			}
		}
		if (curr == SP) {
			out[count++] = endIndex;
		}
		else {
			out[count++] = line0;
		}
		out[count++] = endIndex;
		return count / 2;
	}
	
	public static int splitHeaderField(byte[] data, int beginIndex, int endIndex, int[] tokens) {
		int i,t = 0;
		int sign = 1;
		
		for (i=beginIndex; i<endIndex; ++i) {
			byte c = data[i];
			
			if (c == SP || c == HT) {
				sign = -1;
				continue;
			}
			break;
		}
		
		tokens[t++] = i;
		for (; i<endIndex; ++i) {
			if (data[i] == FS) {
				tokens[t++] = i++;
				break;
			}
		}
		if (t == 1) {
			tokens[t++] = endIndex;
			return t * sign;
		}
		
		for (; i<endIndex; ++i) {
			byte c = data[i];
			
			if (c == SP || c == HT) {
				continue;
			}
			break;
		}
		tokens[t++] = i;
		tokens[t++] = endIndex;
		return t * sign;
	}
	
	public static boolean equals(byte[] data, int[] tokens, int tokenIndex, byte[] value) {
		int i = tokenIndex << 1;
		
		return equals(data, tokens[i], tokens[i+1], value);
	}
	
	public static boolean equals(byte[] data, int beginIndex, int endIndex, byte[] value) {
		int len = endIndex - beginIndex;
		
		if (value.length != len) {
			return false;
		}
		
		for (int i=0; i<len; ++i) {
			if (data[beginIndex++] != value[i]) {
				return false;
			}
		}
		return true;
	}
	
	public static String rtrimAscii(byte[] data, int[] tokens, int tokenIndex) {
		int i = tokenIndex << 1;
		
		return rtrimAscii(data, tokens[i], tokens[i+1]);
	}
	
	public static String ascii(byte[] data, int[] tokens, int tokenIndex) {
		int i = tokenIndex << 1;
		
		return ascii(data, tokens[i], tokens[i+1]);
	}
	
	public static String rtrimAscii(byte[] data, int beginIndex, int endIndex) {
		for (;endIndex > beginIndex; --endIndex) {
			byte c = data[endIndex-1];
			
			if (c != SP && c != HT) {
				break;
			}
		}
		return new String(data, beginIndex, endIndex-beginIndex, StandardCharsets.US_ASCII);
	}

	public static String ascii(byte[] data, int beginIndex, int endIndex) {
		return new String(data, beginIndex, endIndex-beginIndex, StandardCharsets.US_ASCII);
	}
	
	public static Integer digits(byte[] data, int[] tokens, int tokenIndex) {
		int i = tokenIndex << 1;
		
		return digits(data, tokens[i], tokens[i+1]);
	}
	
	public static Integer digits(byte[] data, int beginIndex, int endIndex) {
		int num = 0;
		
		for (int i=beginIndex; i<endIndex; ++i) {
			byte c = data[i];
			
			if (c < DIGIT_0 || c > DIGIT_9) {
				return null;
			}
			num *= 10;
			num += c - DIGIT_0;
		}
		return num;
	}
	
	public static byte[] bytes(String text) {
		return text.getBytes(StandardCharsets.US_ASCII);
	}
	
	public static byte[] statusCode(int code) {
		byte[] bytes = new byte[STATUS_CODE_LENGTH];
		code &= 0xfff;
		
		for (int i=0; i<STATUS_CODE_LENGTH; ++i) {
			bytes[2-i] = (byte)((code % 10) + '0');
			code /= 10;
		}
		return bytes;
	}
	
	public static String values(String[] values) {
		StringBuilder sb = new StringBuilder();
		int i = 0, len = values.length;
		
		if (len > 0) {
			sb.append(values[i++]);
			for (; i<len; ++i) {
				sb.append(", ");
				sb.append(values[i]);
			}
		}
		return sb.toString();
	}
	
	public static List<String> values(String values) {
		return values(values, ",");
	}

	public static List<String> values(String values, String regex) {
		if (values.isEmpty()) {
			return Collections.emptyList();
		}
		
		String[] array = values.split(regex);
		ArrayList<String> list = new ArrayList<String>();
		
		for (String s: array) {
			s = s.trim();
			if (!s.isEmpty()) {
				list.add(s.trim());
			}
		}
		return list;
	}
}
