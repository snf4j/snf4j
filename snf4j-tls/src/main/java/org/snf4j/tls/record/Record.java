/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.record;

import java.nio.ByteBuffer;

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.BadRecordMacAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.engine.EngineDefaults;

public class Record {

	private Record() {}

	public static final int HEADER_LENGTH = 5;
	
	public static final int ALERT_CONTENT_LENGTH = 2;
	
	public static boolean checkForAlert(ByteBuffer dst) {
		return dst.remaining() >= HEADER_LENGTH 
				+ ALERT_CONTENT_LENGTH;
	}

	public static boolean checkForAlert(ByteBuffer dst, int padding, Encryptor encryptor) {
		return dst.remaining() >= HEADER_LENGTH 
				+ ALERT_CONTENT_LENGTH 
				+ 1 
				+ padding 
				+ encryptor.getExpansion();
	}
	
	public static int alert(Alert alert, ByteBuffer dst) {
		header(ContentType.ALERT, ALERT_CONTENT_LENGTH, dst);
		dst.put((byte) alert.getLevel().value());
		dst.put((byte) alert.getDescription().value());
		return HEADER_LENGTH + ALERT_CONTENT_LENGTH;
	}

	public static int alert(Alert alert, int padding, Encryptor encryptor, ByteBuffer dst) throws Alert {
		ByteBuffer content = ByteBuffer.wrap(new byte[ALERT_CONTENT_LENGTH + 1 + padding]);

		content.put((byte) alert.getLevel().value());
		content.put((byte) alert.getDescription().value());
		content.put((byte)ContentType.ALERT.value());
		content.position(0);
		return protect(content, encryptor, dst);
	}

	public static int header(ContentType type, int contentLength, ByteBuffer dst) {
		dst.put((byte) type.value());
		dst.putShort((short) EngineDefaults.LEGACY_VERSION);
		dst.putShort((short) contentLength);
		return HEADER_LENGTH;
	}
	
	public static int protect(ByteBuffer content, Encryptor encryptor, ByteBuffer dst) throws Alert {
		int contentLength = content.remaining();
		int length = contentLength + encryptor.getExpansion();
		byte[] additionalData = new byte[HEADER_LENGTH];
		byte[] nonce = encryptor.nextNonce();

		additionalData[0] = (byte)ContentType.APPLICATION_DATA.value();
		additionalData[1] = (byte)(EngineDefaults.LEGACY_VERSION >> 8);
		additionalData[2] = (byte)(EngineDefaults.LEGACY_VERSION);
		additionalData[3] = (byte)(length >> 8);
		additionalData[4] = (byte)(length);

		dst.mark();
		dst.put(additionalData);
		try {
			encryptor.getAead().encrypt(nonce, additionalData, content, dst);
			encryptor.incProcessedBytes(contentLength);
		} catch (Exception e) {
			encryptor.rollbackSequence();
			dst.reset();
			throw new InternalErrorAlert("Failed to encrypt plaintext", e);
		}
		return length + HEADER_LENGTH;
	}

	public static int protect(ByteBuffer[] content, int contentLength, Encryptor encryptor, ByteBuffer dst) throws Alert {
		int length = contentLength + encryptor.getExpansion();
		byte[] additionalData = new byte[HEADER_LENGTH];
		byte[] nonce = encryptor.nextNonce();

		additionalData[0] = (byte)ContentType.APPLICATION_DATA.value();
		additionalData[1] = (byte)(EngineDefaults.LEGACY_VERSION >> 8);
		additionalData[2] = (byte)(EngineDefaults.LEGACY_VERSION);
		additionalData[3] = (byte)(length >> 8);
		additionalData[4] = (byte)(length);

		dst.mark();
		dst.put(additionalData);
		try {
			encryptor.getAead().encrypt(nonce, additionalData, content, dst);
			encryptor.incProcessedBytes(contentLength);
		} catch (Exception e) {
			encryptor.rollbackSequence();
			dst.reset();
			throw new InternalErrorAlert("Failed to encrypt plaintext", e);
		}
		return length + HEADER_LENGTH;
	}

	public static int unprotect(ByteBuffer record, int contentLength, Decryptor decryptor, ByteBuffer dst) throws Alert {
		byte[] additionalData = new byte[HEADER_LENGTH];
		byte[] nonce = decryptor.nextNonce();
		
		dst.mark();
		ByteBuffer ciphertext = record.duplicate();
		ciphertext.get(additionalData);
		ciphertext.limit(ciphertext.position() + contentLength);
		try {
			int pos0 = dst.position();
			
			decryptor.getAead().decrypt(nonce, additionalData, ciphertext, dst);
			decryptor.incProcessedBytes(dst.position() - pos0);
			record.position(ciphertext.position());
			return dst.position() - pos0;
		} catch (Exception e) {
			decryptor.rollbackSequence();
			dst.reset();
			throw new BadRecordMacAlert("Failed to decrypt record", e);
		}
	}	
}
