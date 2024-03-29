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

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.snf4j.tls.handshake.IHandshake;

public final class ExtensionsUtil {
	
	private ExtensionsUtil() {}
	
	public static int calculateLength(List<IExtension> extensions) {
		int len = extensions.size() * (2 + 2);
		
		for (IExtension e: extensions) {
			len += e.getDataLength();
		}
		return len;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends IExtension> T find(IHandshake handshake, ExtensionType type) {
		int value = type.value();
		
		for (IExtension e: handshake.getExtensions()) {
			if (e.getType().value() == value) {
				return (T) e;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends IExtension> T findLast(IHandshake handshake) {
		int index = handshake.getExtensions().size() - 1;
		
		if (index >= 0) {
			return (T) handshake.getExtensions().get(index);
		}
		return null;
	}
	
	public static IExtension findAnyMultiple(List<IExtension> extensions) {
		BitSet existing = new BitSet(256);
		Set<Integer> existing2 = null;
		boolean multipleFound = false;
		int size = existing.size();
		
		for (IExtension extension: extensions) {
			int id = extension.getType().value();
			
			if (id < size) {
				multipleFound |= existing.get(id);
				existing.set(id);
			}
			else {
				if (existing2 == null) {
					existing2 = new HashSet<Integer>();
				}
				multipleFound |= existing2.contains(id);
				existing2.add(id);
			}
			if (multipleFound) {
				return extension;
			}		
		}
		return null;
	}
}
