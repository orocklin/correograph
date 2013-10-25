package com.cousteau;

import java.lang.reflect.Field;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.MimeMessage.RecipientType;

import com.cousteau.entities.AddressDetails;
import com.orientechnologies.orient.core.exception.OTransactionException;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorreoGraph {
	
	
	private static final Logger _logger = LoggerFactory.getLogger(CorreoGraph.class);
	
	private static volatile String graphDb = "remote:localhost/maildump";
	
	private final OrientGraph graph;
	
	private static final int MAX_RETRY = 10;
	
	//NAMING OF THESE IS VERY IMPORTANT - USED FOR AUTODISCOVERY
	private static final String KEY_ADDRESS_NAME = "addr_name";
	private static final String KEY_ADDRESS_EMAIL_UNIQE = "addr_email";
	
	private static final String LBL_LABEL = "Label";

	private static final class GraphHolder {
		private static final CorreoGraph me = new CorreoGraph(); 
	}
	
	private CorreoGraph() {
		_logger.info("Connecting to database: " + graphDb);
		graph = new OrientGraph(graphDb);
		indexSetup();
	}
	
	public static synchronized CorreoGraph getInstance(String db) {
		graphDb = db;
		return GraphHolder.me;
	}
	
	public static CorreoGraph getInstance() {
		return GraphHolder.me;
	}
	
	
	public final void shutdown() {
		if (graph != null)
			graph.shutdown();
	}
	
	private void indexSetup() {
		//Here we reflectively determine names for indexed properties and verify that we have indices in the graph
		Set<String> indKeys = graph.getIndexedKeys(Vertex.class);
		Field[] flds = CorreoGraph.class.getDeclaredFields();

		for (Field fld : flds) {
			String fld_name = fld.getName();
			_logger.debug("Processing property: " + fld_name);
			if (fld_name.startsWith("KEY_")) {
				try {
					boolean isPub = fld.isAccessible(); 
					fld.setAccessible(true);
					
					String key = (String)fld.get(this);
					_logger.debug("Verifying the index for " + key);
					
					if (!indKeys.contains(key)) {
						if (fld_name.endsWith("_UNIQUE")) {
							graph.createKeyIndex(key, Vertex.class, new Parameter("type", "UNIQUE"));
							_logger.debug("Creating the unique index: " + key);
						} else {
							graph.createKeyIndex(key, Vertex.class);
							_logger.debug("Creating an index: " + key);
						}
					}
					
					fld.setAccessible(isPub);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void finalize() {
		shutdown();
	}
	
	public void addMessage(Message msg) {
		for (int i=0; i < MAX_RETRY; i++) {
			try {
				
				//FROM section
				Address[] f_addr = msg.getFrom();
				if (f_addr == null || f_addr.length == 0) {
					_logger.error("No sender info for the message!!");
					return;
				}
				if (f_addr.length > 1) {
					_logger.warn("Got multiple senders for the email, just saying... Although for now gonna use just the first one - " + f_addr[0].toString());
				}
				AddressDetails f_ad = new AddressDetails(f_addr[0]);
				
				Vertex v_from = findOrCreateAddress(f_ad, false);
				
				//TO section
				Address[] rec_addr = msg.getRecipients(RecipientType.TO);
				if (rec_addr == null || rec_addr.length == 0) {
					_logger.debug("No TO in email");
				} else {
					for (Address addr : rec_addr) {
						AddressDetails t_ad = new AddressDetails(addr);
						
						Vertex v_to = findOrCreateAddress(t_ad, false);
						
						Edge sendsTo = graph.addEdge(null, v_from, v_to, "emails_to");
						sendsTo.setProperty("timestamp", msg.getSentDate().toString());
						
					}
				}
			
				graph.commit();
				break;
			} catch (OTransactionException te) {
				_logger.warn("Retrying transaction. Attempt " + i, te);
				if (i > 0.7 * MAX_RETRY) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				_logger.error("Can't add message to graph", e);
				graph.rollback();
				break;
			}
		}
	}
	
	private Vertex findOrCreateAddress(AddressDetails ad, boolean commit) {
		if (ad == null)
			return null;
		Iterable<Vertex> res = graph.getVertices(KEY_ADDRESS_EMAIL_UNIQE, ad.getEmail());
		if (res == null || !res.iterator().hasNext()) {
			Vertex addr = graph.addVertex(null);
				addr.setProperty(KEY_ADDRESS_EMAIL_UNIQE, ad.getEmail());
				if (ad.getName() != null)
					addr.setProperty(KEY_ADDRESS_NAME, ad.getName());
				
			if (commit)
				graph.commit();
			
			return addr;
		} else {
			Vertex theOne = res.iterator().next();
			if (res.iterator().hasNext()) {
				Vertex theTwo = res.iterator().next();
				_logger.warn("Duplicate vertex detected for " + ad.toString() + " as " + theTwo.getProperty(KEY_ADDRESS_EMAIL_UNIQE));
			}
			if (ad.getName() != null && theOne.getProperty(KEY_ADDRESS_NAME) == null) {
				theOne.setProperty(KEY_ADDRESS_NAME, ad.getName());
				if (commit)
					graph.commit();
			}
			return theOne;
		}
	}
	

}
