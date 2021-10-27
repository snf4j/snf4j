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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class HttpUtilsTest {
	
	byte[] bytes;
	
	byte[] bytes(String s, int off, int padding) {
		ByteBuffer buf = ByteBuffer.allocate(1000);
		
		for (int i=0; i<off; ++i) {
			buf.put((byte)0);
		}
		
		for (int i=0; i<s.length(); ++i) {
			char c = s.charAt(i);
			
			if (c == '|') {
				buf.put((byte) 13);
				buf.put((byte) 10);
			}
			else {
				buf.put((byte) c);
			}
		}

		for (int i=0; i<padding; ++i) {
			buf.put((byte)0);
		}
		
		buf.flip();
		bytes = new byte[buf.remaining()];
		buf.get(bytes);
		return bytes;
	}
	
	byte[] bytes(String s) {
		return bytes(s,0,0);
	}
	
	void assertLines(String s, int[] lines, int off, int count) {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i+1<lines.length; i+=2) {
			if (count != -1 && count-- == 0) {
				break;
			}
			int i0 = lines[i];
			int i1 = lines[i+1];
			
			if (i0 == -1) {
				break;
			}
			sb.append(new String(bytes, i0+off, i1-i0));
			sb.append(',');
		}
		for (int i=0; i<lines.length; ++i) {
			lines[i] = 0;
		}
		assertEquals(s, sb.toString());
	}
	
	void assertAvailable(String bytes, int expected, String lines) {
		int[] l = new int[100];
		
		byte[] b = bytes(bytes);
		assertEquals(expected, HttpUtils.available(b, 0, b.length, l));
		assertLines(lines, l, 0, -1);
		b = bytes(bytes,3,5);
		assertEquals(expected, HttpUtils.available(b, 3, b.length-3-5, l));
		assertLines(lines, l, 3, -1);
	}
	
	@Test
	public void testAvailable() {
		assertAvailable("",0,"");
		assertAvailable("x",0,"");
		assertAvailable("xx",0,"");
		assertAvailable("\n",0,"");
		assertAvailable("\r",0,"");
		assertAvailable("\r\n",0,",");
		assertAvailable("1|22|333|",0,"1,22,333,");
		assertAvailable("1|22|333|4444",0,"1,22,333,");
		
		assertAvailable("||",4,",");
		assertAvailable("||ccc",4,",");
		
		assertAvailable("xxx|yy||",11,"xxx,yy,");
		assertAvailable("xxx|yy||6",11,"xxx,yy,");
		assertAvailable("xxx|yy||86",11,"xxx,yy,");
		assertAvailable("xxx|yy||86|",11,"xxx,yy,");
		assertAvailable("xxx|yy|||",11,"xxx,yy,");
		
		byte[] b = bytes = bytes("x|yy|zzz||");
		int[] lines = new int[0];
		assertEquals(0, HttpUtils.available(b, 0, b.length, lines));
		lines = new int[1];
		assertEquals(0, HttpUtils.available(b, 0, b.length, lines));
		assertEquals(-1,lines[0]);
		lines = new int[2];
		assertEquals(0, HttpUtils.available(b, 0, b.length, lines));
		assertEquals(-1,lines[0]);
		lines = new int[3];
		assertEquals(0, HttpUtils.available(b, 0, b.length, lines));
		assertLines("x,", lines, 0, -1);
		lines = new int[4];
		assertEquals(0, HttpUtils.available(b, 0, b.length, lines));
		assertLines("x,", lines, 0, -1);
		lines = new int[5];
		assertEquals(0, HttpUtils.available(b, 0, b.length, lines));
		assertLines("x,yy,", lines, 0, -1);
		lines = new int[6];
		assertEquals(0, HttpUtils.available(b, 0, b.length, lines));
		assertLines("x,yy,", lines, 0, -1);
		lines = new int[7];
		assertEquals(14, HttpUtils.available(b, 0, b.length, lines));
		assertLines("x,yy,zzz,", lines, 0, -1);
	}
	
	@Test
	public void testAvailableBuffer() {
		byte[] b = bytes("x|yy|zzz||");
		ByteBuffer bb = ByteBuffer.wrap(b);
		int[] lines = new int[100];
		
		assertEquals(14, HttpUtils.available(bb, true, lines));
		assertLines("x,yy,zzz,", lines, 0, -1);
		assertEquals(14, HttpUtils.available(bb, true, lines));
		assertLines("x,yy,zzz,", lines, 0, -1);
		b = bytes("x|yy|zzz||",3,5);
		bb = ByteBuffer.wrap(b, 3, 14);
		assertEquals(14, HttpUtils.available(bb, true, lines));
		assertLines("x,yy,zzz,", lines, 3, -1);
		assertEquals(14, HttpUtils.available(bb, true, lines));
		assertLines("x,yy,zzz,", lines, 3, -1);
		
		b = bytes("x|yy|zzz||");
		bb = ByteBuffer.allocate(100);
		bb.put(b);
		assertEquals(14, HttpUtils.available(bb, false, lines));
		assertLines("x,yy,zzz,", lines, 0, -1);
		assertEquals(14, HttpUtils.available(bb, false, lines));
		assertLines("x,yy,zzz,", lines, 0, -1);
		
		bb = ByteBuffer.allocateDirect(100);
		bb.put(b);
		assertEquals(14, HttpUtils.available(bb, false, lines));
		assertLines("x,yy,zzz,", lines, 0, -1);
		assertEquals(14, HttpUtils.available(bb, false, lines));
		assertLines("x,yy,zzz,", lines, 0, -1);
		bb.flip();
		assertEquals(14, HttpUtils.available(bb, true, lines));
		assertLines("x,yy,zzz,", lines, 0, -1);
		assertEquals(14, HttpUtils.available(bb, true, lines));
		assertLines("x,yy,zzz,", lines, 0, -1);
	}
	
	void assertSplitRequestLine(String bytes, int expected, String items) {
		int[] i = new int[100];
		
		this.bytes = bytes.getBytes();
		assertEquals(expected, HttpUtils.splitRequestLine(this.bytes, 0, this.bytes.length, i));
		assertLines(items, i, 0, expected);
		byte[] bytes2 = new byte[this.bytes.length+3+5];
		System.arraycopy(this.bytes, 0, bytes2, 3, bytes.length());
		int len = this.bytes.length;
		this.bytes = bytes2;
		assertEquals(expected, HttpUtils.splitRequestLine(bytes2, 3, 3+len, i));
		assertLines(items, i, 0, expected);
	}
	
	@Test
	public void testSplitRequestLine() {
		assertSplitRequestLine("", 1, ",");
		assertSplitRequestLine(" ", 2, ",,");
		assertSplitRequestLine("  ", 2, ",,");
		
		assertSplitRequestLine("a", 1, "a,");
		assertSplitRequestLine("ab", 1, "ab,");
		assertSplitRequestLine("a ", 2, "a,,");
		assertSplitRequestLine("ab ", 2, "ab,,");

		assertSplitRequestLine(" a", 2, ",a,");
		assertSplitRequestLine(" ab", 2, ",ab,");
		assertSplitRequestLine(" a ", 3, ",a,,");
		assertSplitRequestLine(" ab ", 3, ",ab,,");
		assertSplitRequestLine("   ab     ", 3, ",ab,,");
		
		assertSplitRequestLine("a b", 2, "a,b,");
		assertSplitRequestLine("a  b", 2, "a,b,");
		assertSplitRequestLine("ac bd", 2, "ac,bd,");
		assertSplitRequestLine("ac  bd", 2, "ac,bd,");
		assertSplitRequestLine(" ac  bd ", 4, ",ac,bd,,");
		
		byte[] b = bytes("aa b2 b3|x1||");
		int[] lines = new int[100];
		assertEquals(16, HttpUtils.available(b, 0, b.length, lines));
		int[] items = new int[100];
		assertEquals(3, HttpUtils.splitRequestLine(b, lines[0], lines[1], items));
		assertLines("aa,b2,b3,", items, 0, 3);
		assertEquals(1, HttpUtils.splitRequestLine(b, lines[2], lines[3], items));
		assertLines("x1,", items, 0, 1);
		assertEquals(-1, lines[4]);
		
		items = new int[0];
		assertEquals(0, HttpUtils.splitRequestLine(b, lines[0], lines[1], items));
		items = new int[1];
		assertEquals(0, HttpUtils.splitRequestLine(b, lines[0], lines[1], items));
		items = new int[2];
		assertEquals(1, HttpUtils.splitRequestLine(b, lines[0], lines[1], items));
		assertLines("aa,", items, 0, 1);
		items = new int[3];
		assertEquals(1, HttpUtils.splitRequestLine(b, lines[0], lines[1], items));
		assertLines("aa,", items, 0, 1);
		items = new int[4];
		assertEquals(2, HttpUtils.splitRequestLine(b, lines[0], lines[1], items));
		assertLines("aa,b2,", items, 0, 2);
		
		b = bytes = " abc def ".getBytes();
		lines = new int[] {0,0,1,8};
		assertEquals(2, HttpUtils.splitRequestLine(b, lines, 1, items));
		assertLines("abc,def,", items, 0, 2);
	}
	
	@Test
	public void testSplitResponseLine() {
		byte[] b = bytes = " abc def ".getBytes();
		int[] lines = new int[] {0,0,1,8};
		int[] items = new int[100];
		
		assertEquals(2, HttpUtils.splitResponseLine(b, lines, 1, items));
		assertLines("abc,def,", items, 0, 2);
	}
	
	@Test
	public void testEquals() {
		byte[] b = "GET /chat HTTP/1.1".getBytes();
		int[] items = new int[100];

		assertEquals(3, HttpUtils.splitRequestLine(b, 0, b.length, items));
		assertTrue(HttpUtils.equals(b, items[0], items[1], "GET".getBytes()));
		assertTrue(HttpUtils.equals(b, items[2], items[3], "/chat".getBytes()));
		assertTrue(HttpUtils.equals(b, items, 1, "/chat".getBytes()));
		assertTrue(HttpUtils.equals(b, items[4], items[5], "HTTP/1.1".getBytes()));
		assertFalse(HttpUtils.equals(b, items[0], items[1], "GETx".getBytes()));
		assertFalse(HttpUtils.equals(b, items[0], items[1], "xGET".getBytes()));
		assertFalse(HttpUtils.equals(b, items[0], items[1], "xET".getBytes()));
		assertFalse(HttpUtils.equals(b, items[0], items[1], "GxT".getBytes()));
		assertFalse(HttpUtils.equals(b, items[0], items[1], "GEx".getBytes()));
	}
	
	@Test
	public void testDigits() {
		byte[] b = " 000 ".getBytes();
		int[] tokens = new int[] {1,4};
		
		assertEquals(0, HttpUtils.digits(b, tokens, 0).intValue());
		b = " 999 ".getBytes();
		assertEquals(999, HttpUtils.digits(b, tokens, 0).intValue());
		b = " 101 ".getBytes();
		assertEquals(101, HttpUtils.digits(b, tokens, 0).intValue());
	
		for (int i=0; i<'0'; ++i) {
			b[1] = (byte)i;
			assertNull(HttpUtils.digits(b, tokens, 0));
		}
		for (int i='9'+1; i<=0xff; ++i) {
			b[1] = (byte)i;
			assertNull(HttpUtils.digits(b, tokens, 0));
		}
		
	}
	
	@Test
	public void testBytes() {
		String s = "abcd" + (char)128 + (char)200;
		
		byte[] b = HttpUtils.bytes(s);
		assertArrayEquals(b, s.getBytes(StandardCharsets.US_ASCII));
	}
	
	@Test
	public void testAscii() {
		byte[] b = "GET /chat HTTP/1.1".getBytes();
		int[] items = new int[100];
		
		assertEquals(3, HttpUtils.splitRequestLine(b, 0, b.length, items));
		assertEquals("GET", HttpUtils.ascii(b, items[0], items[1]));
		
		b = new byte[256];
		for (int i=0; i<b.length; ++i) {
			b[i] = (byte)i;
		}
		assertEquals(new String(b, StandardCharsets.US_ASCII), HttpUtils.ascii(b, 0, b.length));
	}
	
	@Test
	public void testRtrimAscii() {
		byte[] b = bytes("name: value |name2:  \tvalue2 \t \t||");
		int[] l = new int[100];
		int[] t = new int[100];
		assertTrue(HttpUtils.available(b, 0, b.length, l) > 0);
		assertEquals(4, HttpUtils.splitHeaderField(b, l[0], l[1], t));
		assertEquals("value ", HttpUtils.ascii(b, t, 1));
		assertEquals("value", HttpUtils.rtrimAscii(b, t, 1));
		assertEquals(4, HttpUtils.splitHeaderField(b, l[2], l[3], t));
		assertEquals("value2 \t \t", HttpUtils.ascii(b, t, 1));
		assertEquals("value2", HttpUtils.rtrimAscii(b, t, 1));
		
		b = "".getBytes();
		assertEquals("", HttpUtils.ascii(b, 0, 0));
		assertEquals("", HttpUtils.rtrimAscii(b, 0, 0));
		b = " ".getBytes();
		assertEquals(" ", HttpUtils.ascii(b, 0, 1));
		assertEquals("", HttpUtils.rtrimAscii(b, 0, 1));
		b = "\t".getBytes();
		assertEquals("\t", HttpUtils.ascii(b, 0, 1));
		assertEquals("", HttpUtils.rtrimAscii(b, 0, 1));
		b = "\t ".getBytes();
		assertEquals("\t ", HttpUtils.ascii(b, 0, 2));
		assertEquals("", HttpUtils.rtrimAscii(b, 0, 2));
	}
	
	@Test
	public void testStatusCode() {
		assertEquals("101", new String(HttpUtils.statusCode(101)));
		assertEquals("001", new String(HttpUtils.statusCode(1)));
		assertEquals("000", new String(HttpUtils.statusCode(0)));
		assertEquals("000", new String(HttpUtils.statusCode(1000)));
		assertEquals("095", new String(HttpUtils.statusCode(-1)));
		assertEquals("999", new String(HttpUtils.statusCode(999)));
		assertEquals("097", new String(HttpUtils.statusCode(-999)));
		assertEquals("807", new String(HttpUtils.statusCode(9999)));
	}
	
	void assertField(String s, int result, int[] lines, int lineIndex) {
		int i = lineIndex*2;
		int[] f = new int[4];
		
		assertEquals(result, HttpUtils.splitHeaderField(bytes, lines[i], lines[i+1], f));
		assertLines(s, f, 0, 2);
	}
	
	@Test
	public void testSplitHeaderField() {
		byte[] b = bytes("name1: value1|name2:  value2|n3:\t \t\t   v3|:|name4|n5:||");
		int[] l = new int[100];
		int[] f = new int[4];
		assertTrue(HttpUtils.available(b, 0, b.length, l) > 0);

		assertField("name1,value1,", 4, l, 0);
		assertField("name2,value2,", 4, l, 1);
		assertField("n3,v3,", 4, l, 2);
		assertField(",,", 4, l, 3);
		assertField("n5,,", 4, l, 5);
		assertEquals(2, HttpUtils.splitHeaderField(bytes, l[8], l[9], f));
		assertLines("name4,", f, 0, 1);
		
		b = bytes(" name1: value1|\tname2:  value2| \t\t  n3:\t \t\t   v3| :| name4|  n5:|  \t||");
		assertTrue(HttpUtils.available(b, 0, b.length, l) > 0);

		assertField("name1,value1,", -4, l, 0);
		assertField("name2,value2,", -4, l, 1);
		assertField("n3,v3,", -4, l, 2);
		assertField(",,", -4, l, 3);
		assertField("n5,,", -4, l, 5);
		assertEquals(-2, HttpUtils.splitHeaderField(bytes, l[8], l[9], f));
		assertLines("name4,", f, 0, 1);
		assertEquals(-2, HttpUtils.splitHeaderField(bytes, l[12], l[13], f));
		assertLines(",", f, 0, 1);
	}
	
	List<String> values(String[] values) {
		ArrayList<String> list = new ArrayList<String>();
		
		for (String v: values) {
			list.add(v);
		}
		return list;
	}
	
	void assertValues(String s, List<String> list) {
		StringBuilder sb = new StringBuilder();
		
		for (String l: list) {
			sb.append(l);
			sb.append(',');
		}
		assertEquals(s, sb.toString());
	}
	
	@Test
	public void testValues() {
		assertEquals("", HttpUtils.values(new String[0]));
		assertEquals("val1", HttpUtils.values(new String[] {"val1"}));
		assertEquals("val1, val2", HttpUtils.values(new String[] {"val1", "val2"}));
		assertEquals("val1, val2, val3", HttpUtils.values(new String[] {"val1", "val2", "val3"})	);
		
		assertValues("", HttpUtils.values(""));
		assertValues("", HttpUtils.values(" "));
		assertValues("val,", HttpUtils.values("val"));
		assertValues("val,", HttpUtils.values(" val "));
		assertValues("val,", HttpUtils.values(" val, "));
		assertValues("val1,val2,", HttpUtils.values(" val1 , val2"));
	}
	
}
