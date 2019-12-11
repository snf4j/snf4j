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

import java.util.List;

import org.snf4j.core.session.ISession;

/**
 * A compound encoder that processes input data through a chain of the specified encoders.
 * 
 * @param <I>
 *            the type of the accepted inbound objects
 * @param <O>
 *            the type of the produced outbound objects
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public abstract class CompoundEncoder<I,O> extends CompoundCodec<IEncoder<?,?>,I,O> implements IEncoder<I,O> {

	/**
	 * Constructs a compound encoder with a chain of the specified encoders.
	 * <p>
	 * The encoder chain is organized in the following way:
	 * <pre>
	 * {data} -&gt; encoder1 -&gt; encoder2 -&gt; ... -&gt; encoderN -&gt; {out}
	 * </pre>
	 * 
	 * @param encoders
	 *            the chain of encoders
	 * @throws IllegalArgumentException
	 *             if the specified encoders have incompatible inbound or
	 *             outbound types
	 * @throws IllegalStateException if the param O is {@code Void}
	 */
	public CompoundEncoder(IEncoder<?,?>... encoders) {
		super(encoders);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	final void process(ICodec<?,?> codec, ISession session, Object data, List<Object> out) throws Exception {
		((IEncoder)codec).encode(session, data, out);
	}
	
	@Override
	final String type() {
		return "encoder";
	}
	
	/**
	 * Encodes data from one type to another one.
	 * 
	 * @param out
	 *            the {@link List} to which the encoded data should be added
	 */
	@Override
	final public void encode(ISession session, I data, List<O> out) throws Exception {
		process(session, data, out);
	}
	
}
