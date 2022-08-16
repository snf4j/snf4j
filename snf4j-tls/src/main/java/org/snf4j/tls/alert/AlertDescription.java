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
package org.snf4j.tls.alert;

import org.snf4j.tls.IntConstant;

public class AlertDescription extends IntConstant {

	public static final AlertDescription CLOSE_NOTIFY = new AlertDescription(
			"close_notify",0);
	
	public static final AlertDescription UNEXPECTED_MESSAGE = new AlertDescription(
			"unexpected_message",10);
	
	public static final AlertDescription BAD_RECORD_MAC = new AlertDescription(
			"bad_record_mac",20);
	
	public static final AlertDescription RECORD_OVERFLOW = new AlertDescription(
			"record_overflow",22);
	
	public static final AlertDescription HANDSHAKE_FAILURE = new AlertDescription(
			"handshake_failure",40);
	
	public static final AlertDescription BAD_CERTIFICATE = new AlertDescription(
			"bad_certificate",42);
	
	public static final AlertDescription UNSUPPORTED_CERTIFICATE = new AlertDescription(
			"unsupported_certificate",43);
	
	public static final AlertDescription CERTIFICATE_REVOKED = new AlertDescription(
			"certificate_revoked",44);
	
	public static final AlertDescription CERTIFICATE_EXPIRED = new AlertDescription(
			"certificate_expired",45);
	
	public static final AlertDescription CERTIFICATE_UNKNOWN = new AlertDescription(
			"certificate_unknown",46);
	
	public static final AlertDescription ILLEGAL_PARAMETER = new AlertDescription(
			"illegal_parameter",47);
	
	public static final AlertDescription UNKNOWN_CA = new AlertDescription(
			"unknown_ca",48);
	
	public static final AlertDescription ACCESS_DENIED = new AlertDescription(
			"access_denied",49);
	
	public static final AlertDescription DECODE_ERROR = new AlertDescription(
			"decode_error",50);
	
	public static final AlertDescription DECRYPT_ERROR = new AlertDescription(
			"decrypt_error",51);
	
	public static final AlertDescription PROTOCOL_VERSION = new AlertDescription(
			"protocol_version",70);
	
	public static final AlertDescription INSUFFICIENT_SECURITY = new AlertDescription(
			"insufficient_security",71);
	
	public static final AlertDescription INTERNAL_ERROR = new AlertDescription(
			"internal_error",80);
	
	public static final AlertDescription INAPPROPRIATE_FALLBACK = new AlertDescription(
			"inappropriate_fallback",86);
	
	public static final AlertDescription USER_CANCELED = new AlertDescription(
			"user_canceled",90);
	
	public static final AlertDescription MISSING_EXTENSION = new AlertDescription(
			"missing_extension",109);
	
	public static final AlertDescription UNSUPPORTED_EXTENSION = new AlertDescription(
			"unsupported_extension",110);
	
	public static final AlertDescription UNRECOGNIZED_NAME = new AlertDescription(
			"unrecognized_name",112);
	
	public static final AlertDescription BAD_CERTIFICATE_STATUS_RESPONSE = new AlertDescription(
			"bad_certificate_status_response",113);
	
	public static final AlertDescription UNKNOWN_PSK_IDENTITY = new AlertDescription(
			"unknown_psk_identity",115);
	
	public static final AlertDescription CERTIFICATE_REQUIRED = new AlertDescription(
			"certificate_required",116);
	
	public static final AlertDescription NO_APPLICATION_PROTOCOL = new AlertDescription(
			"no_application_protocol",120);
	
	private final static AlertDescription[] KNOWN = new AlertDescription[NO_APPLICATION_PROTOCOL.value()+1];

	private static void known(AlertDescription... knowns) {
		for (AlertDescription known: knowns) {
			KNOWN[known.value()] = known;
		}
	}
	
	static {
		known(
				CLOSE_NOTIFY,
				UNEXPECTED_MESSAGE,
				BAD_RECORD_MAC,
				RECORD_OVERFLOW,
				HANDSHAKE_FAILURE,
				BAD_CERTIFICATE,
				UNSUPPORTED_CERTIFICATE,
				CERTIFICATE_REVOKED,
				CERTIFICATE_EXPIRED,
				CERTIFICATE_UNKNOWN,
				ILLEGAL_PARAMETER,
				UNKNOWN_CA,
				ACCESS_DENIED,
				DECODE_ERROR,
				DECRYPT_ERROR,
				PROTOCOL_VERSION,
				INSUFFICIENT_SECURITY,
				INTERNAL_ERROR,
				INAPPROPRIATE_FALLBACK,
				USER_CANCELED,
				MISSING_EXTENSION,
				UNSUPPORTED_EXTENSION,
				UNRECOGNIZED_NAME,
				BAD_CERTIFICATE_STATUS_RESPONSE,
				UNKNOWN_PSK_IDENTITY,
				CERTIFICATE_REQUIRED,
				NO_APPLICATION_PROTOCOL
				);
	}
	
	protected AlertDescription(String name, int value) {
		super(name, value);
	}

	protected AlertDescription(int value) {
		super(value);
	}
	
	public static AlertDescription of(int value) {
		if (value >= 0 && value < KNOWN.length) {
			AlertDescription known = KNOWN[value];
			
			if (known != null) {
				return known;
			}
		}
		return new AlertDescription(value);
	}
}
