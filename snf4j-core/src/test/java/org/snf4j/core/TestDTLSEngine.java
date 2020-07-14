package org.snf4j.core;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.snf4j.core.TestEngine.Record;

public class TestDTLSEngine extends SSLEngine {
	
	TestSSLSession session = new TestSSLSession();
	
	boolean outboundDone;

	boolean inboundDone;
	
	boolean mode;
	
	SSLException wrapException;
	
	SSLException unwrapException;
	
	SSLEngineResult wrapResult;
	
	SSLEngineResult unwrapResult;
	
	int wrapConsumed = -1;
	
	int unwrapConsumed = -1;

	HandshakeStatus status = HandshakeStatus.NOT_HANDSHAKING;
	
	final List<Record> records = new ArrayList<Record>();
	
	final List<Runnable> tasks = new ArrayList<Runnable>();
	
	static final byte[] PREAMBLE = new byte[100];
	
	static {
		Arrays.fill(PREAMBLE, (byte)0xaa);
	}

	public volatile int handshakeCount;
	
	final Runnable task = new Runnable() {

		@Override
		public void run() {
		}
	};
	
	public void addRecord(String s) {
		synchronized (records) {
			records.add(new Record(s));
		}
	}
	
	public void clearRecords() {
		synchronized (records) {
			records.clear();
		}
	}
	
	public Record nextRecord(boolean remove) {
		Record r = null;
		
		synchronized (records) {
			if (!records.isEmpty()) {
				r = records.get(0);
				if (remove) {
					r = records.remove(0);
				}
				else {
					r = records.get(0);
				}
			}
		}
		return r;
	}
	
	public void addTask() {
		tasks.add(task);
	}
	
	@Override
	public boolean isOutboundDone() {
		return outboundDone;
	}

	@Override
	public boolean isInboundDone() {
		return inboundDone;
	}

	@Override
	public void closeOutbound() {
		if (!outboundDone) {
			clearRecords();
			addRecord("W|NH|-|close|C|-|");
			outboundDone = true;
		}
	}

	@Override
	public void closeInbound() throws SSLException {
		if (!inboundDone) {
			if (isOutboundDone()) {
				status = HandshakeStatus.NOT_HANDSHAKING;
			}
			else {
				status = HandshakeStatus.NEED_WRAP;
			}
			inboundDone = true;
		}
	}

	@Override
	public HandshakeStatus getHandshakeStatus() {
		if (!tasks.isEmpty()) {
			return HandshakeStatus.NEED_TASK;
		}
		return status;
	}

	@Override
	public Runnable getDelegatedTask() {
		if (!tasks.isEmpty()) {
			return tasks.remove(0);
		}
		return null;
	}

	@Override
	public SSLEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws SSLException {
		return null;
	}

	HandshakeStatus status(org.snf4j.core.engine.HandshakeStatus status) {
		return HandshakeStatus.valueOf(status.name());
	}
	
	Status status(org.snf4j.core.engine.Status status) {
		return Status.valueOf(status.name());
	}
	
	SSLEngineResult consumeRecord(ByteBuffer src, ByteBuffer dst) {
		Record record = nextRecord(true);
		int consumed = 0;
		int produced = 0;
		
		if (record.src != null) {
			consumed = record.src.length;
			byte[] b = new byte[consumed];
			src.get(b);
		}
		if (record.dst != null) {
			produced = record.dst.length;
			dst.put(record.dst);
		}
		if (record.resultStatus == org.snf4j.core.engine.HandshakeStatus.NEED_TASK) {
			addTask();
		}
		status = status(record.status);
		return new SSLEngineResult(status(record.result), status(record.resultStatus), consumed, produced);
	}
	
	SSLEngineResult consume(ByteBuffer src, ByteBuffer dst, boolean wrap) throws SSLException {
		int consumed = src.remaining();
		int produced = consumed;
		int consumedAdd = 0;
		
		if (!wrap) {
			if (consumed < PREAMBLE.length) {
				return new SSLEngineResult(Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
			}
			else {
				byte[] b = new byte[PREAMBLE.length];
				src.get(b);
			}
		}
		
		try {
			if (wrap) {
				if (wrapConsumed >= 0) {
					consumed = wrapConsumed;
					produced =  consumed;
					wrapConsumed = -1;
				}
				produced += PREAMBLE.length;
				dst.put(PREAMBLE);
			}
			else {
				if (unwrapConsumed >= 0) {
					consumed = unwrapConsumed;
					produced = consumed;
					unwrapConsumed = -1;
					consumedAdd = PREAMBLE.length;
				}
			}
			if (consumed < src.remaining()) {
				byte[] b = new byte[consumed];
				src.get(b);
				dst.put(b);
			}
			else {
				dst.put(src);
			}
		}
		catch (BufferOverflowException e) {
			return new SSLEngineResult(Status.BUFFER_OVERFLOW, getHandshakeStatus(), 0, 0);
		}
		return new SSLEngineResult(Status.OK, getHandshakeStatus(), consumed + consumedAdd, produced);
	}
	
	@Override
	public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
		if (wrapException != null) {
			SSLException e = wrapException;
			wrapException = null;
			throw e;
		}
		if (wrapResult != null) {
			SSLEngineResult r = wrapResult;
			wrapResult = null;
			return r;
		}
		
		Record record = nextRecord(false);
		
		if (record == null || record.dst == null  || !new String(record.dst).equals("close")) {
			if (isOutboundDone()) {
				return new SSLEngineResult(Status.CLOSED, getHandshakeStatus(), 0, 0);
			}
		}	
		
		if (record != null) {
			if (!record.wrap) {
				return new SSLEngineResult(Status.OK, getHandshakeStatus(), 0, 0);
			}
			return consumeRecord(src, dst);
		}
		return consume(src, dst, true);
	}

