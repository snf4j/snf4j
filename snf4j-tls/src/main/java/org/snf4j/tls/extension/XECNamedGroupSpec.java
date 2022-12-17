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

import java.nio.ByteBuffer;
import java.security.PublicKey;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.crypto.IKeyExchange;

public class XECNamedGroupSpec extends AbstractNamedGroupSpec {
	
	private final int contentLength;
	
	private final static boolean IMPLEMENTED;
	
	static {
		boolean implemented;
		try {
			Class.forName("java.security.interfaces.XECPublicKey");
			implemented = true;
		} catch (ClassNotFoundException e) {
			implemented = false;
		}
		IMPLEMENTED = implemented;
	}
	
	@Override
	public boolean isImplemented() {
		return IMPLEMENTED;
	}

	public XECNamedGroupSpec(int contentLength) {
		this.contentLength = contentLength;
	}

	@Override
	public ParsedKey parse(ByteBufferArray srcs, int remaining) {
		return null;
	}

	@Override
	public PublicKey generateKey(ParsedKey key) throws AlertException {
		return null;
	}
	
	@Override
	public int getDataLength() {
		return contentLength;
	}

	@Override
	public void getData(ByteBuffer buffer, PublicKey key) {
	}

	@Override
	public void getData(ByteBuffer buffer, ParsedKey key) {
	}

	@Override
	public IKeyExchange getKeyExchange() {
		return null;
	}

}
