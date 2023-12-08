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

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.crypto.TrafficKeys;
import org.snf4j.tls.handshake.KeyUpdateRequest;
import org.snf4j.tls.record.Decryptor;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.IDecryptorHolder;
import org.snf4j.tls.record.IEncryptorHolder;
import org.snf4j.tls.record.RecordType;

public class EngineStateListener implements IEngineStateListener, IEncryptorHolder, IDecryptorHolder {

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

	private long keyLimit(IEngineState state, TrafficKeys keys) {
		return state.getHandler().getKeyLimit(
				state.getCipherSuite(), 
				keys.getAead().getKeyLimit());
	}
	
	@Override
	public void onNewTrafficSecrets(IEngineState state, RecordType recordType) throws Alert {
		TrafficKeys keys;
		
		try {
			switch (recordType) {
			case ZERO_RTT:
				keys = state.getKeySchedule().deriveEarlyTrafficKeys();
				break;

			case HANDSHAKE:
				keys = state.getKeySchedule().deriveHandshakeTrafficKeys();
				break;

			case APPLICATION:
				keys = state.getKeySchedule().deriveApplicationTrafficKeys();
				break;

			default:
				return;
			}

			int index = recordType.ordinal();
			long keyLimit = keyLimit(state, keys);
			
			if (state.isClientMode()) {
				encryptors[index] = new Encryptor(
						keys.getAeadEncrypt(true),
						keys.getIv(true),
						keyLimit);
				if (keys.getKey(false) != null) {
					decryptors[index] = new Decryptor(
							keys.getAeadDecrypt(false),
							keys.getIv(false),
							keyLimit);
				}
			}
			else {
				if (keys.getKey(false) != null) {
					encryptors[index] = new Encryptor(
							keys.getAeadEncrypt(false),
							keys.getIv(false),
							keyLimit);
				}
				decryptors[index] = new Decryptor(
						keys.getAeadDecrypt(true),
						keys.getIv(true),
						keyLimit);
			}
			keys.clear();
		}
		catch (Exception e) {
			throw new InternalErrorAlert("Failed to derive new traffic keys", e);
		}
	}

	@Override
	public void onNewReceivingTraficKey(IEngineState state, RecordType recordType) throws Alert {
		Decryptor toErase;
		
		if (recordType == RecordType.NEXT_GEN) {
			boolean client = !state.isClientMode();
			int index = RecordType.APPLICATION.ordinal();
			TrafficKeys keys;
			
			try {
				keys = state.getKeySchedule().deriveNextGenerationTrafficKey(client);
				toErase = decryptors[index];
				decryptors[index] = new Decryptor(
						keys.getAeadDecrypt(client),
						keys.getIv(client),
						keyLimit(state, keys));
			} catch (Exception e) {
				throw new InternalErrorAlert("Failed to derive next generation receiving traffic key", e);
			}
		}
		else {
			int index = recordType.ordinal();
			
			if (decryptor != index) {
				toErase = decryptors[decryptor];
				decryptors[decryptor] = null;
			}
			else {
				toErase = null;
			}
			decryptor = index;
		}
		if (toErase != null) {
			toErase.erase();
		}
	}

	@Override
	public void onNewSendingTraficKey(IEngineState state, RecordType recordType) throws Alert {
		Encryptor toErase;

		if (recordType == RecordType.NEXT_GEN) {
			boolean client = state.isClientMode();
			int index = RecordType.APPLICATION.ordinal();
			TrafficKeys keys;

			try {
				keys = state.getKeySchedule().deriveNextGenerationTrafficKey(client);
				toErase = encryptors[index]; 
				encryptors[index] = new Encryptor(
						keys.getAeadEncrypt(client),
						keys.getIv(client),
						keyLimit(state, keys));
			} catch (Exception e) {
				throw new InternalErrorAlert("Failed to derive next generation sending traffic key", e);
			}
		}
		else {
			int index = recordType.ordinal();
			
			if (encryptor != index) {
				toErase = encryptors[encryptor];
				encryptors[encryptor] = null;
			}
			else {
				toErase = null;
			}
			encryptor = index;
		}
		if (toErase != null) {
			toErase.erase();
		}
	}
	
	@Override
	public void onKeyUpdate(IEngineState state, KeyUpdateRequest request) {		
	}
	
	@Override
	public void produceChangeCipherSpec(IEngineProducer producer) {
		producer.produce(new ProducedHandshake(
				ChangeCipherSpec.INSTANCE, 
				ProducedHandshake.Type.CHANGE_CIPHER_SPEC));
	}

	@Override
	public void prepareChangeCipherSpec(IEngineProducer producer) {
		producer.prepare(new ProducedHandshake(
				ChangeCipherSpec.INSTANCE, 
				ProducedHandshake.Type.CHANGE_CIPHER_SPEC));
	}
}
