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

/**
 * The base <code>interface</code> for the encoders and decoders. It is
 * responsible for determining the types of the inbound and outbound objects
 * which are accepted and produced by given encoder or decoder.
 * 
 * @param <I>
 *            the type of the accepted inbound objects
 * @param <O>
 *            the type of the produced outbound objects, or {@code Void} if no
 *            output is produced (e.g. a logging codec)
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ICodec<I,O> {

	/**
	 * Returns the type of the accepted inbound objects.
	 * 
	 * @return the type of the inbound objects
	 */
	Class<I> getInboundType();
	
	/**
	 * Returns the type of the produced outbound objects.
	 * 
	 * @return the type of the outbound objects
	 */
	Class<O> getOutboundType();

}
