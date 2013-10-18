package com.cousteau;

import javax.mail.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphManager implements Runnable {
	
	private static final Logger _logger = LoggerFactory
			.getLogger(GraphManager.class);
	
	private final MsgTransport transport;
	
	private volatile Thread me;
	
	public GraphManager(MsgTransport transport) {
		this.transport = transport;
	}
	
	@Override
	public void run() {
		CorreoGraph cg = null;
		try {
			cg = CorreoGraph.getInstance();

			if (transport != null) { 
				while(!me.isInterrupted()) {
					Message msg = transport.take();
					cg.addMessage(msg);
				}
			}
			
		} catch (Exception e) {
			_logger.error("Exiting from GraphManager thread", e);
		} finally {
			cg.shutdown();
		}
	}
	
	public synchronized void start() {
		if (me == null) {
			me = new Thread(this, "graph");
			me.start();
		}
	}
	
	public synchronized void stop() {
		if (me != null) {
			me.interrupt();
			me = null;
		}
	}

}
