/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.engine;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.snf4j.tls.Args;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.NoApplicationProtocolAlert;
import org.snf4j.tls.alert.UnsupportedCertificateAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.IALPNExtension;
import org.snf4j.tls.extension.IServerNameExtension;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.session.ISessionManager;
import org.snf4j.tls.session.SessionManager;

public class EngineHandler implements IEngineHandler {

	private final static SessionManager SESSION_MANAGER = new SessionManager(); 
	
	private final static TicketInfo[] EMPTY_TICKETS = new TicketInfo[0];
	
	private final static int DEFAULT_PADDING = 4096; 

	private final static int MAX_CONTENT_LENGTH = 16384; 
	
	private final static ICertificateValidator DEFAULT_CERT_VALIDATOR = new ICertificateValidator() {

		@Override
		public Alert validateCertificates(CertificateValidateCriteria criteria, X509Certificate[] certs)
				throws Alert, Exception {
			return new UnsupportedCertificateAlert("Unsupported certificates");
		}

		@Override
		public Alert validateRawKey(CertificateValidateCriteria criteria, PublicKey key) throws Alert, Exception {
			return new UnsupportedCertificateAlert("Unsupported raw key");
		}
	};
	
	private final static ICertificateSelector DEFAULT_CERT_SELECTOR = new ICertificateSelector() {

		@Override
		public SelectedCertificates selectCertificates(CertificateCriteria criteria)
				throws CertificateSelectorException, Exception {
			throw new CertificateSelectorException("No certificate chain found");
		}
	};
	
	private final static IHostNameVerifier DEFAULT_HOSTNAME_VERIFIER = new IHostNameVerifier() {

		@Override
		public boolean verifyHostName(String hostname) {
			return true;
		}
	};
	
	private final static IApplicationProtocolHandler DEFAULT_PROTOCOL_HANDLER = new IApplicationProtocolHandler() {

		@Override
		public void selectedApplicationProtocol(String protocol) throws Alert {
		}

		@Override
		public String selectApplicationProtocol(String[] offeredProtocols, String[] supportedProtocols) throws Alert {
			return null;
		}
	};
	
	private final ISessionManager manager;
	
	private final ICertificateSelector certificateSelector;
	
	private final ICertificateValidator certificateValidator;
	
	private final SecureRandom random;
	
	private final int padding;
	
	private final IEarlyDataHandler earlyDataHandler;
	
	private final TicketInfo[] ticketInfos;
	
	private final IHostNameVerifier hostNameVerifier;
	
