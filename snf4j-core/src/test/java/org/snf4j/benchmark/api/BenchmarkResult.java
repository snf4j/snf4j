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
package org.snf4j.benchmark.api;

import java.util.ArrayList;
import java.util.List;

class BenchmarkResult {
	List<long[]> warms = new ArrayList<long[]>();
	List<long[]> tests = new ArrayList<long[]>();
	long unit = 1;
	String unitName = "ns";
	
	private long max(List<long[]> times) {
		long max = 0;
		for (int i=0; i<times.size(); ++i) {
			long time = times.get(i)[0];
			if (max < time) {
				max = time; 
			}
		}
		return max;
	}

	private long min(List<long[]> times) {
		long min = Long.MAX_VALUE;
		for (int i=0; i<times.size(); ++i) {
			long time = times.get(i)[0];
			if (min > time) {
				min = time; 
			}
		}
		return min;
	}
	
	private long avg(List<long[]> times) {
		long sum = 0;
		for (int i=0; i<times.size(); ++i) {
			sum += times.get(i)[0];
		}
		return sum/times.size();
	}
	
	public long trunc(long time) {
		return time/unit;
	}
	
	public void calcUnit() {
		long minAvg = Math.min(getWarmAvg(), getTestAvg());
		if (minAvg > 1000000000) {
			unit = 1000000000;
			unitName = "s";
		}
		else if (minAvg > 1000000) {
			unit = 1000000;
			unitName = "ms";
		}
		else if (minAvg > 1000) {
			unit = 1000;
			unitName = "us";
		}
	}
	
	public long getWarmMin() { return min(warms); }
	public long getWarmMax() { return max(warms); }
	public long getTestMin() { return min(tests); }
	public long getTestMax() { return max(tests); }
	public long getTestAvg() { return avg(tests); }
	public long getWarmAvg() { return avg(warms); }
	
	
}
