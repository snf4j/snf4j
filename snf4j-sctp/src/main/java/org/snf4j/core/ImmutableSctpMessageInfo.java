package org.snf4j.core;

import java.net.SocketAddress;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;

public abstract class ImmutableSctpMessageInfo extends MessageInfo {

	abstract MessageInfo unwrap();
	
	public static ImmutableSctpMessageInfo create(int streamNumber) {
		return new OutgoingMessageInfo(null, streamNumber);
	}
	
	public static ImmutableSctpMessageInfo create(SocketAddress address, int streamNumber) {
		return new OutgoingMessageInfo(address, streamNumber);
	}
	
	public static ImmutableSctpMessageInfo create(int streamNumber, int payloadProtocolID) {
		return new OutgoingMessageInfo(null, streamNumber, payloadProtocolID) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(address, streamNumber)
						.payloadProtocolID(payloadProtocolID);
			}			
		};
	}
	
	public static ImmutableSctpMessageInfo create(SocketAddress address, int streamNumber, int payloadProtocolID) {
		return new OutgoingMessageInfo(address, streamNumber, payloadProtocolID) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(address, streamNumber)
						.payloadProtocolID(payloadProtocolID);
			}			
		};
	}
	
	public static ImmutableSctpMessageInfo create(int streamNumber, int payloadProtocolID, boolean unordered) {
		return new OutgoingMessageInfo(null, streamNumber, payloadProtocolID, unordered) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(address, streamNumber)
						.payloadProtocolID(payloadProtocolID)
						.unordered(unordered);
			}			
		};
	}
	
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
