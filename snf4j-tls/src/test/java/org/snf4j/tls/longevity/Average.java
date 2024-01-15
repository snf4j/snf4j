/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
package org.snf4j.tls.longevity;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

public class Average {

	long bytes;
	
	long time;
	
	final AtomicLong value = new AtomicLong(0);
	
	final LinkedList<Record> records = new LinkedList<Record>();
	
	final int size;
	
	public Average(int size) {
		this.size = size;
	}
	
	public void add(long bytes, long time) {
		Record r = new Record(bytes, time);
		
		synchronized (records) {
			this.bytes += bytes;
			this.time += time;
			records.add(r);
			if (records.size() > size) {
				r = records.pollFirst();
				this.bytes -= r.bytes;
				this.time -= r.time;
			}
			if (this.time == 0) {
				this.time = 1;
			}
			value.set(this.bytes * 1000 / this.time);
		}
	}
	
	public long value() {
		return value.get();
	}
	
	static class Record {
		
		final long time;
		
		final long bytes;
		
		Record(long bytes, long time) {
			this.bytes = bytes;
			this.time = time;
		}
	}
}
