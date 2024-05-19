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

/**
 * A base {@code interface} for the QUIC transport parameters formatters. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ITransportParametersFormatter {
	
	/**
	 * Formats the given transport parameters and puts their byte representation to
	 * the destination buffer.
	 * 
	 * @param client determines if the formatting is being performed by a client or
	 *               server
	 * @param params the transport parameters to format
	 * @param dst    the destination buffer
	 */
	void format(boolean client, TransportParameters params, ByteBuffer dst);
	
	/**
	 * Calculates the length of the byte representation of the formatted given
	 * transport parameters.
	 * 
	 * @param client determine if the formatting will be performed by a client or
	 *               server
	 * @param params the transport parameters to format
	 * @return the length in bytes of the formatted transport parameters
	 */
	int length(boolean client, TransportParameters params);
}
