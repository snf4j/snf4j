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

import org.snf4j.tls.IntConstant;

public class ExtensionType extends IntConstant {
	
	/** RFC 6066 */
	public static final ExtensionType SERVER_NAME = new ExtensionType(
			"server_name",0);
	
	/** RFC 6066 */
	public static final ExtensionType MAX_FRAGMENT_LENGTH = new ExtensionType(
			"max_fragment_length",1);
	
	/** RFC 6066 */
	public static final ExtensionType STATUS_REQUEST = new ExtensionType(
			"status_request",5);
	
	/** RFC 8422, 7919 */
	public static final ExtensionType SUPPORTED_GROUPS = new ExtensionType(
			"supported_groups",10);
	
	/** RFC 8446 */
	public static final ExtensionType SIGNATURE_ALGORITHMS = new ExtensionType(
			"signature_algorithms",13);
	
	/** RFC 5764 */
	public static final ExtensionType USE_SRTP = new ExtensionType(
			"use_srtp",14);
	
	/** RFC 6520 */
	public static final ExtensionType HEARTBEAT = new ExtensionType(
			"heartbeat",15);
	
	/** RFC 7301 */
	public static final ExtensionType APPLICATION_LAYER_PROTOCOL_NEGOTIATION = new ExtensionType(
			"application_layer_protocol_negotiation",16);
	
	/** RFC 6962 */
	public static final ExtensionType SIGNED_CERTIFICATE_TIMESTAMP = new ExtensionType(
			"signed_certificate_timestamp",18);
	
	/** RFC 7250 */
	public static final ExtensionType CLIENT_CERTIFICATE_TYPE = new ExtensionType(
			"client_certificate_type",19);

	/** RFC 7250 */
	public static final ExtensionType SERVER_CERTIFICATE_TYPE = new ExtensionType(
			"server_certificate_type",20);
	
	/** RFC 7685 */
	public static final ExtensionType PADDING = new ExtensionType(
			"padding",21);
	
	/** RFC 8446 */
	public static final ExtensionType PRE_SHARED_KEY = new ExtensionType(
			"pre_shared_key",41);
	
	/** RFC 8446 */
	public static final ExtensionType EARLY_DATA = new ExtensionType(
			"early_data",42);
	
	/** RFC 8446 */
	public static final ExtensionType SUPPORTED_VERSIONS = new ExtensionType(
			"supported_versions",43);
	
	/** RFC 8446 */
	public static final ExtensionType COOKIE = new ExtensionType(
			"cookie",44);
	
	/** RFC 8446 */
	public static final ExtensionType PSK_KEY_EXCHANGE_MODES = new ExtensionType(
			"psk_key_exchange_modes",45);
	
	/** RFC 8446 */
	public static final ExtensionType CERTIFICATE_AUTHORITIES = new ExtensionType(
			"certificate_authorities",47);
	
	/** RFC 8446 */
	public static final ExtensionType OID_FILTERS = new ExtensionType(
			"oid_filters",48);
	
	/** RFC 8446 */
	public static final ExtensionType POST_HANDSHAKE_AUTH = new ExtensionType(
			"post_handshake_auth",49);
	
	/** RFC 8446 */
	public static final ExtensionType SIGNATURE_ALGORITHMS_CERT = new ExtensionType(
			"signature_algorithms_cert",50);
	
	/** RFC 8446 */
	public static final ExtensionType KEY_SHARE = new ExtensionType(
			"key_share",51);	

	private final static ExtensionType[] KNOWN = new ExtensionType[KEY_SHARE.value()+1];
	
	private static void known(ExtensionType... knowns) {
		for (ExtensionType known: knowns) {
			KNOWN[known.value()] = known;
		}
	}
	
	static {
		known(
				SERVER_NAME,
				MAX_FRAGMENT_LENGTH,
				STATUS_REQUEST,
				SUPPORTED_GROUPS,
				SIGNATURE_ALGORITHMS,
				USE_SRTP,
				HEARTBEAT,
				APPLICATION_LAYER_PROTOCOL_NEGOTIATION,
				SIGNED_CERTIFICATE_TIMESTAMP,
				CLIENT_CERTIFICATE_TYPE,
				SERVER_CERTIFICATE_TYPE,
				PADDING,
				PRE_SHARED_KEY,
				EARLY_DATA,
				SUPPORTED_VERSIONS,
				COOKIE,
				PSK_KEY_EXCHANGE_MODES,
				CERTIFICATE_AUTHORITIES,
				OID_FILTERS,
				POST_HANDSHAKE_AUTH,
				SIGNATURE_ALGORITHMS_CERT,
				KEY_SHARE
				);
	}
	
	protected ExtensionType(String name, int value) {
		super(name, value);
	}

	protected ExtensionType(int value) {
		super(value);
	}
	
	public static ExtensionType of(int value) {
		if (value >= 0 && value < KNOWN.length) {
			ExtensionType known = KNOWN[value];
			
			if (known != null) {
				return known;
			}
		}
		return new ExtensionType(value);
	}
	
}
