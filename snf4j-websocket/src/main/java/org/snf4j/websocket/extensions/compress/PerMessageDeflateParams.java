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
package org.snf4j.websocket.extensions.compress;

import java.util.List;

import org.snf4j.websocket.extensions.InvalidExtensionException;

class PerMessageDeflateParams {
	
	final static String CLIENT_NO_CONTEXT = "client_no_context_takeover";

	final static String SERVER_NO_CONTEXT = "server_no_context_takeover";
	
	final static String CLIENT_MAX_WINDOW = "client_max_window_bits";

	final static String SERVER_MAX_WINDOW = "server_max_window_bits";
	
	final static int NO_VALUE = -1;
	
	final static int MAX_MAX_WINDOW_VALUE = 15;
	
	final static int MIN_MAX_WINDOW_VALUE = 8;
	
	private final boolean serverNoContext;
	
	private final boolean clientNoContext;
	
	private final Integer serverMaxWindow;
	
	private final Integer clientMaxWindow;

	PerMessageDeflateParams(boolean serverNoContext, boolean clientNoContext, Integer serverMaxWindow, Integer clientMaxWindow) {
		this.serverNoContext = serverNoContext;
		this.clientNoContext = clientNoContext;
		this.serverMaxWindow = serverMaxWindow;
		this.clientMaxWindow = clientMaxWindow;
	}
	
	boolean isServerNoContext() {
		return serverNoContext;
	}
	
	boolean isClientNoContext() {
		return clientNoContext;
	}
	
	Integer getServerMaxWindow() {
		return serverMaxWindow;
	}

	Integer getClientMaxWindow() {
		return clientMaxWindow;
	}

	static boolean checkBits(int bits) {
		return bits >= MIN_MAX_WINDOW_VALUE && bits <= MAX_MAX_WINDOW_VALUE;
	}

	private static void set(int[] params, int index, String value) throws InvalidExtensionException {
		if (params[index] != 0) {
			throw new InvalidExtensionException("Duplicated parameter");
		}
		if (value != null) {
			throw new InvalidExtensionException("Unnecessary parameter value");
		}
		params[index] = NO_VALUE;
	}

	private static void setInt(int[] params, int index, String value, boolean nullable) throws InvalidExtensionException {
		if (params[index] != 0) {
			throw new InvalidExtensionException("Duplicated parameter");
		}
		if (value == null) {
			if (nullable) {
				params[index] = NO_VALUE;
				return;
			}
			else {
				throw new InvalidExtensionException("Missing parameter value");
			}
		}
		
		int v;
		
		try {
			v = Integer.parseInt(value);
		}
		catch (Exception e) {
			throw new InvalidExtensionException("Invalid parameter value format");
		}
		
		if (checkBits(v)) {
			params[index] = v;
		}
		else {
			throw new InvalidExtensionException("Invalid parameter value");
		}
	}
	
	static PerMessageDeflateParams parse(List<String> extension) throws InvalidExtensionException {
		int size = extension.size();
		int[] values = new int[4];
		
		for (int i=1; i<size; i+=2) {
			String p = extension.get(i);
			String v = extension.get(i+1);
			
			if (CLIENT_NO_CONTEXT.equalsIgnoreCase(p)) {
				set(values, 0, v);
			}
			else if (SERVER_NO_CONTEXT.equalsIgnoreCase(p)) {
				set(values, 1, v);
			}
			else if (CLIENT_MAX_WINDOW.equalsIgnoreCase(p)) {
				setInt(values, 2, v, true);
			}
			else if (SERVER_MAX_WINDOW.equalsIgnoreCase(p)) {
				setInt(values, 3, v, false);
			}
			else {
				throw new InvalidExtensionException("Unexpected parameter");
			}
		}
		
		return new PerMessageDeflateParams(values[1] == NO_VALUE,
				values[0] == NO_VALUE,
				values[3] == 0 ? null : values[3],
				values[2] == 0 ? null : values[2]
				);
	}
}
