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
package org.snf4j.core;

import java.net.SocketAddress;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;

/**
 * An immutable implementation of the {@code com.sun.nio.sctp.MessageInfo}.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public abstract class ImmutableSctpMessageInfo extends MessageInfo {

	abstract MessageInfo unwrap();
	
	private ImmutableSctpMessageInfo() {
	}

	/**
	 * Creates an immutable {@code MessageInfo} with specified stream number and
	 * the peer primary address as the preferred peer address.
	 * <p>
	 * The returned instance will have its {@code unordered} flag set to
	 * {@code false}, its {@code timeToLive} value set to 0, its {@code complete}
	 * value set to {@code true}, and its {@code payloadProtocolID} value set to 0.
	 * 
	 * @param streamNumber the stream number that the message will be sent on
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(int streamNumber) {
		return new OutgoingMessageInfo(null, streamNumber);
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified stream number and
	 * the preferred peer address.
	 * <p>
	 * The returned instance will have its {@code unordered} flag set to
	 * {@code false}, its {@code timeToLive} value set to 0, its {@code complete}
	 * value set to {@code true}, and its {@code payloadProtocolID} value set to 0.
	 * 
	 * @param address      the preferred peer address of the association to send the
	 *                     message to, or null to use the peer primary address
	 * @param streamNumber the stream number that the message will be sent on
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(SocketAddress address, int streamNumber) {
		return new OutgoingMessageInfo(address, streamNumber);
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified association, stream
	 * number, and the peer primary address as the preferred peer address of the
	 * association to send the message to.
	 * <p>
	 * The returned instance will have its {@code unordered} flag set to
	 * {@code false}, its {@code timeToLive} value set to 0, its {@code complete}
	 * value set to {@code true}, and its {@code payloadProtocolID} value set to 0.
	 * 
	 * @param association  the association to send the message on
	 * @param streamNumber the stream number that the message will be sent on
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(Association association, int streamNumber) {
		return new OutgoingMessageInfo(association, null, streamNumber) {

			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(association, null, streamNumber);
			}			
		};
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified association, stream
	 * number and the preferred peer address of the association to send the message
	 * to.
	 * <p>
	 * The returned instance will have its {@code unordered} flag set to
	 * {@code false}, its {@code timeToLive} value set to 0, its {@code complete}
	 * value set to {@code true}, and its {@code payloadProtocolID} value set to 0.
	 * 
	 * @param association  the association to send the message on
	 * @param address      the preferred peer address of the association to send the
	 *                     message to, or null to use the peer primary address
	 * @param streamNumber the stream number that the message will be sent on
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(Association association, SocketAddress address, int streamNumber) {
		return new OutgoingMessageInfo(association, address, streamNumber) {

			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(association, address, streamNumber);
			}			
		};
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified stream number,
	 * payload protocol identifier and the peer primary address as the
	 * preferred peer address.
	 * <p>
	 * The returned instance will have its {@code unordered} flag set to
	 * {@code false}, its {@code timeToLive} value set to 0, and its
	 * {@code complete} value set to {@code true}.
	 * 
	 * @param streamNumber      the stream number that the message will be sent on
	 * @param payloadProtocolID The payload protocol identifier.
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(int streamNumber, int payloadProtocolID) {
		return new OutgoingMessageInfo(null, streamNumber, payloadProtocolID) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(null, streamNumber)
						.payloadProtocolID(payloadProtocolID);
			}			
		};
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified stream number,
	 * payload protocol identifier and the preferred peer address.
	 * <p>
	 * The returned instance will have its {@code unordered} flag set to
	 * {@code false}, its {@code timeToLive} value set to 0, and its
	 * {@code complete} value set to {@code true}.
	 * 
	 * @param address           the preferred peer address of the association to
	 *                          send the message to, or null to use the peer primary
	 *                          address
	 * @param streamNumber      the stream number that the message will be sent on
	 * @param payloadProtocolID The payload protocol identifier.
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(SocketAddress address, int streamNumber, int payloadProtocolID) {
		return new OutgoingMessageInfo(address, streamNumber, payloadProtocolID) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(address, streamNumber)
						.payloadProtocolID(payloadProtocolID);
			}			
		};
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified association, stream
	 * number, payload protocol identifier and the peer primary address as the
	 * preferred peer address of the association to send the message to.
	 * <p>
	 * The returned instance will have its {@code unordered} flag set to
	 * {@code false}, its {@code timeToLive} value set to 0, and its
	 * {@code complete} value set to {@code true}.
	 * 
	 * @param association       the association to send the message on
	 * @param streamNumber      the stream number that the message will be sent on
	 * @param payloadProtocolID The payload protocol identifier.
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(Association association, int streamNumber, int payloadProtocolID) {
		return new OutgoingMessageInfo(association, null, streamNumber, payloadProtocolID) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(association, null, streamNumber)
						.payloadProtocolID(payloadProtocolID);
			}			
		};
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified association, stream
	 * number, payload protocol identifier and the preferred peer address of the
	 * association to send the message to.
	 * <p>
	 * The returned instance will have its {@code unordered} flag set to
	 * {@code false}, its {@code timeToLive} value set to 0, and its
	 * {@code complete} value set to {@code true}.
	 * 
	 * @param association       the association to send the message on
	 * @param address           the preferred peer address of the association to
	 *                          send the message to, or null to use the peer primary
	 *                          address
	 * @param streamNumber      the stream number that the message will be sent on
	 * @param payloadProtocolID The payload protocol identifier.
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(Association association, SocketAddress address, int streamNumber, int payloadProtocolID) {
		return new OutgoingMessageInfo(association, address, streamNumber, payloadProtocolID) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(association, address, streamNumber)
						.payloadProtocolID(payloadProtocolID);
			}			
		};
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified stream number,
	 * payload protocol identifier, unordered flag and the peer primary address as
	 * the preferred peer address.
	 * <p>
	 * The returned instance will have its {@code timeToLive} value set to 0, and
	 * its {@code complete} value set to {@code true}.
	 * 
	 * @param streamNumber      the stream number that the message will be sent on
	 * @param payloadProtocolID The payload protocol identifier.
	 * @param unordered         {@code true} requests the un-ordered delivery of the
	 *                          message, {@code false} indicates that the message is
	 *                          ordered.
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(int streamNumber, int payloadProtocolID, boolean unordered) {
		return new OutgoingMessageInfo(null, streamNumber, payloadProtocolID, unordered) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(null, streamNumber)
						.payloadProtocolID(payloadProtocolID)
						.unordered(unordered);
			}			
		};
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified stream number,
	 * payload protocol identifier, unordered flag and the preferred peer address.
	 * <p>
	 * The returned instance will have its {@code timeToLive} value set to 0, and
	 * its {@code complete} value set to {@code true}.
	 * 
	 * @param address           the preferred peer address of the association to
	 *                          send the message to, or null to use the peer primary
	 *                          address
	 * @param streamNumber      the stream number that the message will be sent on
	 * @param payloadProtocolID The payload protocol identifier.
	 * @param unordered         {@code true} requests the un-ordered delivery of the
	 *                          message, {@code false} indicates that the message is
	 *                          ordered.
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(SocketAddress address, int streamNumber, int payloadProtocolID, boolean unordered) {
		return new OutgoingMessageInfo(address, streamNumber, payloadProtocolID, unordered) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(address, streamNumber)
						.payloadProtocolID(payloadProtocolID)
						.unordered(unordered);
			}			
		};
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified association, stream
	 * number, payload protocol identifier, unordered flag and the peer primary
	 * address as the preferred peer address of the association to send the message
	 * to.
	 * <p>
	 * The returned instance will have its {@code timeToLive} value set to 0, and
	 * its {@code complete} value set to {@code true}.
	 * 
	 * @param association       the association to send the message on
	 * @param streamNumber      the stream number that the message will be sent on
	 * @param payloadProtocolID The payload protocol identifier.
	 * @param unordered         {@code true} requests the un-ordered delivery of the
	 *                          message, {@code false} indicates that the message is
	 *                          ordered.
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(Association association, int streamNumber, int payloadProtocolID, boolean unordered) {
		return new OutgoingMessageInfo(association, null, streamNumber, payloadProtocolID, unordered) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(association, null, streamNumber)
						.payloadProtocolID(payloadProtocolID)
						.unordered(unordered);
			}			
		};
	}
	
	/**
	 * Creates an immutable {@code MessageInfo} with specified association, stream
	 * number, payload protocol identifier, unordered flag and the preferred peer
	 * address of the association to send the message to.
	 * <p>
	 * The returned instance will have its {@code timeToLive} value set to 0, and
	 * its {@code complete} value set to {@code true}.
	 * 
	 * @param association       the association to send the message on
	 * @param address           the preferred peer address of the association to
	 *                          send the message to, or null to use the peer primary
	 *                          address
	 * @param streamNumber      the stream number that the message will be sent on
	 * @param payloadProtocolID The payload protocol identifier.
	 * @param unordered         {@code true} requests the un-ordered delivery of the
	 *                          message, {@code false} indicates that the message is
	 *                          ordered.
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(Association association, SocketAddress address, int streamNumber, int payloadProtocolID, boolean unordered) {
		return new OutgoingMessageInfo(association, address, streamNumber, payloadProtocolID, unordered) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(association, address, streamNumber)
						.payloadProtocolID(payloadProtocolID)
						.unordered(unordered);
			}			
		};
	}

	/**
	 * Creates an immutable {@code MessageInfo} that is cloned from the specified
	 * {@code MessageInfo} instance.
	 * 
	 * @param msgInfo the instance from which the immutable {@code MessageInfo}
	 *                should be cloned
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo create(MessageInfo msgInfo) {
		OutgoingMessageInfo o = new OutgoingMessageInfo(msgInfo.address(), msgInfo.streamNumber(), msgInfo.payloadProtocolID(), msgInfo.isUnordered()) {
			
			@Override
			MessageInfo unwrap() {
				if (association == null) {
					return MessageInfo.createOutgoing(address, streamNumber)
							.payloadProtocolID(payloadProtocolID)
							.unordered(unordered)
							.complete(complete)
							.timeToLive(timeToLive);
				}
				return MessageInfo.createOutgoing(association, address, streamNumber)
						.payloadProtocolID(payloadProtocolID)
						.unordered(unordered)
						.complete(complete)
						.timeToLive(timeToLive);
			}			
		}; 
		o.complete = msgInfo.isComplete();
		o.timeToLive = msgInfo.timeToLive();
		o.association = msgInfo.association();
		return o;
	}
	
	/**
	 * Wraps the specified {@code MessageInfo} instance.
	 * <p>
	 * <b>Caution:</b> The return instance is not immutable and have to be handled
	 * carefully after passing it to {@link org.snf4j.core.session.ISctpSession
	 * ISctpSession}'s write methods. The internal state of the wrapped instance
	 * should not be change as it may affect the result of the write
	 * methods.
	 * 
	 * @param msgInfo the {@code MessageInfo} instance to be wrapped
	 * @return the immutable {@code MessageInfo}
	 */
	public static ImmutableSctpMessageInfo wrap(MessageInfo msgInfo) {
		return new WrappingMessageInfo(msgInfo);
	}
	
	private static class OutgoingMessageInfo extends ImmutableSctpMessageInfo {

		SocketAddress address;

		Association association;

		boolean complete = true;

		boolean unordered;

		int payloadProtocolID;

		int streamNumber;

		long timeToLive;
		
		private OutgoingMessageInfo(SocketAddress address, int streamNumber) {
			this.address = address;
			this.streamNumber = streamNumber;
		}
		
		private OutgoingMessageInfo(SocketAddress address, int streamNumber, int payloadProtocolID) {
			this.address = address;
			this.streamNumber = streamNumber;
			this.payloadProtocolID = payloadProtocolID;
		}
		
		private OutgoingMessageInfo(SocketAddress address, int streamNumber, int payloadProtocolID, boolean unordered) {
			this.address = address;
			this.streamNumber = streamNumber;
			this.payloadProtocolID = payloadProtocolID;
			this.unordered = unordered;
		}
		
		private OutgoingMessageInfo(Association association, SocketAddress address, int streamNumber) {
			this.association = association;
			this.address = address;
			this.streamNumber = streamNumber;
		}
		
		private OutgoingMessageInfo(Association association, SocketAddress address, int streamNumber, int payloadProtocolID) {
			this.association = association;
			this.address = address;
			this.streamNumber = streamNumber;
			this.payloadProtocolID = payloadProtocolID;
		}
		
		private OutgoingMessageInfo(Association association, SocketAddress address, int streamNumber, int payloadProtocolID, boolean unordered) {
			this.association = association;
			this.address = address;
			this.streamNumber = streamNumber;
			this.payloadProtocolID = payloadProtocolID;
			this.unordered = unordered;
		}
		
		@Override
		public SocketAddress address() {
			return address;
		}

		@Override
		public Association association() {
			return association;
		}

		@Override
		public int bytes() {
			return 0;
		}

		@Override
		public MessageInfo complete(boolean arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public boolean isUnordered() {
			return unordered;
		}

		@Override
		public int payloadProtocolID() {
			return payloadProtocolID;
		}

		@Override
		public MessageInfo payloadProtocolID(int arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int streamNumber() {
			return streamNumber;
		}

		@Override
		public MessageInfo streamNumber(int arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long timeToLive() {
			return timeToLive;
		}

		@Override
		public MessageInfo timeToLive(long arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MessageInfo unordered(boolean arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		MessageInfo unwrap() {
			return MessageInfo.createOutgoing(address, streamNumber);
		}
	}

	private static class WrappingMessageInfo extends ImmutableSctpMessageInfo {
		
		private final MessageInfo msgInfo;
		
		private WrappingMessageInfo(MessageInfo msgInfo) {
			this.msgInfo = msgInfo;
		}

		@Override
		public SocketAddress address() {
			return msgInfo.address();
		}

		@Override
		public Association association() {
			return msgInfo.association();
		}

		@Override
		public boolean isComplete() {
			return msgInfo.isComplete();
		}

		@Override
		public boolean isUnordered() {
			return msgInfo.isUnordered();
		}

		@Override
		public int streamNumber() {
			return msgInfo.streamNumber();
		}

		@Override
		public int payloadProtocolID() {
			return msgInfo.payloadProtocolID();
		}

		@Override
		public long timeToLive() {
			return msgInfo.timeToLive();
		}

		@Override
		MessageInfo unwrap() {
			return msgInfo;
		}

		@Override
		public int bytes() {
			return msgInfo.bytes();
		}

		@Override
		public MessageInfo complete(boolean arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MessageInfo payloadProtocolID(int arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MessageInfo streamNumber(int arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MessageInfo timeToLive(long arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public MessageInfo unordered(boolean arg0) {
			throw new UnsupportedOperationException();
		}
		
	}	
}
