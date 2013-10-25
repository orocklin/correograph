package com.cousteau.entities;

import javax.mail.Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressDetails {
	
	private static final Logger _logger = LoggerFactory
			.getLogger(AddressDetails.class);
	
	private String name;
	
	private String email;
	
	private String domain;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}
	
	public String getDomain() {
		return domain;
	}

	public void setEmail(String email) {
		this.email = email;
		if (email != null && email.length() > 1) {
			int idx = email.indexOf('@') + 1;
			if (idx > 0 && idx < email.length())
				domain = email.substring(idx);
		}
	}
	
		
	public AddressDetails() {
	}
	
	public AddressDetails(Address addr) {
		if (addr != null) {
			try {
				if (!"rfc822".equalsIgnoreCase(addr.getType())) {
					_logger.warn("Parsing might fail for address by the spec " + addr.getType() + " - RFC 822 is expected");
				}
				
				String full_addr = addr.toString();
				int eadr_far_idx = full_addr.lastIndexOf('>');
				if (eadr_far_idx < 0) {
					name = null;
					setEmail(full_addr.trim()); //We're assuming correctness of the address based on the source being javax.mail.Address class
				} else {
					int eadr_near_idx = full_addr.lastIndexOf('<');
					if (eadr_near_idx < 0) {
						name = null;
						email = null;
						throw new RuntimeException("Unable to parse " + full_addr + " no '<' found to match '>'");
					}
					
					name = full_addr.substring(0, eadr_near_idx).trim();
					setEmail(full_addr.substring(eadr_near_idx + 1, eadr_far_idx));
				}
				
			} catch (Exception e) {
				_logger.error("Unable to parse address " + addr.toString(), e);
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AddressDetails other = (AddressDetails) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AddressDetails [name=" + name + ", email=" + email + "]";
	}
	
	

}
