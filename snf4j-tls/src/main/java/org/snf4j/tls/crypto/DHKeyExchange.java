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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

public class DHKeyExchange implements IDHKeyExchange {

	public final static DHKeyExchange FFDHE2048 = new DHKeyExchange("ffdhe2048", DHGroups.FFDHE2048_P, DHGroups.FFDHE_G, DHGroups.FFDHE2048_P_LENGTH);

	public final static DHKeyExchange FFDHE3072 = new DHKeyExchange("ffdhe3072", DHGroups.FFDHE3072_P, DHGroups.FFDHE_G, DHGroups.FFDHE3072_P_LENGTH);

	public final static DHKeyExchange FFDHE4096 = new DHKeyExchange("ffdhe4096", DHGroups.FFDHE4096_P, DHGroups.FFDHE_G, DHGroups.FFDHE4096_P_LENGTH);

	public final static DHKeyExchange FFDHE6144 = new DHKeyExchange("ffdhe6144", DHGroups.FFDHE6144_P, DHGroups.FFDHE_G, DHGroups.FFDHE6144_P_LENGTH);

	public final static DHKeyExchange FFDHE8192 = new DHKeyExchange("ffdhe8192", DHGroups.FFDHE8192_P, DHGroups.FFDHE_G, DHGroups.FFDHE8192_P_LENGTH);
	
	private final String algorithm;

	private final BigInteger p;
	
	private final BigInteger g;
	
	private final int pLength;
	
	public DHKeyExchange(String algorithm, BigInteger p, BigInteger g, int pLength) {
		this.algorithm = algorithm;
		this.p = p;
		this.g = g;
		this.pLength = pLength;
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
	public int getPLength() {
		return pLength;
	}
	
	@Override
	public byte[] generateSecret(PrivateKey privateKey, PublicKey publicKey, SecureRandom random) throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        
        keyAgreement.init(privateKey, random);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret();
	}

	@Override
	public KeyPair generateKeyPair(SecureRandom random) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
		
		keyPairGenerator.initialize(new DHParameterSpec(p,g), random);
		return keyPairGenerator.generateKeyPair();
	}
	
	@Override
	public PublicKey generatePublicKey(BigInteger y) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return KeyFactory.getInstance("DH").generatePublic(new DHPublicKeySpec(y, p, g));
	}

	@Override
	public BigInteger getY(PublicKey key) {
		return ((DHPublicKey)key).getY();
	}

}
