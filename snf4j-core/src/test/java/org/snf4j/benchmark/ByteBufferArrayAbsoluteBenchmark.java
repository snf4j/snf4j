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
package org.snf4j.benchmark;

import java.nio.ByteBuffer;

import org.snf4j.benchmark.api.Bench;
import org.snf4j.benchmark.api.BenchmarkRunner;
import org.snf4j.core.ByteBufferArray;

public class ByteBufferArrayAbsoluteBenchmark {

	final static int COUNT = 1000000;

	final static ByteBuffer BUFFER;
	
	final static ByteBufferArray ARRAY;
	
	final static int TOTAL_SIZE = 10000;
	
	final static int ARRAY_LENGTH = 10;

	final static int SIZE = TOTAL_SIZE / ARRAY_LENGTH;
	
	public long sum;

	public int index;
	
	static {
		BUFFER = ByteBuffer.allocateDirect(TOTAL_SIZE);
		for (int i=0; i<TOTAL_SIZE; ++i) {
			BUFFER.put((byte)i);
		}
		BUFFER.flip();
		ByteBuffer[] array = new ByteBuffer[ARRAY_LENGTH];
		int k=0;
		for (int i=0; i<ARRAY_LENGTH; ++i) {
			array[i] = ByteBuffer.allocateDirect(SIZE);
			for (int j=0; j<SIZE; ++j) {
				array[i].put((byte)k++);
			}
			array[i].flip();
		}
		ARRAY = ByteBufferArray.wrap(array);
	}
	
	@Bench(name="buffer")
	public void bench1() throws Exception {
		index = TOTAL_SIZE-8;
		long sum = 0;
		for (int i=0; i<COUNT; ++i) {
			sum += BUFFER.getLong(index - (i&0x1ff));
		}
		this.sum = sum;
	}

	@Bench(name="array (from first buffer)")
	public void bench2() {
		index = 0;
		long sum = 0;
		for (int i=0; i<COUNT; ++i) {
			sum += ARRAY.getLong(index + (i&0x1ff));
		}
		this.sum = sum;
	}

	@Bench(name="array (from middle buffer)")
	public void bench3() {
		index = TOTAL_SIZE/2 - 8;
		long sum = 0;
		for (int i=0; i<COUNT; ++i) {
			sum += ARRAY.getLong(index - (i&0x1ff));
		}
		this.sum = sum;
	}

	@Bench(name="array (from last buffer)")
	public void bench4() {
		index = TOTAL_SIZE - 8;
		long sum = 0;
		for (int i=0; i<COUNT; ++i) {
			sum += ARRAY.getLong(index - (i&0x1ff));
		}
		this.sum = sum;
	}
	
	public static void main(String[] args) {
		BenchmarkRunner.run(new ByteBufferArrayAbsoluteBenchmark());
	}
	
}
