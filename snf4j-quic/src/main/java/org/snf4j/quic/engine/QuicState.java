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

import org.snf4j.tls.Args;

/**
 * The state of the QUIC engine. This object keeps all information related to
 * the state of the QUIC engine.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class QuicState {

	private final static byte[] EMPTY_ARRAY = new byte[0];
	
	private final EncryptionContext[] contexts = new EncryptionContext[EncryptionLevel.values().length];
	
	private final PacketNumberSpace[] spaces = new PacketNumberSpace[contexts.length];
	
	private final boolean clientMode;
	
	private byte[] destinationId;

	private byte[] sourceId = EMPTY_ARRAY;
	
	/**
	 * Constructs the state object for the given client mode.
	 * 
	 * @param clientMode determines whether the state object should bes for a client
	 *                   or server
	 */
	public QuicState(boolean clientMode) {
		this.clientMode = clientMode;
		for (int i=0; i<contexts.length; ++i) {
			contexts[i] = new EncryptionContext(10);
		}
		spaces[EncryptionLevel.EARLY_DATA.ordinal()] = new PacketNumberSpace(PacketNumberSpace.Type.APPLICATION_DATA);
		spaces[EncryptionLevel.INITIAL.ordinal()] = new PacketNumberSpace(PacketNumberSpace.Type.INITIAL);
		spaces[EncryptionLevel.HANDSHAKE.ordinal()] = new PacketNumberSpace(PacketNumberSpace.Type.HANDSHAKE);
		spaces[EncryptionLevel.APPLICATION_DATA.ordinal()] = spaces[EncryptionLevel.EARLY_DATA.ordinal()];
	}
	
	/**
	 * Tells if this state object is for a client or server.
	 * 
	 * @return {@code true} if this state object is for a client
	 */
	public boolean isClientMode() {
		return clientMode;
	}
	
	/**
	 * Returns the associated encryption context for the given encryption level.
	 * 
	 * @param level the encryption level
	 * @return the encryption context for the given encryption level
	 */
	public EncryptionContext getContext(EncryptionLevel level) {
		return contexts[level.ordinal()];
	}
	
	/**
	 * Returns the associated packet number space for the given encryption level.
	 * 
	 * @param level the encryption level
	 * @return the packet number space for the given encryption level
	 */
	public PacketNumberSpace getSpace(EncryptionLevel level) {
		return spaces[level.ordinal()];
	}

	/**
	 * Returns the current destination connection id
	 * 
	 * @return the current destination connection id, or {@code null} if it not set
	 *         yet
	 */
	public byte[] getDestinationId() {
		return destinationId;
	}

	/**
	 * Sets the current destination connection id.
	 * 
	 * @param destinationId the destination connection id
	 */
	public void setDestinationId(byte[] destinationId) {
		this.destinationId = destinationId;
	}
	
	/**
	 * Returns the source connection id
	 * 
	 * @return the source connection id
	 */
	public byte[] getSourceId() {
		return sourceId;
	}

	/**
	 * Sets the source connection id.
	 * 
	 * @param sourceId the destination connection id
	 */
	public void setSourceId(byte[] sourceId) {
		Args.checkNull(sourceId, "sourceId");
		this.sourceId = sourceId;
	}

}
