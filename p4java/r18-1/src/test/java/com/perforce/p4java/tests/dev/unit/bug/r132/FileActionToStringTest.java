package com.perforce.p4java.tests.dev.unit.bug.r132;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test FileAction.UNKNOWN.toString().
 */
@RunWith(JUnitPlatform.class)
@Jobs({"job068151"})
@TestId("Dev132_FileActionToStringTest")
public class FileActionToStringTest extends P4JavaTestCase {

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
