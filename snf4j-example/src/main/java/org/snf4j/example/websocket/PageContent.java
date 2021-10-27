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
package org.snf4j.example.websocket;

class PageContent {
	
	private final static String LOCATION = "<LOCATION>";
	
	private final static String[] CONTENT_LINES = {
		"<html><head><title>Web Socket Chat</title></head><body>",
		"<script type=\"text/javascript\">",
		"var socket;",
		"",
		"if (!window.WebSocket) {",
		"  window.WebSocket = window.MozWebSocket;",
		"}",
		"",
		"if (window.WebSocket) {",
		"  socket = new WebSocket(\"" + LOCATION + "\");",
		"  socket.onmessage = function(event) {",
		"    var c = document.getElementById('chat');",
		"    c.value = c.value + '\\n' + event.data",
		"  };",
		"  socket.onopen = function(event) {",
		"    var c = document.getElementById('chat');",
		"    c.value = \"Chat opened!\";",
		"  };",
		"  socket.onclose = function(event) {",
		"    var c = document.getElementById('chat');",
		"    c.value = c.value + \"\\nChat closed!\"; ",
		"  };",
		"} else {",
		"  alert(\"Browser does not support Web Socket.\");",
		"}",
		"",
		"function send(message) {",
		"  if (window.WebSocket) {",
		"    if (socket.readyState == WebSocket.OPEN) {",
		"      socket.send(message);",
		"    } else {",
		"      alert(\"The web socket is not open.\");",
		"    }",
		"  }",
		"}",
		"",
		"</script>",
		"",
		"<form onsubmit=\"return false;\">",
		"<textarea id=\"chat\" style=\"width:600px;height:300px;\"></textarea>",
		"<br>",
		"<input type=\"text\" name=\"message\" value=\"\" style=\"width:600px;\"/>",
		"<input type=\"button\" value=\"Send\" onclick=\"send(this.form.message.value); this.form.message.value = '';\"/>",
		"</form>",
		"",
		"</body></html>"
	};
	
	static String get(String location) {
		StringBuilder sb = new StringBuilder();
		
		for (String line: CONTENT_LINES) {
			sb.append(line.replace(LOCATION, location));
			sb.append("\r\n");
		}
		return sb.toString();
	}
	
	private PageContent() {
	}
}
