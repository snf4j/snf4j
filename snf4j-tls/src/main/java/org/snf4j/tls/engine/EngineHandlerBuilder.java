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

import java.security.SecureRandom;

import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.snf4j.tls.Args;
import org.snf4j.tls.session.ISessionManager;

public class EngineHandlerBuilder {

	private final static TicketInfo[] EMPTY_TICKETS = new TicketInfo[0];
	
	private final boolean managers;
	
	private final X509KeyManager km;
	
	private final String alias;
	
	private final X509TrustManager tm;
	
	private final ICertificateSelector selector;
	
	private final ICertificateValidator validator;
	
	private ISessionManager manager;
	
	private SecureRandom random;
	
	private IEarlyDataHandler earlyDataHandler;
	
	private TicketInfo[] tickets = new TicketInfo[] {TicketInfo.NO_MAX_EARLY_DATA_SIZE};
	
	private int padding = 4096;
	
	private IHostNameVerifier hostNameVerifier;
	
	private IApplicationProtocolHandler protocolHandler;
	
	public EngineHandlerBuilder(X509KeyManager km, String alias, X509TrustManager tm) {
		Args.checkNull(km, "km");
		Args.checkNull(tm, "tm");
		this.km = km;
		this.alias = alias;
		this.tm = tm;
		selector = null;
		validator = null;
		managers = true;
	}

	public EngineHandlerBuilder(X509KeyManager km, X509TrustManager tm) {
		this(km, null, tm);
	}

	public EngineHandlerBuilder(X509KeyManager km, String alias) {
		Args.checkNull(km, "km");
		this.km = km;
		this.alias = alias;
		this.tm = null;
		selector = null;
		validator = null;
		managers = true;
	}

	public EngineHandlerBuilder(X509KeyManager km) {
		this(km, (String)null);
	}
	
	public EngineHandlerBuilder(X509TrustManager tm) {
		Args.checkNull(tm, "tm");
		this.km = null;
		this.alias = null;
		this.tm = tm;
		selector = null;
		validator = null;
		managers = true;
	}

	public EngineHandlerBuilder(ICertificateSelector selector, ICertificateValidator validator) {
		Args.checkNull(selector, "selector");
		Args.checkNull(validator, "validator");
		this.selector = selector;
		this.validator = validator;
		km = null;
		alias = null;
		tm = null;
		managers = false;
	}

	public EngineHandlerBuilder(ICertificateSelector selector) {
		Args.checkNull(selector, "selector");
		this.selector = selector;
		validator = null;
		km = null;
		alias = null;
		tm = null;
		managers = false;
	}

	public EngineHandlerBuilder(ICertificateValidator validator) {
		Args.checkNull(validator, "validator");
		selector = null;
		this.validator = validator;
		km = null;
		alias = null;
		tm = null;
		managers = false;
	}
	
	public EngineHandlerBuilder hostNameVerifier(IHostNameVerifier hostNameVerifier) {
		this.hostNameVerifier = hostNameVerifier;
		return this;
	}
	
	public IHostNameVerifier getHostNameVerifier() {
		return hostNameVerifier;
	}
	
	public EngineHandlerBuilder padding(int padding) {
		Args.checkMin(padding, 1, "padding");
		this.padding = padding;
		return this;
	}
	
	public int getPadding() {
		return padding;
	}
	
	public EngineHandlerBuilder ticketInfos(long... maxEarlyDataSizes) {
		if (maxEarlyDataSizes == null) {
			tickets = null;
		}
		else if (maxEarlyDataSizes.length == 0) {
			tickets = EMPTY_TICKETS;
		}
		else {
			TicketInfo[] tickets = new TicketInfo[maxEarlyDataSizes.length];

			for (int i=0; i<tickets.length; ++i) {
				long size = maxEarlyDataSizes[i];

				tickets[i] = size > 0 ? new TicketInfo(size) : TicketInfo.NO_MAX_EARLY_DATA_SIZE; 
			}
			this.tickets =  tickets;
		}
		return this;
	}
	
	public TicketInfo[] getTicketInfos() {
		return tickets;
	}
	
	public EngineHandlerBuilder sessionManager(ISessionManager manager) {
		this.manager = manager;
		return this;
	}
	
	public ISessionManager getSessionManager() {
		return manager;
	}

	public EngineHandlerBuilder secureRandom(SecureRandom random) {
		this.random = random;
		return this;
	}
	
	public SecureRandom getSecureRandom() {
		return random;
	}
		
	public EngineHandlerBuilder earlyDataHandler(IEarlyDataHandler earlyDataHandler) {
		this.earlyDataHandler = earlyDataHandler;
		return this;
	}
	
	public IEarlyDataHandler getEarlyDataHandler() {
		return earlyDataHandler;
	}
	
	public EngineHandlerBuilder protocolHandler(IApplicationProtocolHandler protocolHandler) {
		this.protocolHandler = protocolHandler;
		return this;
	}
	
	public IApplicationProtocolHandler getProtocolHandler() {
		return protocolHandler;
	}
	
	private static TicketInfo[] safeClone(TicketInfo[] tickets) {
		return tickets == null || tickets.length == 0 ? tickets : tickets.clone();
	}
	
	public EngineHandler build(IEarlyDataHandler earlyDataHandler, IHostNameVerifier hostNameVerifier, IApplicationProtocolHandler protocolHandler) {
		if (managers) {
			return new EngineHandler(
					km, alias, 
					tm, 
					random, 
					manager, 
					padding, 
					safeClone(tickets),
					earlyDataHandler, 
					hostNameVerifier,
					protocolHandler);
		}
		return new EngineHandler(
				selector, 
				validator, 
				random, 
				manager, 
				padding, 
				safeClone(tickets),
				earlyDataHandler, 
				hostNameVerifier,
				protocolHandler);
	}

	public EngineHandler build(IEarlyDataHandler earlyDataHandler, IHostNameVerifier hostNameVerifier) {
		return build(earlyDataHandler, hostNameVerifier, protocolHandler);
	}

	public EngineHandler build(IEarlyDataHandler earlyDataHandler, IApplicationProtocolHandler protocolHandler) {
		return build(earlyDataHandler, hostNameVerifier, protocolHandler);
	}
	
	public EngineHandler build(IEarlyDataHandler earlyDataHandler) {
		return build(earlyDataHandler, hostNameVerifier, protocolHandler);
	}
	
	public EngineHandler build(IHostNameVerifier hostNameVerifier, IApplicationProtocolHandler protocolHandler) {
		return build(earlyDataHandler, hostNameVerifier, protocolHandler);
	}
	
	public EngineHandler build(IHostNameVerifier hostNameVerifier) {
		return build(earlyDataHandler, hostNameVerifier, protocolHandler);
	}
	
	public EngineHandler build(IApplicationProtocolHandler protocolHandler) {
		return build(earlyDataHandler, hostNameVerifier, protocolHandler);
	}
	
	public EngineHandler build() {
		return build(earlyDataHandler, hostNameVerifier, protocolHandler);
	}

}
