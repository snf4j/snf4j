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
 * A encoder that can be managed by a codec pipeline implementing the
 * {@link ICodecPipeline} interface.
 * 
 * @param <I>
 *            the type of the accepted inbound objects
 * @param <O>
 *            the type of the produced outbound objects, or {@code Void} if no
 *            output is produced (e.g. a logging codec)
 *            
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IEncoder<I,O> extends ICodec<I,O> {
	
	/**
	 * Encodes data from one type to another one.
	 * <p>
	 * Encoders that do not produce an output (i.e. the O parameter is
	 * {@code Void}) cannot change the object passed as the {@code data}
	 * argument. They should not also store it for future use as its state can
	 * be changed.
	 * <p>
	 * The above limitations regarding the {@code data} argument do not apply to
	 * encoders that produce an output.
	 * 
	 * @param session
	 *            the {@link ISession} which the passed data belongs to
	 * @param data
	 *            the data to decode to another one
	 * @param out
	 *            the {@link List} to which the decoded data should be added, or
	 *            {@code null} if O is {@code Void}
	 * @throws Exception
	 *             is thrown, if an error occur
	 */
	void encode(ISession session, I data, List<O> out) throws Exception;
}
