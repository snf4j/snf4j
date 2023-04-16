/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.crypto;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.snf4j.tls.Args;
import org.snf4j.tls.handshake.HandshakeType;

public class TranscriptHash implements ITranscriptHash {
	
	private final static HandshakeType[] TYPES = new HandshakeType[] {
			HandshakeType.CLIENT_HELLO,
			null,
			null,
			HandshakeType.SERVER_HELLO,
			HandshakeType.ENCRYPTED_EXTENSIONS,
			HandshakeType.CERTIFICATE_REQUEST,
			HandshakeType.CERTIFICATE,
			HandshakeType.CERTIFICATE_VERIFY,
			HandshakeType.FINISHED,
			HandshakeType.END_OF_EARLY_DATA,
			null,
			null,
			null
	};

	private final static HandshakeType[] TYPES2 = new HandshakeType[] {
			null,
			HandshakeType.SERVER_HELLO,
			HandshakeType.CLIENT_HELLO,
			null,
			null,
			null,
			null,
			null,
			null,
			HandshakeType.END_OF_EARLY_DATA,
			HandshakeType.CERTIFICATE,
			HandshakeType.CERTIFICATE_VERIFY,
			HandshakeType.FINISHED
	};

	private final static HandshakeType[] COMMON_TYPES = new HandshakeType[] {
			HandshakeType.CLIENT_HELLO,
			null,
			null,
			HandshakeType.SERVER_HELLO,
			HandshakeType.ENCRYPTED_EXTENSIONS,
			null,
			null,
			null,
			null,
			HandshakeType.END_OF_EARLY_DATA,
			null,
			null,
			null
	};
	
	private final static HandshakeType[] SERVER_TYPES = new HandshakeType[] {
			null,
			null,
			null,
			null,
			null,
			HandshakeType.CERTIFICATE_REQUEST,
			HandshakeType.CERTIFICATE,
			HandshakeType.CERTIFICATE_VERIFY,
			HandshakeType.FINISHED,
			null,
			null,
			null,
			null
	};

	private final static HandshakeType[] CLIENT_TYPES = new HandshakeType[] {
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			HandshakeType.CERTIFICATE,
			HandshakeType.CERTIFICATE_VERIFY,
			HandshakeType.FINISHED
	};
	
	private final static int[] MAPPING;
	
	private final static int[] MAPPING2;

	private final static int[] COMMON_MAPPING;

	private final static int[] SERVER_MAPPING;

	private final static int[] CLIENT_MAPPING;
	
	private final static int CLIENT_HELLO1_INDEX;

	private final static int CLIENT_HELLO1_MASK;

	private final static int HELLO_RETRY_REQUEST_INDEX;
	
	private final static int HELLO_RETRY_REQUEST_MASK;
	
	private final static int SERVER_FINISHED_MASK;
	
	private static int[] mapping(int length, HandshakeType[] types) {
		int[] mapping = new int[length];
		Arrays.fill(mapping, -1);
		for (int i=0; i<types.length; ++i) {
			if (types[i] != null) {
				mapping[types[i].value()] = i;
			}
		}
		return mapping;
	}
	
	static {
		int max = 0;
		for (HandshakeType type: TYPES) {
			if (type != null && max < type.value()) {
				max = type.value();
			}
		}
		++max;
		MAPPING = mapping(max, TYPES);
		MAPPING2 = mapping(max, TYPES2);
		COMMON_MAPPING = mapping(max, COMMON_TYPES);
		SERVER_MAPPING = mapping(max, SERVER_TYPES);
		CLIENT_MAPPING = mapping(max, CLIENT_TYPES);
		SERVER_FINISHED_MASK = 1 << MAPPING[HandshakeType.FINISHED.value()];
		CLIENT_HELLO1_INDEX = MAPPING[HandshakeType.CLIENT_HELLO.value()];
		CLIENT_HELLO1_MASK = 1 << CLIENT_HELLO1_INDEX;
		HELLO_RETRY_REQUEST_INDEX = MAPPING2[HandshakeType.SERVER_HELLO.value()];
		HELLO_RETRY_REQUEST_MASK = 1 << HELLO_RETRY_REQUEST_INDEX;
	}
	
	private final Item[] items = new Item[TYPES.length];
	
	private int mask;
	
	private final MessageDigest md;
	
	public TranscriptHash(MessageDigest md) {
		Args.checkNull(md, "md");
		this.md = md;
	}
	