	private final IApplicationProtocolHandler protocolHandler;
	
	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, SecureRandom random, ISessionManager manager, int padding, 
			TicketInfo[] ticketInfos, 
			IEarlyDataHandler earlyDataHandler,
			IHostNameVerifier hostNameVerifier, 
			IApplicationProtocolHandler protocolHandler) {
		this(km != null 
					? new X509KeyManagerCertificateSelector(km, alias) 
					: DEFAULT_CERT_SELECTOR,
				tm != null 
					? new X509TrustManagerCertificateValidator(tm) 
					: DEFAULT_CERT_VALIDATOR,
				random,
				manager,
				padding,
				ticketInfos,
				earlyDataHandler,
				hostNameVerifier,
				protocolHandler);
	}

	public EngineHandler(ICertificateSelector selector, ICertificateValidator validator, SecureRandom random, ISessionManager manager, int padding, 
			TicketInfo[] ticketInfos,
			IEarlyDataHandler earlyDataHandler,
			IHostNameVerifier hostNameVerifier, 
			IApplicationProtocolHandler protocolHandler) {
		Args.checkMin(padding, 1, "padding");
		certificateSelector = selector != null 
				? selector 
				: DEFAULT_CERT_SELECTOR;
		certificateValidator = validator != null 
				? validator 
				: DEFAULT_CERT_VALIDATOR;
		this.manager = manager == null ? SESSION_MANAGER : manager;
		this.random = random == null ? new SecureRandom() : random;
		this.padding = padding;
		this.earlyDataHandler = earlyDataHandler != null 
				? earlyDataHandler 
				: NoEarlyDataHandler.INSTANCE;
		this.ticketInfos = ticketInfos != null 
				? ticketInfos 
				: EMPTY_TICKETS;
		this.hostNameVerifier = hostNameVerifier != null
				? hostNameVerifier
				: DEFAULT_HOSTNAME_VERIFIER;
		this.protocolHandler = protocolHandler != null
				? protocolHandler
				: DEFAULT_PROTOCOL_HANDLER;
	}
	
	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, SecureRandom random, ISessionManager manager, int padding) {
		this(km, alias, 
				tm, 
				random, 
				manager, 
				padding, 
				new TicketInfo[] {TicketInfo.NO_MAX_EARLY_DATA_SIZE}, 
				null, 
				null, 
				null);
	}
	
	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, ISessionManager manager, int padding) {
		this(km, alias, tm, null, manager, padding);
	}

	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, SecureRandom random, ISessionManager manager) {
		this(km, alias, tm, random, manager, DEFAULT_PADDING);
	}
	
	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, ISessionManager manager) {
		this(km, alias, tm, manager, DEFAULT_PADDING);
	}
	
	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, SecureRandom random) {
		this(km, alias, tm, random, null, DEFAULT_PADDING);
	}

	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm) {
		this(km, alias, tm, null, DEFAULT_PADDING);
	}

	public EngineHandler(X509KeyManager km, X509TrustManager tm, SecureRandom random, ISessionManager manager, int padding) {
		this(km, null, tm, random, manager, padding);
	}

	public EngineHandler(X509KeyManager km, X509TrustManager tm, ISessionManager manager, int padding) {
		this(km, null, tm, null, manager, padding);
	}

	public EngineHandler(X509KeyManager km, X509TrustManager tm, SecureRandom random, ISessionManager manager) {
		this(km, null, tm, random, manager, DEFAULT_PADDING);
	}
	
	public EngineHandler(X509KeyManager km, X509TrustManager tm, ISessionManager manager) {
		this(km, null, tm, manager, DEFAULT_PADDING);
	}

	public EngineHandler(X509KeyManager km, X509TrustManager tm, SecureRandom random) {
		this(km, null, tm, random, null, DEFAULT_PADDING);
	}
	
	public EngineHandler(X509KeyManager km, X509TrustManager tm) {
		this(km, null, tm, null, DEFAULT_PADDING);
	}

	@Override
	public String selectApplicationProtocol(IALPNExtension alpn, String[] supportedProtocols) throws Alert {
		if (alpn != null && supportedProtocols.length > 0) {
			String[] offeredProtocols = alpn.getProtocolNames();
			String selected = protocolHandler.selectApplicationProtocol(offeredProtocols, supportedProtocols);
			
			if (selected == null) {
				for (String supported: supportedProtocols) {
					for (String offered: offeredProtocols) {
						if (offered.equals(supported)) {
							return offered;
						}
					}
				}
				throw new NoApplicationProtocolAlert("Offered application protocols not supported by server");
			}
			else if (!selected.isEmpty()) {
				return selected;
			}
		}
		return null;
	}

	@Override
	public boolean verifyServerName(IServerNameExtension serverName) {
		return hostNameVerifier.verifyHostName(serverName.getHostName());
	}
	
	@Override
	public void selectedApplicationProtocol(String protocol) throws Alert {
		protocolHandler.selectedApplicationProtocol(protocol);
	}
	
	@Override
	public void handshakeFinished(String protocol) throws Alert{
	}
	
	@Override
	public ICertificateSelector getCertificateSelector() {
		return certificateSelector;
	}

	@Override
	public ICertificateValidator getCertificateValidator() {
		return certificateValidator;
	}

	@Override
	public int calculatePadding(ContentType type, int contentLength) {
		if (contentLength >= MAX_CONTENT_LENGTH) {
			return 0;
		}
		else if (contentLength > MAX_CONTENT_LENGTH - this.padding) {
			return MAX_CONTENT_LENGTH - contentLength;
		}

		int padding = contentLength % this.padding;
			
		if (padding != 0) {
			padding = this.padding - padding;
		}
		return padding;
	}

	@Override
	public long getKeyLimit(CipherSuite cipher, long defaultValue) {
		return defaultValue;
	}

	@Override
	public TicketInfo[] createNewTickets() {
		return ticketInfos;
	}

	@Override
	public ISessionManager getSessionManager() {
		return manager;
	}

	@Override
	public SecureRandom getSecureRandom() {
		return random;
	}

	@Override
	public IEarlyDataHandler getEarlyDataHandler() {
		return earlyDataHandler;
	}
}
