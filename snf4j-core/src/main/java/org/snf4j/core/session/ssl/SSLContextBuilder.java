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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import org.snf4j.core.util.PemUtil;
import org.snf4j.core.util.PemUtil.Label;

/**
 * A builder for the {@link SSLContext}.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SSLContextBuilder implements Destroyable {
	
	private final static String[] KEY_ALGOS = new String[] {"RSA","DSA","EC"};
	
	private final boolean forServer;
	
	private String protocol = ProtocolDefaults.TLS;
	
	private Provider provider;
	
	private String providerName;
	
	private int sessionCacheSize = -1;
	
	private int sessionTimeout = -1;
	
	private TrustManagerFactory trustManager;
	
	private X509Certificate[] trustCerts;

	private PrivateKey key;
	
	private char[] password;
	
	private KeyManagerFactory keyManager;
	
	private X509Certificate[] keyCerts;
	
	private SecureRandom secureRandom;
	
	//SSL engine default
	
	private String[] protocols;

	private ProtocolFilter protocolFilter;
	
	private String[] ciphers;
	
	private CipherFilter cipherFilter;
	
	private Boolean enableRetransmissions;	//JDK9
	
	private int maximumPacketSize = -1; //JDK9
	
	private Boolean useCiphersOrder;
	
	private ClientAuth clientAuth = ClientAuth.NONE;
	
	private SSLContextBuilder(boolean forServer) {
		this.forServer = forServer;
	}

	private static SSLContextBuilder forServer() {
		return new SSLContextBuilder(true);
	}

	/**
	 * Creates a builder for a client-side {@link SSLContext}.
	 * 
	 * @return a builder for a client-side {@link SSLContext}
	 */
	public static SSLContextBuilder forClient() {
		return new SSLContextBuilder(false);
	}

	/**
	 * Creates a builder for a server-side {@link SSLContext}.
	 * 
	 * @param keyFile      a file for a PKCS#8 private key in the PEM encoding
	 * @param keyCertsFile a file for an X.509 certificate chain in the PEM encoding
	 * @return a builder for a server-side {@link SSLContext}
	 * @throws IOException          if a failure occurred while reading the files
	 * @throws KeyException         if a failure occurred while creating the key
	 * @throws CertificateException if a failure occurred while creating the
	 *                              certificates
	 */
	public static SSLContextBuilder forServer(File keyFile, File keyCertsFile) throws IOException, KeyException, CertificateException {
		return forServer().keyManager(keyFile, keyCertsFile);
	}

	/**
	 * Creates a builder for a server-side {@link SSLContext}.
	 * 
	 * @param keyFile      a file for a PKCS#8 private key in the PEM encoding
	 * @param password     the password protecting the private key, or {@code null}
	 *                     if the key is not password-protected
	 * @param keyCertsFile a file for an X.509 certificate chain in the PEM encoding
	 * @return a builder for a server-side {@link SSLContext}
	 * @throws IOException          if a failure occurred while reading the files
	 * @throws KeyException         if a failure occurred while creating the key
	 * @throws CertificateException if a failure occurred while creating the
	 *                              certificates
	 */
	public static SSLContextBuilder forServer(File keyFile, char[] password, File keyCertsFile) throws IOException, KeyException, CertificateException {
		return forServer().keyManager(keyFile, password, keyCertsFile);
	}

	/**
	 * Creates a builder for a server-side {@link SSLContext}.
	 * 
	 * @param keyIn      an input stream for a PKCS#8 private key in the PEM
	 *                   encoding
	 * @param keyCertsIn an input stream for an X.509 certificate chain in the PEM
	 *                   encoding
	 * @return a builder for a server-side {@link SSLContext}
	 * @throws IOException          if a failure occurred while reading from the
	 *                              input streams
	 * @throws KeyException         if a failure occurred while creating the key
	 * @throws CertificateException if a failure occurred while creating the
	 *                              certificates
	 */
	public static SSLContextBuilder forServer(InputStream keyIn, InputStream keyCertsIn) throws IOException, KeyException, CertificateException {
		return forServer().keyManager(keyIn, keyCertsIn);
	}

	/**
	 * Creates a builder for a server-side {@link SSLContext}.
	 * 
	 * @param keyIn    an input stream for a PKCS#8 private key in the PEM encoding
	 * @param password the password protecting the private key, or {@code null} if
	 *                 the key is not password-protected
	 * @param keyCertsIn  an input stream for an X.509 certificate chain in the PEM
	 *                 encoding
	 * @return a builder for a server-side {@link SSLContext}
	 * @throws IOException          if a failure occurred while reading from the
	 *                              input streams
	 * @throws KeyException         if a failure occurred while creating the key
	 * @throws CertificateException if a failure occurred while creating the
	 *                              certificates
	 */
	public static SSLContextBuilder forServer(InputStream keyIn, char[] password, InputStream keyCertsIn) throws IOException, KeyException, CertificateException {
		return forServer().keyManager(keyIn, password, keyCertsIn);
	}
		
	/**
	 * Creates a builder for a server-side {@link SSLContext}.
	 * 
	 * @param key   a PKCS#8 private key
	 * @param keyCerts an X.509 certificate chain
	 * @return a builder for a server-side {@link SSLContext}
	 */
	public static SSLContextBuilder forServer(PrivateKey key, X509Certificate... keyCerts) {
		return forServer().keyManager(key, keyCerts);
	}
	
	/**
	 * Creates a builder for a server-side {@link SSLContext}.
	 * 
	 * @param key      a PKCS#8 private key
	 * @param password the password protecting the private key, or {@code null} if
	 *                 the key is not password-protected
	 * @param keyCerts    an X.509 certificate chain
	 * @return a builder for a server-side {@link SSLContext}
	 */
	public static SSLContextBuilder forServer(PrivateKey key, char[] password, X509Certificate... keyCerts) {
		return forServer().keyManager(key, password, keyCerts);
	}
	
	/**
	 * Creates a builder for a server-side {@link SSLContext}.
	 * 
	 * @param keyFactory a factory for a private key 
	 * @return a builder for a server-side {@link SSLContext}
	 */
	public static SSLContextBuilder forServer(KeyManagerFactory keyFactory) {
		return forServer().keyManager(keyFactory);
	}

	/**
	 * Tells if the builder if for a server-side {@link SSLContext}.
	 * 
	 * @return {@code true} if the builder if for a server-side {@link SSLContext}
	 */
	public boolean isForServer() {
		return forServer;
	}
	
	/**
	 * Tells if the builder if for a client-side {@link SSLContext}.
	 * 
	 * @return {@code true} if the builder if for a client-side {@link SSLContext}
	 */
	public boolean isForClient() {
		return !forServer;
	}
	
	/**
	 * Configures the protocol name of the {@link SSLContext} to be created by this
	 * builder.
	 * 
	 * @param protocol the protocol name
	 * @return this builder
	 */
	public SSLContextBuilder protocol(String protocol) {
		this.protocol = protocol;
		return this;
	}

	/**
	 * Configures protocol versions to enable, or {@code null} to enable the
	 * recommended protocol versions.
	 * <p>
	 * This configuration is used to pre-configure the {@link SSLEngineBuilder}
	 * returned by the {@link #engineBuilder()} method.
	 * 
	 * @param protocols the protocol versions
	 * @return this builder
	 */
	public SSLContextBuilder protocols(String... protocols) {
		this.protocols = protocols == null ? null : protocols.clone();
		return this;
	}

	/**
	 * Configures a filter for protocol versions to enable, or {@code null} to use
	 * the default filter.
	 * <p>
	 * This configuration is used to pre-configure the {@link SSLEngineBuilder}
	 * returned by the {@link #engineBuilder()} method.
	 * 
	 * @param filter the protocol filter
	 * @return this builder
	 */
	public SSLContextBuilder protocolFilter(ProtocolFilter filter) {
		protocolFilter = filter;
		return this;
	}

	/**
	 * Configures cipher suites to enable, or {@code null} to enable the
	 * recommended cipher suites.
	 * <p>
	 * This configuration is used to pre-configure the {@link SSLEngineBuilder}
	 * returned by the {@link #engineBuilder()} method.
	 * 
	 * @param ciphers the cipher suites
	 * @return this builder
	 */
	public SSLContextBuilder ciphers(String... ciphers) {
		this.ciphers = ciphers == null ? null : ciphers.clone();
		return this;
	}

	/**
	 * Configures a filter for cipher suites to enable, or {@code null} to use
	 * the default filter.
	 * <p>
	 * This configuration is used to pre-configure the {@link SSLEngineBuilder}
	 * returned by the {@link #engineBuilder()} method.
	 * 
	 * @param filter the cipher filter
	 * @return this builder
	 */
	public SSLContextBuilder cipherFilter(CipherFilter filter) {
		cipherFilter = filter;
		return this;
	}
	
	/**
	 * Configures if DTLS handshake retransmissions should be enabled.
	 * <p>
	 * This configuration is used to pre-configure the {@link SSLEngineBuilder}
	 * returned by the {@link #engineBuilder()} method.
	 * <p>
	 * NOTE: It requires Java 9 or newer.
	 * 
	 * @param enable {@code true} to enable DTLS handshake retransmissions.
	 * @return this builder
	 */
	public SSLContextBuilder enableRetransmissions(boolean enable) {
		enableRetransmissions = enable ? Boolean.TRUE : Boolean.FALSE;
		return this;
	}
	
	/**
	 * Configures the maximum expected network packet size.
	 * <p>
	 * This configuration is used to pre-configure the {@link SSLEngineBuilder}
	 * returned by the {@link #engineBuilder()} method.
	 * <p>
	 * NOTE: It requires Java 9 or newer.
	 * 
	 * @param maxSize the maximum expected network packet size in bytes, or 0 to use
	 *                the default value that is specified by the underlying
	 *                implementation.
	 * @return this builder
	 */
	public SSLContextBuilder maximumPacketSize(int maxSize) {
		maximumPacketSize = maxSize;
		return this;
	}
	
	/**
	 * Configures if the local cipher suites preferences should be honored during
	 * SSL/TLS/DTLS handshaking
	 * <p>
	 * This configuration is used to pre-configure the {@link SSLEngineBuilder}
	 * returned by the {@link #engineBuilder()} method.
	 * 
	 * @param useOrder {@code true} to honor the local cipher suites preferences
	 * @return this builder
	 */
	public SSLContextBuilder useCiphersOrder(boolean useOrder) {
		useCiphersOrder = useOrder ? Boolean.TRUE : Boolean.FALSE;
		return this;
	}
	
	/**
	 * Configures the client authentication mode for a server-side
	 * {@link SSLEngine}.
	 * <p>
	 * This configuration is used to pre-configure the {@link SSLEngineBuilder}
	 * returned by the {@link #engineBuilder()} method.
	 * 
	 * @param clientAuth the client authentication mode.
	 * @return this builder
	 */
	public SSLContextBuilder clientAuth(ClientAuth clientAuth) {
		this.clientAuth = clientAuth;
		return this;
	}
	
	/**
	 * Configures the provide of the {@link SSLContext} to be created by this
	 * builder.
	 * 
	 * @param provider the provider
	 * @return this builder
	 */
	public SSLContextBuilder provider(Provider provider) {
		this.provider = provider;
		providerName = null;
		return this;
	}

	/**
	 * Configures the provider name of the {@link SSLContext} to be created by this
	 * builder.
	 * 
	 * @param provider the provider name
	 * @return this builder
	 */
	public SSLContextBuilder providerName(String provider) {
		this.providerName = provider;
		provider = null;
		return this;
	}
	
	/**
	 * Configures the timeout limit for the cached SSL session objects.
	 * 
	 * @param timeout the timeout limit in seconds, or 0 to set no limit.
	 * @return this builder
	 */
	public SSLContextBuilder sessionTimeout(int timeout) {
		sessionTimeout = timeout;
		return this;
	}

	/**
	 * Configures the size of the cache used for storing the SSL session objects.
	 * 
	 * @param size the cache size limit, or 0 to set no limit.
	 * @return this builder
	 */
	public SSLContextBuilder sessionCacheSize(int size) {
		sessionCacheSize = size;
		return this;
	}
	
	private X509Certificate[] createCerts(List<byte[]> certs) throws CertificateException {
		if (certs == null) {
			throw new CertificateException("Invalid certificate PEM format");
		}
		if (certs.isEmpty()) {
			throw new CertificateException("No certificate found");
		}
		
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        int len = certs.size();
        X509Certificate[] out = new X509Certificate[len];

        for (int i=0; i<len; ++i) {
        	InputStream in = new ByteArrayInputStream(certs.get(i));
        	
        	try {
        		out[i] = (X509Certificate) cf.generateCertificate(in);
        	}
        	finally {
        		silentClose(in);
        	}
        }
        return out;
	}
	
	static void silentClose(Closeable stream) {
		try {
			stream.close();
		}
		catch (IOException e) {
			//Ignore
		}
	}
	
	private X509Certificate[] readCerts(File file) throws IOException, CertificateException {
		return createCerts(PemUtil.read(Label.CERTIFICATE, file));
	}

	private X509Certificate[] readCerts(InputStream in) throws IOException, CertificateException {
		return createCerts(PemUtil.read(Label.CERTIFICATE, in));
	}
	
	/**
	 * Configures trusted certificates for remote hosts verification.
	 * 
	 * @param trustCertsFile a file for X.509 certificates in the PEM encoding
	 * @return this builder
	 * @throws IOException          if a failure occurred while reading the files
	 * @throws CertificateException if a failure occurred while creating the
	 */
	public SSLContextBuilder trustManager(File trustCertsFile) throws IOException, CertificateException {
		return trustManager(readCerts(trustCertsFile));
	}

	/**
	 * Configures trusted certificates for remote hosts verification.
	 * 
	 * @param trustCertsIn an input stream for X.509 certificates in the PEM encoding
	 * @return this builder
	 * @throws IOException          if a failure occurred while reading the files
	 * @throws CertificateException if a failure occurred while creating the
	 */
	public SSLContextBuilder trustManager(InputStream trustCertsIn) throws IOException, CertificateException {
		return trustManager(readCerts(trustCertsIn));
	}

	/**
	 * Configures trusted certificates for remote hosts verification.
	 * 
	 * @param trustCerts X.509 certificates
	 * @return this builder
	 */
	public SSLContextBuilder trustManager(X509Certificate... trustCerts) {
		this.trustCerts = certs(trustCerts, true, "trustCerts");
		trustManager = null;
		return this;
	}

	/**
	 * Configures trusted certificates for remote hosts verification.
	 * 
	 * @param trustFactory a factory for trusted certificates 
	 * @return this builder
	 */
	public SSLContextBuilder trustManager(TrustManagerFactory trustFactory) {
		this.trustManager = trustFactory;
		trustCerts = null;
		return this;
	}

	private PrivateKey createKey(List<byte[]> keys, char[] password) throws KeyException {
		if (keys == null) {
			throw new KeyException("Invalid private key PEM format");
		}
		if (keys.isEmpty()) {
			throw new KeyException("No private key found");
		}
		
		PKCS8EncodedKeySpec keySpec = null;
		byte[] key = keys.get(0);
		
		if (password == null) {
			keySpec = new PKCS8EncodedKeySpec(keys.get(0));
		}
		else {
			try {
				EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
				SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
				PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
				SecretKey pbeKey;
				Cipher cipher;
				
				try {
					pbeKey = keyFactory.generateSecret(pbeKeySpec);
					try {
						cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
						cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());
						keySpec = encryptedPrivateKeyInfo.getKeySpec(cipher);
					}
					finally {
						pbeKey.destroy();
					}
				}
				finally {
					pbeKeySpec.clearPassword();
				}
			}
			catch (DestroyFailedException e) {
				//Ignore
			}
			catch (Exception e) {
				throw new KeyException("Invalid PKCS8 encoding of password-protected private key", e);
			}
		}
		
		Exception exception = null;
		
		for (int i=0; i < KEY_ALGOS.length; ++i) {
			try {
				return KeyFactory.getInstance(KEY_ALGOS[i]).generatePrivate(keySpec);
			}
			catch (Exception e) {
				exception = e;
			}
		}
		throw new KeyException("Generation of private key failed: none of " + Arrays.toString(KEY_ALGOS) + " worked", exception);
	}
	
	private PrivateKey readKey(File file, char[] password) throws IOException, KeyException {
		return createKey(PemUtil.read(password == null ? Label.PRIVATE_KEY : Label.ENCRYPTED_PRIVATE_KEY, file), 
				password);
	}

	private PrivateKey readKey(InputStream in, char[] password) throws IOException, KeyException {
		return createKey(PemUtil.read(password == null ? Label.PRIVATE_KEY : Label.ENCRYPTED_PRIVATE_KEY, in), 
				password);
	}

	/**
	 * Configures a private key with certificate chain for host identification.
	 * 
	 * @param keyFile      a file for a PKCS#8 private key in the PEM encoding
	 * @param keyCertsFile a file for an X.509 certificate chain in the PEM encoding
	 * @throws IOException          if a failure occurred while reading the files
	 * @throws KeyException         if a failure occurred while creating the key
	 * @throws CertificateException if a failure occurred while creating the
	 *                              certificates
	 * @return this builder
	 */
	public SSLContextBuilder keyManager(File keyFile, File keyCertsFile) throws IOException, KeyException, CertificateException {
		return keyManager(keyFile, null, keyCertsFile);
	}

	/**
	 * Configures a private key with certificate chain for host identification.
	 * 
	 * @param keyFile      a file for a PKCS#8 private key in the PEM encoding
	 * @param password     the password protecting the private key, or {@code null}
	 *                     if the key is not password-protected
	 * @param keyCertsFile a file for an X.509 certificate chain in the PEM encoding
	 * @throws IOException          if a failure occurred while reading the files
	 * @throws KeyException         if a failure occurred while creating the key
	 * @throws CertificateException if a failure occurred while creating the
	 *                              certificates
	 * @return this builder
	 */
	public SSLContextBuilder keyManager(File keyFile, char[] password, File keyCertsFile) throws IOException, KeyException, CertificateException {
		return keyManager(readKey(keyFile, password), password, readCerts(keyCertsFile));
	}

	/**
	 * Configures a private key with certificate chain for host identification.
	 * 
	 * @param keyIn      an input stream for a PKCS#8 private key in the PEM
	 *                   encoding
	 * @param keyCertsIn an input stream for an X.509 certificate chain in the PEM
	 *                   encoding
	 * @throws IOException          if a failure occurred while reading from the
	 *                              input streams
	 * @throws KeyException         if a failure occurred while creating the key
	 * @throws CertificateException if a failure occurred while creating the
	 *                              certificates
	 * @return this builder
	 */
	public SSLContextBuilder keyManager(InputStream keyIn, InputStream keyCertsIn) throws IOException, KeyException, CertificateException {
		return keyManager(keyIn, null, keyCertsIn);
	}

	/**
	 * Configures a private key with certificate chain for host identification.
	 * 
	 * @param keyIn      an input stream for a PKCS#8 private key in the PEM
	 *                   encoding
	 * @param password   the password protecting the private key, or {@code null} if
	 *                   the key is not password-protected
	 * @param keyCertsIn an input stream for an X.509 certificate chain in the PEM
	 *                   encoding
	 * @throws IOException          if a failure occurred while reading from the
	 *                              input streams
	 * @throws KeyException         if a failure occurred while creating the key
	 * @throws CertificateException if a failure occurred while creating the
	 *                              certificates
	 * @return this builder
	 */
	public SSLContextBuilder keyManager(InputStream keyIn, char[] password, InputStream keyCertsIn) throws IOException, KeyException, CertificateException {
		return keyManager(readKey(keyIn, password), password, readCerts(keyCertsIn));
	}
	
	private static X509Certificate[] certs(X509Certificate[] certs, boolean allowEmpty, String name) {
		if (!allowEmpty) {
			if (certs == null) {
				throw new IllegalArgumentException(name + " is null");
			}
			if (certs.length == 0) {
				throw new IllegalArgumentException(name + " is empty");
			}
		}
		if (certs != null) {
			for (X509Certificate cert: certs) {
				if (cert == null) {
					throw new IllegalArgumentException(name + " contains null entry");
				}
			}
			certs = certs.clone();
		}
		return certs;
	}
	
	/**
	 * Configures a private key with certificate chain for host identification.
	 * 
	 * @param key      a PKCS#8 private key
	 * @param keyCerts an X.509 certificate chain
	 * @return this builder
	 */
	public SSLContextBuilder keyManager(PrivateKey key, X509Certificate... keyCerts) {
		return keyManager(key, null, keyCerts);
	}
	
	/**
	 * Configures a private key with certificate chain for host identification.
	 * 
	 * @param key      a PKCS#8 private key
	 * @param password the password protecting the private key, or {@code null} if
	 *                 the key is not password-protected
	 * @param keyCerts an X.509 certificate chain
	 * @return this builder
	 */
	public SSLContextBuilder keyManager(PrivateKey key, char[] password, X509Certificate... keyCerts) {
		keyCerts = certs(keyCerts, false, "keyCerts");
		if (key == null) {
			throw new IllegalArgumentException("key is null");
		}
		this.key = key;
		this.password = password == null ? null : password.clone();
		this.keyCerts = keyCerts;
		this.keyManager = null;
		return this;
	}
	
	/**
	 * Configures a private key with certificate chain for host identification.
	 * 
	 * @param keyFactory a factory for a private key 
	 * @return this builder
	 */
	public SSLContextBuilder keyManager(KeyManagerFactory keyFactory) {
		this.keyManager = keyFactory;
		silentDestroy();
		return this;
	}

	/**
	 * Configures a secure source of randomness.
	 * 
	 * @param random the source of randomness, or {@code null} to use the default
	 *               source.
	 * @return this builder
	 */
	public SSLContextBuilder secureRandom(SecureRandom random) {
		this.secureRandom = random;
		return this;
	}
	
	private TrustManagerFactory buildTrustManager() throws Exception {
		if (trustCerts == null) {
			return null;
		}
        
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        
        ks.load(null, null);
        for (int i=0; i<trustCerts.length; ++i) {
        	ks.setCertificateEntry(Integer.toString(i+1), trustCerts[i]);
        }
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
 
        tmf.init(ks);
		return tmf;
	}

	private KeyManagerFactory buildKeyManager() throws Exception {
		if (key == null) {
			return null;
		}
		
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = this.password == null ? new char[0] : this.password;
        
        ks.load(null, null);
        ks.setKeyEntry("key", key, password, keyCerts);
		
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		
        kmf.init(ks, password);
		return kmf;
	}
	
	/**
	 * Creates a new {@link SSLEngine} builder pre-configured with the current
	 * configuration settings. The returned builder is constructed with a new
	 * {@link SSLContext} created by calling the {@link #build()} method.
	 * 
	 * @return the new {@link SSLEngine} builder
	 * @throws SSLContextCreateException if a failure occurred while building the
	 *                                   {@link SSLContext} instance used to
	 *                                   construct the new {@link SSLEngine} builder
	 */
	public SSLEngineBuilder engineBuilder() throws SSLContextCreateException {
		SSLEngineBuilder builder = new SSLEngineBuilder(build(), forServer);
		Boolean value;
		
		builder.protocols(protocols);
		builder.ciphers(ciphers);
		builder.clientAuth(clientAuth);
		builder.cipherFilter(cipherFilter);
		builder.protocolFilter(protocolFilter);
		builder.maximumPacketSize(maximumPacketSize);
		value = enableRetransmissions;
		if (value != null) {
			builder.enableRetransmissions(value.booleanValue());
		}
		value = useCiphersOrder;
		if (value != null) {
			builder.useCiphersOrder(value.booleanValue());
		}
		return builder;
	}
	
	private enum Phase {
		
		NONE(null),
		GET_DEFAULT_CTX("Getting of the default SSL context failed"),
		BUILD_TRUST_MGR_FACTORY("Building of the trust manager factory failed"),
		BUILD_KEY_MGR_FACTORY("Building of the key manager factory failed"),
		BUILD_CTX("Building of the SSL context failed");
		
		String msg;
		
		private Phase(String msg) {
			this.msg = msg;
		}
		
		private String exceptionMessage() {
			return msg;
		}
	}
	
	/**
	 * Builds a new {@link SSLContext} instance based on the current configuration
	 * settings.
	 * 
	 * @return the new {@link SSLContext} instance.
	 * @throws SSLContextCreateException if a failure occurred while building the
	 *                                   {@link SSLContext} instance
	 */
	public SSLContext build() throws SSLContextCreateException {
		String protocol = this.protocol;
		Phase phase = Phase.NONE;
		
		try {
			if (protocol == null) {
				phase = Phase.GET_DEFAULT_CTX;
				return SSLContext.getDefault();
			}
		
			TrustManagerFactory tmf;
			KeyManagerFactory kmf;
		
			phase = Phase.BUILD_TRUST_MGR_FACTORY;
			tmf = trustManager != null ? trustManager : buildTrustManager();
			
			phase = Phase.BUILD_KEY_MGR_FACTORY;
			kmf = keyManager != null ? keyManager : buildKeyManager();
		
			String providerName = this.providerName;
			Provider provider = this.provider;
			SSLContext context;
			SSLSessionContext sessionContext;

			phase = Phase.BUILD_CTX;
			if (provider != null) {
				context = SSLContext.getInstance(protocol, provider);
			}
			else if (providerName != null) {
				context = SSLContext.getInstance(protocol, providerName);
			}
			else {
				context = SSLContext.getInstance(protocol);
			}
			
			context.init(
					kmf == null ? null : kmf.getKeyManagers(), 
					tmf == null ? null : tmf.getTrustManagers(), 
					secureRandom);	
			sessionContext = forServer ? context.getServerSessionContext() : context.getClientSessionContext();
			
			if (sessionCacheSize >= 0) {
				sessionContext.setSessionCacheSize(sessionCacheSize);
			}
			if (sessionTimeout >= 0) {
				sessionContext.setSessionTimeout(sessionTimeout);
			}
			
			return context;
		}
		catch (Exception e) {
			throw new SSLContextCreateException(phase.exceptionMessage(), e);
		}
	}
	
	private void silentDestroy() {
		try {
			destroy();
		}
		catch (DestroyFailedException e) {
			//Ignore
		}
	}
	
	/**
	 * Destroys sensitive information associated with this builder (i.e. password
	 * and private key).
	 *
	 * @throws DestroyFailedException if the destroy operation failed
	 */
	@Override
	public void destroy() throws DestroyFailedException {
		if (password != null) {
			Arrays.fill(password, (char)0);
			password = null;
		}
		try {
			if (key != null) {
				key.destroy();
			}
		}
		finally {
			key = null;
			keyCerts = null;
		}
	}
	
	/**
	 * Tells if sensitive information associated with this builder is destroyed
	 * 
	 * @return {@code true} if the sensitive information is destroyed
	 */
	@Override
	public boolean isDestroyed() {
		return key == null;
	}
}
