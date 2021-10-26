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
package org.snf4j.core.proxy;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.snf4j.core.handler.SessionEvent;

/**
 * Handles client proxy connections via the HTTP tunneling protocol. For more
 * details about the protocol refer to <a href=
 * "http://en.wikipedia.org/wiki/HTTP_tunnel#HTTP_CONNECT_tunneling">HTTP
 * CONNECT tunneling</a>
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class HttpProxyHandler extends AbstractProxyHandler {

	private final static byte CR = (byte)13;
	
	private final static byte LF = (byte)10;
	
	private final static String SP_TEXT = " ";

	private final static byte SP = ' ';
	
	private final static String COLON_TEXT = ":";

	private final static byte COLON = ':';
	
	private final static byte[] CRLF = new byte[] {CR,LF};	
	
	private final static byte[] HTTP_CONNECT = toBytes("CONNECT");

	private final static String HTTP_VERSION_TEXT = "HTTP/1.1";

	private final static byte[] HTTP_VERSION = toBytes(HTTP_VERSION_TEXT);

	private final static byte[] HOST = toBytes("Host");
	
	private final static int OK = 200;
	
	private final List<byte[]> headers = new LinkedList<byte[]>(); 

	private final int minEof;
	
	private final URI uri;
	
	private Integer statusCode;
	
	private int headersLength;
	
	/**
	 * Constructs an HTTP tunnel connection handler with the default (10 seconds)
	 * connection timeout.
	 * 
	 * @param uri the URI identifying the remote host to which the HTTP tunnel
	 *            should be established
	 */
	public HttpProxyHandler(URI uri) {	
		this(uri, false);
	}
	
	/**
	 * Constructs an HTTP tunnel connection handler with the default (10 seconds)
	 * connection timeout and an option to change the default handling of line
	 * terminators.
	 * 
	 * @param uri                  the URI identifying the remote host to which the
	 *                             HTTP tunnel should be established
	 * @param allowBothTerminators {@code true} to allow both CRLF and LF line
	 *                             terminators in the responses from a HTTP proxy
	 *                             server, or otherwise (default option) only CRLF
	 *                             will be allowed
	 */
	public HttpProxyHandler(URI uri, boolean allowBothTerminators) {	
		if (uri == null) {
			throw new IllegalArgumentException("uri is null");
		}
		this.uri = uri;
		minEof = allowBothTerminators ? 1 : 2;
	}

	/**
	 * Constructs an HTTP tunnel connection handler with the specified connection
	 * timeout.
	 * 
	 * @param uri               the URI identifying the remote host to which the
	 *                          HTTP tunnel should be established
	 * @param connectionTimeout the proxy connection timeout in milliseconds, or
	 *                          {@code 0} to wait an infinite amount of time for 
	 *                          establishing the HTTP tunnel.
	 */
	public HttpProxyHandler(URI uri, long connectionTimeout) {
		this(uri, connectionTimeout, false);
	}
	
	/**
	 * Constructs an HTTP tunnel connection handler with the specified connection
	 * timeout and an option to change the default handling of line terminators.
	 * 
	 * @param uri                  the URI identifying the remote host to which the
	 *                             HTTP tunnel should be established
	 * @param connectionTimeout    the proxy connection timeout in milliseconds, or
	 *                             {@code 0} to wait an infinite amount of time for
	 *                             establishing the HTTP tunnel.
	 * @param allowBothTerminators {@code true} to allow both CRLF and LF line
	 *                             terminators in the responses from a HTTP proxy
	 *                             server, or otherwise (default option) only CRLF
	 *                             will be allowed
	 */
	public HttpProxyHandler(URI uri, long connectionTimeout, boolean allowBothTerminators) {
		super(connectionTimeout);
		if (uri == null) {
			throw new IllegalArgumentException("uri is null");
		}
		this.uri = uri;
		minEof = allowBothTerminators ? 1 : 2;
	}
	
	private static final byte[] toBytes(String s) {
		return s.getBytes(StandardCharsets.US_ASCII);
	}
	
	private int eol(byte[] data, int off, int end) {
		switch (end-off) {
		case 0:
			return 0;
			
		case 1:
			return data[off] == LF ? 1 : 0;
			
		default:
			byte b = data[off];
			
			if (b == CR) {
				return data[off+1] == LF ? 2 : 0;
			}
			return b == LF ? 1 :0;
		}
	}
	
	@Override
	public int available(byte[] data, int off, int len) {
		int lastEol = off;
		int end = off+len;
		
		for (int i=off; i<end; ++i) {
			int eol = eol(data, i, end);
			
			if (eol >= minEof) {
				lastEol = i + eol;
				i += eol;
				eol = eol(data, i, end);
				if (eol >= minEof) {
					lastEol = i + eol; 
				}
			}
		}
		return lastEol - off;
	}

	private int findEol(byte[] data, int off) {
		int end = data.length;
		
		for (int i=off; i<end; ++i) {
			int eol = eol(data, i, end);
			
			if (eol >= minEof) {
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public void read(byte[] data) {
		int off = 0;
		int eol;
		String line;
		
		do {
			eol = findEol(data, off);
			if (eol == -1) {
				throw new ProxyConnectionException("Unexpected internal EOF handling");
			}
			line = new String(data, off, eol-off, StandardCharsets.US_ASCII);
			off = eol + (data[eol] == CR ? 2 : 1);
			
			if (statusCode == null) {
				String[] items = line.split(SP_TEXT);

				if (items.length < 3) {
					throw new ProxyConnectionException("Invalid HTTP proxy response");
				}
				if (!items[0].equalsIgnoreCase(HTTP_VERSION_TEXT)) {
					throw new ProxyConnectionException("Unexpected HTTP proxy response version");
				}
				
				int sc;
				
				try {
					sc = Integer.parseInt(items[1]); 
				}
				catch (Exception e) {
					throw new ProxyConnectionException("Invalid status code format in HTTP proxy response");
				}
				if (sc != OK) {
					throw new ProxyConnectionException("HTTP proxy response status code: " + sc);
				}
				getSession().getPipeline().markDone();
				statusCode = sc;
			}
			else if (line.isEmpty()) {
				getSession().close();
			}
		}
		while (off < data.length);
	}

	@Override
	public void read(ByteBuffer data) {
		byte[] bytes = new byte[data.remaining()];
		
		data.get(bytes);
		getSession().release(data);
		read(bytes);
	}

	@Override
	public void event(SessionEvent event) {
		if (event == SessionEvent.OPENED) {
			getSession().getPipeline().markUndone(new ProxyConnectionException("Incomplete HTTP proxy protocol"));
		}
		super.event(event);
	}
	
	/**
	 * Appends an HTTP header to the HTTP CONNECT method request being sent to an
	 * HTTP proxy server.
	 * <p>
	 * There is no need to append the Host header as it is appended by default.
	 * 
	 * @param name  the name of the HTTP header
	 * @param value the value of the HTTP header
	 */
	public void appendHeader(String name, String value) {
		byte[] bytes;
		
		synchronized (headers) {
			bytes = toBytes(name);
			headersLength += bytes.length;
			headers.add(bytes);
			bytes = toBytes(value);
			headersLength += bytes.length;
			headers.add(bytes);
			headersLength += 4 /* COLON + SP + CRLF */;
		}
	}
	
	/**
	 * Returns the default port number associated with the scheme in the provided
	 * URI.
	 * <p>
	 * By default it calls {@code uri.toURL().getDefaultPort()}
	 * 
	 * @param uri the provided URI
	 * @return the default port number, or {@code -1} if the default port number is
	 *         not defined for the scheme in the provided URI
	 * @throws Exception if the provided URI was malformed
	 */
	protected int getDefaultPort(URI uri) throws Exception {
		return uri.toURL().getDefaultPort();
	}
	
	private void append(ByteBuffer frame, byte[] name, byte[] value) {
		frame.put(name);
		frame.put(COLON);
		frame.put(SP);
		frame.put(value);
		frame.put(CRLF);
	}
	
	@Override
	protected void handleReady() throws Exception {
		String uriHost = uri.getHost();
		
		if (uriHost == null) {
			throw new ProxyConnectionException("Undefined host");
		}
		
		int uriPort = uri.getPort();
		int defaultPort;
		
		try {
			defaultPort = getDefaultPort(uri);
			if (uriPort == -1) {
				uriPort = defaultPort;
			}
		} catch (Exception e) {
			defaultPort = -1;
		}
		
		byte[] fullHost;
		byte[] host;
		
		if (uriPort != -1) {
			fullHost = toBytes(uriHost + COLON_TEXT + Integer.toString(uriPort));
		}
		else {
			fullHost = toBytes(uriHost);
		}
		if (uriPort != defaultPort) {
			host = fullHost;
		}
		else {
			host = toBytes(uriHost);
		}
		
		ByteBuffer frame;
		
		synchronized (headers) {
			int length = HTTP_CONNECT.length 
				+ 1 /* SP */
				+ fullHost.length 
				+ 1 /* SP */
				+ HTTP_VERSION.length 
				+ CRLF.length
				+ HOST.length 
				+ 2 + /* COLON + SP */
				host.length 
				+ CRLF.length
				+ headersLength
				+ CRLF.length;
		
			frame = getSession().allocate(length);
			frame.put(HTTP_CONNECT);
			frame.put(SP);
			frame.put(fullHost);
			frame.put(SP);
			frame.put(HTTP_VERSION);
			frame.put(CRLF);
			append(frame, HOST, host);

			Iterator<byte[]> i = headers.iterator();
			
			while (i.hasNext()) {
				byte[] name = i.next();
				byte[] value = i.next();
				append(frame, name, value);
			}
		}
		
		frame.put(CRLF);
		frame.flip();
		getSession().writenf(frame);
	}

	@Override
	public void read(Object msg) {
	}

}
