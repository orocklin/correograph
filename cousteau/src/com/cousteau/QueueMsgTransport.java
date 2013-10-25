package com.cousteau;

import java.util.concurrent.ArrayBlockingQueue;

import javax.mail.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueMsgTransport implements MsgTransport {
	
	private static final Logger _logger = LoggerFactory
			.getLogger(QueueMsgTransport.class);
	
	private final ArrayBlockingQueue<Message> q;
	
	public QueueMsgTransport() {
		q = new ArrayBlockingQueue<>(500, true);
	}

	@Override
	public void submitMessage(Message msg) {
		try {
			q.put(msg);
		} catch (InterruptedException e) {
			_logger.error("Put operation on a blocking queue interrupted", e);
		}
	}

	@Override
	public Message take() {
		try {
			return q.take();
		} catch (InterruptedException e) {
			_logger.error("Take operation on a blocking queue interrupted", e);
		}
		return null;
	}

}
