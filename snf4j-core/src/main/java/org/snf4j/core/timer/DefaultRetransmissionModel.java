package org.snf4j.core.timer;

public class DefaultRetransmissionModel implements IRetransmissionModel {

	private final int granularity;
	
	private final long max;
	
	private long current;
	
	public DefaultRetransmissionModel(int granularity, long max) {
		this.granularity = granularity;
		this.max = max;
		current = granularity;
	}
	
	public DefaultRetransmissionModel() {
		this(1000, 60000);
	}
	
	@Override
	public long next() {
		long n = current;
		
		if (n < max) {
			current <<= 1;
			if (current > max) {
				current = max;
			}
		}
		return n;
	}
	
	@Override
	public long current() {
		return current;
	}

	@Override
	public void reset() {
		current = granularity;
	}


}
