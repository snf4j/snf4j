/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.benchmark;

import java.util.Random;

import org.snf4j.benchmark.api.Bench;
import org.snf4j.benchmark.api.BenchmarkRunner;

public class Base64DecodeBenchmark {
	
	final static int COUNT = 100000;
	
	final static byte[] DECODED_DATA = new byte[1000];
	
	final static byte[] ENCODED_DATA;
	
	static {
		new Random().nextBytes(DECODED_DATA);
		ENCODED_DATA = java.util.Base64.getEncoder().encode(DECODED_DATA);
	}
	
	@Bench(name="org.snf4j.core.util.Base64Util.decode()")
	public void bench1() throws Exception {
		for (int i=0; i<COUNT; ++i) {
			org.snf4j.core.util.Base64Util.decode(ENCODED_DATA);
		}
	}

	@Bench(name="java.util.Base64.decode()")
	public void bench2() {
		for (int i=0; i<COUNT; ++i) {
			try {
				java.util.Base64.getDecoder().decode(ENCODED_DATA);
			}
			catch (Exception e) {
			}
		}
	}

	public static void main(String[] args) {
		BenchmarkRunner.run(new Base64DecodeBenchmark());
	}

}
