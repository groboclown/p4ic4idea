package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import java.io.File;

public class FilePathHelper {
    
    public static String getLocal( String root, String local ) {
        
        String path = "";
        
        // The goal here is to sensibly append 'root' and 'local' to
        // form a path.
        
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows")) {
            
            // Whose drive (x:) should we use, from root or from local?
    
            // If local has a drive, use it.
            // If local is in UNC, use no drive.
            // If root has a drive, use it.
    
            if( local.length() >= 2 && local.charAt(1) == ':' ) {
                path = local.substring(0, 2);
                local = local.substring(2);
            } else if( local.length() >= 2 && local.startsWith(File.separator + File.separator) ) {
                // Local is UNC.  Don't use root's drive.
            } else if( root.length() >= 2 && root.charAt(1) == ':' ) {
                path = root.substring(0, 2);
                root = root.substring(2);
            }
    
            // If local is rooted, root is irrelevant.
    
            if( local.startsWith("\\") || local.startsWith("/") ) {
                path += local;
                return path;
            }
    
            // Start with root.
            // Climb up root for every .. in local
    
            path += root;
            
        } else {
            if( local.charAt(0) == '/' ) {
                return local;
            } else {
                // Allow SetLocal( this, ... )

                path = root;

            }
        }
        
        for(;;) {
            if( local.endsWith(File.separator) || local.endsWith("/") ) {
                local =  local.substring(1);
            } else if( local.startsWith("..") ) {
                local = local.substring(2);
                new File( path ).getParentFile().getAbsoluteFile();
            } else if( !local.startsWith(".") ) {
                break;
            }
        }

        // Ensure local (if any) is separated from root with backslash.

        if( path.length() > 0 && !(path.endsWith(File.separator) || path.endsWith("/")) && local.length() > 0) {
            path += File.separator;
        }

        path += local;
        return path;
    }
}
    
    
