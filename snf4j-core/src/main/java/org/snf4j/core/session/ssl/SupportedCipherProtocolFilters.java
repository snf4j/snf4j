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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A set of cipher and protocol filters that filter out all requested ciphers and
 * protocols that are not supported by the current {@link SSLEngine}. If no
 * cipher or protocol is requested they return the recommended values.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public final class SupportedCipherProtocolFilters implements CipherFilter, ProtocolFilter {

	/** The instance of this class */
	public static final SupportedCipherProtocolFilters INSATNCE = new SupportedCipherProtocolFilters();
	
	private SupportedCipherProtocolFilters() {}
	
	private final String[] filter(String[] items, String[] recommendedItems, Set<String> supportedItems) {
		if (items == null) {
			return recommendedItems;
		}
		
		List<String> itemList = new ArrayList<String>();
		
		for (String item: items) {
			if (supportedItems.contains(item)) {
				itemList.add(item);
			}
		}
		return itemList.toArray(new String[itemList.size()]);
	}
	
	@Override
	public String[] filterCiphers(String[] ciphers, String[] recommendedCiphers, Set<String> supportedCiphers) {
		return filter(ciphers, recommendedCiphers, supportedCiphers);
	}

	@Override
	public String[] filterProtocols(String[] protocols, String[] recommendedProtocols, Set<String> supportedProtocols) {
		return filter(protocols, recommendedProtocols, supportedProtocols);
	}
}
