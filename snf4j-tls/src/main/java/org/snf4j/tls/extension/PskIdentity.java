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

import java.nio.ByteBuffer;

import org.snf4j.tls.Args;

public class PskIdentity {
	
    private final byte[] identity;
    
    private final long obfuscatedTicketAge;

	public PskIdentity(byte[] identity, long obfuscatedTicketAge) {
		Args.checkNull(identity, "identity");
		this.identity = identity;
		this.obfuscatedTicketAge = obfuscatedTicketAge;
	}

	public byte[] getIdentity() {
		return identity;
	}

	public long getObfuscatedTicketAge() {
		return obfuscatedTicketAge;
	}
	
	public int getDataLength() {
		return 2 + 4 + identity.length;
	}
	
	public void getData(ByteBuffer buffer) {
		buffer.putShort((short) identity.length);
		buffer.put(identity);
		buffer.putInt((int) obfuscatedTicketAge);
	}
}
