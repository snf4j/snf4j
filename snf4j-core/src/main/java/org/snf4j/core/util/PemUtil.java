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
package org.snf4j.core.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A class with the privacy-enhanced mail (PEM) encoding utility functions.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */

public final class PemUtil {

	private final static byte[] BEGIN = bytes("-----BEGIN ");

	private final static byte[] END = bytes("-----END ");

	private final static byte[] DASHES = bytes("-----");

	/**
	 * Predefined label types identifying PEM encoded structures as defined in RFC
	 * 7468.
	 */
	public enum Label {

		/** The "CERTIFICATE" label (RFC 5280) */
		CERTIFICATE("CERTIFICATE"),

		/** The "X509 CRL" label (RFC 5280) */
		X509_CRL("X509 CRL"),

		/** The "CERTIFICATE REQUEST" label (RFC 2986) */
		CERTIFICATE_REQUEST("CERTIFICATE REQUEST"),

		/** The "PKCS7" label (RFC 2315) */
		PKCS7("PKCS7"),

		/** The "CMS" label (RFC 5652) */
		CMS("CMS"),

		/** The "PRIVATE KEY" label (RFC 5208, RFC 5958) */
		PRIVATE_KEY("PRIVATE KEY"),

		/** The "ENCRYPTED PRIVATE KEY" label (RFC 5958) */
		ENCRYPTED_PRIVATE_KEY("ENCRYPTED PRIVATE KEY"),

		/** The "ATTRIBUTE CERTIFICATE" label (RFC 5755) */
		ATTRIBUTE_CERTIFICATE("ATTRIBUTE CERTIFICATE"),

		/** The "PUBLIC KEY" label (RFC 5280) */
		PUBLIC_KEY("PUBLIC KEY");

		private final byte[] label;

		Label(String label) {
			this.label = bytes(label);
		}

		private byte[] label() {
			return label;
		}
	}

	private PemUtil() {
	}

	private static byte[] bytes(String s) {
		return s.getBytes(StandardCharsets.US_ASCII);
	}

	private static boolean compare(byte[] pattern, byte[] data, int start, int end) {
		int pend = start + pattern.length;

		if (pend > end) {
			return false;
		} else if (pattern[0] != data[start]) {
			return false;
		}

		for (int i = 1, j = start + 1; j < pend; ++i, ++j) {
			if (pattern[i] != data[j]) {
				return false;
			}
		}
		return true;
	}

	private static List<int[]> indices(byte[] label, byte[] data, int start, int end) {
		List<int[]> out = new LinkedList<int[]>();
		int begin = -1;

		for (int i = start; i < end; ++i) {
			if (begin == -1) {
				if (compare(BEGIN, data, i, end)) {
					i += BEGIN.length;
					if (compare(label, data, i, end)) {
						i += label.length;
						if (compare(DASHES, data, i, end)) {
							i += DASHES.length;
							begin = i;
						}
					}
					--i;
				}
			} else {
				if (compare(END, data, i, end)) {
					int end0 = i;
					i += END.length;
					if (compare(label, data, i, end)) {
						i += label.length;
						if (compare(DASHES, data, i, end)) {
							i += DASHES.length;
							out.add(new int[] { begin, end0 });
						}
					}
					begin = -1;
					--i;
				}
			}
		}
		return out;
	}

	private static List<byte[]> read(byte[] label, byte[] data, int off, int len) {
		List<int[]> indices = indices(label, data, off, off + len);
		List<byte[]> out = new ArrayList<byte[]>();

		for (int[] i : indices) {
			byte[] bytes = Base64Util.decode(data, i[0], i[1] - i[0], true);

			if (bytes == null) {
				return null;
			}
			out.add(bytes);
		}
		return out;
	}

	/**
	 * Reads the specified PEM encoded data and converts it into a list of DERs.
	 * 
	 * @param label the label identifying the PEM encoded structure to read
	 * @param data  the PEM encoded data
	 * @return a list of DERs
	 */
	public static List<byte[]> read(String label, byte[] data) {
		return read(bytes(label), data, 0, data.length);
	}

