/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
	 * Short name for the API.
	 */
	public final static String SHORT_NAME = "SNF4J";
	
	private Constants() {
	}
}
