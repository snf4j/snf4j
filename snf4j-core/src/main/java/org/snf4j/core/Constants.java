/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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

/**
 * Holder for constants globally used by the API.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 *
 */
public class Constants {
	
	private final static String PROPERTY_PREFIX = "org.snf4j.";
	
	/**
	 * System property specifying the full class name of an external logger
	 * factory. The pointed class must implement the
	 * {@link org.snf4j.core.logger.ILoggerFactory} interface.
	 */
	public final static String LOGGER_FACTORY_SYSTEM_PROERTY = PROPERTY_PREFIX + "LoggerFactory";
	
	/**
	 * System property specifying the full class name of an external exception
	 * logger. The pointed class must implement the
	 * {@link org.snf4j.core.logger.IExceptionLogger} interface.
	 */
	public final static String EXCEPTION_LOGGER_SYSTEM_PROERTY = PROPERTY_PREFIX + "ExceptionLogger";

	/**
	 * System property specifying the threshold for the automatic selector rebuild. 
	 * It is provided as a way to work around the 100% CPU bug in JDK (JDK-6403933).
	 * <p>
	 * The default value for this property is 512. 
	 */
	public final static String SELECTOR_REBUILD_THRESHOLD_SYSTEM_PROPERY = PROPERTY_PREFIX + "SelectorRebuildThreshold";
	
	/**
	 * System property specifying the maximum time in milliseconds the selector will block
	 * while waiting for a channel to become ready. If set to zero the selector will block 
	 * Indefinitely.
	 * <p>
	 * The default value for this property is 1000
	 * 
	 */
	public final static String SELECTOR_SELECT_TIMEOUT = PROPERTY_PREFIX + "SelectorSelectTimeout";
	
	/**
	 * System property specifying if it will be possible to create engine driven
	 * datagram-oriented sessions without a session timer. 
	 * <p>
	 * The allowed values are:
	 * <br> 0 - {@code IllegalStateException} will be thrown if no timer is configured,
	 * <br> 1 - no exception will be thrown, 
	 * <br> any other value will be interpreted as 0
	 * <p>
	 * NOTE: Allowing to create an engine driven datagram-oriented session without a timer
	 * will make the session not capable of retransmitting lost datagrams and timing out the 
	 * handshake phase.
	 * <p>
	 * The default value for this property is 0
	 */
	public final static String IGNORE_NO_SESSION_TIMER_EXCEPTION = PROPERTY_PREFIX + "IgnoreNoSessionTimerException";
	
	/**
	 * System property specifying a threshold for the maximum number of loops needed
	 * to finish the handshake phase for the engine driven sessions. Usually the
	 * handshake phase requires small number of loops to finish so reaching the
	 * threshold set to a big value may indicate that an
	 * {@link org.snf4j.core.engine.IEngine IEngine} implementation used to drive a
	 * session stuck in some bad state causing an infinite loop.
	 * <p>
	 * The default value for this property is 500.
	 */
	public final static String MAX_HANDSHAKE_LOOPS_THRESHOLD = PROPERTY_PREFIX + "MaxHandshakeLoopsThreshold";
	
	/**
	 * Short name for the API.
	 */
	public final static String SHORT_NAME = "SNF4J";
	
	/**
	 * Value Yes.
	 */
	public final static String YES = "1";
	
	/**
	 * Value No.
	 */
	public final static String NO = "0";
	
	private Constants() {
	}
}
