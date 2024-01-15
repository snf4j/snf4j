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
package org.snf4j.tls.handshake;

import org.junit.Test;
import org.snf4j.tls.IntConstantTester;

public class HandshakeTypeTest {
	
	final static String ENTRIES = 
			"|" + "client_hello(1)," +
			"|" + "server_hello(2)," +
			"|" + "new_session_ticket(4)," +
			"|" + "end_of_early_data(5)," +
			"|" + "encrypted_extensions(8)," +
			"|" + "certificate(11)," +
			"|" + "certificate_request(13)," +
			"|" + "certificate_verify(15)," +
			"|" + "finished(20)," +
			"|" + "key_update(24)," +
			"|" + "message_hash(254),";
	
	@Test
	public void testValues() throws Exception {
		new IntConstantTester<HandshakeType>(ENTRIES, HandshakeType.class, HandshakeType[].class).assertValues();
	}
			 
	@Test
	public void testOf() throws Exception {
		new IntConstantTester<HandshakeType>(ENTRIES, HandshakeType.class, HandshakeType[].class).assertOf();
	}
}
