package org.snf4j.tls.crypto;

import org.snf4j.tls.handshake.HandshakeType;

public interface ITranscriptHash {
	
	void update(HandshakeType type, byte[] message);
	
	void updateHelloRetryRequest(byte[] message);
	
	byte[] getHash(HandshakeType type);
	
	byte[] getHash(HandshakeType type, boolean client);
}
