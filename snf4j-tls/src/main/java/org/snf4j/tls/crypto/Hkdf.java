package org.snf4j.tls.crypto;

import java.security.InvalidKeyException;
import java.security.Key;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.snf4j.tls.Args;

public class Hkdf implements IHkdf {

	private final Mac mac;
	
	public Hkdf(Mac mac) {
		Args.checkNull(mac, "mac");
		this.mac = mac;
	}
	
	@Override
	public byte[] extract(Key salt, byte[] ikm) throws InvalidKeyException {
		mac.init(salt);
		return mac.doFinal(ikm);
	}
	
	@Override
	public byte[] extract(byte[] salt, byte[] ikm)  throws InvalidKeyException {
		return extract(new SecretKeySpec(salt, mac.getAlgorithm()), ikm);
	}

	@Override
	public byte[] extract(byte[] ikm)  throws InvalidKeyException {
		return extract(new byte[getMacLength()], ikm);
	}

	@Override
	public int getMacLength() {
		return mac.getMacLength();
	}

	@Override
	public byte[] expand(Key prk, byte[] info, int length) throws InvalidKeyException {
		mac.init(prk);
		return expand(info, length);
	}

	@Override
	public byte[] expand(byte[] prk, byte[] info, int length) throws InvalidKeyException {
		return expand(new SecretKeySpec(prk, mac.getAlgorithm()), info, length);
	}

	@Override
	public byte[] expand(byte[] info, int length) {
		int hashLen = getMacLength();
		Args.checkRange(length, 1, hashLen*255, "length");
		
		byte[] t = new byte[0];
		byte[] okm = new byte[length];
		int n = (length+hashLen-1)/hashLen;
		int off = 0;
		
		for (int i=1; i<=n; ++i) {
			mac.reset();
			mac.update(t);
			mac.update(info);
			mac.update((byte)i);
			t = mac.doFinal();
			if (length >= hashLen) {
				System.arraycopy(t, 0, okm, off, hashLen);
				off += hashLen;
				length -= hashLen;
			}
			else {
				System.arraycopy(t, 0, okm, off, length);
				break;
			}
		}
		return okm;
	}

}
