/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.alert;

import java.util.HashMap;
import java.util.Map;

class AlertUtil {

	private AlertUtil() {}
	
	private final static Map<Integer, Alert> ALERTS = new HashMap<Integer, Alert>();
	
	private static String message(AlertDescription description) {
		return "Received '" + description.name() + "' error alert from peer";
	}
	
	private static void put(Alert alert) {
		ALERTS.put(alert.getDescription().value(), alert);
	}
	
	private static String className(AlertDescription desc) {
		String name = desc.name();
		int len = name.length();
		StringBuilder className = new StringBuilder(len+5);
		boolean upper = true;
		
		for (int i=0; i<len; ++i) {
			char c = name.charAt(i);
			
			if (c == '_') {
				upper = true;
				continue;
			}
			className.append(upper ? Character.toUpperCase(c) : c);
			upper = false;
		}
		className.append("Alert");
		return className.toString();
	}
	
	static {
		String alertClassName = Alert.class.getName();
		
		for (int i=0; i<256; ++i) {
			AlertDescription desc = AlertDescription.of(i);
			
			if (desc.isKnown()) {
				String className = alertClassName.replace("Alert", className(desc));
				
				
				try {
					Alert alert = (Alert) Class.forName(className)
							.getConstructor(String.class)
							.newInstance(message(desc));
					
					put(alert);
				} catch (Exception e) {
					//Ignore
				}
			}
		}
	}
	
	public static Alert of(AlertLevel level, AlertDescription description) {
		Alert alert = ALERTS.get(description.value());
		
		if (alert == null) {
			return new Alert(message(description), level, description);
		}
		return alert;
	}
}