	/**
	 * Reads the specified PEM encoded data and converts it into a list of DERs.
	 * 
	 * @param label  the label identifying the PEM encoded structure to read
	 * @param data   the PEM encoded data
	 * @param offset offset within the array of the first byte to be converted
	 * @param length number of bytes to be converted
	 * @return a list of DERs
	 */
	public static List<byte[]> read(String label, byte[] data, int offset, int length) {
		return read(bytes(label), data, offset, length);
	}

	/**
	 * Reads the specified PEM encoded data and converts it into a list of DERs.
	 * 
	 * @param label the predefined label identifying the PEM encoded structure to
	 *              read
	 * @param data  the PEM encoded data
	 * @return a list of DERs
	 */
	public static List<byte[]> read(Label label, byte[] data) {
		return read(label.label(), data, 0, data.length);
	}

	/**
	 * Reads the specified PEM encoded data and converts it into a list of DERs.
	 * 
	 * @param label  the predefined label identifying the PEM encoded structure to
	 *               read
	 * @param data   the PEM encoded data
	 * @param offset offset within the array of the first byte to be converted
	 * @param length number of bytes to be converted
	 * @return a list of DERs
	 */
	public static List<byte[]> read(Label label, byte[] data, int offset, int length) {
		return read(label.label(), data, offset, length);
	}

	private static List<byte[]> read(byte[] label, InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int len;

		try {
			for (;;) {
				len = in.read(buf);
				if (len < 0) {
					break;
				}
				out.write(buf, 0, len);
			}

			byte[] data = out.toByteArray();

			return read(label, data, 0, data.length);
		} finally {
			silentClose(out);
		}
	}

	static void silentClose(Closeable stream) {
		try {
			stream.close();
		} catch (Exception e) {
			// Igonre
		}
	}

	/**
	 * Reads the PEM encoded data from the specified input stream and converts it
	 * into a list of DERs.
	 * 
	 * @param label the label identifying the PEM encoded structure to read
	 * @param in    the input stream with PEM encoded data
	 * @return a list of DERs
	 * @throws IOException if a failure occurred while reading from the input
	 *                     stream
	 */
	public static List<byte[]> read(String label, InputStream in) throws IOException {
		return read(bytes(label), in);
	}

	/**
	 * Reads the PEM encoded data from the specified input stream and converts it
	 * into a list of DERs.
	 * 
	 * @param label the predefined label identifying the PEM encoded structure to
	 *              read
	 * @param in    the input stream with PEM encoded data
	 * @return a list of DERs
	 * @throws IOException if a failure occurred while reading from the input
	 *                     stream
	 */
	public static List<byte[]> read(Label label, InputStream in) throws IOException {
		return read(label.label(), in);
	}

	private static List<byte[]> read(byte[] label, File file) throws IOException {
		InputStream in = new FileInputStream(file);

		try {
			return read(label, in);
		} finally {
			silentClose(in);
		}
	}

	/**
	 * Reads the PEM encoded data from the specified file and converts it into a
	 * list of DERs.
	 * 
	 * @param label the label identifying the PEM encoded structure to read
	 * @param file  the file with PEM encoded data
	 * @return a list of DERs
	 * @throws IOException if a failure occurred while reading the file
	 */
	public static List<byte[]> read(String label, File file) throws IOException {
		return read(bytes(label), file);
	}

	/**
	 * Reads the PEM encoded data from the specified file and converts it into a
	 * list of DERs.
	 * 
	 * @param label the predefined label identifying the PEM encoded structure to
	 *              read
	 * @param file  the file with PEM encoded data
	 * @return a list of DERs
	 * @throws IOException if a failure occurred while reading the file
	 */
	public static List<byte[]> read(Label label, File file) throws IOException {
		return read(label.label(), file);
	}
}
