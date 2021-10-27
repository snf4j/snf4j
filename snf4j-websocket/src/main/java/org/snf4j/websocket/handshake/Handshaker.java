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
package org.snf4j.websocket.handshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.snf4j.core.SSLSession;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.session.ISession;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.extensions.ExtensionGroup;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.extensions.InvalidExtensionException;

/**
 * Default handshaker responsible for processing of the Web Socket handshake phase.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class Handshaker implements IHandshaker {
	
	private final static IExtension[] EMPTY = new IExtension[0];
	
	private final boolean clientMode;
	
	private final IWebSocketSessionConfig config;
	
	private volatile boolean finished;
	
	private boolean closing;
	
	private String key;
	
	private volatile String subProtocol;
	
	private volatile URI uri;
	
	private ISession session;
	
	private String cause;
	
	private volatile List<IExtension> extensions;
	
	/**
	 * Constructs the handshaker.
	 * 
	 * @param config     the Web Socket configuration of the session this
	 *                   handshaker should be associated with
	 * @param clientMode the mode in witch this handshaker should operate
	 */
	public Handshaker(IWebSocketSessionConfig config, boolean clientMode) {
		this.config = config;
		this.clientMode = clientMode;
	}
	
	@Override
	public void setSession(ISession session) {
		this.session = session;
	}
	
	@Override
	public boolean isClientMode() {
		return clientMode;
	}
	
	@Override
	public boolean isFinished() {
		return finished;
	}
	
	@Override
	public boolean isClosing() {
		return closing;
	}
	
	@Override
	public String getSubProtocol() {
		return subProtocol;
	}
	
	@Override
	public boolean hasExtensions() {
		return extensions != null;
	}
	
	@Override
	public IExtension getExtension(String name) {
		if (extensions != null) {
			for (IExtension extension: extensions) {
				if (extension.getName().equals(name)) {
					return extension;
				}
			}
		}
		return null;
	}
	
	@Override
	public IExtension[] getExtensions() {
		return extensions == null ? EMPTY : extensions.toArray(new IExtension[extensions.size()]);
	}
	
	@Override
	public String[] getExtensionNames() {
		List<IExtension> extensions = this.extensions;
		
		if (extensions == null) {
			return new String[0];
		}
		else {
			Object[] objects = extensions.toArray();
			String[] names = new String[objects.length];

			for (int i=0; i<names.length; ++i) {
				names[i] = ((IExtension)objects[i]).getName();
			}
			return names;
		}
	}
	
	@Override
	public void updateExtensionEncoders(ICodecPipeline pipeline) {
		for (IExtension extension: extensions) {
			extension.updateEncoders(pipeline);
		}
	}

	@Override
	public void updateExtensionDecoders(ICodecPipeline pipeline) {
		for (IExtension extension: extensions) {
			extension.updateDecoders(pipeline);
		}
	}
	
	@Override
	public URI getUri() {
		return uri;
	}
	
	@Override
	public String getClosingReason() {
		return cause;
	}
	
	void cause(String cause) {
		this.cause = cause;
	}
	
	
	HandshakeRequest request() {
		String[] protocols = config.getSupportedSubProtocols();
		IExtension[] extensions = config.getSupportedExtensions();
		String origin = config.getRequestOrigin(); 
		uri = config.getRequestUri();
		HandshakeRequest frame = new HandshakeRequest(HandshakeUtils.requestUri(uri));
		
		frame.addValue(HttpUtils.HOST, HandshakeUtils.host(uri));
		frame.addValue(HttpUtils.UPGRADE, HandshakeUtils.UPGRADE_VALUE);
		frame.addValue(HttpUtils.CONNECTION, HandshakeUtils.CONNECTION_VALUE);
		key = HandshakeUtils.generateKey();
		frame.addValue(HandshakeUtils.SEC_WEB_SOCKET_KEY, key);
		if (origin != null) {
			frame.addValue(HandshakeUtils.ORIGIN, origin);
		}
		frame.addValue(HandshakeUtils.SEC_WEB_SOCKET_VERSION, HandshakeUtils.VERSION);
		if (protocols != null && protocols.length > 0) {
			frame.addValue(HandshakeUtils.SEC_WEB_SOCKET_PROTOCOL, HttpUtils.values(protocols));
		}
		if (extensions != null && extensions.length > 0) {
			String[] offers = new String[extensions.length];
			
			for (int i=0; i<extensions.length; ++i) {
				offers[i] = HandshakeUtils.extension(extensions[i].offer());
			}
			frame.addValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS, HttpUtils.values(offers));
		}
		config.customizeHeaders(frame);
		return frame;
	}
	
	void acceptVersion(HandshakeRequest request) throws HandshakeAcceptException {
		String s = request.getValue(HandshakeUtils.SEC_WEB_SOCKET_VERSION);
		
		if (s != null) {
			int version;

			for (String value: HttpUtils.values(s)) {
				try {
					version = Integer.parseInt(value);
				}
				catch (Exception e) {
					cause("Incorrect websocket version: " + value);
					throw new HandshakeAcceptException(HttpStatus.BAD_REQUEST);
				}
				if (version == HandshakeUtils.VERSION) {
					return;
				}
			}
			HandshakeResponse response = new HandshakeResponse(HttpStatus.UPGRADE_REQUIRED);

			response.addValue(HandshakeUtils.SEC_WEB_SOCKET_VERSION, HandshakeUtils.VERSION);
			cause("Unsupported websocket version: " + s);
			throw new HandshakeAcceptException(response);
		}
		cause("Missing websocket version");
		throw new HandshakeAcceptException(HttpStatus.BAD_REQUEST);
	}
	
	void acceptBasicFields(HandshakeRequest request) throws HandshakeAcceptException {
		if (!validateBasicFields(request)) {
			throw new HandshakeAcceptException(HttpStatus.BAD_REQUEST);
		}
	}
	
	byte[] acceptKey(HandshakeRequest request) throws HandshakeAcceptException {
		String s = request.getValue(HandshakeUtils.SEC_WEB_SOCKET_KEY);
		
		if (s != null) {
			byte[] key = HandshakeUtils.parseKey(s);
			
			if (key != null) {
				return key;
			}
			cause("Invalid websocket key: " + s);
		}
		else {
			cause("Missing websocket key");
		}
		throw new HandshakeAcceptException(HttpStatus.BAD_REQUEST);
	}
	
	void acceptSubProtocol(HandshakeRequest request) {
		String s = request.getValue(HandshakeUtils.SEC_WEB_SOCKET_PROTOCOL);
		
		if (s != null && !s.isEmpty()) {
			String[] supported = config.getSupportedSubProtocols();
			
			if (supported != null) {
				List<String> requested = HttpUtils.values(s);

				for (String req: requested) {
					for (String sup: supported) {
						if ("*".equals(sup) || req.equals(sup)) {
							subProtocol = req;
							return;
						}
					}
				}
			}
		}
	}
	
	private boolean addExtension(IExtension extension) {
		if (extensions == null) {
			extensions = new ArrayList<IExtension>();
			extensions.add(extension);
		}
		else if (getExtension(extension.getName()) == null) {
			extensions.add(extension);
		}
		else {
			return false;
		}
		return true;
	}
	
	void acceptExtensions(HandshakeRequest request) throws HandshakeAcceptException {
		String s = request.getValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS);
		boolean[] groups = new boolean[ExtensionGroup.values().length];
		
		if (s != null && !s.isEmpty()) {
			try {
				List<String> extensions = HttpUtils.values(s);
				IExtension[] supportedExtensions = config.getSupportedExtensions();

				if (supportedExtensions != null) {
					for (String extension: extensions) {
						List<String> splitted = HandshakeUtils.extension(extension);

						for (IExtension supportedExtension: supportedExtensions) {
							IExtension e = supportedExtension.acceptOffer(splitted);
							
							if (e != null && !groups[e.getGroup().ordinal()]) {
								groups[e.getGroup().ordinal()] = true;
								addExtension(e);
							}
						}
					}
				}
			}
			catch (InvalidExtensionException e) {
				extensions = null;
				cause("Invalid websocket request extension");
				throw new HandshakeAcceptException(HttpStatus.BAD_REQUEST);
			}
		}
	}
	
	void acceptUri(HandshakeRequest request) throws HandshakeAcceptException {		
		try {
			URI uri = new URI(request.getUri());
			
			if (!uri.isAbsolute()) {
				String host = request.getValue(HttpUtils.HOST);
				String scheme;
				
				if (host == null && !config.ignoreHostHeaderField()) {
					cause("Missing websocket request host");
					throw new HandshakeAcceptException(HttpStatus.BAD_REQUEST);
				}
				scheme = (session instanceof SSLSession) ? HandshakeUtils.WSS : HandshakeUtils.WS;
				uri = new URI(scheme + "://" + host + request.getUri());
			}
			else {
				String scheme;
				
				if (HandshakeUtils.isHttp(uri)) {
					scheme = HandshakeUtils.WS;
				}
				else if (HandshakeUtils.isHttps(uri)) {
					scheme = HandshakeUtils.WSS;
				}
				else {
					scheme = null;
				}
				
				if (scheme != null) {
					uri = new URI(scheme + "://" + uri.getHost() + uri.getPath());
				}
			}		
			
			if (config.acceptRequestUri(uri)) {
				this.uri = uri;
			}
			else {
				cause("Unacceptable websocket request uri: " + uri);
				throw new HandshakeAcceptException(HttpStatus.NOT_FOUND);
			}
			
		} catch (URISyntaxException e) {
			cause("Invalid websocket request uri: " + e.getMessage());
			throw new HandshakeAcceptException(HttpStatus.BAD_REQUEST);
		}
	}
	
	HandshakeResponse accept(HandshakeRequest request) {
		try {
			acceptVersion(request);	
			acceptBasicFields(request);
			acceptUri(request);
			acceptKey(request);
			acceptSubProtocol(request);
			acceptExtensions(request);
		} catch (HandshakeAcceptException e) {
			return e.getResponse();
		}
		
		HandshakeResponse frame = new HandshakeResponse(HttpStatus.SWITCHING_PROTOCOLS);
		
		frame.addValue(HttpUtils.UPGRADE, HandshakeUtils.UPGRADE_VALUE);
		frame.addValue(HttpUtils.CONNECTION, HandshakeUtils.CONNECTION_VALUE);
		frame.addValue(HandshakeUtils.SEC_WEB_SOCKET_ACCEPT,
				HandshakeUtils.generateAnswerKey(request.getValue(HandshakeUtils.SEC_WEB_SOCKET_KEY)));
		if (subProtocol != null) {
			frame.addValue(HandshakeUtils.SEC_WEB_SOCKET_PROTOCOL, subProtocol);
		}
		if (extensions != null) {
			String[] responses = new String[extensions.size()];
			int i = 0;
			
			for (IExtension extension: extensions) {
				responses[i++] = HandshakeUtils.extension(extension.response());
			}
			frame.addValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS, HttpUtils.values(responses));
		}
		config.customizeHeaders(frame);
		return frame;
	}
	
	static boolean contains(String tokens, String value) {
		boolean contains = false;
		
		for (String token: HttpUtils.values(tokens)) {
			if (token.equalsIgnoreCase(value)) {
				contains = true;
				break;
			}
		}
		return contains;
	}
	
	boolean validateBasicFields(HandshakeFrame frame) {
		String u = frame.getValue(HttpUtils.UPGRADE);
		String c = frame.getValue(HttpUtils.CONNECTION);
		
		if (u == null) {
			cause("Missing websocket upgrade");
			return false;
		}
		if (c == null) {
			cause("Missing websocket connection");
			return false;
		}
		if (contains(u, HandshakeUtils.UPGRADE_VALUE)) {
			if (contains(c, HandshakeUtils.CONNECTION_VALUE)) {
				return true;
			}
			else {
				cause("Invalid websocket connection: " + c);
			}
		}
		else {
			cause("Invalid websocket upgrade: " + u);
		}
		return false;
	}
	
	boolean validateKeyChallenge(HandshakeResponse response) {
		String actual = response.getValue(HandshakeUtils.SEC_WEB_SOCKET_ACCEPT);
		
		if (actual != null) {
			String expected = HandshakeUtils.generateAnswerKey(key);
			
			if (actual.equals(expected)) {
				return true;
			}
			cause("Invalid websocket key challenge. Actual: " + actual + ". Expected: " + expected);
			return false;
		}
		cause("Missing websocket key challenge");
		return false;
	}
	
	boolean validateSubProtocol(HandshakeResponse response) {
		String received = response.getValue(HandshakeUtils.SEC_WEB_SOCKET_PROTOCOL);
		String[] supported = config.getSupportedSubProtocols();
		
		if (supported != null && supported.length > 0) {
			if (received != null) {
				for (String s: supported) {
					if (received.equals(s)) {
						subProtocol = received;
						return true;
					}
				}
			}
			else {
				cause("Missing websocket sub protocol");
				return false;
			}
		}
		else if (received == null) {
			return true;
		}
		cause("Invalid websocket sub protocol: " + received);
		return false;
	}
	
	boolean validateExtensions(HandshakeResponse response) {
		String received = response.getValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS);
		IExtension[] supported = config.getSupportedExtensions();
		
		if (supported != null && supported.length > 0) {
			if (received != null) {
				List<String> extensions = HttpUtils.values(received);
				boolean[] groups = new boolean[ExtensionGroup.values().length];
				
				for (String e: extensions) {
					List<String> extension = HandshakeUtils.extension(e);
					boolean valid = false;
					
					for (IExtension s: supported) {
						IExtension ext;
						
						try {
							ext = s.validateResponse(extension);
						} catch (InvalidExtensionException e1) {
							cause("Invalid extension: " + e1.getMessage());
							this.extensions = null;
							return false;
						}
						
						if (ext != null) {
							if (groups[ext.getGroup().ordinal()] || !addExtension(ext)) {
								this.extensions = null;
								return false;
							}
							groups[ext.getGroup().ordinal()] = true;
							valid = true;
							break;
						}
					}
					if (!valid) {
						this.extensions = null;
						return false;
					}
				}
				return true;
			}
		}
		if (received == null) {
			return true;
		}
		return false;
	}
	
	boolean validate(HandshakeResponse response) {
		if (response.getStatus() == HttpStatus.SWITCHING_PROTOCOLS.getStatus()) {
			return validateBasicFields(response)
				&& validateKeyChallenge(response)
				&& validateSubProtocol(response)
				&& validateExtensions(response);
		}
		cause("Invalid websocket response status: " + response.getStatus());
		return false;
	}
	
	@Override
	public HandshakeFrame handshake() {
		if (clientMode) {
			return request();
		}
		return null;
	}
	
	@Override
	public HandshakeFrame handshake(HandshakeFrame frame) throws InvalidHandshakeException {
		if (clientMode) {
			if (frame instanceof HandshakeResponse) {
				if (validate((HandshakeResponse) frame)) {
					finished = true;
				}
				else {
					closing = true;
				}
				return null;
			}
		}
		else if (frame instanceof HandshakeRequest) {
			HandshakeResponse response = accept((HandshakeRequest) frame);

			if (response.getStatus() == HttpStatus.SWITCHING_PROTOCOLS.getStatus()) {
				finished = true;
			}
			else {
				closing = true;
			}
			return response;
		}
		throw new InvalidHandshakeException();
	}
}
