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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BenchmarkWrapper implements Benchmark {
	private String name;
	private Method preRun;
	private Method run;
	private Object benchmark;
	
	BenchmarkWrapper(String name, Object benchmark, Method preRun, Method run) {
		this.name = name;
		this.benchmark = benchmark;
		this.preRun = preRun;
		this.run = run;
	}
	
	void setPreRun(Method preRun) {
		this.preRun = preRun;
	}
	
	void setRun(Method run) {
		this.run = run;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void preRun() {
		if (preRun != null) {
			try {
				preRun.invoke(benchmark, new Object[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		if (run != null) {
			try {
				run.invoke(benchmark, new Object[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
