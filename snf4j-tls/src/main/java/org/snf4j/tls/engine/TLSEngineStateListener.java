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
package org.snf4j.tls.engine;

import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.record.Decryptor;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.IDecryptorHolder;
import org.snf4j.tls.record.IEncryptorHolder;
import org.snf4j.tls.record.RecordType;

public class TLSEngineStateListener implements IEngineStateListener, IEncryptorHolder, IDecryptorHolder {

	private final Encryptor[] encryptors = new Encryptor[RecordType.values().length];

	private final Decryptor[] decryptors = new Decryptor[RecordType.values().length];
	
	private int decryptor;
	
	private int encryptor;
	
	@Override
	public Decryptor getDecryptor() {
		return decryptors[decryptor];
	}

	@Override
	public Encryptor getEncryptor(RecordType type) {
		return encryptors[type.ordinal()];
	}
	
	@Override
	public Encryptor getEncryptor() {
		return encryptors[encryptor];
	}
	
	@Override
	public void onEarlyTrafficSecret(EngineState state) throws Exception {
		KeySchedule keySchedule = state.getKeySchedule();
		int index = RecordType.ZERO_RTT.ordinal();
		
		keySchedule.deriveEarlyTrafficKeys();
		if (state.isClientMode()) {
			encryptors[index] = new Encryptor(
					keySchedule.getAeadEncrypt(true),
					keySchedule.getIv(true));
		}
		else {
			decryptors[index] = new Decryptor(
					keySchedule.getAeadDecrypt(true),
					keySchedule.getIv(true));
		}
	}

	@Override
	public void onHandshakeTrafficSecrets(EngineState state) throws Exception {
		KeySchedule keySchedule = state.getKeySchedule();
		int index = RecordType.HANDSHAKE.ordinal();
		
		keySchedule.deriveHandshakeTrafficKeys();
		if (state.isClientMode()) {
			encryptors[index] = new Encryptor(
					keySchedule.getAeadEncrypt(true),
					keySchedule.getIv(true));
			decryptors[index] = new Decryptor(
					keySchedule.getAeadDecrypt(false),
					keySchedule.getIv(false));
		}
		else {
			encryptors[index] = new Encryptor(
					keySchedule.getAeadEncrypt(false),
					keySchedule.getIv(false));
			decryptors[index] = new Decryptor(
					keySchedule.getAeadDecrypt(true),
					keySchedule.getIv(true));
		}
	}

	@Override
	public void onApplicationTrafficSecrets(EngineState state) throws Exception {
	}

	@Override
	public void onReceivingTraficKey(RecordType recordType) {
		decryptor = recordType.ordinal();
	}

	@Override
	public void onSendingTraficKey(RecordType recordType) {
		encryptor = recordType.ordinal();
	}
}
