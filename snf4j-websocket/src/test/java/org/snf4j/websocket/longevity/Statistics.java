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
package org.snf4j.websocket.longevity;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.snf4j.core.session.IEngineSession;
import org.snf4j.core.session.ISession;

public class Statistics {
	
	static AtomicLong totalSessions = new AtomicLong(0);
	
	static AtomicInteger currentSessions = new AtomicInteger(0);
	
	static AtomicInteger sslSessions = new AtomicInteger(0);
	
	static AtomicLong totalPackets = new AtomicLong(0);
	
	static AtomicLong nopPackets = new AtomicLong(0);
	
	static AtomicLong totalBytes = new AtomicLong(0);
	
	static AtomicLong totalTime  = new AtomicLong(0);
	
	static AtomicLong avgTotalBytes = new AtomicLong(0);

	static AtomicLong avgTotalTime = new AtomicLong(0);

	static AtomicLong avgTotalSpeed = new AtomicLong(0);

	static AtomicLong avgSslBytes = new AtomicLong(0);

	static AtomicLong avgSslTime = new AtomicLong(0);

	static AtomicLong avgSslSpeed = new AtomicLong(0);
	
	static AtomicInteger totalExceptions = new AtomicInteger(0);
	
	static AtomicInteger totalIncidents = new AtomicInteger(0);
	
	static AtomicInteger totalNotConnected = new AtomicInteger(0);
	
	static AtomicLong longestSession = new AtomicLong(0);
	
	static AtomicLong total = new AtomicLong(0);

	static final long time0 = System.currentTimeMillis();
	
	static LinkedList<AvgRecord> avgTotals = new LinkedList<AvgRecord>();
	
	static LinkedList<AvgRecord> avgSslTotals = new LinkedList<AvgRecord>();
	
	static String[] timeUnit = {"", "s", "k", "m", "g", "t"};
	
	static String[] bytesUnit = {"", "k", "m", "g", "t"};

	public static void incTotalSessions() {
		totalSessions.incrementAndGet();
	}
	
	static void incSslSessions() {
		sslSessions.incrementAndGet();
	}

	public static void updateSessions(int size) {
		currentSessions.set(size);
		print();
	}
	
	public static void incPackets() {
		totalPackets.incrementAndGet();
	}
	
	public static void incNopPackets() {
		nopPackets.incrementAndGet();
	}
	
	public static void incExceptions() {
		totalExceptions.incrementAndGet();
	}
	
	public static void incIncidents() {
		totalIncidents.incrementAndGet();
	}
	
	public static void incNotConnected() {
		totalNotConnected.incrementAndGet();
	}
	
	public static void updateLongestSession(long l) {
		longestSession.set(l);
	}
	
	public static void sessionEnding(ISession s) {
		AvgRecord r = new AvgRecord(s.getReadBytes() + s.getWrittenBytes(), s.getLastIoTime() - s.getCreationTime());
		
		totalBytes.addAndGet(r.bytes);
		totalTime.set(System.currentTimeMillis() - time0);
		synchronized (avgTotals) {
			avgTotalBytes.addAndGet(r.bytes);
			avgTotalTime.addAndGet(r.time);
			avgTotals.add(r);
			if (avgTotals.size() > 100) {
				AvgRecord r0 = avgTotals.pollFirst();
				avgTotalBytes.addAndGet(-r0.bytes);
				avgTotalTime.addAndGet(-r0.time);
			}
			avgTotalSpeed.set(avgTotalBytes.get()*1000 / (avgTotalTime.get()+1));
		}
		if (s instanceof IEngineSession) {
			synchronized (avgSslTotals) {
				avgSslBytes.addAndGet(r.bytes);
				avgSslTime.addAndGet(r.time);
				avgSslTotals.add(r);
				if (avgSslTotals.size() > 100) {
					AvgRecord r0 = avgSslTotals.pollFirst();
					avgSslBytes.addAndGet(-r0.bytes);
					avgSslTime.addAndGet(-r0.time);
				}
				avgSslSpeed.set(avgSslBytes.get()*1000 / (avgSslTime.get()+1));
			}
		}
	}

	static class AvgRecord {
		long bytes;
		long time;
		
		AvgRecord(long bytes, long time) {
			this.bytes = bytes;
			this.time = time;
		}
	}
	
	static String print(long t, String[] units) {
		int i=0;
		for (; i<units.length; ++i) {
			if (t >= 1000000) {
				t /= 1000;
			}
			else {
				break;
			}
		}
		String s = Long.toString(t);
		int l = s.length();
		if (l > 3 && i < units.length-1) {
			s = s.substring(0, l-3) + "." + s.substring(l-3) + units[i+1];
		}
		return s;
	}
	
	static String printTime(long t) {
		return print(t, timeUnit);
	}
	
	static String printBytes(long t) {
		return print(t, bytesUnit);
	}
	
	
	
	static void print() {
		System.out.println(
				"sessions: " + currentSessions.get() + 
				"/" + printBytes(totalSessions.get()) + 
				"(" + printBytes(sslSessions.get()) +")" +
				"\tpackets: " + printBytes(totalPackets.get()) + 
				"\tbytes: " + printBytes(totalBytes.get()) +
				"\terr: " + totalExceptions.get() +
				"(" + printBytes(totalNotConnected.get()) + ")" +
				"\tinc: " + totalIncidents.get() +
				"\tmax: " + printTime(longestSession.get()) +
				"\tspeed: " +printBytes(avgTotalSpeed.get()) + "/s" + 
				"(" + printBytes(avgSslSpeed.get()) +"/s)" +
				" " + printBytes(totalBytes.get()*1000 / (totalTime.get()+1)) +"/s" +
				"\talloc: " + printBytes(SessionStructureFactory.METRIC.getAllocatingCount()) + "/" +
				printBytes(SessionStructureFactory.METRIC.getAllocatedCount()) + " (" +
				printBytes(SessionStructureFactory.METRIC.getMaxCapacity()) + ")"
		);
	}
}
