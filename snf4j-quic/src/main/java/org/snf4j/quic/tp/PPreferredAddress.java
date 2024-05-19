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
package org.snf4j.quic.tp;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.packet.PacketUtil;

class PPreferredAddress extends AbstractParameter {

	private final static byte[] ZEROS6 = new byte[6];

	PPreferredAddress(TransportParameterType type, ParameterMode mode) {
		super(type, mode);
	}

	static InetAddress ip(byte[] data) throws QuicException {
		try {
			return InetAddress.getByAddress(data);
		} catch (UnknownHostException e) {
			throw new QuicException(TransportError.INTERNAL_ERROR, "Illegal IP address length");
		}
	}
	
	static boolean onlyZeros(byte[] data) {
		for (int i=0; i<data.length; ++i) {
			if (data[i] != 0) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void parse(ByteBuffer src, int length, TransportParametersBuilder builder) throws QuicException {
		if (length > 4 + 2 + 16 + 2) {
			byte[] data = new byte[4];
			src.get(data);
			int ip4Port = src.getShort() & 0xffff;
			Inet4Address ip4 = (onlyZeros(data) && ip4Port == 0) ? null : (Inet4Address) ip(data);
			
			data = new byte[16];
			src.get(data);
			int ip6Port = src.getShort() & 0xffff;
			Inet6Address ip6 = (onlyZeros(data) && ip6Port == 0) ? null : (Inet6Address) ip(data);
			
			int len = src.get() & 0xff;
			
			if (length == 4 + 2 + 16 + 2 + 1 + len + 16) {
				byte[] connectionId = new byte[len];
				byte[] retryTag = new byte[16];
				
				src.get(connectionId);
				src.get(retryTag);
				
				builder.preferredAddress(new PreferredAddress(
						ip4, 
						ip4Port, 
						ip6, 
						ip6Port, 
						connectionId, 
						retryTag));
				return;
			}
		}
		throw new QuicException(TransportError.TRANSPORT_PARAMETER_ERROR, "Invalid preferred_adddress format");
	}

	int length(PreferredAddress address) {
		return 4 + 2 + 16 + 2 + 1 + address.getConnectionId().length + 16;
	}
	
	@Override
	public void format(TransportParameters params, ByteBuffer dst) {
		PreferredAddress address = params.preferredAddress();
		
		if (address != null) {
			InetAddress ip;
			byte[] data;
			
			dst.put((byte) getType().typeId());
			PacketUtil.encodeInteger(length(address), dst);
			
			ip = address.getIp4();
			if (ip != null) {
				dst.put(ip.getAddress());
				dst.putShort((short) address.getIp4Port());
			}
			else {
				dst.put(ZEROS6);
			}
			
			ip = address.getIp6();
			if (ip != null) {
				dst.put(ip.getAddress());
				dst.putShort((short) address.getIp6Port());
			}
			else {
				dst.put(ZEROS6);
				dst.put(ZEROS6);
				dst.put(ZEROS6);
			}
			
			data = address.getConnectionId();
			dst.put((byte) data.length);
			dst.put(data);
			dst.put(address.getResetToken());
		}
	}

	@Override
	public int length(TransportParameters params) {
		PreferredAddress address = params.preferredAddress();
		
		if (address != null) {
			return 1 + PacketUtil.encodedIntegerLength(length(address))
			+ 4 + 2 + 16 + 2 + 1 + address.getConnectionId().length + 16;
		}
		return 0;
	}

}
