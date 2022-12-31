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
package org.snf4j.tls.extension;

import org.snf4j.tls.handshake.HandshakeType;

public class ExtensionValidator implements IExtensionValidator {

	public final static IExtensionValidator DEFAULT = new ExtensionValidator();
	
	private final static int CH = HandshakeType.CLIENT_HELLO.value();
	
	private final static int SH = HandshakeType.SERVER_HELLO.value();
	
	private final static int EE = HandshakeType.ENCRYPTED_EXTENSIONS.value();
	
	private final static int CT = HandshakeType.CERTIFICATE.value();
	
	private final static int CR = HandshakeType.CERTIFICATE_REQUEST.value();
	
	private final static int NST = HandshakeType.NEW_SESSION_TICKET.value();
	
	private final static int HRR = 6;
	
	private final static int[] MAPPING = new int[ExtensionType.KEY_SHARE.value() + 1];
	
	private static void addMapping(ExtensionType extensionType, int... handshakeTypes) {
		int bits = 0;
		
		for (int handshakeType: handshakeTypes) {
			bits |= 1 << handshakeType;
		}
		MAPPING[extensionType.value()] = bits;
	}
	
	static {
		addMapping(ExtensionType.SERVER_NAME, CH, EE );
		addMapping(ExtensionType.MAX_FRAGMENT_LENGTH, CH, EE );
		addMapping(ExtensionType.STATUS_REQUEST, CH, CR, CT );
		addMapping(ExtensionType.SUPPORTED_GROUPS, CH, EE );
		addMapping(ExtensionType.SIGNATURE_ALGORITHMS, CH, CR );
		addMapping(ExtensionType.USE_SRTP, CH, EE );
		addMapping(ExtensionType.HEARTBEAT, CH, EE );
		addMapping(ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION, CH, EE );
		addMapping(ExtensionType.SIGNED_CERTIFICATE_TIMESTAMP, CH, CR, CT );
		addMapping(ExtensionType.CLIENT_CERTIFICATE_TYPE, CH, EE );
		addMapping(ExtensionType.SERVER_CERTIFICATE_TYPE, CH, EE );
		addMapping(ExtensionType.PADDING, CH );
		addMapping(ExtensionType.KEY_SHARE, CH, SH, HRR );
		addMapping(ExtensionType.PRE_SHARED_KEY, CH, SH );
		addMapping(ExtensionType.PSK_KEY_EXCHANGE_MODES, CH );
		addMapping(ExtensionType.EARLY_DATA, CH, EE, NST );
		addMapping(ExtensionType.COOKIE, CH, HRR );
		addMapping(ExtensionType.SUPPORTED_VERSIONS, CH, SH, HRR );
		addMapping(ExtensionType.CERTIFICATE_AUTHORITIES, CH, CR );
		addMapping(ExtensionType.OID_FILTERS, CR );
		addMapping(ExtensionType.POST_HANDSHAKE_AUTH, CH );
		addMapping(ExtensionType.SIGNATURE_ALGORITHMS_CERT, CH, CR );
	}

	private boolean isAllowed(ExtensionType extensionType, int handshakeValue) {
		int type = extensionType.value();
		
		if (type >=0 && type < MAPPING.length) {
			if (handshakeValue >= 0 && handshakeValue < 31) {
				return (MAPPING[type] & (1 << handshakeValue)) != 0; 
			}
		}
		return false;
	}
	
	@Override
	public boolean isAllowed(ExtensionType extensionType, HandshakeType handshakeType) {
		return isAllowed(extensionType, handshakeType.value());
	}

	@Override
	public boolean isAllowedInHelloRetryRequest(ExtensionType extensionType) {
		return isAllowed(extensionType, HRR);
	}
}
