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
package org.snf4j.quic.tp;

import java.nio.ByteBuffer;

import org.snf4j.tls.alert.Alert;

/**
 * A base {@code interface} for the QUIC transport parameters parsers. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ITransportParametersParser {
	
	/**
	 * Parses transport parameters from the given source buffer.
	 * 
	 * @param client    determines if the formatting is being performed by a client
	 *                  or server
	 * @param src       the source buffer
	 * @param remaining the remaining bytes in the source buffer
	 * @param builder   a transport parameters builder that should be used be used
	 *                  to store the parsed parameters and finally to build the
	 *                  transport parameters object
	 * @throws Alert if an error occurred during the parsing
	 */
	void parse(boolean client, ByteBuffer src, int remaining, TransportParametersBuilder builder) throws Alert;
}
