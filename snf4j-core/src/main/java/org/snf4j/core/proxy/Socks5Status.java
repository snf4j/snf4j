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

enum Socks5Status {
    SUCCESS(0, "Succeeded"),
    FAILURE(1, "General SOCKS server failure"),
    FORBIDDEN(2, "Connection not allowed by ruleset"),
    NETWORK_UNREACHABLE(3, "Network unreachable"),
    HOST_UNREACHABLE(4, "Host unreachable"),
    CONNECTION_REFUSED(5, "Connection refused"),
    TTL_EXPIRED(6, "TTL expired"),
    COMMAND_UNSUPPORTED(7, "Command not supported"),
    ADDRESS_UNSUPPORTED(8, "Address type not supported"),
    UNKNOWN(-1, "Unknown");
	
	private final int code;
	
	private final String description;
	
	Socks5Status(int code, String description) {
		this.code = code;
		this.description = description;
	}
	
	public int code() {
		return code;
	}
	
	public String description() {
		return description;
	}
	
	public static Socks5Status valueOf(int code) {
		for (Socks5Status value: values()) {
			if (value.code == code) {
				return value;
			}
		}
		return UNKNOWN;
	}

}
