/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.scalability;

public interface Config {
	
	static final String HOST = "127.0.0.1";
	
	static final boolean ENABLE_METRIC = true;
	
	static final boolean ENABLE_ALLOCATOR_METRIC = true;
	
	static final boolean ENABLE_METRIC_PRINT = true;
	
	static final boolean SINGLE_ALLOCATOR = true;
	
	static final int ALLOCATOR_MIN_CAPACITY = 128;
	
	static final boolean SSL = true;

	static final int FIRST_PORT = 7000;
	
	static final int SERVER_LOOP_COUNT = 1;
	
	static final int SERVER_LOOP_POOL_SIZE = 32;
	
	static final int CLIENT_LOOP_COUNT = 32;
	
	static final int LISTENER_COUNT = 1;
	
	static final int FIRST_LISTENING_PORT = 7000;
	
	static final int PACKET_SIZE = 1024;
	
	static final long SESSION_SIZE = 1024*1024;
	
	static final int MAX_SESSIONS = 2000;
	
	static final int CLIENT_RESPONSE_DELAY = 0;
	
	static final int SERVER_SLEEP_TIME = 0;
}
