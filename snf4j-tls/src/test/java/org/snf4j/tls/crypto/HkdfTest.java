package org.snf4j.tls.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import javax.crypto.Mac;

import org.junit.Test;
import org.snf4j.tls.CommonTest;

public class HkdfTest extends CommonTest {

	@Test
	public void testExtract() throws Exception {
		Hkdf h = new Hkdf(Mac.getInstance("HmacSHA256"));
		assertEquals(32, h.getMacLength());

		byte[] ikm = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		byte[] salt = bytes("000102030405060708090a0b0c");
		byte[] prk = bytes("077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5"); 
		h.extract(salt, ikm);
		assertArrayEquals(prk, h.extract(salt, ikm));
		
		ikm = bytes("000102030405060708090a0b0c0d0e0f"
				+ "101112131415161718191a1b1c1d1e1f"
				+ "202122232425262728292a2b2c2d2e2f"
				+ "303132333435363738393a3b3c3d3e3f"
				+ "404142434445464748494a4b4c4d4e4f");
		salt = bytes("606162636465666768696a6b6c6d6e6f"
				+ "707172737475767778797a7b7c7d7e7f"
				+ "808182838485868788898a8b8c8d8e8f"
				+ "909192939495969798999a9b9c9d9e9f"
				+ "a0a1a2a3a4a5a6a7a8a9aaabacadaeaf");
		prk = bytes("06a6b88c5853361a06104c9ceb35b45cef760014904671014a193f40c15fc244");
		h.extract(salt, ikm);
		assertArrayEquals(prk, h.extract(salt, ikm));
	
		h = new Hkdf(Mac.getInstance("HmacSHA1"));
		assertEquals(20, h.getMacLength());
		
		ikm = bytes("0b0b0b0b0b0b0b0b0b0b0b");
		salt = bytes("000102030405060708090a0b0c");
		prk = bytes("9b6c18c432a7bf8f0e71c8eb88f4b30baa2ba243");
		h.extract(salt, ikm);
		assertArrayEquals(prk, h.extract(salt, ikm));
		ikm = bytes("0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c");
		prk = bytes("2adccada18779e7c2077ad2eb19d3f3e731385dd");
		h.extract(ikm);
		assertArrayEquals(prk, h.extract(ikm));
		
		h = new Hkdf(Mac.getInstance("HmacSHA384"));
		assertEquals(48, h.getMacLength());
		assertEquals(48, h.extract(ikm).length);
	}
	
	@Test
	public void testExpand() throws Exception {
		Hkdf h = new Hkdf(Mac.getInstance("HmacSHA256"));
		
		byte[] prk = bytes("077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5");
		byte[] info = bytes("f0f1f2f3f4f5f6f7f8f9");
		byte[] okm = bytes("3cb25f25faacd57a90434f64d0362f2a"
				+ "2d2d0a90cf1a5a4c5db02d56ecc4c5bf"
				+ "34007208d5b887185865");
		assertArrayEquals(okm, h.expand(prk, info, 42));
		assertArrayEquals(okm, h.expand(info, 42));
		
		prk = bytes("06a6b88c5853361a06104c9ceb35b45cef760014904671014a193f40c15fc244");
		info = bytes("b0b1b2b3b4b5b6b7b8b9babbbcbdbebf"
				+ "c0c1c2c3c4c5c6c7c8c9cacbcccdcecf"
				+ "d0d1d2d3d4d5d6d7d8d9dadbdcdddedf"
				+ "e0e1e2e3e4e5e6e7e8e9eaebecedeeef"
				+ "f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff");
		okm = bytes("b11e398dc80327a1c8e7f78c596a4934"
				+ "4f012eda2d4efad8a050cc4c19afa97c"
				+ "59045a99cac7827271cb41c65e590e09"
				+ "da3275600c2f09b8367793a9aca3db71"
				+ "cc30c58179ec3e87c14c01d5c1f3434f"
				+ "1d87");
		assertArrayEquals(okm, h.expand(prk, info, 82));
		assertArrayEquals(okm, h.expand(info, 82));
		
		h = new Hkdf(Mac.getInstance("HmacSHA1"));

		prk = bytes("9b6c18c432a7bf8f0e71c8eb88f4b30baa2ba243");
		info = bytes("f0f1f2f3f4f5f6f7f8f9");
		okm = bytes("085a01ea1b10f36933068b56efa5ad81"
				+ "a4f14b822f5b091568a9cdd4f155fda2"
				+ "c22e422478d305f3f896");
		assertArrayEquals(okm, h.expand(prk, info, 42));
		assertArrayEquals(okm, h.expand(info, 42));
		okm = bytes("085a01ea1b10f36933068b56efa5ad81"
				+ "a4f14b82");
		assertArrayEquals(okm, h.expand(prk, info, 20));
		assertArrayEquals(okm, h.expand(info, 20));
		okm = bytes("085a01ea1b10f36933068b56efa5ad81"
				+ "a4f14b");
		assertArrayEquals(okm, h.expand(prk, info, 19));
		assertArrayEquals(okm, h.expand(info, 19));
		okm = bytes("08");
		assertArrayEquals(okm, h.expand(prk, info, 1));
		assertArrayEquals(okm, h.expand(info, 1));
	}	
}
