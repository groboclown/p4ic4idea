package com.perforce.p4java.tests.qa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.Test;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.impl.generic.admin.ProtectionEntry;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetProtectionEntriesTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;

    // create a group for testing
    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        IUserGroup group = new UserGroup();

        ArrayList<String> users = new ArrayList<String>();
        users.add(ts.getUser());
        group.setUsers(users);
        group.setName("test");
        group.setOwners(users);

        server.createUserGroup(group, null);
    }

    // reset the table
    @Before
    public void before() {

        try {

            List<IProtectionEntry> table = new ArrayList<IProtectionEntry>();
            server.updateProtectionEntries(table);

        } catch (Throwable t) {

            h.error(t);

        }

    }

    // basic use of getProtectionEntries
    @Test
    public void getDefaultTable() {
        try {
            server.getProtectionEntries(true, null, null, null, null);

            fail("did not see requestException");

        } catch (Throwable t) {

            assertThat("wrong error", t.getLocalizedMessage(), containsString("Protections table is empty."));

        }
    }

    // basic use of getProtectionEntries
    @Test
    public void addLines() throws Throwable {
        List<IProtectionEntry> table = new ArrayList<IProtectionEntry>();

        IProtectionEntry entry = new ProtectionEntry();
        entry.setPath("//...");
        entry.setGroup(false);
        entry.setName(user.getLoginName());
        entry.setHost("*");
        entry.setMode("super");

        table.add(entry);

        server.updateProtectionEntries(table);

        table = server.getProtectionEntries(true, null, null, null, null);

        assertEquals("wrong number of entries", 1, table.size());
        assertThat("incorrect mode", table.get(0).getMode(), containsString("super"));
    }

    // attempt to create table with a bad entry
    @Test
    public void badEntry() {
        try {
            List<IProtectionEntry> table = new ArrayList<IProtectionEntry>();

            IProtectionEntry entry = new ProtectionEntry();
            entry.setPath("//...");
            entry.setGroup(false);
            entry.setName(user.getLoginName());
            entry.setHost("*");

            table.add(entry);

            server.updateProtectionEntries(table);

            fail("should have failed table creation");
        } catch (Throwable t) {

            assertThat(t.getLocalizedMessage(), containsString("Wrong number of words for field 'Protections'"));

        }
    }

    // basic use of getProtectionEntries
    @Test
    public void singleLineProtectTable() throws Throwable {
        List<IProtectionEntry> table = new ArrayList<IProtectionEntry>();

        IProtectionEntry entry = new ProtectionEntry();
        entry.setPath("//...");
        entry.setGroup(false);
        entry.setName(user.getLoginName());
        entry.setHost("*");
        entry.setMode("super");
        entry.setOrder(0);
        table.add(entry);

        server.updateProtectionEntries(table);

        // should only have one item
        table = server.getProtectionEntries(true, null, null, null, null);

        assertEquals("wrong number of entries", 1, table.size());
        assertThat("incorrect mode", table.get(0).getMode(), containsString("super"));
        assertThat("incorrect mode", table.get(0).getName(), containsString(user.getLoginName()));
    }

    // create illegal table
    @Test
    public void noSuperUser() {
        try {
            List<IProtectionEntry> table = new ArrayList<IProtectionEntry>();

            IProtectionEntry entry = new ProtectionEntry();
            entry.setPath("//...");
            entry.setGroup(false);
            entry.setName(user.getLoginName());
            entry.setHost("*");
            entry.setMode("write");
            entry.setOrder(0);
            table.add(entry);

            server.updateProtectionEntries(table);

            fail("table should not update");
        } catch (Throwable t) {

            assertThat("incorrect error", t.getLocalizedMessage(), containsString("Can't delete last valid 'super' entry"));

        }
    }

    // verify that group entries are created correctly
    @Test
    public void createTableWithGroup() throws Throwable {
        List<IProtectionEntry> table = new ArrayList<IProtectionEntry>();

        IProtectionEntry entry = new ProtectionEntry();
        entry.setPath("//...");
        entry.setGroup(true);
        entry.setName("test");
        entry.setHost("*");
        entry.setMode("super");
        entry.setOrder(0);
        table.add(entry);

        server.updateProtectionEntries(table);

        // should only have one item
        table = server.getProtectionEntries(true, null, null, null, null);

        assertEquals("wrong number of entries", 1, table.size());
        assertThat("incorrect mode", table.get(0).getMode(), containsString("super"));
        assertThat("incorrect mode", table.get(0).getName(), containsString("test"));
        assertTrue("not a group", table.get(0).isGroup());
    }

    // verify exclusions work, implicit creation
    @Test
    public void exclusionLines() throws Throwable {
        List<IProtectionEntry> table = new ArrayList<IProtectionEntry>();

        IProtectionEntry entry = new ProtectionEntry();
        entry.setPath("//...");
        entry.setGroup(false);
        entry.setName(user.getLoginName());
        entry.setHost("*");
        entry.setMode("super");
        entry.setOrder(0);
        table.add(entry);

        entry = new ProtectionEntry();
        entry.setPath("-//depot/foo/...");
        entry.setGroup(false);
        entry.setName(user.getLoginName());
        entry.setHost("*");
        entry.setMode("super");
        entry.setOrder(1);
        table.add(entry);

        server.updateProtectionEntries(table);

        // should only have one item
        table = server.getProtectionEntries(true, null, null, null, null);

        assertEquals("wrong number of entries", 2, table.size());
        assertThat("incorrect mode", table.get(1).getMode(), containsString("super"));
        assertThat("incorrect mode", table.get(1).getName(), containsString(user.getLoginName()));
        assertTrue("not an exclusion", table.get(1).isPathExcluded());
        assertThat("wrong path", table.get(1).getPath(), containsString("//depot/foo/..."));
    }

    // verify exclusions work, explicit creation
    @Test
    public void exclusionLinesExplicitly() throws Throwable {
        List<IProtectionEntry> table = new ArrayList<IProtectionEntry>();

        IProtectionEntry entry = new ProtectionEntry();
        entry.setPath("//...");
        entry.setGroup(false);
        entry.setName(user.getLoginName());
        entry.setHost("*");
        entry.setMode("super");
        entry.setOrder(0);
        table.add(entry);

        entry = new ProtectionEntry();
        entry.setPath("//depot/foo/...");
        entry.setGroup(false);
        entry.setName(user.getLoginName());
        entry.setHost("*");
        entry.setMode("super");
        entry.setOrder(1);
        entry.setPathExcluded(true);
        table.add(entry);

        server.updateProtectionEntries(table);

        // should only have one item
        table = server.getProtectionEntries(true, null, null, null, null);

        assertEquals("wrong number of entries", 2, table.size());
        assertThat("incorrect mode", table.get(1).getMode(), containsString("super"));
        assertThat("incorrect mode", table.get(1).getName(), containsString(user.getLoginName()));
        assertTrue("not an exclusion", table.get(1).isPathExcluded());
        assertThat("wrong path", table.get(1).getPath(), containsString("//depot/foo/..."));
    }

    // create table with a lot of lines
    @Test
    public void bigTable() throws Throwable {
        List<IProtectionEntry> table = new ArrayList<IProtectionEntry>();

        IProtectionEntry entry = null;

        entry = new ProtectionEntry();
        entry.setPath("//...");
        entry.setGroup(false);
        entry.setName(user.getLoginName());
        entry.setHost("*");
        entry.setMode("super");
        entry.setOrder(0);
        table.add(entry);

        for (int i = 1; i < 10000; i++) {
            entry = new ProtectionEntry();
            entry.setPath("//...");
            entry.setGroup(false);
            entry.setName(user.getLoginName() + i);
            entry.setHost("*");
            entry.setMode("super");
            entry.setOrder(i);
            table.add(entry);
        }

        server.updateProtectionEntries(table);

        // should only have one item
        table = server.getProtectionEntries(true, null, null, null, null);

        assertEquals("wrong number of entries", 10000, table.size());
        assertThat("incorrect mode", table.get(1).getMode(), containsString("super"));
        assertThat("incorrect mode", table.get(1).getName(), containsString(user.getLoginName()));
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	