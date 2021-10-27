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
package org.snf4j.example.websocket;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.snf4j.core.SSLSession;
import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IStreamSession;

public class IndexPageDecoder implements IDecoder<byte[],byte[]>, IBaseDecoder<byte[], byte[]> {
	
	final static byte CR = 13;
	
	final static byte LF = 10;
	
	final static byte[] CRLF = new byte[] {CR, LF};
	
	final static byte[] CRLF2 = new byte[] {CR, LF, CR, LF};

	final static String GET = "GET";
	
	final static String HTTP_VERSION = "HTTP/1.1";
	
	final static String OK = "200 OK";
	
	final static String BAD_REQUEST = "400 Bad Request";
	
	final static String FORBIDDEN = "403 Forbidden";
	
	final static String NOT_FOUND = "404 Not Found";
	
	final static String CONTENT_TYPE = "Content-Type: text/html; charset=UTF-8";
	
	final static String INDEX_PAGE_DECODER = "index-page-decoder";
	
	final static String[] INDEX_PAGE_ENDPOINTS = new String[] { "/", "/index.htm", "/index.html" };

	final static String[] WEBSOCKET_ENDPOINTS = new String[] { SessionConfig.CHAT_PATH, SessionConfig.ECHO_PATH };
	
	final private String host;
	
	static boolean matches(String value, String[] expectedValues) {
		for (String ev: expectedValues) {
			if (ev.equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}
	
	public IndexPageDecoder(String host) {
		this.host = host;
	}
	
	@Override
	public Class<byte[]> getInboundType() {
		return byte[].class;
	}

	@Override
	public Class<byte[]> getOutboundType() {
		return byte[].class;
	}

	@Override
	public int available(ISession session, ByteBuffer buffer, boolean flipped) {
		ByteBuffer duplicate = buffer.duplicate();
		byte[] data;
		
		if (!flipped) {
			duplicate.flip();
		}
		data = new byte[duplicate.remaining()];
		duplicate.get(data);
		return available(session, data, 0, data.length);
	}

	@Override
	public int available(ISession session, byte[] buffer, int off, int len) {
		if (len < CRLF2.length) {
			return 0;
		}
		for (int i=off; i<off+len-3; ++i) {
			boolean found = true;
			
			for (int j=0; j<4; ++j) {
				if (buffer[i+j] != CRLF2[j]) {
					found = false;
				}
			}
			if (found) {
				return i+4;
			}
		}
		return 0;
	}

	void response(ISession session, String status, String[] fields, String content) {
		StringBuilder response = new StringBuilder();
		
		response.append(HTTP_VERSION);
		response.append(" ");
		response.append(status);
		response.append(new String(CRLF));
		if (fields != null && fields.length > 0) {
			for (String field: fields) {
				response.append(field);
				response.append(new String(CRLF));
			}
		}
		response.append(new String(CRLF));
		if (content != null && !content.isEmpty()) {
			response.append(content);
		}
		((IStreamSession)session).writenf(response.toString().getBytes(StandardCharsets.US_ASCII));
		session.close();
	}
	
	@Override
	public void decode(ISession session, byte[] data, List<byte[]> out) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
		
		try {
			String line = in.readLine();
			
			if (line != null) {
				String[] items = line.split(" ");
				
				if (items.length > 2 && HTTP_VERSION.equals(items[2])) {
					if (GET.equalsIgnoreCase(items[0])) {
						if (matches(items[1], INDEX_PAGE_ENDPOINTS)) {
							String protocol = (session instanceof SSLSession) 	? "wss://" : "ws://";

							response(session, OK, new String[] { CONTENT_TYPE },
									PageContent.get(protocol + host + SessionConfig.CHAT_PATH));
						}
						else if (matches(items[1], WEBSOCKET_ENDPOINTS)) {
							session.getCodecPipeline().remove(INDEX_PAGE_DECODER);
							out.add(data);
						}
						else {
							response(session, NOT_FOUND, null, null);
						}
						return;
					}
					else {
						response(session, FORBIDDEN, null, null);
					}
				}
			}
			response(session, BAD_REQUEST, null, null);
		}
		finally {
			in.close();
		}
	}

}
