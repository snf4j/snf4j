/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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

class EncoderContext extends CodecContext {
	
	final private IEncoder<?,?> encoder;
	
	EncoderContext(Object key, IEncoder<?,?> encoder) {
		super(key, encoder);
		this.encoder = encoder;
	}

	@SuppressWarnings("rawtypes")
	final IEncoder getEncoder() {
		return encoder;
	}	
	
	private final EncoderContext findPreviousNotClogged(CodecContext previous) {
		while (previous != null && previous.isClogged()) {
			previous = previous.prev;
		}
		return (EncoderContext) previous;
	}
	
	@Override
	final boolean isValid(CodecContext previous) {
		if (previous == null) {
			return clogged ? inboundByte : outboundByte;
		}
		if (previous instanceof EncoderContext) {
			EncoderContext found = findPreviousNotClogged(previous);
			
			if (found != null) {
				if (clogged) {
					return encoder.getInboundType().isAssignableFrom(found.encoder.getInboundType());
				}
				return found.encoder.getInboundType().isAssignableFrom(encoder.getOutboundType());
			}
			else {
				return isValid(null);
			}
		}
		return false;
	}
	
	@Override
	final boolean isDecoder() {
		return false;
	}
	
}
