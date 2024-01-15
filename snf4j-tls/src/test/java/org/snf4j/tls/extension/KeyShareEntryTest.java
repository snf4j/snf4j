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
package org.snf4j.tls.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import static org.snf4j.tls.extension.NamedGroup.X448;
import static org.snf4j.tls.extension.NamedGroup.X25519;
import static org.snf4j.tls.extension.NamedGroup.SECP256R1;

import org.junit.Test;

public class KeyShareEntryTest {

	static KeyShareEntry[] entries(NamedGroup... groups) {
		KeyShareEntry[] entries = new KeyShareEntry[groups.length];
		
		for (int i=0; i<groups.length; ++i) {
			entries[i] = new KeyShareEntry(groups[i], new byte[i]);
		}
		return entries;
	}
	
	static NamedGroup[] groups(NamedGroup... groups) {
		return groups;
	}
	
	@Test
	public void testFind() {
		assertEquals(0, KeyShareEntry.find(entries(X448, X25519), X448).getRawKey().length);
		assertEquals(1, KeyShareEntry.find(entries(X448, X25519), X25519).getRawKey().length);
		assertEquals(0, KeyShareEntry.find(entries(X448), X448).getRawKey().length);
		
		assertNull(KeyShareEntry.find(entries(X448, X25519), SECP256R1));
	}

	@Test
	public void testFindMatch() {
		assertEquals(0, KeyShareEntry.findMatch(entries(X448, X25519), groups(X448,X25519)).getRawKey().length);
		assertEquals(1, KeyShareEntry.findMatch(entries(X448, X25519), groups(X25519,X448)).getRawKey().length);
		assertEquals(0, KeyShareEntry.findMatch(entries(X448, X25519), groups(SECP256R1,X448,X25519)).getRawKey().length);
		assertEquals(1, KeyShareEntry.findMatch(entries(X448, X25519), groups(SECP256R1,X25519,X448)).getRawKey().length);
	
		assertNull(KeyShareEntry.findMatch(entries(X448, X25519), groups(SECP256R1)));
		assertNull(KeyShareEntry.findMatch(entries(X448, X25519), groups()));
		assertNull(KeyShareEntry.findMatch(entries(), groups(SECP256R1)));
	}
	
}
