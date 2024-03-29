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
package org.snf4j.tls.engine;

import static org.snf4j.tls.extension.ExtensionsUtil.find;

import java.nio.ByteBuffer;

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IALPNExtension;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;

public class EncryptedExtensionsConsumer  implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.ENCRYPTED_EXTENSIONS;
	}

	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws Alert {
		if (state.getState() != MachineState.CLI_WAIT_EE) {
			throw new UnexpectedMessageAlert("Unexpected EncryptedExtensions");
		}
		
		String selectedProtocol = null;
		IALPNExtension alpn = find(handshake, ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION);
		if (alpn != null) {
			String[] protocols = state.getParameters().getApplicationProtocols();
			
			if (protocols.length == 0) {
				throw new IllegalParameterAlert("Unexpected ALPN extension");
			}
			
			selectedProtocol = alpn.getProtocolNames()[0];
			boolean expected = false;
			for(String protocol: protocols) {
				if (selectedProtocol.equals(protocol)) {
					expected = true;
					break;
				}
			}
			if (!expected) {
				throw new IllegalParameterAlert("Unexpected selected application protocol (" + selectedProtocol + ")");
			}
		}
		state.setApplicationProtocol(selectedProtocol);
		state.getHandler().selectedApplicationProtocol(selectedProtocol);
		
		IEarlyDataContext ctx = state.getEarlyDataContext();
		if (ctx.getState() == EarlyDataState.PROCESSING) {
			if (find(handshake, ExtensionType.EARLY_DATA) == null) {
				ctx.rejecting();
				ctx.complete();
				state.getHandler().getEarlyDataHandler().rejectedEarlyData();
			}
			else {
				state.getHandler().getEarlyDataHandler().acceptedEarlyData();				
			}
		}
		
		state.getTranscriptHash().update(handshake.getType(), data);
		state.changeState(state.getKeySchedule().isUsingPsk() 
				? MachineState.CLI_WAIT_FINISHED 
				: MachineState.CLI_WAIT_CERT_CR);
	}

}
