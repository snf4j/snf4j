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

import java.nio.ByteBuffer;

import org.snf4j.tls.Args;

abstract class AbstractHello extends AbstractHandshake {

	final static int RANDOM_LENGTH = 32;
	
	final static int SESSION_ID_MAX_LENGTH = 32;

	final static int COMMON_PART_MIN_LENGTH = 2 + RANDOM_LENGTH + 1;

	private final int legacyVersion;
	
	private final byte[] random;
	
	private final byte[] legacySessionId;
	
	public AbstractHello(HandshakeType type, int legacyVersion, byte[] random, byte[] legacySessionId) {
		super(type);
		Args.checkFixed(random, RANDOM_LENGTH, "random");
		Args.checkMax(legacySessionId, SESSION_ID_MAX_LENGTH, "legacySessionId");
		this.legacyVersion = legacyVersion;
		this.random = random;
		this.legacySessionId = legacySessionId;
	}

	public int getLegacyVersion() {
		return legacyVersion;
	}
	
	public byte[] getRandom() {
		return random;
	}

	public byte[] getLegacySessionId() {
		return legacySessionId;
	}
	
	@Override
	public int getDataLength() {
		return COMMON_PART_MIN_LENGTH + legacySessionId.length;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.putShort((short) legacyVersion);
		buffer.put(random);
		buffer.put((byte) legacySessionId.length);
		if (legacySessionId.length > 0) {
			buffer.put(legacySessionId);
		}
	}
}
