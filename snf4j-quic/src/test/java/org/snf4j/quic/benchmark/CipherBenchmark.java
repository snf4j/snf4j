/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.benchmark;

import java.lang.reflect.Constructor;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.snf4j.benchmark.api.Bench;
import org.snf4j.benchmark.api.BenchmarkRunner;
import org.snf4j.quic.crypto.TestChaCha20ParameterSpec;

public class CipherBenchmark {

	final static int CIPHER_COUNT = 100000;

	final static int PARAM_COUNT = 10000000;
	
	SecretKey key = new SecretKeySpec(new byte[16], "AES");
	
	@Bench(name="Cipher (getInstance)")
	public void bench1() throws Exception {
		byte[] data = new byte[16];

		for (int i=0; i<CIPHER_COUNT; ++i) {
			Cipher c = Cipher.getInstance("AES/ECB/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, key);
			c.doFinal(data);
		}
	}

	@Bench(name="Cipher")
	public void bench2() throws Exception {
		Cipher c = Cipher.getInstance("AES/ECB/NoPadding");
		byte[] data = new byte[16];
		
		for (int i=0; i<CIPHER_COUNT; ++i) {
			try {
				c.init(Cipher.ENCRYPT_MODE, key);
			}
			catch (Exception e) {}
			c.init(Cipher.ENCRYPT_MODE, key);
			c.doFinal(data);
		}
	}
	
	@Bench(name="ParamSpec")
	public void bench3() throws Exception {
		byte[] data = new byte[16];
		
		for (int i=0; i<PARAM_COUNT; ++i) {
			new TestChaCha20ParameterSpec(data, 100);
		}
	}

	@Bench(name="ParamSpec (reflection)")
	public void bench4() throws Exception {
		byte[] data = new byte[16];
		Constructor<?> c = Class.forName("org.snf4j.quic.crypto.TestChaCha20ParameterSpec").getConstructor(byte[].class, int.class);
		
		for (int i=0; i<PARAM_COUNT; ++i) {
			c.newInstance(data, 100);
		}
	}
	
	public static void main(String[] args) {
		BenchmarkRunner.run(new CipherBenchmark());
	}

}
