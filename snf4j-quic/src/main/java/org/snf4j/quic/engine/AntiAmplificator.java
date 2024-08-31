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

/**
 * The default anti-amplificator allowing to limit the amount of data an
 * endpoint sends to an unvalidated address to three times the amount of data
 * received from that address.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class AntiAmplificator implements IAntiAmplificator {
	
	private IAntiAmplificator delegate;
	
	/**
	 * Contructs a anti-amplificator associated with the given QUIC state.
	 * 
	 * @param state the QUIC state
	 */
	public AntiAmplificator(QuicState state) {
		delegate = new Armed(state);
	}
	
	@Override 
	public boolean isArmed() {
		return delegate.isArmed();
	}
	
	@Override
	public void disarm() {
		delegate.disarm();
		delegate = DisarmedAnitAmplificator.INSTANCE;
	}
	
	@Override
	public void incReceived(int amount) {
		delegate.incReceived(amount);;
	}
	
	@Override
	public void incSent(int amount) {
		delegate.incSent(amount);
	}
	
	@Override
	public int available() {
		return delegate.available();
	}
	
	@Override
	public boolean isBlocked() {
		return delegate.isBlocked();
	}

	@Override
	public void lock(int amount) {
		delegate.lock(amount);
	}

	@Override
	public boolean needUnlock() {
		return delegate.needUnlock();
	}
	
	@Override
	public void unlock() {
		delegate.unlock();
	}
	
	@Override
	public String name() {
		return "anti-amplificator";
	}

	private static class Armed extends AbstractDataBlockable implements IAntiAmplificator {

		private final QuicState state;
		
		private int received;
		
		private int sent;
				
		Armed(QuicState state) {
			this.state = state;
		}
		
		@Override 
		public boolean isArmed() {
			return true;
		}
		
		@Override
		public void disarm() {
		}
		
		@Override
		public void incReceived(int amount) {
			received += amount;
		}
		
		@Override
		public void incSent(int amount) {
			sent += amount;
		}

		@Override
		public int available() {
			return received * 3 - sent;
		}
		
		@Override
		public boolean isBlocked() {
			if (!state.isAddressValidated()) {
				int available = available();
				
				return available == 0 || available < locked;
			}
			return false;
		}

	}

}
