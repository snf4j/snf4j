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
package org.snf4j.longevity.sctp;

import org.snf4j.core.ImmutableSctpMessageInfo;

import com.sun.nio.sctp.MessageInfo;

public interface Config {
	
	public final static int MAX_SESSIONS = 60;
	
	public final static int MAX_PACKET_IN_SESSION = 1000;
	
	public final static int WRITE_ALLOCATED_BUFFER_RATIO = 100;
	
	public final static int OPTIMIZE_DATA_COPING_RATIO = 100;
	
	public final static int DELAYED_WRITE_RATIO = 50;
	
	public final static int MAX_WRITE_DELAY = 200;
	
	public final static int MULTI_PACKET_RATIO = 20;
	
	public final static int MAX_MULTI_PACKET = 20;
	
	public final static int CACHING_ALLOCATOR_RATIO = 100;
	
	public final static int DIRECT_ALLOCATOR_RATIO = 0;
	
	public final static int FIRST_PORT = 7001;
	
	public final static int MAX_PACKET_SIZE = 8000;
	
	public final static int MAX_STREAM_NUM = 9;
	
	public final static int MAX_PROTO_ID = 9;
	
	public final static int NO_CONNECTION_RATIO = 0;
	
	public final static int SYNC_WITH_TIMEOUT_RATIO = 50;
	
	public final static MessageInfo MSG_INFO_1_O = ImmutableSctpMessageInfo.create(0);
	
	public final static MessageInfo MSG_INFO_2_O = ImmutableSctpMessageInfo.create(1,2);
	
	public final static MessageInfo MSG_INFO_3_U = ImmutableSctpMessageInfo.create(2,4,true);
	
	public final static MessageInfo[] MSG_INFOS = new MessageInfo[] {MSG_INFO_1_O,MSG_INFO_2_O,MSG_INFO_3_U};
	
	public final static String HOST = "127.0.0.1";
}
