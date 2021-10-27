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
package org.snf4j.websocket.handshake;

import java.nio.charset.Charset;
import java.util.Arrays;

class Base64 {
	
    private final static byte PAD = (byte)'=';
    
    private final static int PAD_INDEX = 64;
    
    private final static byte[] ALPHABET = new byte[('Z'-'A'+1)*2 + 10 + 2 + 1];
    
    private final static byte[] DECODING = new byte[256];
    static {
    	int i = 0;
    	
    	for (byte c='A'; c <= 'Z'; c++) {ALPHABET[i++] = c;}
    	for (byte c='a'; c <= 'z'; c++) {ALPHABET[i++] = c;}
    	for (byte c='0'; c <= '9'; c++) {ALPHABET[i++] = c;}
		ALPHABET[i++] = '+';
		ALPHABET[i++] = '/';
		ALPHABET[i++] = PAD;
		
		Arrays.fill(DECODING, (byte)-1);
		for (i=0; i<ALPHABET.length-1; ++i) {
			DECODING[ALPHABET[i]] = (byte)i;
		}
    }    
        
    private Base64() {    	
    }
    
    static byte[] encode(byte[] data) {
    	int len = data.length;
    	
    	if (len == 0) {
    		return data;
    	}

    	byte[] encoded = new byte[(len / 3 + (len % 3 == 0 ? 0 : 1))*4];
    	int c = 0, i = 0;
    	int i1,i2,i3,i4;
    	
    	for (; i<len; i+=3) {
    		if (i+3 > len) {
    			break;
    		}

    		i1 =  data[i] >>> 2 & 0x3f;
    		i2 = (data[i] << 4 | data[i+1] >>> 4 & 0x0f) & 0x3f;
    		i3 = (data[i+1] << 2 | data[i+2] >>> 6 & 0x03) & 0x3f;
    		i4 = data[i+2] & 0x3f;
    		
    		encoded[c++] = ALPHABET[i1];
    		encoded[c++] = ALPHABET[i2];
    		encoded[c++] = ALPHABET[i3];
    		encoded[c++] = ALPHABET[i4];
    	}
    	
    	switch (len-i) {
    	case 1:
    		i1 = data[i] >>> 2 & 0x3f;
    		i2 = data[i] << 4 & 0x3f;
    		i3 = PAD_INDEX;
    		i4 = PAD_INDEX;
    		break;
    		
    	case 2:
    		i1 =  data[i] >>> 2 & 0x3f;
    		i2 = (data[i] << 4 | data[i+1] >>> 4 & 0x0f) & 0x3f;
    		i3 = data[i+1] << 2 & 0x3f;
    		i4 = PAD_INDEX;
    		break;
    		
    	default:
    		return encoded;
    	}
    	
		encoded[c++] = ALPHABET[i1];
		encoded[c++] = ALPHABET[i2];
		encoded[c++] = ALPHABET[i3];
		encoded[c++] = ALPHABET[i4];
		return encoded;
    }
    
    static String encode(byte[] data, Charset charset) {
    	return new String(encode(data), charset);
    }
    
    static byte[] decode(byte[] data) {
    	int len = data.length;
    	
    	if (len == 0) {
    		return data;
    	}
    	else if (len < 2) {
    		return null;
    	}
    	
    	if (data[len-1] == PAD) {
    		--len;
    		if (data[len-1] == PAD) {
    			--len;
    		}
    	}
    	
    	if (len == 0) {
    		return new byte[0];
    	}
    	
    	int calcLen = (len / 4) * 3;
    	
    	switch (len & 0x03) {
    	case 1:
    		return null;
    		
    	case 2:
    		calcLen++;
    		break;
    		
    	case 3:
   			calcLen += 2;
   			
   		default:
    	}
    	
    	byte[] decoded = new byte[calcLen];
    	int d = 0, i = 0;
    	int v, jlen,shift;
    	
    	for (; i<len; i+=4) {
    		jlen = len-i;
    		if (jlen > 4) {
    			jlen = 4;
    			shift = 0;
    		}
    		else {
    			shift = (4-jlen) * 6;
    		}
    		
    		v = 0;
    		for (int j=0; j<jlen; ++j) {
    			byte c = DECODING[data[i+j] & 0xff];
    			
    			if (c == -1) {
    				return null;
    			}
    			v <<= 6;
    			v |= c;
    		}
    		if (shift > 0) {
    			decoded[d++] = (byte)(v >> (16-shift));
    			if (shift == 6) {
        			decoded[d++] = (byte)(v >> (8-shift));
    			}
    		}
    		else {
    			decoded[d++] = (byte)(v >> 16);
    			decoded[d++] = (byte)(v >> 8);
    			decoded[d++] = (byte)v;
    		}
    	}
    	
    	return decoded;
    }
    
    static byte[] decode(String data, Charset charset) {
    	return decode(data.getBytes(charset));
    }
}
