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
package org.snf4j.example.earlydata;

import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

public class KeyStoreLoader {

	static final char[] PASSWORD = "password".toCharArray();
	
	private static void load(KeyStore ks, String fileName, char[] password) throws Exception {
		InputStream in = KeyStoreLoader.class.getResourceAsStream(fileName);
		
		try {
			ks.load(in, password);
		}
		finally {
			in.close();
		}
	}

	static X509KeyManager keyManager() throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		KeyManagerFactory kmf;
		
		load(ks, "/keystore.jks", PASSWORD);
		kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, PASSWORD);
		return (X509KeyManager) kmf.getKeyManagers()[0];
	}

	static X509TrustManager trustManager() throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		TrustManagerFactory tmf;
		
		load(ks, "/keystore.jks", PASSWORD);
		tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(ks);
		return (X509TrustManager) tmf.getTrustManagers()[0];
	}

}
