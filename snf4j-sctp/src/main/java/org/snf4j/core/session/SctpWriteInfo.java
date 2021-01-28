package org.snf4j.core.session;

import java.net.SocketAddress;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;

public abstract class SctpWriteInfo {
	
	public abstract SocketAddress getAddress();
	
	public abstract Association getAssociation();
	
	public abstract boolean isComplete();
	
	public abstract boolean isUnordered();
	
	public abstract int getStreamNumber();
	
	public abstract int getProtocolID();
	
	public abstract long getTimeToLive();
	
	public abstract MessageInfo unwrap();
	
	public static SctpWriteInfo create(int streamNumber) {
		return new OutgoingMessageInfo(null, streamNumber);
	}
	
	public static SctpWriteInfo create(SocketAddress address, int streamNumber) {
		return new OutgoingMessageInfo(address, streamNumber);
	}
	
	public static SctpWriteInfo create(int streamNumber, int protocolID) {
		return new OutgoingMessageInfo(null, streamNumber, protocolID) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(address, streamNumber)
						.payloadProtocolID(protocolID);
			}			
		};
	}
	
	public static SctpWriteInfo create(SocketAddress address, int streamNumber, int protocolID) {
		return new OutgoingMessageInfo(address, streamNumber, protocolID) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(address, streamNumber)
						.payloadProtocolID(protocolID);
			}			
		};
	}

	public static SctpWriteInfo create(int streamNumber, int protocolID, boolean unordered) {
		return new OutgoingMessageInfo(null, streamNumber, protocolID, unordered) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(address, streamNumber)
						.payloadProtocolID(protocolID)
						.unordered(unordered);
			}			
		};
	}
	
	public static SctpWriteInfo create(SocketAddress address, int streamNumber, int protocolID, boolean unordered) {
		return new OutgoingMessageInfo(address, streamNumber, protocolID, unordered) {
			
			@Override
			public MessageInfo unwrap() {
				return MessageInfo.createOutgoing(address, streamNumber)
						.payloadProtocolID(protocolID)
						.unordered(unordered);
			}			
		};
	}
	
	public static SctpWriteInfo create(MessageInfo msgInfo) {
		return new OutgoingMessageInfo(msgInfo.address(), msgInfo.streamNumber(), msgInfo.payloadProtocolID(), msgInfo.isUnordered()) {
			
			@Override
			public MessageInfo unwrap() {
				if (association == null) {
					return MessageInfo.createOutgoing(address, streamNumber)
							.payloadProtocolID(protocolID)
							.unordered(unordered)
							.complete(complete)
							.timeToLive(timeToLive);
				}
				return MessageInfo.createOutgoing(association, address, streamNumber)
						.payloadProtocolID(protocolID)
						.unordered(unordered)
						.complete(complete)
						.timeToLive(timeToLive);
			}			
		}.complete(msgInfo.isComplete()).timeToLive(msgInfo.timeToLive()).association(msgInfo.association());
	}
	
	public static SctpWriteInfo wrap(MessageInfo msgInfo) {
		return new WrappingMessageInfo(msgInfo);
	}
	
	
	private static class OutgoingMessageInfo extends SctpWriteInfo {

		SocketAddress address;
		
		Association association;
		
		boolean complete = true;
		
		boolean unordered;
		
		int streamNumber;
		
		int protocolID;
		
		long timeToLive;
		
		private OutgoingMessageInfo(SocketAddress address, int streamNumber) {
			this.address = address;
			this.streamNumber = streamNumber;
		}
		
		private OutgoingMessageInfo(SocketAddress address, int streamNumber, int protocolID) {
			this.address = address;
			this.streamNumber = streamNumber;
			this.protocolID = protocolID;
		}
		
		private OutgoingMessageInfo(SocketAddress address, int streamNumber, int protocolID, boolean unordered) {
			this.address = address;
			this.streamNumber = streamNumber;
			this.protocolID = protocolID;
			this.unordered = unordered;
		}
		
		@Override
		public SocketAddress getAddress() {
			return address;
		}

		OutgoingMessageInfo association(Association association) {
			this.association = association;
			return this;
		}
		
		@Override
		public Association getAssociation() {
			return association;
		}

		OutgoingMessageInfo complete(boolean complete) {
			this.complete = complete;
			return this;
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
		public int getStreamNumber() {
			return streamNumber;
		}

		@Override
		public int getProtocolID() {
			return protocolID;
		}

		OutgoingMessageInfo timeToLive(long timeToLive) {
			this.timeToLive = timeToLive;
			return this;
		}
		
		@Override
		public long getTimeToLive() {
			return timeToLive;
		}

		@Override
		public MessageInfo unwrap() {
			return MessageInfo.createOutgoing(address, streamNumber);
		}
	}
	
	private static class WrappingMessageInfo extends SctpWriteInfo {
		
		private final MessageInfo msgInfo;
		
		private WrappingMessageInfo(MessageInfo msgInfo) {
			this.msgInfo = msgInfo;
		}

		@Override
		public SocketAddress getAddress() {
			return msgInfo.address();
		}

		@Override
		public Association getAssociation() {
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
		public int getStreamNumber() {
			return msgInfo.streamNumber();
		}

		@Override
		public int getProtocolID() {
			return msgInfo.payloadProtocolID();
		}

		@Override
		public long getTimeToLive() {
			return msgInfo.timeToLive();
		}

		@Override
		public MessageInfo unwrap() {
			return msgInfo;
		}
		
	}
}
