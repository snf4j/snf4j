/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.core.session.ssl;

import javax.net.ssl.SSLEngine; 
import java.util.Set;

/**
 * A filter providing means for selecting ciphers enabled for use on the
 * {@link SSLEngine} based upon the requested, recommended and supported
 * ciphers.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface CipherFilter {
	
	/**
	 * Selects ciphers enabled for use on the {@link SSLEngine} based upon the
	 * requested, recommended and supported ciphers.
	 * 
	 * @param ciphers            the requested ciphers, or {@code null} if no cipher was
	 *                           requested
	 * @param recommendedCiphers the recommended ciphers for the current
	 *                           {@link SSLEngine}
	 * @param supportedCiphers   the supported ciphers for the current
	 *                           {@link SSLEngine}
	 * @return the ciphers enabled for use on the current {@link SSLEngine}
	 */
	String[] filterCiphers(String[] ciphers, String[] recommendedCiphers, Set<String> supportedCiphers);
}
