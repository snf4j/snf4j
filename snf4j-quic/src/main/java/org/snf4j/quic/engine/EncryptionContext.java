/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.engine;

/**
 * The encryption context associated with a given encryption level. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class EncryptionContext {
	
	private final static int PHASES_NUMBER = KeyPhase.values().length;
	
	private final static int CURRENT_PHASE = KeyPhase.CURRENT.ordinal();

	private final static int NEXT_PHASE = KeyPhase.NEXT.ordinal();
	
	private final static IEncryptionContextListener DEFAULT_LISTENER = new IEncryptionContextListener() {

		@Override
		public void onNewEncryptor(EncryptionLevel level, KeyPhase phase) {
		}

		@Override
		public void onNewDecryptor(EncryptionLevel level, KeyPhase phase) {
		}
	};
	
	private Encryptor[] encryptors = new Encryptor[PHASES_NUMBER];

	private Decryptor[] decryptors = new Decryptor[PHASES_NUMBER];
		
	private final EncryptionLevel level;
	
	private final IEncryptionContextListener listener;
	
	private boolean keyPhaseBit;
	
	private final int maxBufferSize;
	
	private PacketBuffer buffer;
	
	private boolean erased;

	/**
	 * Constructs a encryption context with the given maximum buffer size for
	 * buffered packets and encryption context listener.
	 * 
	 * @param level         the encryption level for this context
	 * @param maxBufferSize the maximum buffer size
	 * @param listener      the encryption context listener that should associated
	 *                      with this context
	 */
	public EncryptionContext(EncryptionLevel level, int maxBufferSize, IEncryptionContextListener listener) {
		this.maxBufferSize = maxBufferSize;
		this.level = level;
		this.listener = listener;
	}
	
	/**
	 * Constructs a encryption context with the given maximum buffer size for
	 * buffered packets.
	 * 
	 * @param level         the encryption level for this context
	 * @param maxBufferSize the maximum buffer size
	 */
	public EncryptionContext(EncryptionLevel level, int maxBufferSize) {
		this(level, maxBufferSize, DEFAULT_LISTENER);
	}

	/**
	 * Constructs a encryption context with the default buffer size (10) for
	 * buffered packets.
	 * 
	 * @param level the encryption level for this context
	 */
	public EncryptionContext(EncryptionLevel level) {
		this(level, 10, DEFAULT_LISTENER);
	}
	
	/**
	 * Returns the buffer for packets received when this encryption context was not
	 * yet initiated with keys.
	 * 
	 * @return the buffer for buffered packets
	 */
	PacketBuffer getBuffer() {
		if (buffer == null) {
			buffer = new PacketBuffer(maxBufferSize);
		}
		return buffer;
	}
	
	/**
	 * Sets an encryptor for the current key phase.
	 * <p>
	 * It is the equivalent for {@code setEncryptor(encryptor, KeyPhase.CURRENT)}
	 * 
	 * @param encryptor the encryptor
	 */
	public void setEncryptor(Encryptor encryptor) {
		encryptors[CURRENT_PHASE] = encryptor;
		listener.onNewEncryptor(level, KeyPhase.CURRENT);
	}

	/**
	 * Sets an encryptor for the given key phase.
	 * <p>
	 * This setter can be used by all encryption contexts, however, the PREVIOUS and
	 * NEXT key phases are only applicable for the
	 * {@link EncryptionLevel#APPLICATION_DATA APPLICATION_DATA} encryption level.
	 * 
	 * @param encryptor the encryptor
	 * @param phase     the key phase
	 */
	public void setEncryptor(Encryptor encryptor, KeyPhase phase) {
		encryptors[phase.ordinal()] = encryptor;
		listener.onNewEncryptor(level, phase);
	}

	/**
	 * Returns the encryptor for the current key phase.
	 * <p>
	 * It is the equivalent for {@code getEncryptor(KeyPhase.CURRENT)}
	 * 
	 * @return the encryptor for the current key phase, or {@code null} if it not
	 *         set or not applicable for 0-RTT
	 */
	public Encryptor getEncryptor() {
		return encryptors[CURRENT_PHASE];
	}

	/**
	 * Returns the encryptor for the given key phase.
	 * 
	 * @param phase     the key phase
	 * @return the encryptor for the given key phase, or {@code null} if it not
	 *         set or not applicable
	 */
	public Encryptor getEncryptor(KeyPhase phase) {
		return encryptors[phase.ordinal()];
	}
	
	/**
	 * Sets a decryptor for the current key phase.
	 * <p>
	 * It is the equivalent for {@code setDecryptor(decryptor, KeyPhase.CURRENT)}
	 * 
	 * @param decryptor the decryptor
	 */
	public void setDecryptor(Decryptor decryptor) {
		decryptors[CURRENT_PHASE] = decryptor;
		listener.onNewDecryptor(level, KeyPhase.CURRENT);
	}
	
	/**
	 * Sets a decryptor for the given key phase.
	 * <p>
	 * This setter can be used by all encryption contexts, however, the PREVIOUS and
	 * NEXT key phases are only applicable for the
	 * {@link EncryptionLevel#APPLICATION_DATA APPLICATION_DATA} encryption level.
	 * 
	 * @param decryptor the decryptor
	 * @param phase     the key phase
	 */
	public void setDecryptor(Decryptor decryptor, KeyPhase phase) {
		decryptors[phase.ordinal()] = decryptor;
		listener.onNewDecryptor(level, phase);
	}

	/**
	 * Returns the decryptor for the current key phase.
	 * <p>
	 * It is the equivalent for {@code getDecryptor(KeyPhase.CURRENT)}
	 * 
	 * @return the decryptor for the current key phase, or {@code null} if it not
	 *         set or not applicable for 0-RTT
	 */
	public Decryptor getDecryptor() {
		return decryptors[CURRENT_PHASE];
	}

	/**
	 * Returns the decryptor for the given key phase.
	 * 
	 * @param phase     the key phase
	 * @return the decryptor for the given key phase, or {@code null} if it not
	 *         set or not applicable
	 */
	public Decryptor getDecryptor(KeyPhase phase) {
		return decryptors[phase.ordinal()];
	}
	
	/**
	 * Selects the best decryptor to unprotect the received packet based on its key
	 * phase bit and packet number. This method is only applicable for the
	 * encryption contexts for the {@link EncryptionLevel#APPLICATION_DATA
	 * APPLICATION_DATA} encryption level.
	 * 
	 * @param keyPhaseBit  the key phase bit in the received packet
	 * @param packetNumber the packet number in the received packet
	 * @return the selected decryptor, which may for one of all possible key phases:
	 *         PREVIOUS, CURRENT or NEXT
	 * @throws IllegalStateException if no decryptor could have been found
	 */
	public Decryptor getDecryptor(boolean keyPhaseBit, long packetNumber) {
		Decryptor decryptor = getDecryptor();
		
		if (keyPhaseBit != this.keyPhaseBit) {
			long minPacketNumber = decryptor.getMinPacketNumber();

			if (minPacketNumber != Long.MAX_VALUE && packetNumber < minPacketNumber) {
				decryptor = getDecryptor(KeyPhase.PREVIOUS);
				if (decryptor != null) {
					return decryptor;
				}
			}
			decryptor = getDecryptor(KeyPhase.NEXT);
		}
		if (decryptor == null) {
			throw new IllegalStateException("No decryptor found");
		}
		return decryptor;
	}
	
	/**
	 * Rotates the keys between key phases and switches the state of the key phase
	 * bit. This method is only applicable for the encryption contexts for the
	 * {@link EncryptionLevel#APPLICATION_DATA APPLICATION_DATA} encryption level.
	 * <p>
	 * During this operation: <br>
	 * - the decryptor for the current key phase will be moved to the previous key
	 * phase <br>
	 * - the decryptor and encryptor for the next key phase will be moved to the
	 * current key phase. <br>
	 * - new decryptor and encryptor for the next key phase will not be set, that is
	 * calling the {@link #hasNextKeys()} will return {@code false} <br>
	 * - all no longer used decrytptors and encryptors will be erased <br>
	 * - the key phase bit will be switched
	 */
	public void rotateKeys() {
		if (decryptors[0] != null) {
			decryptors[0].erase(true);
		}
		if (encryptors[0] != null) {
			encryptors[0].erase(true);
		}
		if (encryptors[CURRENT_PHASE] != null) {
			encryptors[CURRENT_PHASE].erase(true);
			encryptors[CURRENT_PHASE] = null;
		}
		
		int last = PHASES_NUMBER-1;
	
		for (int i=1; i<=last; ++i) {
			encryptors[i-1] = encryptors[i];
			decryptors[i-1] = decryptors[i];
		}
		encryptors[last] = null;
		decryptors[last] = null;
		keyPhaseBit = !keyPhaseBit;
	}
	
	/**
	 * Tells if this encryption context has both the encryptor and decryptor set for
	 * the next key phase.
	 * 
	 * @return {@code true} if both the encryptor and decryptor are set for the next key
	 *         phase
	 */
	public boolean hasNextKeys() {
		return encryptors[NEXT_PHASE] != null && decryptors[NEXT_PHASE] != null;
	}
	
	/**
	 * Returns the current state of the key phase bit
	 * 
	 * @return {@code true} for value 1 and {@code false} for value 0
	 */
	public boolean getKeyPhaseBit() {
		return keyPhaseBit;
	}

	/**
	 * Erases all sensitive (e.g. secrets, keys, etc.} information stored by this
	 * encryption context.
	 */
	public void erase() {
		boolean skipEncryptorHp = false;
		boolean skipDecryptorHp = false;

		erased = true;
		buffer = null;
		for (int i=0; i<PHASES_NUMBER; ++i) {
			if (encryptors[i] != null) {
				encryptors[i].erase(skipEncryptorHp);
				encryptors[i] = null;
				skipEncryptorHp = true;
			}
			if (decryptors[i] != null) {
				decryptors[i].erase(skipDecryptorHp);
				decryptors[i] = null;
				skipDecryptorHp = true;
			}
		}		
	}

	/**
	 * Tells if this encryption context has been erased.
	 * 
	 * @return {@code true} if this context has been erased
	 */
	public boolean isErased() {
		return erased;
	}

	/**
	 * The encryption level of this encryption context.
	 * 
	 * @return the encryption level
	 */
	public EncryptionLevel getLevel() {
		return level;
	}
	
	
}
