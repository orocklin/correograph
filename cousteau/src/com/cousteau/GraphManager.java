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
			_logger.debug("before graph");
			cg = CorreoGraph.getInstance();
			_logger.debug("afta-graph");
			if (cg == null) {
				System.out.println("Can't initialize the graph");
				System.exit(1);
			}

			if (transport != null) { 
				while(!me.isInterrupted()) {
					Message msg = transport.take();
					_logger.debug("got msg from transport. subj: " + msg.getSubject());
					cg.addMessage(msg);
				}
			}
			
		} catch (Exception e) {
			_logger.error("Exiting from GraphManager thread", e);
		} finally {
			if (cg != null)
				cg.shutdown();
			else
				_logger.error("Graph was never initialized!");
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
