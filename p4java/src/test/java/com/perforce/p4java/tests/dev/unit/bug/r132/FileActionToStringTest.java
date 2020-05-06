package com.perforce.p4java.tests.dev.unit.bug.r132;

import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test FileAction.UNKNOWN.toString().
 */

@Jobs({ "job068151" })
@TestId("Dev132_FileActionToStringTest")
public class FileActionToStringTest extends P4JavaRshTestCase {

    /**
     * Test FileAction.UNKNOWN.toString().
     */
    @Test
    public void testFileActionToString() throws Exception {
        String fileAction = FileAction.UNKNOWN.toString();
        assertThat(fileAction, notNullValue());
        assertThat(fileAction, is("unknown"));
    }
}
