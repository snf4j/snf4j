/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
package org.snf4j.tls.longevity;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.snf4j.core.session.ISession;
import org.snf4j.tls.session.SessionCache;
import org.snf4j.tls.session.SessionManager;

public class Metric implements Config {
	
	static AtomicLong totalSessions = new AtomicLong(0);

	static AtomicLong sessions = new AtomicLong(0);
	
	static AtomicLong longestSession = new AtomicLong();
	
	static AtomicLong bytesReceived = new AtomicLong(0);
	
	static AtomicLong bytesSent = new AtomicLong(0);
	
	static AtomicInteger totalExceptions = new AtomicInteger(0);
	
	static AtomicInteger totalIncidents = new AtomicInteger(0);

	static Average avgSessionReceived = new Average(100);
	
	static Average avgTotalReceived = new Average(100);
	
	static long avgReceivedTime0 = 0;
	
	static long avgReceived0 = 0;
	
	static final long TIME0 = System.currentTimeMillis();
	
	public static final AllocatorMetric ALLOCATOR_METRIC = new AllocatorMetric();
	
	static void sessionCreated(ISession s) {
		if (ENABLE_METRIC) {
			totalSessions.incrementAndGet();
			sessions.incrementAndGet();
		}
		print();
	}
	
	static void sessionEnding(ISession s) {
		if (ENABLE_METRIC) {
			sessions.decrementAndGet();
			avgSessionReceived.add(s.getReadBytes(), s.getLastReadTime() - s.getCreationTime());
		}
	}
	
	static void longestSession(ISession s) {	
		longestSession.set(s.getCreationTime());
	}
	
	static void dataReceived(ISession s, long size) {
		if (ENABLE_METRIC) {
			bytesReceived.addAndGet(size);
		
			long t = System.currentTimeMillis();
			synchronized (avgTotalReceived) {
				if (avgReceivedTime0 == 0) {
					avgReceivedTime0 = t;
					avgReceived0 = size;
				}
				else if (t - avgReceivedTime0 > 1000) {
					avgReceived0 += size;
					avgTotalReceived.add(avgReceived0, t - avgReceivedTime0);
					avgReceivedTime0 = t;
					avgReceived0 = 0;
				}
				else {
					avgReceived0 += size;
				}
			}
		}
	}
	
	static void dataSent(ISession s, long size) {
		if (ENABLE_METRIC) {
			bytesSent.addAndGet(size);
		}
	}
	
	public static void incExceptions() {
		totalExceptions.incrementAndGet();
	}
	
	public static void incIncidents() {
		totalIncidents.incrementAndGet();
	}

	static String[] bytesUnit = {"", "k", "m", "g", "t"};
	
	static String[] timeUnit = {"", "s", "k", "m", "g", "t"};
	
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
	
	static String printBytes(long t) {
		return print(t, bytesUnit);
	}
	
	static String printTime(long t) {
		return print(t, timeUnit);
	}

	static int cacheSize(SessionManager mgr, String name) {
		int size = 0;
		
		try {
			Field f = SessionManager.class.getDeclaredField(name);
			f.setAccessible(true);
			SessionCache<?> c = (SessionCache<?>) f.get(mgr);
			
			synchronized(c) {
				size = c.size();
			}
			
		} catch (Exception e) {
		}
		return size;
	}
	
	static void print() {
		if (!ENABLE_METRIC_PRINT) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		
		sb.append("sessions: ");
		sb.append(sessions.get());
		sb.append('/');
		sb.append(printBytes(totalSessions.get()));
		sb.append('\t');
		
		sb.append("bytes R/S: ");
		sb.append(printBytes(bytesReceived.get()));
		sb.append('/');
		sb.append(printBytes(bytesSent.get()));
		sb.append('\t');
		
		sb.append("avg: ");
		sb.append(printBytes(avgSessionReceived.value()));
		sb.append("/s");
		sb.append(" (");
		sb.append(printBytes(avgTotalReceived.value()));
		sb.append("/s)\t");

		sb.append("err/inc: ");
		sb.append(totalExceptions.get());
		sb.append('/');
		sb.append(totalIncidents.get());
		sb.append('\t');
		
		sb.append("max: ");
		sb.append(printTime(System.currentTimeMillis() - longestSession.get()));
		sb.append("\t");
				
		sb.append("alloc: ");
		sb.append(printBytes(ALLOCATOR_METRIC.getAllocatingCount()));
		sb.append('/');
		sb.append(printBytes(ALLOCATOR_METRIC.getAllocatedCount()));
		sb.append(" (");
		sb.append(printBytes(ALLOCATOR_METRIC.getAllocatedSize()));
		sb.append("/");
		sb.append(printBytes(ALLOCATOR_METRIC.getMaxCapacity()));
		sb.append(")\t");
		
		sb.append("cache S/C: ");
		sb.append(cacheSize(Controller.serverManager, "cacheById"));
		sb.append('/');
		sb.append(cacheSize(Controller.clientManager, "cacheByIpPort"));

		System.out.println(sb.toString());		
	}
	
}
