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
package org.snf4j.quic.frame;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;

public class AckFrameTest extends CommonTest {
	
	final static byte TYPE = 2;
	
	final static byte ECN_TYPE = 3;
	
	static final IFrameParser parser = AckFrame.getParser();
	
	AckFrame parse(String data, int remaining, boolean ecn) throws Exception {
		buffer(data);
		assertEquals(ecn ? ECN_TYPE : TYPE, buffer.get());
		return parser.parse(
			buffer,
			remaining == -1 ? buffer.remaining() : remaining,
			ecn ? ECN_TYPE : TYPE);
	}

	AckRange[] ranges(int... vals) {
		AckRange[] ranges = new AckRange[vals.length/2];
		
		for (int i=0; i<ranges.length; ++i) {
			ranges[i] = new AckRange(vals[i*2], vals[i*2+1]);
		}
		return ranges;
	}

	AckRange[] rangesByCount(long from, int count) {
		AckRange[] ranges = new AckRange[count];
		
		for (int i=0; i<ranges.length; ++i) {
			ranges[i] = new AckRange(from);
			from -= 2;
		}
		return ranges;
	}
	
	static void assertRanges(AckFrame f, long... values) {
		int len = values.length/2;
		assertTrue((values.length & 0x01) == 0);
		assertEquals(len, f.getRanges().length);
		for (int i=0; i<len; ++i) {
			AckRange range = f.getRanges()[i];
			
			assertEquals("from", values[i*2], range.getFrom());
			assertEquals("to", values[i*2+1], range.getTo());
		}
	}
	
	@Test
	public void testGetType() {
		assertEquals(0x02, FrameType.ACK.firstValue());
		assertEquals(0x03, FrameType.ACK.lastValue());
		assertSame(FrameType.ACK, new AckFrame(10, 10).getType());
		assertSame(FrameType.ACK, AckFrame.getParser().getType());
	}
	
	@Test
	public void testGetLength() {
		//largest acked, dalay
		assertEquals(5, new AckFrame(63, 63).getLength());
		assertEquals(6, new AckFrame(64, 63).getLength());
		assertEquals(6, new AckFrame(63, 64).getLength());
		assertEquals(7, new AckFrame(64, 64).getLength());
		
		//ranges
		assertEquals(5, new AckFrame(ranges(63,63), 63).getLength());
		assertEquals(7, new AckFrame(ranges(63,63, 61,61), 63).getLength());
		assertEquals(9, new AckFrame(ranges(63,63, 61,61, 59,59), 63).getLength());
		assertEquals(11, new AckFrame(ranges(1000,900, 800,700), 63).getLength());
		assertEquals(15, new AckFrame(ranges(1000,900, 800,700, 600,500), 63).getLength());
		
		//range count
		assertEquals(6, new AckFrame(rangesByCount(1000,1), 63).getLength());
		assertEquals(130, new AckFrame(rangesByCount(1000,63), 63).getLength());
		assertEquals(132, new AckFrame(rangesByCount(1000,64), 63).getLength());
		assertEquals(135, new AckFrame(rangesByCount(1000,65), 63).getLength());
	}

	@Test
	public void testGetLengthEcn() {
		assertEquals(8, new EcnAckFrame(63, 63, 63, 63, 63).getLength());
		assertEquals(9, new EcnAckFrame(63, 63, 64, 63, 63).getLength());
		assertEquals(9, new EcnAckFrame(63, 63, 63, 64, 63).getLength());
		assertEquals(9, new EcnAckFrame(63, 63, 63, 63, 64).getLength());
		assertEquals(11, new EcnAckFrame(63, 63, 64, 64, 64).getLength());
	}
	
