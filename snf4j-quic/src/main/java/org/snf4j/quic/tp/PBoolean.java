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

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;

class PBoolean extends AbstractParameter {
	
	private final IBooleanSetter setter;

	private final IBooleanGetter getter;

	PBoolean(TransportParameterType type, ParameterMode mode, IBooleanSetter setter, IBooleanGetter getter) {
		super(type, mode);
		this.setter = setter;
		this.getter = getter;
	}

	@Override
	public void parse(ByteBuffer src, int length, TransportParametersBuilder builder) throws QuicException {
		if (length != 0) {
			throw new QuicException(TransportError.TRANSPORT_PARAMETER_ERROR, "Invalid boolean format");
		}
		setter.set(builder, true);
	}

	@Override
	public void format(TransportParameters params, ByteBuffer dst) {
		if (getter.get(params)) {
			dst.put((byte) getType().typeId());
			dst.put((byte) 0);
		}
	}

	@Override
	public int length(TransportParameters params) {
		if (getter.get(params)) {
			return 2;
		}
		return 0;
	}
}
