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

import java.nio.ByteBuffer;

import org.snf4j.quic.QuicAlert;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.packet.PacketUtil;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecodeErrorAlert;

/**
 * The default QUIC transport parameters parsers. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class TransportParametersParser implements ITransportParametersParser {
	
	public final static ITransportParametersParser INSTANCE = new TransportParametersParser();
	
	@Override
	public void parse(boolean client, ByteBuffer src, int remaining, TransportParametersBuilder builder) throws Alert {
		try {
			int[] remainings = new int[] {remaining};

			while (remainings[0] > 0) {
				long id = PacketUtil.decodeInteger(src, remainings);

				if (remainings[0] > 0) {
					long len = PacketUtil.decodeInteger(src, remainings);
					
					if (remainings[0] >= len) {
						IParameter p;

						if (id < Parameters.PARAMS.length) {
							p = Parameters.PARAMS[(int) id];
						}
						else {
							p = null;
						}
						if (p != null) {
							if (p.getMode().isValid(!client)) {
								p.parse(src, (int) len, builder);
							}
							else {
								throw new QuicAlert(TransportError.TRANSPORT_PARAMETER_ERROR, 
										"Unexpected " + p.getType().typeName());
							}
						}
						else {
							src.position((src.position() + (int) len));
						}
						remainings[0] -= len;
						continue;
					}
				}
				throw new DecodeErrorAlert(
						"Extension 'quic_transport_parameters' parsing failure: Inconsistent length");
			}
		}
		catch (QuicException e) {
			throw new DecodeErrorAlert(
					"Extension 'quic_transport_parameters' parsing failure: Invalid format");
		}
	}

}
