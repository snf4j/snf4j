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

import java.lang.reflect.Method;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.snf4j.core.session.SSLEngineCreateException;

/**
 * A builder for the {@link SSLEngine}.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SSLEngineBuilder implements Cloneable {
	
	private final static Method ENABLE_RETRANSMISSIONS = get("setEnableRetransmissions", boolean.class);

	private final static Method MAXIMUM_PACKET_SIZE = get("setMaximumPacketSize", int.class);
	
	private final boolean forServer;

	private final SSLContext context;
	
	private String[] protocols;

	private ProtocolFilter protocolFilter = DefaultCipherProtocolFilters.INSATNCE;

	private String[] ciphers;
	
	private CipherFilter cipherFilter = DefaultCipherProtocolFilters.INSATNCE;

	private Boolean enableRetransmissions;	//JDK9
	
	private int maximumPacketSize = -1; //JDK9
	
	private Boolean useCiphersOrder;
	
	private ClientAuth clientAuth = ClientAuth.NONE;
	
	SSLEngineBuilder(SSLContext context, boolean forServer) {
		this.context = context;
		this.forServer = forServer;
	}
	
	static Method get(String name, Class<?>... parameterTypes) {
		try {
			Method method = SSLParameters.class.getDeclaredMethod(name, parameterTypes);
			
			method.setAccessible(true);
			return method;
		} catch (Exception e) {}
		return null;
	}
	
	static void set(Method method, SSLParameters params, Object... args) throws Exception {
		if (method != null) {
			method.invoke(params, args);
		}
	}
	
	/**
	 * Creates a builder for a client-side {@link SSLEngine}.
	 * 
	 * @param context the SSL context used by the builder to create
	 *                {@link SSLEngine}
	 * @return a builder for a client-side {@link SSLEngine}
	 */
	public static SSLEngineBuilder forClient(SSLContext context) {
		return new SSLEngineBuilder(context, false);
	}
	
	/**
	 * Creates a builder for a server-side {@link SSLEngine}.
	 * 
	 * @param context the SSL context used by the builder to create
	 *                {@link SSLEngine}
	 * @return a builder for a server-side {@link SSLEngine}
	 */
	public static SSLEngineBuilder forServer(SSLContext context) {
		return new SSLEngineBuilder(context, true);
	}
	
	/**
	 * Returns the SSL context used by the builder to create {@link SSLEngine}
	 * 
	 * @return the SSL context
	 */
	public SSLContext context() {
		return context;
	}
	
	/**
	 * Tells if the builder if for a server-side {@link SSLEngine}.
	 * 
	 * @return {@code true} if the builder if for a server-side {@link SSLEngine}
	 */
	public boolean isForServer() {
		return forServer;
	}
	
	/**
	 * Tells if the builder if for a client-side {@link SSLEngine}.
	 * 
	 * @return {@code true} if the builder if for a client-side {@link SSLEngine}
	 */
	public boolean isForClient() {
		return !forServer;
	}
	
	/**
	 * Configures protocol versions to enable, or {@code null} to enable the
	 * recommended protocol versions.
	 * 
	 * @param protocols the protocol versions
	 * @return this builder
	 */
	public SSLEngineBuilder protocols(String... protocols) {
		this.protocols = protocols == null ? null : protocols.clone();
		return this;
	}

	/**
	 * Configures a filter for protocol versions to enable, or {@code null} to use
	 * the default filter.
	 * 
	 * @param filter the protocol filter
	 * @return this builder
	 */
	public SSLEngineBuilder protocolFilter(ProtocolFilter filter) {
		protocolFilter = filter == null ? DefaultCipherProtocolFilters.INSATNCE : filter;
		return this;
	}

	/**
	 * Configures cipher suites to enable, or {@code null} to enable the
	 * recommended cipher suites.
	 * 
	 * @param ciphers the cipher suites
	 * @return this builder
	 */
	public SSLEngineBuilder ciphers(String... ciphers) {
		this.ciphers = ciphers == null ? null : ciphers.clone();
		return this;
	}

	/**
	 * Configures a filter for cipher suites to enable, or {@code null} to use
	 * the default filter.
	 * 
	 * @param filter the cipher filter
	 * @return this builder
	 */
	public SSLEngineBuilder cipherFilter(CipherFilter filter) {
		cipherFilter = filter == null ? DefaultCipherProtocolFilters.INSATNCE : filter;
		return this;
	}
	
	/**
	 * Configures if DTLS handshake retransmissions should be enabled.
	 * <p>
	 * NOTE: It requires Java 9 or newer.
	 * 
	 * @param enable {@code true} to enable DTLS handshake retransmissions.
	 * @return this builder
	 */
	public SSLEngineBuilder enableRetransmissions(boolean enable) {
		enableRetransmissions = enable ? Boolean.TRUE : Boolean.FALSE;
		return this;
	}
	
	/**
	 * Configures the maximum expected network packet size.
	 * <p>
	 * NOTE: It requires Java 9 or newer.
	 * 
	 * @param maxSize the maximum expected network packet size in bytes, or 0 to use
	 *                the default value that is specified by the underlying
	 *                implementation.
	 * @return this builder
	 */
	public SSLEngineBuilder maximumPacketSize(int maxSize) {
		maximumPacketSize = maxSize;
		return this;
	}
	
	/**
	 * Configures if the local cipher suites preferences should be honored during
	 * SSL/TLS/DTLS handshaking
	 * 
	 * @param useOrder {@code true} to honor the local cipher suites preferences
	 * @return this builder
	 */
	public SSLEngineBuilder useCiphersOrder(boolean useOrder) {
		useCiphersOrder = useOrder ? Boolean.TRUE : Boolean.FALSE;
		return this;
	}
	
	/**
	 * Configures the client authentication mode for a server-side
	 * {@link SSLEngine}.
	 * 
	 * @param clientAuth the client authentication mode.
	 * @return this builder
	 */
	public SSLEngineBuilder clientAuth(ClientAuth clientAuth) {
		this.clientAuth = clientAuth;
		return this;
	}
	
	private SSLEngine configure(SSLEngine engine) throws Exception {
		ProtocolDefaults defaults = ProtocolDefaults.instance(engine);
		
		engine.setUseClientMode(!forServer);
		engine.setEnabledProtocols(protocolFilter.filterProtocols(
				protocols,
				defaults.defaultProtocols(),
				defaults.supportedProtocols()
				));
		engine.setEnabledCipherSuites(cipherFilter.filterCiphers(
				ciphers, 
				defaults.defaultCiphers(),
				defaults.supportedCiphers()
				));
		
		if (forServer) {
			switch (clientAuth) {
			case REQUESTED:
				engine.setWantClientAuth(true);
				break;
				
			case REQUIRED:
				engine.setNeedClientAuth(true);
				break;
				
			default:		
			}
		}
		
		if (maximumPacketSize >= 0 || enableRetransmissions != null || useCiphersOrder != null) {
			SSLParameters params = engine.getSSLParameters();
			
			if (useCiphersOrder != null) {
				params.setUseCipherSuitesOrder(useCiphersOrder);
			}
			if (maximumPacketSize >= 0) {
				set(MAXIMUM_PACKET_SIZE, params, maximumPacketSize);
			}
			if (enableRetransmissions != null) {
				set(ENABLE_RETRANSMISSIONS, params, enableRetransmissions);
			}
			engine.setSSLParameters(params);
		}
		
		return engine;
	}
	
	/**
	 * Builds a new {@link SSLEngine} instance based on the current configuration
	 * settings.
	 * 
	 * @return the new {@link SSLEngine} instance.
	 * @throws SSLEngineCreateException if a failure occurred while building the
	 *                                   {@link SSLEngine} instance
	 */
	public SSLEngine build() throws SSLEngineCreateException {
		try {
			return configure(context.createSSLEngine());
		}
		catch (Exception e) {
			throw new SSLEngineCreateException("Building of SSL engine failed", e);
		}
	}

	/**
	 * Builds a new {@link SSLEngine} instance based on the current configuration
	 * settings and advisory peer information.
	 * 
	 * @param peerHost the non-authoritative name of the host
	 * @param peerPort the non-authoritative port
	 * @return the new {@link SSLEngine} instance.
	 * @throws SSLEngineCreateException if a failure occurred while building the
	 *                                  {@link SSLEngine} instance
	 * @see SSLContext#createSSLEngine(String, int)
	 */
	public SSLEngine build(String peerHost, int peerPort) throws SSLEngineCreateException {
		try {
			return configure(context.createSSLEngine(peerHost, peerPort));
		}
		catch (Exception e) {
			throw new SSLEngineCreateException("Building of SSL engine with peer information failed", e);
		}
	}
	
	SSLEngineBuilder superClone() throws CloneNotSupportedException {
		return (SSLEngineBuilder) super.clone();
	}
	
	/**
	 * Generates a new copy of this builder. Subsequent changes to this builder will
	 * not affect the new copy, and vice versa.
	 */
	@Override
	public SSLEngineBuilder clone() {
		SSLEngineBuilder b;
		
		try {
			b = superClone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		b.protocols(protocols);
		b.ciphers(ciphers);
		return b;
	}
}
