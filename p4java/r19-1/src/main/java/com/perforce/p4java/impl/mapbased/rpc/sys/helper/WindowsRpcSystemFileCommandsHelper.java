package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

/**
 * Windows specific implementation to help with file permissions.
 */
public class WindowsRpcSystemFileCommandsHelper extends SymbolicLinkHelper {

    /** Default exe paths. */
    private static final List<String> DEFAULT_WINDOWS_EXE_EXT;

    static {
        // Set up a list of default values for executable file extensions in
        // Windows  if the PATHEXT env variable is not set otherwise use PATHEXT
        DEFAULT_WINDOWS_EXE_EXT = new ArrayList<>();
        String exeExtensions = System.getenv("PATHEXT");
        if (isEmpty(exeExtensions)) {
            DEFAULT_WINDOWS_EXE_EXT.addAll(Arrays.asList(new String[] { ".COM", ".EXE", ".BAT",
                    ".CMD", ".VBS", ".VBE", ".JS", ".JSE", ".WSF", ".WSH", ".MSC" }));
        } else {
            DEFAULT_WINDOWS_EXE_EXT.addAll(Arrays.asList(exeExtensions.toUpperCase().split(";")));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#canExecute
     * (java.lang.String)
     */
    @Override
    public boolean canExecute(final String fileName) {
        for (String ext : DEFAULT_WINDOWS_EXE_EXT) {
            if (ext.toUpperCase()
                    .equals("." + FilenameUtils.getExtension(fileName).toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#
     * setWritable(java.lang.String, boolean)
     */
    @Override
    public boolean setWritable(final String fileName, final boolean writable) {
        // skip symbolic; Java changes the link's target permission not the link
        if (SymbolicLinkHelper.isSymbolicLink(fileName)) {
            return true;
        }
        return new File(fileName).setWritable(writable);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#
     * setReadable(java.lang.String, boolean, boolean)
     */
    @Override
    public boolean setReadable(final String fileName, final boolean readable,
                               final boolean ownerOnly) {
        try {
            Path path = Paths.get(fileName);
            Files.setAttribute(path, "dos:readonly", readable);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#
     * setOwnerReadOnly(java.lang.String)
     */
    @Override
    public boolean setOwnerReadOnly(final String fileName) {
        boolean set = setReadable(fileName, false, false);
        set &= setReadable(fileName, true, true);
        return set;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#
     * setExecutable(java.lang.String, boolean, boolean)
     */
    @Override
    public boolean setExecutable(final String fileName, final boolean executable,
                                 final boolean ownerOnly) {
        return new File(fileName).setExecutable(executable, ownerOnly);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#isSymlink(
     * java.lang.String)
     */
    @Override
    public boolean isSymlink(final String fileName) {
        return isSymbolicLink(fileName);
    }
}
