package com.cousteau;

import javax.mail.Message;

public interface MsgTransport {
	
	public void submitMessage(Message msg);
	
	public Message take();
	
	
}
