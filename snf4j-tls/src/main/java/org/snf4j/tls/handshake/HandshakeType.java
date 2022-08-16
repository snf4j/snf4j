/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.tls.handshake;

import org.snf4j.tls.IntConstant;

public class HandshakeType extends IntConstant {
	
	public static final HandshakeType CLIENT_HELLO = new HandshakeType(
			"client_hello",1);
	
	public static final HandshakeType SERVER_HELLO =  new HandshakeType(
			"server_hello",2);
	
	public static final HandshakeType NEW_SESSION_TICKET =  new HandshakeType(
			"new_session_ticket",4);
	
	public static final HandshakeType END_OF_EARLY_DATA =  new HandshakeType(
			"end_of_early_data",5);
	
	public static final HandshakeType ENCRYPTED_EXTENSIONS =  new HandshakeType(
			"encrypted_extensions",8);
	
	public static final HandshakeType CERTIFICATE =  new HandshakeType(
			"certificate",11);
	
	public static final HandshakeType CERTIFICATE_REQUEST =  new HandshakeType(
			"certificate_request",13);
	
	public static final HandshakeType CERTIFICATE_VERIFY =  new HandshakeType(
			"certificate_verify",15);
	
	public static final HandshakeType FINISHED =  new HandshakeType(
			"finished",20);
	
	public static final HandshakeType KEY_UPDATE =  new HandshakeType(
			"key_update",24);
	
	public static final HandshakeType MESSAGE_HASH =  new HandshakeType(
			"message_hash",254);

	private final static HandshakeType[] KNOWN = new HandshakeType[MESSAGE_HASH.value()+1];
	
	private static void known(HandshakeType... knowns) {
		for (HandshakeType known: knowns) {
			KNOWN[known.value()] = known;
		}
	}
	
	static {
		known(
				CLIENT_HELLO,
				SERVER_HELLO,
				NEW_SESSION_TICKET,
				END_OF_EARLY_DATA,
				ENCRYPTED_EXTENSIONS,
				CERTIFICATE,
				CERTIFICATE_REQUEST,
				CERTIFICATE_VERIFY,
				FINISHED,
				KEY_UPDATE,
				MESSAGE_HASH
				);
	}
	
	protected HandshakeType(String name, int value) {
		super(name, value);
	}

	protected HandshakeType(int value) {
		super(value);
	}

	public static HandshakeType of(int value) {
		if (value >= 0 && value < KNOWN.length) {
			HandshakeType known = KNOWN[value];
			
			if (known != null) {
				return known;
			}
		}
		return new HandshakeType(value);
	}
	
}
