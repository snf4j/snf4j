/*
* -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.engine;

import java.util.LinkedList;
import java.util.List;

import org.snf4j.quic.frame.IFrame;

/**
 * A holder for flying frames carried in one packet.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class FlyingFrames {

	private final List<IFrame> frames = new LinkedList<>();
	
	private long sentTime;
	
	/**
	 * Constructs an empty holder for flying frames.
	 */
	public FlyingFrames() {
	}
	
	/**
	 * Return a list of flying frames.
	 * 
	 * @return a list of flying frames
	 */
	public List<IFrame> getFrames() {
		return frames;
	}
	
	/**
	 * Returns the time in nanoseconds the frames in this holder were sent.
	 * 
	 * @return the time the frames in this holder were sent, or 0 if the frames were
	 *         not sent yet
	 */
	public long getSentTime() {
		return sentTime;
	}

	/**
	 * Sets the time in nanoseconds the frames in this holder were sent.
	 * 
	 * @param sentTime the time the frames in this holder were sent
	 */
	public void setSentTime(long sentTime) {
		this.sentTime = sentTime;
	}

}
