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

import org.junit.Test;
import org.snf4j.tls.IntConstantTester;
import org.snf4j.tls.crypto.IDHKeyExchange;
import org.snf4j.tls.crypto.IECKeyExchange;
import org.snf4j.tls.crypto.IXDHKeyExchange;

public class NamedGroupTest {
	
	final static String ENTRIES = 
			"|" + "secp256r1(0x0017), " +
			"|" + "secp384r1(0x0018), " +
			"|" + "secp521r1(0x0019), " +
			"|" + "x25519(0x001D), " +
			"|" + "x448(0x001E), " +
			"|" + "ffdhe2048(0x0100), " + 
			"|" + "ffdhe3072(0x0101), " + 
			"|" + "ffdhe4096(0x0102), " +
			"|" + "ffdhe6144(0x0103), " + 
			"|" + "ffdhe8192(0x0104),";
	
	@Test
	public void testValues() throws Exception {
		new IntConstantTester<NamedGroup>(ENTRIES, NamedGroup.class, NamedGroup[].class).assertValues("0x%04X");
	}
			 
	@Test
	public void testOf() throws Exception {
		new IntConstantTester<NamedGroup>(ENTRIES, NamedGroup.class, NamedGroup[].class).assertOf(0, 0x200);
	}

	@Test
	public void testSpecifications() {
		IDHKeyExchange ex;
		
		ex = (IDHKeyExchange) NamedGroup.FFDHE2048.spec().getKeyExchange();
		assertEquals("FFDHE2048".toLowerCase(), ex.getAlgorithm());
		assertEquals(256, ex.getPLength());
		
		ex = (IDHKeyExchange) NamedGroup.FFDHE3072.spec().getKeyExchange();
		assertEquals("FFDHE3072".toLowerCase(), ex.getAlgorithm());
		assertEquals(384, ex.getPLength());
		
		ex = (IDHKeyExchange) NamedGroup.FFDHE4096.spec().getKeyExchange();
		assertEquals("FFDHE4096".toLowerCase(), ex.getAlgorithm());
		assertEquals(512, ex.getPLength());

		ex = (IDHKeyExchange) NamedGroup.FFDHE6144.spec().getKeyExchange();
		assertEquals("FFDHE6144".toLowerCase(), ex.getAlgorithm());
		assertEquals(768, ex.getPLength());

		ex = (IDHKeyExchange) NamedGroup.FFDHE8192.spec().getKeyExchange();
		assertEquals("FFDHE8192".toLowerCase(), ex.getAlgorithm());
		assertEquals(1024, ex.getPLength());
		
		IECKeyExchange ex2;

		ex2 = (IECKeyExchange) NamedGroup.SECP256R1.spec().getKeyExchange();
		assertEquals("SECP256R1".toLowerCase(), ex2.getAlgorithm());
		
		ex2 = (IECKeyExchange) NamedGroup.SECP384R1.spec().getKeyExchange();
		assertEquals("SECP384R1".toLowerCase(), ex2.getAlgorithm());

		ex2 = (IECKeyExchange) NamedGroup.SECP521R1.spec().getKeyExchange();
		assertEquals("SECP521R1".toLowerCase(), ex2.getAlgorithm());
		
		IXDHKeyExchange ex3;

		ex3 = (IXDHKeyExchange) NamedGroup.X25519.spec().getKeyExchange();
		assertEquals("X25519", ex3.getAlgorithm());
		
		ex3 = (IXDHKeyExchange) NamedGroup.X448.spec().getKeyExchange();
		assertEquals("X448", ex3.getAlgorithm());
	}
	
}
