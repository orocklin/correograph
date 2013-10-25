package com.cousteau;

import java.io.IOException;
import java.io.InputStream;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import mireka.MailData;
import mireka.address.ReversePath;
import mireka.filter.AbstractDataRecipientFilter;
import mireka.filter.DataRecipientFilterAdapter;
import mireka.filter.Filter;
import mireka.filter.FilterType;
import mireka.filter.MailTransaction;
import mireka.filter.RecipientContext;
import mireka.smtp.RejectExceptionExt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardedMailPropagator implements FilterType {

	private static final Logger _logger = LoggerFactory.getLogger(ForwardedMailPropagator.class);
	

	@Override
	public Filter createInstance(MailTransaction mailTran) {
        FilterImpl filterInstance = new FilterImpl(mailTran, KafkaNnector.getInstance());
        return new DataRecipientFilterAdapter(filterInstance, mailTran);
	}

    private class FilterImpl extends AbstractDataRecipientFilter {
    	
    	private final Producer<String, String> prod;
    	private final String defTopic;
    	
    	private StringBuilder sb = new StringBuilder();
    	private byte[] buf = new byte[2048];
    	
    	private int cnt = 0;

		public FilterImpl(MailTransaction mailTransaction, KafkaNnector kafka) {
			super(mailTransaction);
			this.prod = kafka.getProducer();
			this.defTopic = kafka.getTopic();
		}
		
	    @Override
	    public void begin() {
	        sb = new StringBuilder();
	        cnt++;
	        if (cnt > 1)
	        	_logger.debug("The filter is reused - implement thread safety! " + cnt);
	    }
		
		@Override
	    public void from(ReversePath from) {
			sb.append("[<[FROM]-:]").append(from.getSmtpText()).append("[:-[FROM]>]");
			_logger.debug("added from: " + sb.toString());
		}
		
	    @Override
	    public void recipient(RecipientContext recipientContext)
	            throws RejectExceptionExt {
	    	sb.append("[<[RECIPIENT]-:]").append(recipientContext.recipient.sourceRouteStripped()).append("[:-[RECIPIENT]>]");
	    	_logger.debug("added recepient: " + sb.toString());
	    }
		
		@Override
		public void data(MailData data) {
			sb.append("[<[DATA]-:]");
			int len = 0;
			InputStream in = null;
			try {
				in = data.getInputStream();
				while ((len = in.read(buf)) > 0) {
					sb.append(new String(buf, 0, len));
				}
			} catch (IOException e) {
				_logger.error("Can't pull mail data stream", e);
			} finally {
				if (in != null)
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			
			sb.append("[:-[DATA]>]");
			_logger.debug("added body");
		}
		
	    @Override
	    public void done() {
	    	sb.append("[<[REMOTE]-:]");
	    	sb.append(mailTransaction.getMessageContext().getRemoteAddress().toString());
	    	sb.append("[:-[REMOTE]>]");
	    	
	    	prod.send(new KeyedMessage<String, String>(defTopic, sb.toString()));
	        _logger.debug("SENT!");
	    }
	    
    }
	
}
