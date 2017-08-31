package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.core.IFileLineMatch.MatchType.AFTER;
import static com.perforce.p4java.core.IFileLineMatch.MatchType.BEFORE;
import static com.perforce.p4java.core.IFileLineMatch.MatchType.MATCH;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IFileLineMatch;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.MatchingLinesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class GetMatchingLinesTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;
    private static List<IFileSpec> testFileSpecs = null;
    private static String testFileName = "`1234567890-=][poiuytrewqasdfghjkl;'.,mnbvcxz ~!@#$%^&()_+}{POIUYTREWQASDFGHJKLMNBVCXZ.txt";
    private static String string1024Characters = "1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters1024characters10";
    private static String stringAllCharacters = "`1234567890-=\\][poiuytrewqasdfghjkl;'/.,mnbvcxz ~!@#$%^&*()_+|}{POIUYTREWQASDFGHJKL:\"?><MNBVCXZ";
    private static File testFile2 = null;
    private static String testFile2Name = "test2.txt";
    private static File longFile = null;
    private static String longFileName = "long.txt";

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

        String testFileContent = "gone";
        testFile = new File(client.getRoot() + FILE_SEP + testFileName);
        testFileSpecs = h.addFile(server, user, client, testFile.getAbsolutePath(), testFileContent, "text");

        testFileContent = "one";
        IChangelist changelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), testFileContent, changelist, client);
        changelist.submit(null);

        testFileContent += "\ntwo";
        changelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), testFileContent, changelist, client);
        changelist.submit(null);

        testFileContent += "\nthree";
        changelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), testFileContent, changelist, client);
        changelist.submit(null);

        testFileContent += "\n" + string1024Characters;
        changelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), testFileContent, changelist, client);
        changelist.submit(null);

        testFileContent += "\n" + stringAllCharacters;
        changelist = h.createChangelist(server, user, client);
        h.editFile(testFile.getAbsolutePath(), testFileContent, changelist, client);
        changelist.submit(null);

        testFile2 = new File(client.getRoot() + FILE_SEP + testFile2Name);
        h.addFile(server, user, client, testFile2.getAbsolutePath(), "two", "text");

        longFile = new File(client.getRoot() + FILE_SEP + longFileName);
        String longText = "1234567890";
        for (int i = 0; i < 500; i++) {
            longText = longText.concat("1234567890");
        }
        h.addFile(server, user, client, longFile.getAbsolutePath(), longText);
    }


    // file specs
    @Test(expected = NullPointerException.class)
    public void fileSpecsNull() throws Throwable {

        server.getMatchingLines(null, "one", null);

    }


    // pattern
    @Test(expected = NullPointerException.class)
    public void patternNull() throws Throwable {

        server.getMatchingLines(testFileSpecs, null, null);

    }


    // depot file
    @Test
    public void depotFileAllCharacters() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(testFileSpecs, "three", null);

        assertEquals(1, matches.size());
        assertEquals("//depot/" + h.expandASCII(testFileName), matches.get(0).getDepotFile());
    }

    @Test
    public void depotFileMultipleFiles() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(makeFileSpecList("//depot/..."), "two", null);

        assertEquals(2, matches.size());
        assertEquals("//depot/" + h.expandASCII(testFileName), matches.get(0).getDepotFile());
        assertEquals("//depot/" + h.expandASCII(testFile2Name), matches.get(1).getDepotFile());
    }


    // line
    @Test
    public void line1024Characters() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(testFileSpecs, "1024", null);

        assertEquals(1, matches.size());
        assertEquals(string1024Characters, matches.get(0).getLine());
    }

    @Test
    public void lineAllCharacters() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(testFileSpecs, "`123", null);

        assertEquals(1, matches.size());
        assertEquals(stringAllCharacters, matches.get(0).getLine());
    }


    // line number
    @Test
    public void lineNumberFirst() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(testFileSpecs, "one", new MatchingLinesOptions().setIncludeLineNumbers(true));

        assertEquals(1, matches.size());
        assertEquals(1, matches.get(0).getLineNumber());
    }

    @Test
    public void lineNumberLast() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(testFileSpecs, "`123", new MatchingLinesOptions().setIncludeLineNumbers(true));

        assertEquals(1, matches.size());
        assertEquals(5, matches.get(0).getLineNumber());
    }


    // revision
    @Test
    public void revisionFirst() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(makeFileSpecList(testFileSpecs.get(0).getOriginalPathString() + "#1,#head"), "gone", new MatchingLinesOptions().setAllRevisions(true));

        assertEquals(1, matches.size());
        assertEquals(1, matches.get(0).getRevision());
    }

    @Test
    public void revisionLast() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(testFileSpecs, "`123", null);

        assertEquals(1, matches.size());
        assertEquals(6, matches.get(0).getRevision());
    }


    // type
    @Test
    public void typeMatch() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(testFileSpecs, "`123", null);

        assertEquals(1, matches.size());
        assertEquals(MATCH, matches.get(0).getType());
    }

    @Test
    public void typeBefore() throws Throwable {
        List<IFileLineMatch> matches = server.getMatchingLines(testFileSpecs, "`123", new MatchingLinesOptions().setLeadingContext(1));

        assertEquals(2, matches.size());
        assertEquals(BEFORE, matches.get(0).getType());
    }

    @Test
    public void typeAfter() throws Throwable {
        List<String> warnings = new ArrayList<String>();
        List<IFileLineMatch> matches = server.getMatchingLines(testFileSpecs, "1024", warnings, new MatchingLinesOptions().setTrailingContext(1));

        assertEquals(2, matches.size());
        assertEquals(AFTER, matches.get(1).getType());
        assertEquals("should not have warnings", 0, warnings.size());
    }

    @Test
    public void warnings() throws Throwable {
        List<String> warnings = new ArrayList<String>();

        List<IFileLineMatch> matches = server.getMatchingLines(makeFileSpecList("//depot/..."), "123", warnings, null);

        assertEquals(1, matches.size());
        assertEquals("wrong number of warnings", 1, warnings.size());
        assertThat(warnings.get(0), containsString("maximum line length of 4096 exceeded"));
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }


}