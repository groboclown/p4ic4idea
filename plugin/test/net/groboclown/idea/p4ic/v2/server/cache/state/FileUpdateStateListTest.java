package net.groboclown.idea.p4ic.v2.server.cache.state;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.mock.MockFilePath;
import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FileUpdateStateListTest {

    @Test
    public void testAdd_different() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping(null, createFilePath("a.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));
        P4FileUpdateState s2 = new P4FileUpdateState(
                new P4ClientFileMapping(null, createFilePath("b.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        list.add(s2);
        assertThat("two adds: " + list, list.copy().size(), is(2));
    }

    @Test
    public void testAdd_same() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping(null, createFilePath("a.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));
        P4FileUpdateState s2 = new P4FileUpdateState(
                new P4ClientFileMapping(null, createFilePath("a.txt")),
                2, FileUpdateAction.DELETE_FILE, true);
        assertThat(s1.hashCode(), is(s2.hashCode()));
        list.add(s2);
        assertThat("two adds, same file: " + list, list.copy().size(), is(1));
        assertThat("new copy type",
                list.iterator().next().getFileUpdateAction(),
                is(FileUpdateAction.DELETE_FILE));
    }

    @Test
    public void testReplaceWith() throws Exception {

    }

    @Test
    public void testRemove_Same_Local() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping(null, createFilePath("a.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));

        list.remove(s1);
        assertThat("one remove: " + list, list.copy().size(), is(0));
    }

    @Test
    public void testRemove_Same_LocalDepot() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping("//depot/1/2/3", createFilePath("a.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));

        list.remove(s1);
        assertThat("one remove: " + list, list.copy().size(), is(0));
    }

    @Test
    public void testRemove_Same_Depot() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping("//depot/1/2/3"),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));

        list.remove(s1);
        assertThat("one remove: " + list, list.copy().size(), is(0));
    }

    @Test
    public void testRemove_Equal_Local() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping(null, createFilePath("a.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));

        P4FileUpdateState s2 = new P4FileUpdateState(
                new P4ClientFileMapping(null, createFilePath("a.txt")),
                2, FileUpdateAction.DELETE_FILE, true);
        list.remove(s2);
        assertThat("one remove: " + list, list.copy().size(), is(0));
    }

    @Test
    public void testRemove_Equal_LocalDepot() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping("//depot/1/2/3", createFilePath("a.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));

        P4FileUpdateState s2 = new P4FileUpdateState(
                new P4ClientFileMapping("//depot/1/2/3", createFilePath("a.txt")),
                2, FileUpdateAction.DELETE_FILE, true);
        list.remove(s2);
        assertThat("one remove: " + list, list.copy().size(), is(0));
    }

    @Test
    public void testRemove_Equal_Depot() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping("//depot/1/2/3"),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));

        P4FileUpdateState s2 = new P4FileUpdateState(
                new P4ClientFileMapping("//depot/1/2/3"),
                2, FileUpdateAction.DELETE_FILE, true);
        list.remove(s2);
        assertThat("one remove: " + list, list.copy().size(), is(0));
    }

    private FilePath createFilePath(String f) {
        return createFilePath(new File(f));
    }

    private FilePath createFilePath(File f) {
        return new MockFilePath(f);
    }
}
