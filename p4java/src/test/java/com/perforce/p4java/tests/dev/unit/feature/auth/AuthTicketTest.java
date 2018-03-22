/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.perforce.p4java.server.AuthTicket;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("AuthTicketTest")
public class AuthTicketTest extends P4JavaTestCase {

    /**
     * Test empty ticket
     */
    @Test
    public void testEmpty() {
	AuthTicket ticket = new AuthTicket();
	assertNull(ticket.getServerAddress());
	assertNull(ticket.getTicketValue());
	assertNull(ticket.getUserName());
	assertNotNull(ticket.toString());

	assertEquals(ticket, new AuthTicket());
	assertEquals(ticket.hashCode(), new AuthTicket().hashCode());
	assertEquals(ticket.toString(), new AuthTicket().toString());
    }

    /**
     * Test building up a ticket
     */
    @Test
    public void testBuild() {
	String address = "server:1666";
	String value = "ABCDEF123123";
	String user = "bruno";

	AuthTicket ticket = new AuthTicket();
	ticket.setServerAddress(address);
	assertEquals(address, ticket.getServerAddress());
	ticket.setUserName(user);
	assertEquals(user, ticket.getUserName());
	ticket.setTicketValue(value);
	assertEquals(value, ticket.getTicketValue());
    }

    /**
     * Test ticket with valid values
     */
    @Test
    public void testValid() {
	String address = "server:1666";
	String value = "ABCDEF123123";
	String user = "bruno";

	AuthTicket ticket = new AuthTicket(address, user, value);
	assertEquals(address, ticket.getServerAddress());
	assertEquals(user, ticket.getUserName());
	assertEquals(value, ticket.getTicketValue());
	assertNotNull(ticket.toString());
    }

}