	@Override
	public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
		if (unwrapException != null) {
			SSLException e = unwrapException;
			unwrapException = null;
			throw e;
		}
		if (unwrapResult != null) {
			SSLEngineResult r = unwrapResult;
			unwrapResult = null;
			return r;
		}
		
		if (isInboundDone()) {
			return new SSLEngineResult(Status.CLOSED, getHandshakeStatus(), 0, 0);
		}
		Record record = nextRecord(false);
		
		if (record != null) {
			if (record.wrap) {
				return new SSLEngineResult(Status.OK, getHandshakeStatus(), 0, 0);
			}
			return consumeRecord(src, dst);
		}
		
		if (src.remaining() == 5) {
			byte[] b = new byte[src.remaining()];
			
			src.duplicate().get(b);
			if (new String(b).equals("close")) {
				src.get(b);
				closeInbound();
				return new SSLEngineResult(Status.CLOSED, getHandshakeStatus(), b.length, 0);
			}
		}
		
		return consume(src, dst, false);
	}
	
	@Override
	public void setUseClientMode(boolean mode) {
		this.mode = mode;
		if (mode) {
			addRecord("W|NU|-|1|OK|-|");
			addRecord("U|NW|22|-|OK|NT|");
			addRecord("W|NU|-|333|OK|-|");
			addRecord("U|NW|4444|-|OK|-|");
			addRecord("W|NW|-|55555|OK|-|");
			addRecord("W|NW|-|666666|OK|-|");
			addRecord("W|NW|-|7777777|OK|-|");
			addRecord("W|NW|-|-|OK|NT|");
			addRecord("W|NU|-|-|OK|F|");
			addRecord("U|NU|88888888|-|OK|-|");
			addRecord("U|NH|999999999|-|OK|-|");
		}
		else {
			addRecord("W|NU|-|-|OK|-|");
			addRecord("U|NW|1|-|OK|NT|");
			addRecord("W|NU|-|22|OK|-|");
			addRecord("U|NW|333|-|OK|-|");
			addRecord("W|NU|-|4444|OK|-|");
			addRecord("U|NU|55555|-|OK|-|");
			addRecord("U|NU|666666|-|OK|-|");
			addRecord("U|NW|7777777|-|OK|-|");
			addRecord("W|NW|-|88888888|OK|-|");
			addRecord("W|NH|-|999999999|OK|F|");
		}
	}

	@Override
	public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
		return wrap(srcs[0], dst);
	}

	@Override
	public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
		return unwrap(src, dsts[0]);
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return null;
	}

	@Override
	public String[] getEnabledCipherSuites() {
		return null;
	}

	@Override
	public void setEnabledCipherSuites(String[] suites) {
	}

	@Override
	public String[] getSupportedProtocols() {
		return null;
	}

	@Override
	public String[] getEnabledProtocols() {
		return null;
	}

	@Override
	public void setEnabledProtocols(String[] protocols) {
	}

	@Override
	public SSLSession getSession() {
		return session;
	}

	@Override
	public void beginHandshake() throws SSLException {
		handshakeCount++;
	}

	@Override
	public boolean getUseClientMode() {
		return false;
	}

	@Override
	public void setNeedClientAuth(boolean need) {
	}

	@Override
	public boolean getNeedClientAuth() {
		return false;
	}

	@Override
	public void setWantClientAuth(boolean want) {
	}

	@Override
	public boolean getWantClientAuth() {
		return false;
	}

	@Override
	public void setEnableSessionCreation(boolean flag) {
	}

	@Override
	public boolean getEnableSessionCreation() {
		return false;
	}

	@Override
	public String toString() {
		return "DTLSEngine["+(mode ? "client]" : "server]");
	}
}
