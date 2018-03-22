package com.perforce.p4java.tests.dev.unit.bug.r123;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.server.FingerprintsHelper;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

@RunWith(JUnitPlatform.class)
@Jobs({"job057797"})
@TestId("Dev123_FingerprintsHelperTest")
public class FingerprintsHelperTest extends P4JavaTestCase {
  @Test
  public void testSaveFingerprints() throws Exception {
    int randNo = getRandomInt();

    String address = "server:1666";
    String value = "ABCDEF123123";
    String user = "bruno";

    String fingerprintsFilePath = System.getProperty("user.dir");
    assertThat(fingerprintsFilePath, notNullValue());
    fingerprintsFilePath += File.separator + "realfingerprintsfile" + randNo;

    try {
      // write 20 tickets
      for (int i = 0; i < 5; i++) {
        address += i;
        value += i;
        user += i;

        FingerprintsHelper.saveFingerprint(user, address, value, fingerprintsFilePath);
      }
    } finally {
      File fingerprintsFile = new File(fingerprintsFilePath);
      boolean deleted = fingerprintsFile.delete();
      assertThat(deleted, is(true));
    }
  }

}
