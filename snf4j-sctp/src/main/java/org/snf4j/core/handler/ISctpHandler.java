package org.snf4j.core.handler;

import org.snf4j.core.ISctpReader;
import org.snf4j.core.session.ISctpSessionConfig;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.NotificationHandler;

public interface ISctpHandler extends IHandler, ISctpReader {
	
	@Override
	ISctpSessionConfig getConfig();

	void read(Object msg, MessageInfo msgInfo);
	
	Object getNotificationAttachment();
	
	NotificationHandler<Object> getNotificationHandler();

}
