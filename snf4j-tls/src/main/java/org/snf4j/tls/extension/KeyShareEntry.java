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

import java.nio.ByteBuffer;
import java.security.PublicKey;

import org.snf4j.tls.Args;

public class KeyShareEntry {
	
	private final NamedGroup namedGroup;
	
	private final PublicKey key;
	
	private final ParsedKey parsedKey;
	
	private final byte[] rawKey;
	
	public KeyShareEntry(NamedGroup namedGroup, PublicKey key) {
		Args.checkNull(namedGroup, "namedGroup");
		Args.checkNull(key, "key");
		this.namedGroup = namedGroup;
		this.key = key;
		parsedKey = null;
		rawKey = null;
	}

	public KeyShareEntry(NamedGroup namedGroup, ParsedKey parsedKey) {
		Args.checkNull(namedGroup, "namedGroup");
		Args.checkNull(parsedKey, "parsedKey");
		this.namedGroup = namedGroup;
		key = null;
		this.parsedKey = parsedKey;
		rawKey = null;
	}
	
	public KeyShareEntry(NamedGroup namedGroup, byte[] rawKey) {
		Args.checkNull(namedGroup, "namedGroup");
		Args.checkNull(rawKey, "rawKey");
		this.namedGroup = namedGroup;
		key = null;
		parsedKey = null;
		this.rawKey = rawKey;
	}

	public NamedGroup getNamedGroup() {
		return namedGroup;
	}
	
	public PublicKey getKey() {
		return key;
	}

	public ParsedKey getParsedKey() {
		return parsedKey;
	}
	
	public byte[] getRawKey() {
		return rawKey;
	}
	
	public int getDataLength() {
		return 2 + 2 + namedGroup.spec().getDataLength();
	}
	
	public void getData(ByteBuffer buffer) {
		buffer.putShort((short) namedGroup.value());
		buffer.putShort((short) namedGroup.spec().getDataLength());
		if (key != null) {
			namedGroup.spec().getData(buffer, key);
		}
		else if (parsedKey != null) {
			namedGroup.spec().getData(buffer, parsedKey);
		}
		else {
			buffer.put(rawKey);
		}
	}
	
	public static KeyShareEntry find(KeyShareEntry[] entries, NamedGroup group) {
		int value = group.value();
		
		for (KeyShareEntry e: entries) {
			if (e.namedGroup.value() == value) {
				return e;
			}
		}
		return null;
	}
	
	public static KeyShareEntry findMatch(KeyShareEntry[] entries, NamedGroup[] groups) {
		for (NamedGroup g: groups) {
			int value = g.value();

			for (KeyShareEntry e: entries) {
				if (e.namedGroup.value() == value) {
					return e;
				}
			}
		}
		return null;
	}
	
}