	@Test
	public void testParse() throws Exception {

		//largest acked, dalay
		AckFrame f = parse("02 10 11 00 00", -1, false);
		assertEquals(17, f.getDelay());
		assertRanges(f, 16,16);
		assertEquals(5, f.getLength());
		assertEquals(0, buffer.remaining());
		assertFalse(f.hasEcnCounts());
		assertEquals(-1, f.getEct0Count());
		assertEquals(-1, f.getEct1Count());
		assertEquals(-1, f.getEcnCeCount());
		f = parse("02 3f 11 00 00", -1, false);
		assertEquals(17, f.getDelay());
		assertRanges(f, 63,63);
		assertEquals(5, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 3e 3f 00 00", -1, false);
		assertEquals(63, f.getDelay());
		assertRanges(f, 62,62);
		assertEquals(5, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 4040 3f 00 00", -1, false);
		assertEquals(63, f.getDelay());
		assertRanges(f, 64,64);
		assertEquals(6, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 3f 4040 00 00", -1, false);
		assertEquals(64, f.getDelay());
		assertRanges(f, 63,63);
		assertEquals(6, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 4041 4040 00 00", -1, false);
		assertEquals(64, f.getDelay());
		assertRanges(f, 65,65);
		assertEquals(7, f.getLength());
		assertEquals(0, buffer.remaining());
	
		//first range
		f = parse("02 4100 01 00 3f", -1, false);
		assertEquals(1, f.getDelay());
		assertRanges(f, 256,193);
		assertEquals(6, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 4100 01 00 4040", -1, false);
		assertEquals(1, f.getDelay());
		assertRanges(f, 256,192);
		assertEquals(7, f.getLength());
		assertEquals(0, buffer.remaining());
		
		//second range
		f = parse("02 4100 01 01 00 02 03", -1, false);
		assertEquals(1, f.getDelay());
		assertRanges(f, 256,256, 252,249);
		assertEquals(8, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 4100 01 01 00 3f 3f", -1, false);
		assertEquals(1, f.getDelay());
		assertRanges(f, 256,256, 191,128);
		assertEquals(8, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 4100 01 01 00 4040 3f", -1, false);
		assertEquals(1, f.getDelay());
		assertRanges(f, 256,256, 190,127);
		assertEquals(9, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 4100 01 01 00 3f 4040", -1, false);
		assertEquals(1, f.getDelay());
		assertRanges(f, 256,256, 191,127);
		assertEquals(9, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 4100 01 01 00 4041 4040", -1, false);
		assertEquals(1, f.getDelay());
		assertRanges(f, 256,256, 189,125);
		assertEquals(10, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 4100 01 02 00 4041 4040 02 01", -1, false);
		assertEquals(1, f.getDelay());
		assertRanges(f, 256,256, 189,125, 121,120);
		assertEquals(12, f.getLength());
		assertEquals(0, buffer.remaining());
		
		//range count
		f = parse("02 7fff 01 02 00 0000 0000", -1, false);
		assertEquals(1, f.getDelay());
		assertRanges(f, 16383,16383, 16381,16381, 16379,16379);
		assertEquals(10, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 7fff 01 3f 00 " + hex(new byte[63*2]), -1, false);
		assertEquals(1, f.getDelay());
		assertEquals(64, f.getRanges().length);
		assertEquals(6+2*63, f.getLength());
		assertEquals(0, buffer.remaining());
		f = parse("02 7fff 01 4040 00 " + hex(new byte[64*2]), -1, false);
		assertEquals(1, f.getDelay());
		assertEquals(65, f.getRanges().length);
		assertEquals(7+2*64, f.getLength());
		assertEquals(0, buffer.remaining());		
	}
	
	@Test
	public void testParseEcn() throws Exception {
		AckFrame f = parse("03 10 11 00 00 3f 3e 3d", -1, true);
		assertEquals(17, f.getDelay());
		assertRanges(f, 16,16);
		assertEquals(8, f.getLength());
		assertEquals(0, buffer.remaining());
		assertTrue(f.hasEcnCounts());
		assertEquals(63, f.getEct0Count());
		assertEquals(62, f.getEct1Count());
		assertEquals(61, f.getEcnCeCount());

		f = parse("03 10 11 00 00 4040 4041 4042", -1, true);
		assertEquals(17, f.getDelay());
		assertRanges(f, 16,16);
		assertEquals(11, f.getLength());
		assertEquals(0, buffer.remaining());
		assertTrue(f.hasEcnCounts());
		assertEquals(64, f.getEct0Count());
		assertEquals(65, f.getEct1Count());
		assertEquals(66, f.getEcnCeCount());
	}	
	
	@Test
	public void testGetBytes() {

		//largest acked, dalay
		AckFrame f = new AckFrame(63, 62);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 3f 3e 00 00"), bytesAndClear());
		f = new AckFrame(ranges(63,63), 62);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 3f 3e 00 00"), bytesAndClear());
		f = new AckFrame(ranges(63,62), 62);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 3f 3e 00 01"), bytesAndClear());
		f = new AckFrame(ranges(64,62), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4040 3f 00 02"), bytesAndClear());
		f = new AckFrame(ranges(63,62), 64);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 3f 4040 00 01"), bytesAndClear());
		f = new AckFrame(64, 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4040 3f 00 00"), bytesAndClear());
		f = new AckFrame(63, 64);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 3f 4040 00 00"), bytesAndClear());

		//first range
		f = new AckFrame(ranges(256,193), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 00 3f"), bytesAndClear());
		f = new AckFrame(ranges(256,192), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 00 4040"), bytesAndClear());
		
		//second range
		f = new AckFrame(ranges(256,256, 191,191), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 01 00 3f 00"), bytesAndClear());
		f = new AckFrame(ranges(256,256, 190,190), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 01 00 4040 00"), bytesAndClear());
		f = new AckFrame(ranges(256,256, 190,127), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 01 00 4040 3f"), bytesAndClear());
		f = new AckFrame(ranges(256,256, 190,126), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 01 00 4040 4040"), bytesAndClear());
		f = new AckFrame(ranges(256,256, 190,126, 124,123), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 02 00 4040 4040 00 01"), bytesAndClear());
		
		//range count
		f = new AckFrame(ranges(256,256, 254,254, 252,252), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 02 00 00 00 00 00"), bytesAndClear());
		f = new AckFrame(rangesByCount(256,3), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 02 00 00 00 00 00"), bytesAndClear());
		f = new AckFrame(rangesByCount(256,64), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 3f 00 " + hex(new byte[63*2])), bytesAndClear());
		f = new AckFrame(rangesByCount(256,65), 63);
		f.getBytes(buffer);
		assertArrayEquals(bytes("02 4100 3f 4040 00 " + hex(new byte[64*2])), bytesAndClear());
	}
	
	@Test
	public void testGetBytesEcn() {
		AckFrame f = new EcnAckFrame(63, 62, 1, 2, 3);
		f.getBytes(buffer);
		assertArrayEquals(bytes("03 3f 3e 00 00 01 02 03"), bytesAndClear());
		f = new EcnAckFrame(63, 62, 64, 2, 3);
		f.getBytes(buffer);
		assertArrayEquals(bytes("03 3f 3e 00 00 4040 02 03"), bytesAndClear());
		f = new EcnAckFrame(63, 62, 63, 64, 3);
		f.getBytes(buffer);
		assertArrayEquals(bytes("03 3f 3e 00 00 3f 4040 03"), bytesAndClear());
		f = new EcnAckFrame(63, 62, 63, 63, 64);
		f.getBytes(buffer);
		assertArrayEquals(bytes("03 3f 3e 00 00 3f 3f 4040"), bytesAndClear());
		f = new EcnAckFrame(63, 62, 64, 65, 66);
		f.getBytes(buffer);
		assertArrayEquals(bytes("03 3f 3e 00 00 4040 4041 4042"), bytesAndClear());
	}
	
	String assertFailure(String data, int remaining, boolean ecn) throws Exception {
		try {
			parse(data, remaining, ecn);
			fail("Remaining " + remaining);
		} 
		catch (QuicException e) {
			assertSame(TransportError.FRAME_ENCODING_ERROR, e.getTransportError());
			return e.getMessage();
		}
		return null;
	}

	@Test
	public void testParseFailure() throws Exception {
		String data = "02 3f 3e 02 00 10 00 08 00";
		int len = bytes(data).length-1;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i, false);
		}
		
		data = "02 3f 3e 40 00 00";
		len = bytes(data).length - 1;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i, false);
		}

		data = "02 3f 3e 01 00 40 00 00";
		len = bytes(data).length - 1;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i, false);
		}

		data = "02 3f 3e ffffffffffffffff 00 40 00 00";
		len = bytes(data).length - 1;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i, false);
		}
		
		data = "03 3f 3e 40 00 00 01 02 03";
		len = bytes(data).length - 1;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i, true);
		}

		data = "03 3f 3e 40 00 00 01 40 02 03";
		len = bytes(data).length - 1;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i, true);
		}

		data = "03 3f 3e 40 00 00 40 01 40 02 40 03";
		len = bytes(data).length - 1;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i, true);
		}
		
		//negative
		String msg = "Negative packet number in Ack range";
		assertEquals(msg, assertFailure("02 10 11 00 11", 5, false));
		assertEquals(msg, assertFailure("02 10 11 01 00 0f 00", 7, false));
		assertEquals(msg, assertFailure("02 10 11 01 00 08 07", 7, false));
	}
	
}
