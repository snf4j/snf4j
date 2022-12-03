package org.snf4j.tls.crypto;

import java.security.InvalidKeyException;
import java.security.Key;

public interface IHkdf {
	
	byte[] extract(Key salt, byte[] ikm) throws InvalidKeyException;
	
	byte[] extract(byte[] salt, byte[] ikm) throws InvalidKeyException;
	
	byte[] extract(byte[] ikm) throws InvalidKeyException;
	
	byte[] expand(Key prk, byte[] info, int length) throws InvalidKeyException;
	
	byte[] expand(byte[] prk, byte[] info, int length) throws InvalidKeyException;

	byte[] expand(byte[] info, int length);
	
	int getMacLength();
}
