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
package org.snf4j.tls;

public final class Args {
	
	private Args() {}
	
	public static void checkNull(Object value, String name) {
		if (value == null) {
			throw new IllegalArgumentException(name + " is null");
		}
	}

	public static void checkFixed(byte[] array, int fixed, String name) {
		if (array == null) {
			throw new IllegalArgumentException(name + " is null");
		}
		if (array.length != fixed) {
			throw new IllegalArgumentException(name + "'s length is not " + fixed);
		}
	}

	public static void checkFixed(Object[] array, int fixed, String name) {
		if (array == null) {
			throw new IllegalArgumentException(name + " is null");
		}
		if (array.length != fixed) {
			throw new IllegalArgumentException(name + "'s length is not " + fixed);
		}
	}

	public static void checkFixed(int[] array, int fixed, String name) {
		if (array == null) {
			throw new IllegalArgumentException(name + " is null");
		}
		if (array.length != fixed) {
			throw new IllegalArgumentException(name + "'s length is not " + fixed);
		}
	}
	
	public static void checkRange(int value, int min, int max, String name) {
		if (value < min) {
			throw new IllegalArgumentException(name + " is less than " + min);
		}
		if (value > max) {
			throw new IllegalArgumentException(name + " is greater than " + max);
		}
	}

	public static void checkMax(int value, int max, String name) {
		if (value > max) {
			throw new IllegalArgumentException(name + " is greater than " + max);
		}
	}

	public static void checkMax(byte[] array, int max, String name) {
		if (array == null) {
			throw new IllegalArgumentException(name + " is null");
		}
		if (array.length > max) {
			throw new IllegalArgumentException(name + "'s length is greater than " + max);
		}
	}
	
	public static void checkMin(Object[] array, int min, String name) {
		if (array == null) {
			throw new IllegalArgumentException(name + " is null");
		}
		if (array.length < min) {
			throw new IllegalArgumentException(name + "'s length is less than " + min);
		}
	}

	public static void checkMin(int[] array, int min, String name) {
		if (array == null) {
			throw new IllegalArgumentException(name + " is null");
		}
		if (array.length < min) {
			throw new IllegalArgumentException(name + "'s length is less than " + min);
		}
	}
	
}
