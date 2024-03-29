/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2022 SNF4J contributors
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
package org.snf4j.core.codec;

import java.nio.ByteBuffer;

import org.snf4j.core.IByteBufferHolder;

abstract class CodecContext {

	CodecContext prev, next;
	
	final Object key;

	final boolean inboundByte;
	
	final boolean inboundByteArray;
	
	final boolean inboundHolder;

	final boolean outboundByte;
	
	final boolean outboundByteArray;
	
	final boolean outboundHolder;
	
	final boolean clogged;
	
	CodecContext(Object key, ICodec<?,?> codec) {
		this.key = key;
		inboundByteArray = codec.getInboundType() == byte[].class;
		inboundHolder = codec.getInboundType() == IByteBufferHolder.class;
		inboundByte = inboundByteArray || inboundHolder ? true : codec.getInboundType().isAssignableFrom(ByteBuffer.class);
		outboundByteArray = codec.getOutboundType() == byte[].class;
		outboundHolder = IByteBufferHolder.class.isAssignableFrom(codec.getOutboundType());
		outboundByte = outboundByteArray || outboundHolder ? true : ByteBuffer.class.isAssignableFrom(codec.getOutboundType());
		clogged = codec.getOutboundType() == Void.class;
	}
	
	abstract boolean isValid(CodecContext previous);
	
	abstract boolean isDecoder();
	
	final boolean isOutboundByte() {
		return outboundByte;
	}
	
	final boolean isOutboundHolder() {
		return outboundHolder;
	}
	
	final boolean isOutboundByteArray() {
		return outboundByteArray;
	}	

	final boolean isInboundByte() {
		return inboundByte;
	}
	
	final boolean isInboundHolder() {
		return inboundHolder;
	}
	
	final boolean isInboundByteArray() {
		return inboundByteArray;
	}	

	final boolean isClogged() {
		return clogged;
	}
	
	final Object getKey() {
		return key;
	}
	
}
