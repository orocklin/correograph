package com.cousteau;

import static org.junit.Assert.*;

import org.junit.Test;

public class KafkaNnectorTest {

	@Test
	public void testGetInstance() {
		KafkaNnector conn = KafkaNnector.getInstance();
		assertNotNull(conn);
		assertNotNull(conn.getProducer());
	}

}
