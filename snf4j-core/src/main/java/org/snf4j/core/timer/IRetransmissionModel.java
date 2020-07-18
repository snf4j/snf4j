package org.snf4j.core.timer;

public interface IRetransmissionModel {
	
	long next();
	
	long current();
	
	void reset();
}
