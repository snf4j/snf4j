/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;

import org.junit.Test;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.handshake.ClientHello;

public class ExtensionsUtilTest {

	@Test
	public void testCalculateLength() {
		ArrayList<IExtension> extensions = new ArrayList<IExtension>();
		IExtension extension;
		
		assertEquals(0, ExtensionsUtil.calculateLength(extensions));
		extension = new UnknownExtension(ExtensionType.KEY_SHARE, new byte[10]);
		extensions.add(extension);
		assertEquals(10, extension.getDataLength());
		assertEquals(14, ExtensionsUtil.calculateLength(extensions));
		extensions.add(new UnknownExtension(ExtensionType.COOKIE, new byte[11]));
		assertEquals(29, ExtensionsUtil.calculateLength(extensions));
	}
	
	@Test
	public void testFind() {
		ArrayList<IExtension> extensions = new ArrayList<IExtension>();
		
		extensions.add(new ServerNameExtension("xxxx"));
		extensions.add(new UnknownExtension(ExtensionType.COOKIE, new byte[11]));
		extensions.add(new UnknownExtension(ExtensionType.KEY_SHARE, new byte[10]));
		ClientHello ch = new ClientHello(0x0303, new byte[32], new byte[10], new CipherSuite[2], new byte[0], extensions);
		
		ServerNameExtension e = ExtensionsUtil.find(ch, ExtensionType.SERVER_NAME);
		assertNotNull(e);
		IServerNameExtension e2 = ExtensionsUtil.find(ch, ExtensionType.SERVER_NAME);
		assertNotNull(e2);
		
		assertNull(ExtensionsUtil.find(ch, ExtensionType.HEARTBEAT));
		assertNotNull(ExtensionsUtil.find(ch, ExtensionType.COOKIE));
	}
}
