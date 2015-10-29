package net.groboclown.idea.p4ic.v2.server.cache.state;

import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FileUpdateStateListTest {

    @Test
    public void testAdd_different() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping(null, FilePathUtil.getFilePath("a.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));
        P4FileUpdateState s2 = new P4FileUpdateState(
                new P4ClientFileMapping(null, FilePathUtil.getFilePath("b.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE);
        list.add(s2);
        assertThat("two adds: " + list, list.copy().size(), is(2));
    }

    @Test
    public void testAdd_same() throws Exception {
        FileUpdateStateList list = new FileUpdateStateList();
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping(null, FilePathUtil.getFilePath("a.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE);
        list.add(s1);
        assertThat("one add: " + list, list.copy().size(), is(1));
        P4FileUpdateState s2 = new P4FileUpdateState(
                new P4ClientFileMapping(null, FilePathUtil.getFilePath("a.txt")),
                2, FileUpdateAction.DELETE_FILE);
        list.add(s2);
        assertThat("two adds, same file: " + list, list.copy().size(), is(1));
    }
}
