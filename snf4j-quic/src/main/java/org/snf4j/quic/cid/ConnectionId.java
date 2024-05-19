/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.cid;

import org.snf4j.tls.Args;

/**
 * An object representing the QUIC connection id.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ConnectionId {

	private final int sequenceNumber;
	
	private final byte[] id;

	private final byte[] resetToken;
	
	private final long code;
	
	/**
	 * Constructs a connection id with the given sequence number, connection id
	 * and stateless reset token.
	 * 
	 * @param sequenceNumber the sequence number
	 * @param id             the connection id
	 * @param resetToken     the stateless reset token, or {@code null} if no token
	 *                       should be used with the conection id
	 */
	public ConnectionId(int sequenceNumber, byte[] id, byte[] resetToken) {
		if (resetToken != null) {
			Args.checkFixed(resetToken, 16, "resetToken");
		}
		this.sequenceNumber = sequenceNumber;
		this.id = id;
		this.resetToken = resetToken;
		
		long code = 0;
		int len = Math.min(id.length, 8);
		int i = id.length - len;
		
		switch (len) {
		case 8: code |= (long)id[i++] << 56;
		case 7: code |= ((long)id[i++] << 48) & 0xff000000000000L;
		case 6: code |= ((long)id[i++] << 40) & 0xff0000000000L;
		case 5: code |= ((long)id[i++] << 32) & 0xff00000000L;
		case 4: code |= ((long)id[i++] << 24) & 0xff000000L;
		case 3: code |= ((long)id[i++] << 16) & 0xff0000L;
		case 2: code |= ((long)id[i++] << 8 ) & 0xff00L;
		case 1: code |= ((long)id[i++]      ) & 0xffL;
		case 0: break;
		}
		this.code = code;
	}

	private ConnectionId(int sequenceNumber, byte[] id, byte[] resetToken, long code) {
		this.sequenceNumber = sequenceNumber;
		this.id = id;
		this.resetToken = resetToken;
		this.code = code;
	}
	
	/**
	 * Returns the sequence number associated with this connection id.
	 * 
	 * @return the sequence number
	 */
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * Returns the identifier of this connection id.
	 * 
	 * @return the identifier
	 */
	public byte[] getId() {
		return id;
	}

	/**
	 * Returns the stateless reset token associated with this connection id.
	 * 
	 * @return the stateless reset token, or {@code null} if no token is present
	 */
	public byte[] getResetToken() {
		return resetToken;
	}

	/**
	 * Returns the code identifying this connection id. It is calculated from up to
	 * 8 least significant bytes of the identifier. Assuming the length of
	 * identifiers is less or equal to 8 bytes this code uniquely identifies
	 * connection id of the same length.
	 * 
	 * @return the code identifying this connection id
	 */
	public long getCode() {
		return code;
	}
	
	/**
	 * Creates and returns a copy of this connection id with updated the stateless
	 * reset token.
	 * 
	 * @param resetToken the stateless reset token.
	 * @return a clone of this connection id
	 */
	ConnectionId cloneWith(byte[] resetToken) {
		return new ConnectionId(sequenceNumber, id, resetToken, code);
	}
}
