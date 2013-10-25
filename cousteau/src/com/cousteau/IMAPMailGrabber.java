package com.cousteau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IMAPMailGrabber {
	
	private static final boolean debug = true;
	
	private static final String INBOX_FOLDER = "INBOX";
	
	private final FetchProfile fetchProfile = new FetchProfile();
	
    private final Logger logger = LoggerFactory.getLogger(IMAPMailGrabber.class);
	
	private Store store = null;
	
	private int fetch_cnt = 20;
	
	private String server = "imap.gmail.com";
	private String user = "<username>@gmail.com";
	private String pwd = "<password>";
	
	private MsgTransport transport;
	
	public IMAPMailGrabber(String user, String pwd) {
		this.user = user;
		this.pwd = pwd;
		
		fetchProfile.add(FetchProfile.Item.ENVELOPE);
		fetchProfile.add("Message-ID");
		fetchProfile.add("In-Reply-To");
		fetchProfile.add("References:");
	}
	
	public IMAPMailGrabber(String server, String user, String pwd) {
		this(user, pwd);
		this.server = server;
	}
	
	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	
	public int getFetchCount() {
		return fetch_cnt;
	}

	public void setFetchCount(int cnt) {
		this.fetch_cnt = cnt;
	}
	
	public MsgTransport getTransport() {
		return transport;
	}

	public void setTransport(MsgTransport transport) {
		this.transport = transport;
	}

	public void createSession() {
		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", "imaps");
		try {
			Session session = Session.getDefaultInstance(props, null);
			store = session.getStore("imaps");
			store.connect(server, user, pwd);

		} catch (NoSuchProviderException e) {
			logger.error("Can't open IMAP connection to " + server + " for user " + user, e);
		} catch (MessagingException e) {
			logger.error("Can't open IMAP connection to " + server + " for user " + user, e);
		}
	}
	
	public void disconnect() {
		if (isConnected()) {
			try {
				store.close();
			} catch (MessagingException e) {
				logger.error("Can't close IMAP connection to " + server, e);
			}
		}
	}
	
	public boolean isConnected() {
		if (store == null || !store.isConnected())
			return false;
		else
			return true;
	}
	
	public List<String> getFolders() {
		Folder[] folders;
		try {
			folders = store.getDefaultFolder().list("*");
		} catch (MessagingException e) {
			e.printStackTrace();
			return null;
		}
		List<String> res = new ArrayList<String>(folders.length);
		for (Folder f : folders) {
			res.add(f.getName());
			try {
				logger.debug(f.getName() + "\tnew messages: " + f.getNewMessageCount() + "\tunread:" + f.getUnreadMessageCount() + "\ttotal: " + f.getMessageCount());
			} catch (MessagingException e) {
				logger.error("Can't read folder messgae count for " + f.getName(), e);
			}
		}
		return res;
	}
	
	
	public void traverseFolder(String name) {
		try {
			Folder fd = store.getFolder(name);
			if (fd == null)
				return;
			
			if (!fd.isOpen())
				fd.open(Folder.READ_ONLY);

			int cnt = fd.getMessageCount();
			Message[] msgs = null;
			if (fetch_cnt > 0)
				msgs = fd.getMessages((cnt >= fetch_cnt) ? cnt - fetch_cnt : 0, cnt);
			else
				msgs = fd.getMessages();
			
			fd.fetch(msgs, fetchProfile);
			
			for (Message msg : msgs) {
				
				//BEWARE: this section may block, depending on the transport implementation
				if (transport != null) {
					transport.submitMessage(msg);
				}
				
				if (debug) {
					System.out.println("===================");
					System.out.println("From: " + Arrays.toString(msg.getFrom()));
					System.out.println("Reply To Addr: " + Arrays.toString(msg.getReplyTo()));
					
					System.out.println("To: " + Arrays.toString(msg.getRecipients(RecipientType.TO)));
					System.out.println("CC: " + Arrays.toString(msg.getRecipients(RecipientType.CC)));
					System.out.println("BCC: " + Arrays.toString(msg.getRecipients(RecipientType.BCC)));
					
					System.out.println("Subj: " + msg.getSubject());
					System.out.println("Sent on: " + msg.getSentDate().toString());
					System.out.println("Message ID: " + Arrays.toString(msg.getHeader("Message-ID")));
					System.out.println("In-Reply-To: " + Arrays.toString(msg.getHeader("In-Reply-To")));
					System.out.println(System.currentTimeMillis());
					System.out.println("-------------------");
				}
			}

			fd.close(false);
		} catch (MessagingException e) {
			logger.error("Can't traverse folder " + name, e);
		}
	}
	
	public void traverseFolder() {
		traverseFolder(INBOX_FOLDER);
	}
	
	
	public static void main(String ... args) {
		IMAPMailGrabber grabber = new IMAPMailGrabber("orokhlin@gmail.com", "xap6kob80");
		grabber.createSession();
		List<String> fldrs = grabber.getFolders(); 
		for (String f : fldrs) {
			System.out.println(f);
		}
		
		grabber.traverseFolder();
		
		grabber.disconnect();
	}

}
