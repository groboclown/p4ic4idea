/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import com.perforce.p4java.server.Fingerprint;
import com.perforce.p4java.server.FingerprintsHelper;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.fail;

@Jobs({"job059814"})
@TestId("Dev123_InMemoryFingerprintsHelperConcurrencyTest")
public class InMemoryFingerprintsHelperConcurrencyTest extends P4JavaRshTestCase {

	class FingerprintsWriter implements Runnable {

		private String user = null;
		private String address = null;
		private String value = null;
		private String trustFilePath = null;

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
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	class FingerprintsReader implements Runnable {

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
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
	}

	/**
	 * Test saving fingerprints
	 */
	@Test
	public void testSaveFingerprintsConcurrently() {
		String address = "server:1666";
		String value = "ABCDEF123123";
		String user = "**++**";

		String fingerprintsFilePath = null;

		try {
			// Create the first fingerprints file
			FingerprintsHelper.saveFingerprint(user, address, value,
					fingerprintsFilePath);

			// Run concurrent reads and writes
			ExecutorService executor = Executors.newFixedThreadPool(10);
			for (int i = 0; i < 25; i++) {
				String addr = address + i;
				String val = value + i;
				String usr = user + i;

				Runnable task = null;

				if ((i % 2) == 0) {
					task = new FingerprintsWriter(usr, addr, val,
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

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}

}
