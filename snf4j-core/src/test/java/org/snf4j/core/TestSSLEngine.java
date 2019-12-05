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
package org.snf4j.core;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

public class TestSSLEngine extends SSLEngine {

	SSLSession session;
	
	final SSLEngine engine;
	
	int unwrapBytes = -1;
	
	volatile int unwrapCounter;
	volatile int wrapCounter;
	
	final SSLEngineResult[] unwrapResult = new SSLEngineResult[10];
	final SSLEngineResult[] wrapResult = new SSLEngineResult[10];
	
	SSLException unwrapException;
	SSLException wrapException;

	volatile int delegatedTaskCounter = 1;
	RuntimeException delegatedTaskException;
	
	public TestSSLEngine(SSLEngine engine) {
		this.engine = engine;
	}

	public TestSSLEngine(SSLSession session) {
		this.session = session;
		engine = null;
	}
	
	public TestSSLEngine() {
		engine = null;
	}
	
	SSLEngineResult getUnwrapResult() {
		int i = unwrapCounter++;
		
		if (i <unwrapResult.length) {
			return unwrapResult[i];
		}
		return null;
	}

	SSLEngineResult getWrapResult() {
		int i = wrapCounter++;

		if (i <wrapResult.length) {
			return wrapResult[i];
		}
		return null;
	}
	
	@Override
	public void beginHandshake() throws SSLException {
		if (engine != null) {
			engine.beginHandshake();
		}
	}

	@Override
	public void closeInbound() throws SSLException {
		if (engine != null) {
			engine.closeInbound();
		}
	}

	@Override
	public void closeOutbound() {
		if (engine != null) {
			engine.closeOutbound();
		}
	}

	@Override
	public Runnable getDelegatedTask() {
		if (engine != null) {
			if (delegatedTaskException != null) {
				if (delegatedTaskCounter > 0) {
					delegatedTaskCounter--;
					return new Runnable() {

						@Override
						public void run() {
							throw delegatedTaskException;
						}
						
					};
				}
				else {
					return null;
				}
			}
			return engine.getDelegatedTask();
		}
		return null;
	}

	@Override
	public boolean getEnableSessionCreation() {
		if (engine != null) {
			return engine.getEnableSessionCreation();
		}
		return false;
	}

	@Override
	public String[] getEnabledCipherSuites() {
		if (engine != null) {
			return engine.getEnabledCipherSuites();
		}
		return null;
	}

	@Override
	public String[] getEnabledProtocols() {
		if (engine != null) {
			return engine.getEnabledProtocols();
		}
		return null;
	}

	@Override
	public HandshakeStatus getHandshakeStatus() {
		if (engine != null) {
			return engine.getHandshakeStatus();
		}
		return null;
	}

	@Override
	public boolean getNeedClientAuth() {
		if (engine != null) {
			return engine.getNeedClientAuth();
		}
		return false;
	}

	@Override
	public SSLSession getSession() {
		if (engine != null) {
			return engine.getSession();
		}
		return session;
	}

	@Override
	public String[] getSupportedCipherSuites() {
		if (engine != null) {
			return engine.getSupportedCipherSuites();
		}
		return null;
	}

	@Override
	public String[] getSupportedProtocols() {
		if (engine != null) {
			return engine.getSupportedProtocols();
		}
		return null;
	}

	@Override
	public boolean getUseClientMode() {
		if (engine != null) {
			return engine.getUseClientMode();
		}
		return false;
	}

	@Override
	public boolean getWantClientAuth() {
		if (engine != null) {
			return engine.getWantClientAuth();
		}
		return false;
	}

	@Override
	public boolean isInboundDone() {
		if (engine != null) {
			return engine.isInboundDone();
		}
		return false;
	}

	@Override
	public boolean isOutboundDone() {
		if (engine != null) {
			return engine.isOutboundDone();
		}
		return false;
	}

	@Override
	public void setEnableSessionCreation(boolean flag) {
		if (engine != null) {
			engine.setEnableSessionCreation(flag);
		}
	}

	@Override
	public void setEnabledCipherSuites(String[] suites) {
		if (engine != null) {
			engine.setEnabledCipherSuites(suites);
		}
	}

	@Override
	public void setEnabledProtocols(String[] protocols) {
		if (engine != null) {
			engine.setEnabledProtocols(protocols);
		}
	}

	@Override
	public void setNeedClientAuth(boolean need) {
		if (engine != null) {
			engine.setNeedClientAuth(need);
		}
	}

	@Override
	public void setUseClientMode(boolean mode) {
		if (engine != null) {
			engine.setUseClientMode(mode);
		}
	}

	@Override
	public void setWantClientAuth(boolean want) {
		if (engine != null) {
			engine.setWantClientAuth(want);
		}
	}

	@Override
	public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset,
			int length) throws SSLException {
		if (unwrapException != null) {
			throw unwrapException;
		}
		SSLEngineResult result = this.getUnwrapResult();
		if (result == null) {
			if (engine != null) {
				return engine.unwrap(src, dsts, offset, length);
			}

			byte[] data = new byte[unwrapBytes == -1 ? src.remaining() : unwrapBytes];

			src.get(data);
			dsts[0].put(data);
			dsts[0].put(data);
			return new SSLEngineResult(SSLEngineResult.Status.OK,
					SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
					data.length, data.length * 2);
		}
		return result;
	}

	@Override
	public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length,
			ByteBuffer dst) throws SSLException {
		if (wrapException != null) {
			throw wrapException;
		}
		SSLEngineResult result = this.getWrapResult();
		if (result == null) {
			if (engine != null) {
				return engine.wrap(srcs, offset, length, dst);
			}
		}
		return result;
	}

}
