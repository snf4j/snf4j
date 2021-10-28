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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

class HandshakeUtils {

	final static String SEC_WEB_SOCKET_KEY = "Sec-WebSocket-Key";
	
	final static String SEC_WEB_SOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
	
	final static String SEC_WEB_SOCKET_VERSION = "Sec-WebSocket-Version";
	
	final static String SEC_WEB_SOCKET_ACCEPT = "Sec-WebSocket-Accept";
	
	final static String SEC_WEB_SOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";
	
	final static String ORIGIN = "Origin";
	
	final static String UPGRADE_VALUE = "websocket";

	final static String CONNECTION_VALUE = HttpUtils.UPGRADE;
	
	final static int VERSION = 13;
	
	final static Random RANDOM = new Random();
	
	final static byte[] KEY_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(StandardCharsets.US_ASCII);
	
	final static String WS = "ws";
	
	final static String WSS = "wss";
	
	final static int REQUEST_LENGTH = HttpUtils.GET.length 
			+ 1 
			+ /*URI+*/ 1 
			+ HttpUtils.HTTP_VERSION.length 
			+ HttpUtils.CRLF.length 
			+ /*FLDS+*/ HttpUtils.CRLF.length;
	
	final static int RESPONSE_LENGTH = HttpUtils.HTTP_VERSION.length 
			+ 1 
			+ HttpUtils.STATUS_CODE_LENGTH 
			+ 1 
			+ /*REASON+*/ HttpUtils.CRLF.length
			+ /*FLDS+*/ HttpUtils.CRLF.length;
	
	private HandshakeUtils() {	
	}
	
	static String generateKey() {
		byte[] bytes = new byte[16];
		
		RANDOM.nextBytes(bytes);
		return generateKey(bytes);
	}
	
	static String generateKey(byte[] bytes) {
		bytes = Base64.encode(bytes);
		return HttpUtils.ascii(bytes, 0, bytes.length);
	}
	
	static String generateAnswerKey(String key) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] bytes;
			
			md.update(HttpUtils.bytes(key));
			bytes = Base64.encode(md.digest(KEY_GUID));
			return HttpUtils.ascii(bytes, 0, bytes.length);
		}
		catch (Exception e) {
			return null;
		}
	}
	
	static byte[] parseKey(String key) {
		byte[] bytes = Base64.decode(HttpUtils.bytes(key));
			
		if (bytes != null && bytes.length == 16) {
			return bytes;
		}
		return null;
	}
	
	static String requestUri(URI uri) {
		String path = uri.getRawPath();
		String query = uri.getRawQuery();
		
		if (path == null || path.isEmpty()) {
			path = "/";
		}
		if (query != null && !query.isEmpty()) {
			path += '?' + query;
		}
		return path;
	}
	
	static boolean isHttp(URI uri) {
		String scheme = uri.getScheme();
		
		return HttpUtils.HTTP.equalsIgnoreCase(scheme);
	}

	static boolean isHttps(URI uri) {
		String scheme = uri.getScheme();
		
		return HttpUtils.HTTPS.equalsIgnoreCase(scheme);
	}
	
	static boolean isNotSecure(URI uri) {
		String scheme = uri.getScheme();
		
		return HttpUtils.HTTP.equalsIgnoreCase(scheme) || WS.equalsIgnoreCase(scheme);
	}

	static boolean isSecure(URI uri) {
		String scheme = uri.getScheme();
		
		return HttpUtils.HTTPS.equalsIgnoreCase(scheme) || WSS.equalsIgnoreCase(scheme);
	}
	
	static int port(URI uri) {
		int port = uri.getPort();
		
		if (port == -1) {
			if (isNotSecure(uri)) {
				port = HttpUtils.HTTP_PORT;
			}
			else if (isSecure(uri)) {
				port = HttpUtils.HTTPS_PORT;
			}
		}
		return port;
	}
	
	static String host(URI uri) {
		int port = uri.getPort();
		
		if (port == -1) {
			return uri.getHost();
		}
		
		String host = uri.getHost();
		
		switch (port) {
		case HttpUtils.HTTP_PORT:
			if (isNotSecure(uri)) {
				port = -1;
			}
			break;
			
		case HttpUtils.HTTPS_PORT:
			if (isSecure(uri)) {
				port = -1;
			}
			break;
		}
		
		return port == -1 ? host : host + ':' + port;
	}	
	
	static List<String> extension(String extension) {
		List<String> items = HttpUtils.values(extension, ";");
		
		if (items.size() <= 1) {
			return items;
		}

		List<String> parsed = new ArrayList<String>();
		Iterator<String> i = items.iterator();
		
		parsed.add(i.next());
		while (i.hasNext()) {
			String item = i.next();
			int pos = item.indexOf('=');

			if (pos >= 0) {
				String value = item.substring(pos+1).trim();
				int len = value.length();
				
				if (len > 1) {
					if (value.charAt(0) == '"' && value.charAt(len-1) == '"') {
						value = value.substring(1, len-1);
					}
				}
				parsed.add(item.substring(0,pos).trim());
				parsed.add(value);
			}
			else {
				parsed.add(item);
				parsed.add(null);
			}
		}
		return parsed;
	}
	
	static String extension(List<String> items) {
		StringBuilder sb = new StringBuilder();
		Iterator<String> i = items.iterator();
		
		sb.append(i.next());
		for (; i.hasNext();) {
			String s;
			
			sb.append("; ");
			sb.append(i.next());
			s = i.next();
			if (s != null) {
				sb.append("=");
				sb.append(s);
			}
		}
		return sb.toString();
	}
	
}
