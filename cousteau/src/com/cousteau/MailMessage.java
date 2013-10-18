package com.cousteau;

import java.util.List;
import java.util.UUID;

public class MailMessage {
	
	public enum Read {READ, UNREAD, UNKNOWN};
	
	private UUID id;
	
	private String from;
	
	private List<String> to_recipients;
	
	private List<String> cc_recipients;
	
	private String sendingServer;
	
	private String headers;
	
	private String body;
	
	private Read read = Read.UNKNOWN;
	
	

}
