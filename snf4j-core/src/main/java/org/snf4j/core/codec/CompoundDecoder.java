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
 * A compound decoder that processes input data through a chain of the specified decoders.
 * 
 * @param <I>
 *            the type of the accepted inbound objects
 * @param <O>
 *            the type of the produced outbound objects
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public abstract class CompoundDecoder<I,O> extends CompoundCodec<IDecoder<?,?>,I,O> implements IDecoder<I,O> {
	
	/**
	 * Constructs a compound decoder with a chain of the specified decoders.
	 * <p>
	 * The decoder chain is organized in the following way:
	 * <pre>
	 * {data} -&gt; decoder1 -&gt; decoder2 -&gt; ... -&gt; decoderN -&gt; {out}
	 * </pre>
	 * 
	 * @param decoders
	 *            the chain of decoders
	 * @throws IllegalArgumentException
	 *             if the specified decoders have incompatible inbound or
	 *             outbound types
	 * @throws IllegalStateException if the param O is {@code Void}
	 */
	public CompoundDecoder(IDecoder<?,?>... decoders) {
		super(decoders);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	final void process(ICodec<?,?> codec, ISession session, Object data, List<Object> out) throws Exception {
		 ((IDecoder)codec).decode(session, data, out);
	}
	
	@Override
	final String type() {
		return "decoder";
	}

	/**
	 * Decodes data from one type to another one.
	 * 
	 * @param out
	 *            the {@link List} to which the decoded data should be added
	 */
	@Override
	final public void decode(ISession session, I data, List<O> out) throws Exception {
		process(session, data, out);
	}

}