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

import java.util.Set;

import javax.net.ssl.SSLEngine;

/**
 * A filter providing means for selecting protocols enabled for use on the
 * {@link SSLEngine} based upon the requested, recommended and supported
 * protocols.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ProtocolFilter {
	
	/**
	 * Selects protocols enabled for use on the {@link SSLEngine} based upon the
	 * requested, recommended and supported protocols.
	 * 
	 * @param protocols            the requested protocols, or {@code null} if no
	 *                             protocol was requested
	 * @param recommendedProtocols the recommended protocols for the current
	 *                             {@link SSLEngine}
	 * @param supportedProtocols   the supported protocols for the current
	 *                             {@link SSLEngine}
	 * @return the protocols enabled for use on the current {@link SSLEngine}
	 */
	String[] filterProtocols(String[] protocols, String[] recommendedProtocols, Set<String> supportedProtocols);

}
