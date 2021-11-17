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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BenchmarkRunner {
	private static final int WARM_COUNT=20;
	private static final int TEST_COUNT=20;
	
	Map<String, BenchmarkResult> results = new HashMap<String, BenchmarkResult>();
	Map<String, Benchmark> benchmarks = new HashMap<String, Benchmark>();
	
	protected BenchmarkResult getResult(Benchmark benchmark) {
		BenchmarkResult result = results.get(benchmark.getName());
		if (result == null) {
			result = new BenchmarkResult();
			results.put(benchmark.getName(), result);
		}
		return result;
	}

	protected void warm(Benchmark benchmark) {
		long t0,t1;
		BenchmarkResult result = getResult(benchmark);
		for (int i=0; i<WARM_COUNT; ++i) {
			benchmark.preRun();
			t0 = System.nanoTime();
			benchmark.run();
			t1 = System.nanoTime();
			result.warms.add(new long[] {t1-t0});
		}
	}

	protected void test(Benchmark benchmark) {
		long t0,t1;
		BenchmarkResult result = getResult(benchmark);
		for (int i=0; i<TEST_COUNT; ++i) {
			benchmark.preRun();
			t0 = System.nanoTime();
			benchmark.run();
			t1 = System.nanoTime();
			result.tests.add(new long[] {t1-t0});
		}
	}
	
	protected void show(Benchmark benchmark) {
		BenchmarkResult result = getResult(benchmark);
		result.calcUnit();
		
		System.out.println("Benchmark name: " + benchmark.getName());
		StringBuilder sb = new StringBuilder();
		sb.append("Warming:");
		for (int i=0; i<result.warms.size(); ++i) {
			sb.append(' ');
			sb.append(result.trunc(result.warms.get(i)[0]));
			sb.append(result.unitName);
		}
		sb.append(" (avg=");
		sb.append(result.trunc(result.getWarmAvg()));
		sb.append(result.unitName);
		sb.append(")");
		System.out.println(sb);
		
		sb = new StringBuilder();
		sb.append("Testing:");
		for (int i=0; i<result.tests.size(); ++i) {
			sb.append(' ');
			sb.append(result.trunc(result.tests.get(i)[0]));
			sb.append(result.unitName);
		}
		sb.append(" (avg=");
		sb.append(result.trunc(result.getTestAvg()));
		sb.append(result.unitName);
		sb.append(")");
		System.out.println(sb);
	}

	protected void run(Benchmark benchmark) {
		warm(benchmark);
		test(benchmark);
		show(benchmark);
	}
	
	protected void run() {
		for (Benchmark benchmark: benchmarks.values()) {
			run(benchmark);
		}
	}
	
	protected BenchmarkRunner scan(Object obj) {
		Method[] methods = obj.getClass().getMethods();
		Benchmark benchmark;
		
		for (Method method: methods) {
			PreBench preBench = method.getAnnotation(PreBench.class);
			if (preBench != null) {
				benchmark = benchmarks.get(preBench.name());
				if (benchmark == null) {
					benchmark = new BenchmarkWrapper(preBench.name(), obj, method, null);
					benchmarks.put(benchmark.getName(), benchmark);
				}
				else if (benchmark instanceof BenchmarkWrapper) {
					((BenchmarkWrapper) benchmark).setPreRun(method);
				}
			}
			Bench bench = method.getAnnotation(Bench.class);
			if (bench != null) {
				benchmark = benchmarks.get(bench.name());
				if (benchmark == null) {
					benchmark = new BenchmarkWrapper(bench.name(), obj, null, method);
					benchmarks.put(benchmark.getName(), benchmark);
				}
				else if (benchmark instanceof BenchmarkWrapper) {
					((BenchmarkWrapper) benchmark).setRun(method);
				}
			}
		}
		return this;
	}
	
	public static void run(Object obj) {
		if (obj instanceof Benchmark) {
			new BenchmarkRunner().run((Benchmark)obj);
		}
		else {
			new BenchmarkRunner().scan(obj).run();
		}
	}
}
