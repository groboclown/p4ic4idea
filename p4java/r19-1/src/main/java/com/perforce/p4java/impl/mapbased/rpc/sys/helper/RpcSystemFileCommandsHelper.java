package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default JDK 6 implementation of the ISystemFileCommandsHelper interface. Uses
 * introspection so it can be compiled (if not actually work) on JDK 5 systems.
 * Actual use of this on JDK 5 systems is OK to the extent that things like edit
 * or sync may end up with the wrong permissions on the client, but much else
 * will work just fine.
 *
 * @author sshou clean code & add vargs to log methods, like slf4j
 */
public class RpcSystemFileCommandsHelper extends SymbolicLinkHelper {
    @Override
    public boolean setWritable(String fileName, boolean writable) {
        // skip symbolic; Java changes the link's target permission not the link
        if(SymbolicLinkHelper.isSymbolicLink(fileName)) {
            return true;
        }
        return new File(fileName).setWritable(writable);
    }

    @Override
    public boolean setReadable(String fileName, boolean readable, boolean ownerOnly) {
        return new File(fileName).setReadable(readable, ownerOnly);
    }

    @Override
    public boolean setOwnerReadOnly(String fileName) {
        boolean set = setReadable(fileName, false, false);
        set &= setReadable(fileName, true, true);
        return set;
    }

    @Override
    public boolean setExecutable(String fileName, boolean executable, boolean ownerOnly) {
        return new File(fileName).setExecutable(executable, ownerOnly);
    }

    @Override
    public boolean canExecute(String fileName) {
        return Files.isExecutable(Paths.get(fileName));
    }

    @Override
    public boolean isSymlink(String fileName) {
        return isSymbolicLink(fileName);
    }
}
