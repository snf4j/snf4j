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
package org.snf4j.websocket.extensions.compress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.websocket.extensions.InvalidExtensionException;
import org.snf4j.websocket.handshake.HandshakeUtilsTest;

public class PerMessageDeflateParamsTest {

	boolean upperCase;
	
	String format(String ext) {
		ext = ext.replace("EX", "permessage-deflate");
		ext = ext.replace("SN", "server_no_context_takeover");
		ext = ext.replace("CN", "client_no_context_takeover");
		ext = ext.replace("SX", "server_max_window_bits");
		ext = ext.replace("CX", "client_max_window_bits");
		return upperCase ? ext.toUpperCase(): ext;
	}
	
	void assertParse(String ext, boolean cn, boolean sn, Integer cx, Integer sx) throws Exception {
		PerMessageDeflateParams p = PerMessageDeflateParams.parse(HandshakeUtilsTest.parseExtension(format(ext)));
		assertEquals(cn, p.isClientNoContext());
		assertEquals(sn, p.isServerNoContext());
		assertEquals(cx, p.getClientMaxWindow());
		assertEquals(sx, p.getServerMaxWindow());
	}
	
	void assertParse(String ext, String exception) throws Exception {
		try {
			PerMessageDeflateParams.parse(HandshakeUtilsTest.parseExtension(format(ext)));
			fail();
		}
		catch (InvalidExtensionException e) {
			assertEquals(exception, e.getMessage());
		}
	}
	
	@Test
	public void testParse() throws Exception {
		for (int i=0; i<2; ++i) {
			assertParse("EX", false, false, null, null);
			assertParse("EX; SN", false, true, null, null);
			assertParse("EX; CN", true, false, null, null);
			assertParse("EX; SX", "Missing parameter value");
			assertParse("EX; SX=8", false, false, null, 8);
			assertParse("EX; CX", false, false, -1, null);
			assertParse("EX; CX=\"15\"", false, false, 15, null);
			assertParse("EX; xxx", "Unexpected parameter");
			upperCase = true;
		}
		upperCase = false;
		
		assertParse("EX;SN; CN  ;SX = 9; CX=10", true, true, 10, 9);
		assertParse("EX;SN;SX = 9; CX=10", false, true, 10, 9);
		assertParse("EX;SN;SX = 9; CX", false, true, -1, 9);
		
	}

	@Test
	public void testParseDuplicateParam() throws Exception {
		assertParse("EX; SN; CN; SX=9; CX=10; SN", "Duplicated parameter");
		assertParse("EX; SN; CN; SX=9; CX=10; CN", "Duplicated parameter");
		assertParse("EX; SN; CN; SX=9; CX=10; SX=8", "Duplicated parameter");
		assertParse("EX; SN; CN; SX=9; CX=10; CX=8", "Duplicated parameter");
	}
	
	@Test
	public void testParseInvalidValue() throws Exception {
		assertParse("EX; CX=x", "Invalid parameter value format");
		assertParse("EX; CX=7", "Invalid parameter value");
		assertParse("EX; CX=16", "Invalid parameter value");
		assertParse("EX; CX=0", "Invalid parameter value");
		assertParse("EX; CX=-1", "Invalid parameter value");
		assertParse("EX; CX=", "Invalid parameter value format");
		assertParse("EX; SN=5", "Unnecessary parameter value");
		assertParse("EX; SN=", "Unnecessary parameter value");
	}
}
