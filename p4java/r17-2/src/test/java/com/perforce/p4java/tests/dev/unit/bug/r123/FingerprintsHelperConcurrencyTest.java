package com.perforce.p4java.tests.dev.unit.bug.r123;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.server.Fingerprint;
import com.perforce.p4java.server.FingerprintsHelper;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

@RunWith(JUnitPlatform.class)
@Jobs({"job059813"})
@TestId("Dev123_FingerprintsHelperConcurrencyTest")
public class FingerprintsHelperConcurrencyTest extends P4JavaTestCase {
  /**
   * Test saving fingerprints
   */
  @Test
  public void testSaveFingerprintsConcurrently() throws Exception {
    int randNo = getRandomInt();

    String address = "server:1666";
    String value = "ABCDEF123123";
    String user = "**++**";

    String fingerprintsFilePath = System.getProperty("user.dir");
    assertThat(fingerprintsFilePath, notNullValue());
    fingerprintsFilePath += File.separator + "realfingerprintsfile" + randNo;

    try {
      // Create the first fingerprints file
      FingerprintsHelper.saveFingerprint(
          user,
          address,
          value,
          fingerprintsFilePath);

      // Run concurrent reads and writes
      ExecutorService executor = Executors.newFixedThreadPool(10);
      for (int i = 0; i < 25; i++) {
        String addr = address + i;
        String val = value + i;
        String usr = user + i;

        Runnable task;
        if ((i % 2) == 0) {
          task = new FingerprintsWriter(
              usr,
              addr,
              val,
              fingerprintsFilePath);
        } else {
          task = new FingerprintsReader(fingerprintsFilePath);
        }

        executor.execute(task);
      }

      executor.shutdown();

      while (!executor.isTerminated()) {
        System.out.println("Threads are still running...");
        Thread.sleep(2000);
      }

      System.out.println("Finished all threads");

    } finally {
      File fingerprintsFile = new File(fingerprintsFilePath);
      boolean deleted = fingerprintsFile.delete();
      assertThat(deleted, is(true));
    }
  }

  private class FingerprintsWriter implements Runnable {
    private String user = null;
    private String address = null;
    private String value = null;
    private String trustFilePath = null;

    FingerprintsWriter(
        final String user,
        final String address,
        final String value,
        final String trustFilePath) {

      this.user = user;
      this.address = address;
      this.value = value;
      this.trustFilePath = trustFilePath;
    }

    public void run() {
      try {
        FingerprintsHelper.saveFingerprint(
            user,
            address,
            value,
            trustFilePath);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private class FingerprintsReader implements Runnable {
    private String trustFilePath = null;

    FingerprintsReader(final String trustFilePath) {
      this.trustFilePath = trustFilePath;
    }

    public void run() {
      try {
        Fingerprint[] fingerprints =
            FingerprintsHelper.getFingerprints(trustFilePath);
        for (Fingerprint fingerprint : fingerprints) {
          debugPrint(fingerprint.toString());
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
