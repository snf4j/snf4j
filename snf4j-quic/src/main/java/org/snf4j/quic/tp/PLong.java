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
import org.snf4j.quic.packet.PacketUtil;

class PLong extends AbstractParameter {

	private final long defaultValue;
	
	private final ILongSetter setter;

	private final ILongGetter getter;
	
	public PLong(TransportParameterType type, ParameterMode mode, ILongSetter setter, ILongGetter getter) {
		this(type, mode, 0, setter, getter);
	}
	
	public PLong(TransportParameterType type, ParameterMode mode, long defaultValue, ILongSetter setter, ILongGetter getter) {
		super(type, mode);
		this.defaultValue = defaultValue;
		this.setter = setter;
		this.getter = getter;
	}

	@Override
	public void parse(ByteBuffer src, int length, TransportParametersBuilder builder) throws QuicException {
		int[] remaining = new int[] {length};
		setter.set(builder, PacketUtil.decodeInteger(src, remaining));
		if (remaining[0] != 0) {
			throw new QuicException(TransportError.TRANSPORT_PARAMETER_ERROR, "Invalid variable-length integer format");
		}
	}

	@Override
	public void format(TransportParameters params, ByteBuffer dst) {
		long value = getter.get(params);
		
		if (value != defaultValue) {
			int len = PacketUtil.encodedIntegerLength(value);

			dst.put((byte) getType().typeId());
			PacketUtil.encodeInteger(len, dst);
			PacketUtil.encodeInteger(value, dst);
		}
	}

	@Override
	public int length(TransportParameters params) {
		long value = getter.get(params);

		if (value != defaultValue) {
			int len = PacketUtil.encodedIntegerLength(value);
			
			return 1 + PacketUtil.encodedIntegerLength(len) + len;
		}
		return 0;
	}
}
