package net.groboclown.idea.p4ic.server.tasks;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.IChangelist;
import net.groboclown.idea.p4ic.ProjectRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.fail;

public class MoveFilesBetweenChangelistsTaskTest {
    @Rule
    public ProjectRule projectFixture;

    @Test
    public void testRunDoubleDefaults() throws Exception {
        try {
            new MoveFilesBetweenChangelistsTask(
                    projectFixture.getProject(),
                    IChangelist.DEFAULT, IChangelist.DEFAULT,
                    Collections.<FilePath>emptyList());
            fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testRun() throws Exception {

    }
}
