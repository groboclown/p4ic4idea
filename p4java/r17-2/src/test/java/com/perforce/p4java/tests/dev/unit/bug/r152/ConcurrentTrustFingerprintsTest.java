/**
 * Copyright (c) 2015 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.bug.r152;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.perforce.p4java.Log;
import com.perforce.p4java.server.Fingerprint;
import com.perforce.p4java.server.FingerprintsHelper;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test save trust fingerprints with multiple threads.
 */
@Jobs({"job083624"})
@TestId("ConcurrentTrustFingerprintsTest")
public class ConcurrentTrustFingerprintsTest extends P4JavaTestCase {
    private class FingerprintsWriter implements Runnable {

        private String user = null;
        private String address = null;
        private String value = null;
        private String trustFilePath = null;
        private int totalSuccessSaveFingerprints = 0;

        FingerprintsWriter(String user, String address, String value,
                           String trustFilePath) {
            this.user = user;
            this.address = address;
            this.value = value;
            this.trustFilePath = trustFilePath;
        }

        public void run() {
            try {
                FingerprintsHelper.saveFingerprint(this.user, this.address,
                        this.value, this.trustFilePath);
                totalSuccessSaveFingerprints++;
            } catch (IOException ignore) {
            }
        }

        int getTotalSuccessSaveFingerprints() {
            return totalSuccessSaveFingerprints;
        }
    }

    private class FingerprintsReader implements Runnable {
        private String trustFilePath = null;

        FingerprintsReader(String trustFilePath) {
            this.trustFilePath = trustFilePath;
        }

        public void run() {
            try {
                Fingerprint[] fingerprints = FingerprintsHelper
                        .getFingerprints(this.trustFilePath);
                for (Fingerprint fingerprint : fingerprints) {
                    debugPrint(fingerprint.toString());
                }
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Test save trust fingerprints with multiple threads.
     */
    @Test
    public void testSaveFingerprintsConcurrently() throws Exception {
        int randNo = getRandomInt();

        String address = "server:1666";
        String value = "ABCDEF123123";
        String user = "**++**";

        String trustFilePath = System.getProperty("user.dir");
        assertNotNull(trustFilePath);
        trustFilePath += File.separator + "realfingerprintsfile" + randNo;

        try {
            // Run concurrent reads and writes
            List<FingerprintsWriter> fingerprintsWriters = newArrayList();
            ExecutorService executor = Executors.newFixedThreadPool(10);
            for (int i = 0; i < 25; i++) {
                String addr = address + i;
                String val = value + i;
                String usr = user + i;

                Runnable task;

                if ((i % 2) == 0) {
                    task = new FingerprintsWriter(usr, addr, val, trustFilePath);
                    fingerprintsWriters.add((FingerprintsWriter) task);
                } else {
                    task = new FingerprintsReader(trustFilePath);
                }

                executor.execute(task);
            }

            executor.shutdown();

            while (!executor.isTerminated()) {
                Log.info("Threads are still running...");
                Thread.sleep(2000);
            }

            Log.info("Finished all threads");

            // Check the number of fingerprints in the file
            Fingerprint[] fingerprints = FingerprintsHelper.getFingerprints(trustFilePath);
            assertNotNull(fingerprints);

            int numFingerprints = fingerprintsWriters.stream()
                    .mapToInt(FingerprintsWriter::getTotalSuccessSaveFingerprints).sum();
            assertEquals(numFingerprints, fingerprints.length);
        } finally {
            File trustFile = new File(trustFilePath);
            boolean deleted = trustFile.delete();
            assertTrue(deleted);
        }
    }
}
