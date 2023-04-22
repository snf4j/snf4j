/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.crypto;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.KeyAgreement;

public class ECKeyExchange implements IECKeyExchange {

	public final static ECKeyExchange SECP256R1 = new ECKeyExchange("secp256r1");

	public final static ECKeyExchange SECP384R1 = new ECKeyExchange("secp384r1");

	public final static ECKeyExchange SECP521R1 = new ECKeyExchange("secp521r1");
	
	private final String algorithm;
	
	public ECKeyExchange(String algorithm) {
		this.algorithm = algorithm;
	}

	@Override
	public String getAlgorithm() {
		return algorithm;
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}
	
	@Override
	public byte[] generateSecret(PrivateKey privateKey, PublicKey publicKey, SecureRandom random) throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        
        keyAgreement.init(privateKey, random);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
	}

	@Override
	public KeyPair generateKeyPair(SecureRandom random) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
		
        keyPairGenerator.initialize(new ECGenParameterSpec(algorithm), random);
        return keyPairGenerator.genKeyPair();
	}

	@Override
	public PublicKey generatePublicKey(BigInteger x, BigInteger y) throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
		KeyFactory kf = KeyFactory.getInstance("EC");
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        
		params.init(new ECGenParameterSpec(algorithm));
		return kf.generatePublic(new ECPublicKeySpec(new ECPoint(x, y), params.getParameterSpec(ECParameterSpec.class)));
	}

	@Override
	public BigInteger getX(PublicKey key) {
		return ((ECPublicKey)key).getW().getAffineX();
	}

	@Override
	public BigInteger getY(PublicKey key) {
		return ((ECPublicKey)key).getW().getAffineY();
	}

}
