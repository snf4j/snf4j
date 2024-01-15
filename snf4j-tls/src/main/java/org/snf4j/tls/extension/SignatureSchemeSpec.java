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

import org.snf4j.tls.crypto.ECDSASignature;
import org.snf4j.tls.crypto.EdDSASignature;
import org.snf4j.tls.crypto.ISignature;
import org.snf4j.tls.crypto.RSAPKCS1Signature;
import org.snf4j.tls.crypto.RSASSAPSSSignature;

public class SignatureSchemeSpec implements ISignatureSchemeSpec {

	public final static SignatureSchemeSpec RSA_PSS_RSAE_SHA256 = new SignatureSchemeSpec(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256);

	public final static SignatureSchemeSpec RSA_PSS_RSAE_SHA384 = new SignatureSchemeSpec(RSASSAPSSSignature.RSA_PSS_RSAE_SHA384);

	public final static SignatureSchemeSpec RSA_PSS_RSAE_SHA512 = new SignatureSchemeSpec(RSASSAPSSSignature.RSA_PSS_RSAE_SHA512);

	public final static SignatureSchemeSpec RSA_PSS_PSS_SHA256 = new SignatureSchemeSpec(RSASSAPSSSignature.RSA_PSS_PSS_SHA256);

	public final static SignatureSchemeSpec RSA_PSS_PSS_SHA384 = new SignatureSchemeSpec(RSASSAPSSSignature.RSA_PSS_PSS_SHA384);

	public final static SignatureSchemeSpec RSA_PSS_PSS_SHA512 = new SignatureSchemeSpec(RSASSAPSSSignature.RSA_PSS_PSS_SHA512);
	
	public final static SignatureSchemeSpec ECDSA_SHA1 = new SignatureSchemeSpec(ECDSASignature.ECDSA_SHA1);

	public final static SignatureSchemeSpec ECDSA_SECP256R1_SHA256 = new SignatureSchemeSpec(ECDSASignature.ECDSA_SECP256R1_SHA256);
	
	public final static SignatureSchemeSpec ECDSA_SECP384R1_SHA384 = new SignatureSchemeSpec(ECDSASignature.ECDSA_SECP384R1_SHA384);
	
	public final static SignatureSchemeSpec ECDSA_SECP521R1_SHA512 = new SignatureSchemeSpec(ECDSASignature.ECDSA_SECP521R1_SHA512);

	public final static SignatureSchemeSpec RSA_PKCS1_SHA1 = new SignatureSchemeSpec(RSAPKCS1Signature.RSA_PKCS1_SHA1);

	public final static SignatureSchemeSpec RSA_PKCS1_SHA256 = new SignatureSchemeSpec(RSAPKCS1Signature.RSA_PKCS1_SHA256);

	public final static SignatureSchemeSpec RSA_PKCS1_SHA384 = new SignatureSchemeSpec(RSAPKCS1Signature.RSA_PKCS1_SHA384);

	public final static SignatureSchemeSpec RSA_PKCS1_SHA512 = new SignatureSchemeSpec(RSAPKCS1Signature.RSA_PKCS1_SHA512);

	public final static SignatureSchemeSpec ED25519 = new SignatureSchemeSpec(EdDSASignature.ED25519);

	public final static SignatureSchemeSpec ED448 = new SignatureSchemeSpec(EdDSASignature.ED448);
	
	private final ISignature signature;
	
	public SignatureSchemeSpec(ISignature signature) {
		this.signature = signature;
	}
	
	public ISignature getSignature() {
		return signature;
	}

	@Override
	public boolean isImplemented() {
		return signature.isImplemented();
	}
}
