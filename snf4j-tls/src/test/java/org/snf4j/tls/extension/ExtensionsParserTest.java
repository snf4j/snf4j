/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.tls.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.snf4j.tls.alert.DecodeErrorAlertException;

public class ExtensionsParserTest extends ExtensionTest {
	
	ExtensionsParser parser;
	
	final ExtensionDecoder decoder = new ExtensionDecoder();
	
	@Override
	public void before() {
		super.before();
		decoder.clearParsers();
		decoder.addParser(ServerNameExtension.getParser());
		parser = new ExtensionsParser(0, 0xffff, decoder);
	}
	
	@Test
	public void testParseRealData() throws DecodeErrorAlertException {
		byte[] data = new byte[] {
				0x00, (byte)0xa3, 0x00, 0x00, 0x00, 0x18, 0x00, 0x16, 0x00, 0x00, 0x13, 0x65, 0x78, 0x61, 
				0x6d, 0x70, 0x6c, 0x65, 0x2e, 0x75, 0x6c, 0x66, 0x68, 0x65, 0x69, 0x6d, 0x2e, 0x6e, 0x65, 
				0x74, 0x00, 0x0b, 0x00, 0x04, 0x03, 0x00, 0x01, 0x02, 0x00, 0x0a, 0x00, 0x16, 0x00, 0x14, 
				0x00, 0x1d, 0x00, 0x17, 0x00, 0x1e, 0x00, 0x19, 0x00, 0x18, 0x01, 0x00, 0x01, 0x01, 0x01, 
				0x02, 0x01, 0x03, 0x01, 0x04, 0x00, 0x23, 0x00, 0x00, 0x00, 0x16, 0x00, 0x00, 0x00, 0x17, 
				0x00, 0x00, 0x00, 0x0d, 0x00, 0x1e, 0x00, 0x1c, 0x04, 0x03, 0x05, 0x03, 0x06, 0x03, 0x08, 
				0x07, 0x08, 0x08, 0x08, 0x09, 0x08, 0x0a, 0x08, 0x0b, 0x08, 0x04, 0x08, 0x05, 0x08, 0x06, 
				0x04, 0x01, 0x05, 0x01, 0x06, 0x01, 0x00, 0x2b, 0x00, 0x03, 0x02, 0x03, 0x04, 0x00, 0x2d, 
				0x00, 0x02, 0x01, 0x01, 0x00, 0x33, 0x00, 0x26, 0x00, 0x24, 0x00, 0x1d, 0x00, 0x20, 0x35, 
				(byte)0x80,	0x72, (byte)0xd6, 0x36, 0x58, (byte)0x80, (byte)0xd1, (byte)0xae, (byte)0xea, 
				0x32, (byte)0x9a, (byte)0xdf, (byte)0x91, 0x21, 0x38, 0x38,	0x51, (byte)0xed, 0x21, 
				(byte)0xa2, (byte)0x8e, 0x3b, 0x75, (byte)0xe9, 0x65, (byte)0xd0, (byte)0xd2, (byte)0xcd, 
				0x16, 0x62, 0x54				
		};
		
		int[] sizes = new int[0];
		
		for (int i=0; i<2; ++i) {
			parser.parse(array(data, 0, sizes));
			assertTrue(parser.isComplete());
			assertEquals(10, parser.getExtensions().size());
			assertSame(ExtensionType.SERVER_NAME, parser.getExtensions().get(0).getType());
			assertEquals(11, parser.getExtensions().get(1).getType().value());
			assertSame(ExtensionType.SUPPORTED_GROUPS, parser.getExtensions().get(2).getType());
			assertEquals(35, parser.getExtensions().get(3).getType().value());
			assertEquals(22, parser.getExtensions().get(4).getType().value());
			assertEquals(23, parser.getExtensions().get(5).getType().value());
			assertSame(ExtensionType.SIGNATURE_ALGORITHMS, parser.getExtensions().get(6).getType());
			assertSame(ExtensionType.SUPPORTED_VERSIONS, parser.getExtensions().get(7).getType());
			assertSame(ExtensionType.PSK_KEY_EXCHANGE_MODES, parser.getExtensions().get(8).getType());
			assertSame(ExtensionType.KEY_SHARE, parser.getExtensions().get(9).getType());
			parser.reset();
			sizes = new int[data.length-1];
			Arrays.fill(sizes, 1);
		}
	}
}
