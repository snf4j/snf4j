/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SupportedGroupsExtension;

public class NewSessionTicketTest extends HandshakeTest {
	
	@Test
	public void testParseRealData() throws Alert {
		byte[] data = bytes(
				("04 00 00 d5 00 00 1c 20 00 00 00 00 08 00 00 00 00 00 00 00 00 00 c0 41"+
				"42 43 44 45 46 47 48 49 4a 4b 4c 4d 4e 4f 00 49 56 44 41 54 41 49 56 44"+
				"41 54 41 00 41 45 53 cb 11 9d 4d bd 2a 21 ec c2 26 a6 09 0e e8 ca 58 df"+
				"09 03 9b 35 96 f4 de 79 98 0e a3 25 d5 14 62 5c 0c 21 c5 0f 03 26 1d c4"+
				"2c e7 c5 97 0c 4c 01 ea 33 1c ff c8 99 66 ef 54 8b e4 df 9a 8b a4 38 5b"+
				"eb 86 80 fd 0b 78 df b8 e9 8e fc 8f cc d8 14 fe cd 1d 9b ce 89 ca 05 dc"+
				"28 c2 49 e5 bd 61 d0 3a 56 8f 9a 0a 46 fb fd 05 30 2d b6 b2 f7 a3 13 e3"+
				"32 67 bf 0b cb dc ec fb 04 a4 d8 2f 5a 69 45 1f 56 7a b5 19 9b b2 6c 5c"+
				"f2 00 72 f0 45 03 73 02 8f e0 71 d4 f4 1d 8f 61 ae 02 4d 69 bb ae 4c 00 00")
				.replace(" ", ""));
		
		IHandshake h = NewSessionTicket.getParser().parse(array(data, 4), data.length-4, ExtensionDecoder.DEFAULT);
		assertSame(HandshakeType.NEW_SESSION_TICKET, h.getType());

		NewSessionTicket ee = (NewSessionTicket) h;
		assertEquals(0, ee.getExtensions().size());
		assertEquals(0, ee.getAgeAdd());
		assertEquals(0x1c20, ee.getLifetime());
		assertArrayEquals(new byte[8], ee.getNonce());
		assertEquals(0xc0, ee.getTicket().length);
		assertArrayEquals(Arrays.copyOfRange(data, 23, 23+0xc0), ee.getTicket());

		assertEquals(data.length-4, ee.getDataLength());
		ee.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, ee.getLength());
		buffer.clear();
	}

	@Test
	public void testMinValues() throws Exception {
		NewSessionTicket nst = new NewSessionTicket(bytes(1), bytes(), 0, 0, new ArrayList<IExtension>());
		assertEquals(0, nst.getExtensions().size());
		assertEquals(0, nst.getAgeAdd());
		assertEquals(0, nst.getLifetime());
		assertArrayEquals(bytes(), nst.getNonce());
		assertArrayEquals(bytes(1), nst.getTicket());
		
		nst.getBytes(buffer);
		byte[] data = buffer();
		NewSessionTicket nst2 = (NewSessionTicket) NewSessionTicket.getParser().parse(
				array(data, 4), 
				data.length-4, 
				ExtensionDecoder.DEFAULT);
		assertEquals(0, nst2.getExtensions().size());
		assertEquals(0, nst2.getAgeAdd());
		assertEquals(0, nst2.getLifetime());
		assertArrayEquals(bytes(), nst2.getNonce());
		assertArrayEquals(bytes(1), nst2.getTicket());
	}
	
	@Test
	public void testWithExtensions() throws Exception {
		List<IExtension> extensions = new ArrayList<IExtension>();
		extensions.add(new SupportedGroupsExtension(NamedGroup.X25519, NamedGroup.X448));
		extensions.add(new ServerNameExtension());
		
		NewSessionTicket nst = new NewSessionTicket(bytes(1,2,3,4), bytes(5,6), 4000, 56, extensions);
		assertEquals(2, nst.getExtensions().size());
		assertEquals(56, nst.getAgeAdd());
		assertEquals(4000, nst.getLifetime());
		assertArrayEquals(bytes(5,6), nst.getNonce());
		assertArrayEquals(bytes(1,2,3,4), nst.getTicket());
		
		nst.getBytes(buffer);
		byte[] data = buffer();
		assertEquals(data.length-4, nst.getDataLength());
		
		NewSessionTicket nst2 = (NewSessionTicket) NewSessionTicket.getParser().parse(
				array(data, 4), 
				data.length-4, 
				ExtensionDecoder.DEFAULT);
		assertEquals(2, nst2.getExtensions().size());
		assertEquals(56, nst2.getAgeAdd());
		assertEquals(4000, nst2.getLifetime());
		assertArrayEquals(bytes(5,6), nst2.getNonce());
		assertArrayEquals(bytes(1,2,3,4), nst2.getTicket());
		
		buffer.clear();
		nst.getBytes(buffer);
		assertArrayEquals(data, buffer());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgument1() {
		new NewSessionTicket(null, bytes(), 0, 0, new ArrayList<IExtension>());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgument2() {
		new NewSessionTicket(bytes(), bytes(), 0, 0, new ArrayList<IExtension>());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgument3() {
		new NewSessionTicket(bytes(1), null, 0, 0, new ArrayList<IExtension>());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgument4() {
		new NewSessionTicket(bytes(1), bytes(), 0, 0, null);
	}
	
	@Test
	public void testParser() {
		assertSame(HandshakeType.NEW_SESSION_TICKET, NewSessionTicket.getParser().getType());
		assertSame(NewSessionTicket.getParser().getType(), NewSessionTicket.getParser().getType());
	}

	@Test
	public void testParsingFailures() throws Exception {
		List<IExtension> extensions = new ArrayList<IExtension>();
		extensions.add(new SupportedGroupsExtension(NamedGroup.X25519, NamedGroup.X448));
		extensions.add(new ServerNameExtension());

		NewSessionTicket nst = new NewSessionTicket(bytes(1,2,3,4), bytes(5,6), 4000, 56, extensions);
		nst.getBytes(buffer);
		buffer.put((byte) 255);
		byte[] bytes = buffer();
		
		for (int i=4; i<=bytes.length; ++i) {
			try {
				NewSessionTicket.getParser().parse(array(bytes, 4), i-4, ExtensionDecoder.DEFAULT);
				if (i != bytes.length-1) {
					fail();
				}
			} catch (DecodeErrorAlert e) {
				assertEquals("Handshake message 'new_session_ticket' parsing failure: Inconsistent length", e.getMessage());
			}
		}
		byte[] data = bytes(0,0,0,0,1,1,1,1,0,0,0,0,0); 
		try {
			NewSessionTicket.getParser().parse(array(data,0), data.length, ExtensionDecoder.DEFAULT);
		} catch (DecodeErrorAlert e) {
			assertEquals("Handshake message 'new_session_ticket' parsing failure: Empty ticket", e.getMessage());
		}
	}

}