	private static MessageDigest clone(MessageDigest md) {
		try {
			return (MessageDigest) md.clone();
		} catch (CloneNotSupportedException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	
	private void update(int index, byte[] message, ByteBuffer[] buffers) {
		if ((mask & (0xffffffff << index)) != 0) {
			throw new IllegalStateException();
		}
		Item item = null;
		for (int i=index-1; i>=0; --i) {
			item = items[i];
			if (item != null) {
				break;
			}
		}
		MessageDigest md = item != null ? clone(item.md) : this.md;
		if (message != null) {
			md.update(message);
		}
		else {
			for (ByteBuffer buffer: buffers) {
				md.update(buffer);
			}
		}
		items[index] = new Item(md);
		mask |= 1 << index;
	}
	
	@Override
	public String getAlgorithm() {
		return md.getAlgorithm();
	}
	
	@Override
	public MessageDigest getHashFunction() {
		try {
			MessageDigest md = (MessageDigest) this.md.clone();

			md.reset();
			return md;
		} catch (CloneNotSupportedException e) {
			try {
				return MessageDigest.getInstance(md.getAlgorithm(), md.getProvider());
			} catch (NoSuchAlgorithmException e1) {
				throw new UnsupportedOperationException();
			}
		}
	}
	
	@Override
	public int getHashLength() {
		return md.getDigestLength();
	}

	@Override
	public void update(HandshakeType type, byte[] message) {
		update(type, message, null);
	}

	@Override
	public void update(HandshakeType type, ByteBuffer[] message) {
		update(type, null, message);
	}
	
	private void update(HandshakeType type, byte[] message, ByteBuffer[] buffers) {
		int index = -1;
		
		if (type.value() == HandshakeType.CLIENT_HELLO.value()) {
			if ((mask & HELLO_RETRY_REQUEST_MASK) != 0) {
				index = MAPPING2[type.value()];
			}
		}
		if (index == -1) {
			if ((mask & SERVER_FINISHED_MASK) != 0) {
				index = MAPPING2[type.value()];
			}
			else {
				index = MAPPING[type.value()];
			}
		}
		if (index != -1) {
			update(index, message, buffers);
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void updateHelloRetryRequest(byte[] message) {
		updateHelloRetryRequest(message, null);
	}

	@Override
	public void updateHelloRetryRequest(ByteBuffer[] message) {
		updateHelloRetryRequest(null, message);
	}
	
	private void updateHelloRetryRequest(byte[] message, ByteBuffer[] buffers) {
		if (mask != CLIENT_HELLO1_MASK) {
			throw new IllegalStateException();
		}
		
		int i = CLIENT_HELLO1_INDEX;
		Item item = items[i];
		
		MessageDigest md = clone(item.md);
		md.reset();
		md.update((byte) HandshakeType.MESSAGE_HASH.value());
		md.update(new byte[] {0,0,(byte) md.getDigestLength()});
		md.update(item.hash());
		items[i] = new Item(md);
				
		i = HELLO_RETRY_REQUEST_INDEX;
		md = clone(md);
		if (message != null) {
			md.update(message);
		}
		else {
			for (ByteBuffer buffer: buffers) {
				md.update(buffer);
			}
		}
		items[i] = new Item(md);
		mask |= HELLO_RETRY_REQUEST_MASK;
	}

	private byte[] getHash(HandshakeType type, int[] mapping) {
		int index = mapping[type.value()];
		if (index != -1) {
			Item item = items[index];
			
			if (item == null) {
				for (int i=index-1; i>=0; --i) {
					item = items[i];
					if (item != null) {
						break;
					}
				}
				if (item == null) {
					return clone(md).digest();
				}
			}
			return item.hash();
		}
		throw new IllegalArgumentException();
	}

	private int[] mapping(HandshakeType type) {
		if (type.value() == HandshakeType.CLIENT_HELLO.value()) {
			if ((mask & HELLO_RETRY_REQUEST_MASK) != 0) {
				return MAPPING2;
			}
		}
		return COMMON_MAPPING;
	}
	
	@Override
	public byte[] getHash(HandshakeType type) {
		return getHash(type, mapping(type));
	}

	@Override
	public byte[] getHash(HandshakeType type, boolean client) {
		return getHash(type, client ? CLIENT_MAPPING : SERVER_MAPPING);
	}
	
	private MessageDigest md(HandshakeType type, int[] mapping) {
		int index = mapping[type.value()];
		if (index != -1) {
			Item item = null;
			
			for (int i=index-1; i>=0; --i) {
				item = items[i];
				if (item != null) {
					break;
				}
			}
			MessageDigest md;
			
			if (item != null) {
				md =  clone(item.md);
			}
			else {
				md = clone(this.md);
				md.reset();
			}
			return md;
		}		
		throw new IllegalArgumentException();
	}
	
	@Override
	public byte[] getHash(HandshakeType type, byte[] replacement, int length) {
		MessageDigest md = md(type, mapping(type));

		md.update(replacement, 0, length);
		return md.digest();
	}

	@Override
	public byte[] getHash(HandshakeType type, ByteBuffer[] replacement) {
		MessageDigest md = md(type, mapping(type));

		for (ByteBuffer buffer: replacement) {
			md.update(buffer);
		}
		return md.digest();
	}
	
	private class Item {
		
		final MessageDigest md;

		private byte[] hash;
		
		Item(MessageDigest md) {
			this.md = md;
		}
		
		byte[] hash() {
			if (hash == null) {
				hash = TranscriptHash.clone(md).digest();
			}
			return hash;
		}
	}
}
