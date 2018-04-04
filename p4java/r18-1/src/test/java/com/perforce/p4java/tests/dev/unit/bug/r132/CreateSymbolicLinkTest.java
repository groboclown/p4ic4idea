package com.perforce.p4java.tests.dev.unit.bug.r132;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test create symbolic link with non-existing target.
 */
@RunWith(JUnitPlatform.class)
public class CreateSymbolicLinkTest extends P4JavaTestCase {
    /**
     * Test create symbolic link with non-existing target.
     */
    @Test
    public void testCreateSymbolicLink() throws IOException {
        // Check if symlink capable (JDK 7 or above)
        if (SymbolicLinkHelper.isSymbolicLinkCapable()) {
            String target = File.createTempFile("link-to-me", ".tmp").getAbsolutePath();
            String link = System.getProperty("java.io.tmpdir") + "/p4java-bin-" + getRandomInt();

            // Create symbolic link
            debugPrint("Creating link " + link + " to " + target);
            String path = SymbolicLinkHelper.createSymbolicLink(link, target);
            assertNotNull("Failed to create a symbolic link from " + link + " to " + target);
            assertTrue(path + " is not a symbolic link", SymbolicLinkHelper.isSymbolicLink(path));

            debugPrint("Creating a new file object for " + path);
            File file = new File(path);
            assertTrue("A new File object to " + path + " is not reported as a symbolic link.",
                    Files.isSymbolicLink(file.toPath()));
            assertTrue("A new File object to " + path + " is not reported as not existing.",
                    file.exists());

            debugPrint("Checking the parent path " + path);
            String parentPath = file.getParent();
            assertNotNull(file + " has not parent path.", parentPath);

            debugPrint("Deleting " + file);
            assertTrue("Failed to delete " + file.toPath(), Files.deleteIfExists(file.toPath()));
        }
    }

}
