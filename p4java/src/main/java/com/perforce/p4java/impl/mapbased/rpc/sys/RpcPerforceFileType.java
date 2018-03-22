/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.sys;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.perforce.p4java.CharsetConverter;
import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.env.SystemInfo;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientMessage.ClientMessageId;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.StringHelper;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.TextNormalizationHelper;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * Definitions for Perforce client and server file types.<p>
 *
 * Perforce defines a surprising variety of basic file types
 * and associated modifiers that determine things like how to
 * send and receive file contents between the client and the
 * server (UTF-8 vs UTF-16, compressed binary vs. uncompressed
 * binary, plain old "text", etc.) and how to interpret
 * file metadata. These types and the associated panoply of
 * methods, etc., are mostly used in the clientCheckFile,
 * clientOpen, clientClose, clientWrite, etc. methods on the
 * various client function classes.<p>
 *
 * A file's type is stored in the server for all known files,
 * (see e.g. "p4 help typemap" and "p4 help filetypes"),
 * and in most cases we simply accept what we're given if we
 * can cope with that type (there are some types we don't
 * process at all here -- see below). What sort of file types
 * a server (as opposed to the client) knows about and can
 * process depends on the server's xlevel protocol variable:
 * <pre>
 * 	- xfiles unset: return text, binary.
 * 	- xfiles >= 0: also return xtext, xbinary.
 * 	- xfiles >= 1: also return symlink.
 * 	- xfiles >= 2; also return resource (mac resource file).
 * 	- xfiles >= 3; also return ubinary
 * 	- xfiles >= 4; also return apple
 * </pre>
 * In general, the client has to honour the server's xlevel
 * capabilities, so the client may have to do a bit of work
 * to get things right here.
 *
 * Unfortunately, in some cases it's very difficult to know
 * what Perforce type a file should be, and there's a bunch
 * of digging around that must be done to intuit the proper
 * type for files the server doesn't (yet) know about.<p>
 *
 * Also somewhat unfortunately, the file type is encoded
 * quite differently depending on whether it's coming from
 * the server (usually encoded as a string representation of
 * hex numbers) or going to the server (where it's usually
 * done as plain old "text" or "ubinary", etc.).
 * 
 *
 */
// See p4/client/clientservice.cc
public enum RpcPerforceFileType {
	
    // Basic file types:
	
    FST_TEXT,        // file is text
    FST_BINARY,        // file is binary
    FST_GZIP,        // file is gzip
    FST_DIRECTORY,    // file is a directory
    FST_SYMLINK,    // it's a symlink
    FST_RESOURCE,    // Macintosh resource file
    FST_SPECIAL,    // not a regular file
    FST_MISSING,    // no file at all
    FST_CANTTELL,    // can read file to find out
    FST_EMPTY,        // file is empty
    FST_UNICODE,    // file is unicode (utf-8?)
    FST_GUNZIP,        // stream is gzip
    FST_UTF8,        // stream is utf8
    FST_UTF16,        // stream is utf8 convert to utf16

    // Derived file types (i.e. modified basic types):
    // (forbidden types are given below for completeness;
    // these are usually weeded out or detected elsewhere)

    FST_ATEXT,        // append-only text
    FST_XTEXT,        // executable text
    FST_RTEXT,        // raw text
    FST_RXTEXT,        // executable raw text
    FST_CBINARY,    // pre-compressed binary
    FST_XBINARY,    // executable binary
    FST_XSYMLINK,    // forbidden -- not used
    FST_XRESOURCE,    // ditto
    FST_APPLETEXT,    // apple format text
    FST_APPLEFILE,    // apple format binary
    FST_XAPPLEFILE,    // executable apple format binary
    FST_XAPPLETEXT,    // forbidden
    FST_XUNICODE,    // executable unicode text
    FST_XRTEXT,        // executable raw text (also forbidden)
    FST_XUTF8,        // executable utf8
    FST_XUTF16,        // executable utf8 convert to utf16
    FST_XGUNZIP,    // executable in gkzip form
    FST_RCS            // RCS temporary file: raw text, sync on close
    ;

    public static final String TRACE_PREFIX = "RpcPerforceFileType";
	private static final ISystemFileCommandsHelper fileCommands
							= SysFileHelperBridge.getSysFileCommands();

	private enum CtAction { 
        OK,    // use forceType/the discovered type
        ASS,    // missing/unreadable/empty: assume it is forceType/text
        CHKSZ,  // size is to big for default, use alternate
        SUBST,    // server can't handle it: substitute altType
        CANT    // just can't be added
    }

    public static class RpcServerTypeStringSpec {

        private String serverTypeString = null;
        private RpcMessage error = null;

		public RpcServerTypeStringSpec(String str, RpcMessage error) {
            this.serverTypeString = str;
            this.error = error;
        }

        public String getServerTypeString() {
            return serverTypeString;
        }

        public RpcMessage getMsg() {
            return error;
        }
    }

    private static class ActionTableElement {
		public RpcPerforceFileType checkType = null;
		public int xlevel = 0;
		public CtAction[] ctActions = new CtAction[2];
		public String type = null;
		public String altType = null;
        public String cmpType = null;

        ActionTableElement(RpcPerforceFileType checkType,
                           int xlevel,
                           CtAction ctActions0,
                           CtAction ctActions1,
                           String type,
                           String altType,
                           String cmpType) {

            this.checkType = checkType;
            this.xlevel = xlevel;
            this.ctActions[0] = ctActions0; // action to take if element's xlevel > server's xlevel
            this.ctActions[1] = ctActions1; // action to take if element's xlevel <= server's xlevel
            this.type = type;
            this.altType = altType;
            this.cmpType = cmpType;
        }

        ActionTableElement(RpcPerforceFileType checkType,
                           int xlevel,
                           CtAction ctActions0,
                           CtAction ctActions1,
                           String type,
                           String altType) {

            this(checkType, xlevel, ctActions0, ctActions1, type, altType, null);
        }
    }

    // Symbolic link capable?
	private static CtAction symlinkAction = SymbolicLinkHelper.isSymbolicLinkCapable() ? CtAction.OK : CtAction.CANT;

    // See p4/client/clientservice.cc
    private static final ActionTableElement[] actionTable = {
            new ActionTableElement(FST_TEXT, 		0, CtAction.OK,    CtAction.CHKSZ, "text",      "text",    "ctext"),
            new ActionTableElement(FST_XTEXT, 		0, CtAction.SUBST, CtAction.CHKSZ, "xtext",     "text",    "text+Cx"),
            new ActionTableElement(FST_BINARY, 		0, CtAction.OK,    CtAction.OK,    "binary",    "binary"),
            new ActionTableElement(FST_XBINARY, 	0, CtAction.SUBST, CtAction.OK,    "xbinary",   "binary"),
            new ActionTableElement(FST_APPLEFILE, 	4, CtAction.SUBST, CtAction.OK,    "apple",     "binary"),
            new ActionTableElement(FST_XAPPLEFILE, 	4, CtAction.SUBST, CtAction.OK,    "apple+x",   "binary"),
            new ActionTableElement(FST_CBINARY, 	3, CtAction.SUBST, CtAction.OK,    "ubinary",   "binary"),
            new ActionTableElement(FST_SYMLINK, 	1, CtAction.CANT,  symlinkAction,  "symlink",    null),
            new ActionTableElement(FST_RESOURCE, 	2, CtAction.CANT,  CtAction.OK,    "resource",   null),
            new ActionTableElement(FST_SPECIAL,    -1, CtAction.CANT,  CtAction.CANT,  "special",    null),
            new ActionTableElement(FST_DIRECTORY,  -1, CtAction.CANT,  CtAction.CANT,  "directory",  null),
            new ActionTableElement(FST_MISSING,    -1, CtAction.ASS,   CtAction.ASS,   "missing",    "text"),
            new ActionTableElement(FST_CANTTELL,   -1, CtAction.ASS,   CtAction.ASS,   "unreadable", "text"),
            new ActionTableElement(FST_EMPTY,      -1, CtAction.ASS,   CtAction.ASS,   "empty",      "text"),
            new ActionTableElement(FST_UNICODE, 	5, CtAction.SUBST, CtAction.CHKSZ, "unicode",    "text",   "unicode+C"),
            new ActionTableElement(FST_XUNICODE, 	5, CtAction.SUBST, CtAction.CHKSZ, "xunicode",   "text",   "xunicode+C"),
            new ActionTableElement(FST_UTF16, 		6, CtAction.SUBST, CtAction.CHKSZ, "utf16",      "binary", "utf16+C"),
            new ActionTableElement(FST_XUTF16, 		6, CtAction.SUBST, CtAction.CHKSZ, "xutf16",     "binary", "xutf16+C"),
            new ActionTableElement(FST_UTF8, 		7, CtAction.SUBST, CtAction.CHKSZ, "utf8",       "text",   "utf8+C"),
            new ActionTableElement(FST_XUTF8, 		7, CtAction.SUBST, CtAction.CHKSZ, "xutf8",      "text",   "xutf8+C")
    };

	private static final byte[] pdfMagic = { '%', 'P', 'D', 'F', '-' };

	private static final byte[][] cBinaryMagicTable = {
            {'G', 'I', 'F'},    // GIF
            {(byte) 0377, (byte) 0330, (byte) 0377, (byte) 0340},    // JPEG
            {(byte) 0377, (byte) 0330, (byte) 0377, (byte) 0341},    // EXIF
            {(byte) 037, (byte) 0213},    // GZIP
            {(byte) 0377, (byte) 037},    // compa (?)
            {(byte) 037, (byte) 0235},    // compr (?)
            { 'P', 'K', (byte) 003, (byte) 004 },	// normal PKZIP, including JAR, WAR, etc.
            {'P', 'K', (byte) 005, (byte) 006},		// empty PKZIP, including JAR, WAR, etc.
            {(byte) 0211, 'P', 'N', 'G'},    // PNG
            {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE},    // Java class file, natch...
    };

    /**
     * Decode the file type from the string sent by the server. This
     * is (usually) a three-character hex encoding, e.g. "101" or
     * "01D".
     */
	
    public static RpcPerforceFileType decodeFromServerString(String str) {
		
		if (str == null) {
            return FST_TEXT;
        }

		// Copied wholesale from the C++ API...

        // fileType [ lineType [ uncompress ] ]

        int tf = 0;
		@SuppressWarnings("unused") // See ClientLineEnding.decodeFromServerString()
		int tl = 0;
        int tu = 0;

        switch (str.length()) {
            default:
			case 3:	tu = StringHelper.hexcharToInt(str.charAt(2));
			case 2:	tl = StringHelper.hexcharToInt(str.charAt(1));
			case 1:	tf = StringHelper.hexcharToInt(str.charAt(0));
            case 0:    // nothing??? ;
        }

        // Map '[ uncompress ] fileType' into FileSysType.

		switch (( tu << 8 ) | tf)
		{
            // Normal.

			case 0x000: return FST_TEXT; 
			case 0x001: return FST_BINARY; 
			case 0x002: return FST_XTEXT;
			case 0x003: return FST_XBINARY; 
			case 0x004: return FST_SYMLINK;
			case 0x005: return FST_RESOURCE; 
			case 0x006: return FST_XSYMLINK;
			case 0x007: return FST_XRESOURCE;
			case 0x008: return FST_UNICODE;
			case 0x009: return FST_RTEXT;
			case 0x00A: return FST_XUNICODE;
			case 0x00B: return FST_XRTEXT;
			case 0x00C: return FST_APPLETEXT;
			case 0x00D: return FST_APPLEFILE;
			case 0x00E: return FST_XAPPLETEXT;
			case 0x00F: return FST_XAPPLEFILE;
			case 0x014: return FST_UTF8;
			case 0x016: return FST_XUTF8;
			case 0x018: return FST_UTF16;
			case 0x01A: return FST_XUTF16;

            // Uncompressing.

			case 0x101: return FST_GUNZIP;
			case 0x103: return FST_XGUNZIP;

            // Stop-gap.

			default:   return FST_BINARY;
        }
    }

    /**
     * Checking for executable file types - excluding "forbidden" types.
     */
    public boolean isExecutable() {
        switch (this) {
            case FST_XTEXT:
            case FST_XRTEXT:
            case FST_XAPPLEFILE:
            case FST_XBINARY:
            case FST_XUNICODE:
            case FST_XUTF8:
            case FST_XUTF16:
            case FST_XGUNZIP:
                return true;
            default:
                return false;
        }
    }

    /**
     * Infer (or even intuit) the Perforce file type of the passed-in
     * Perforce file. This is an arbitrarily complex operation, and may
     * involve reading in the first few bytes of a file to see what the
     * contents say about the type.<p>
     *
     * Note that Java does not allow us to directly access most file
     * metadata. This probably doesn't matter in most cases, but we do need
     * to keep an eye on this -- HR.
     */

	public static RpcPerforceFileType inferFileType(File file, int scanSize, boolean isUnicodeServer, Charset clientCharset) {

		if (file == null) {
			throw new NullPointerError(
				"Null file handle passed to RpcPerforceFileType.inferFileType()");
		}

        try {
            if (isProbablySymLink(file)) {
                // Better safe than sorry -- users can always override this explicitly...
                return FST_SYMLINK;
            }

            if (!file.exists()) {
                return FST_MISSING;
            }

            if (file.isDirectory()) {
                return FST_DIRECTORY;
            }

            if (!file.isFile()) {
                // Hmmm. This might be a symlink, but we'll just return
                // FST_CANTTELL for the moment... (FIXME -- HR).
                return FST_CANTTELL;
            }

            if (file.length() == 0) {
                return FST_EMPTY;
            }

            // Otherwise, we have to look inside it:
            return inferFileTypeFromContents(file,
                    fileCommands.canExecute(file.getPath()),
                    scanSize,
                    isUnicodeServer,
                    clientCharset);
        } catch (Exception exc) {
            // We can probably do better than this in the long term,
            // but at the moment this is the safest choice...
            Log.exception(exc);
            return FST_CANTTELL;
        }
    }

    /**
     * Given a Perforce file type and the Perforce server's xfiles level (from the protocol
     * parameters), determine what server file type to send to the server to represent
     * the passed-in file type as a string, and / or what error or info message
     * to send to the user.<p>
     */

    public static RpcServerTypeStringSpec getServerFileTypeString(
            String clientPath,
            boolean overSize,
            RpcPerforceFileType fileType,
            String forceType,
            int xlevel) {
        if (fileType != null) {
            for (ActionTableElement atElement : actionTable) {
                if (atElement.checkType == fileType) {
                    return getAction(clientPath, overSize, xlevel, atElement, forceType);
                }
            }
        }

        Log.error("Encountered null or unknown filetype in getServerFileTypeString()");
        return new RpcServerTypeStringSpec(null,
                new RpcMessage(
                        ClientMessageId.CANT_ADD_FILE_TYPE,
                        MessageSeverityCode.E_INFO,
                        MessageGenericCode.EV_CLIENT,
                        new String[]{clientPath, "unknown"}
                ));
    }

    /**
     * Return true if there's some reason to believe this file is a
     * Unix or Linux symbolic link. This is just a hack that's here
     * until I can do something better with native code...
     */

    public static boolean isProbablySymLink(File file) {

        if (file != null) {
            // Check with the symbolic link helper class (JDK 7 or above)
            if (SymbolicLinkHelper.isSymbolicLinkCapable()) {
                return SymbolicLinkHelper.isSymbolicLink(file.getPath());
            }

            // Check with the file helper class...
            ISystemFileCommandsHelper helper = SysFileHelperBridge.getSysFileCommands();
            if ((helper != null) && helper.isSymlink(file.getPath())) {
                return true;
            }

            // Note that if the file path contains high ascii characters and the
            // JDK (version 5 or below) is not capable of text normalization,
            // then the following logic might give false positives.
            // In such case, you should override the file type explicitly
            // (i.e. p4 add -t filetype).

            // On Unix variants symlinks often have different absolute and
            // canonical paths -- and that's all we have to go on if the helper
            // class doesn't help...
            if (!Server.isRunningOnWindows()) {
                try {
                    String absoPath = file.getAbsolutePath();
                    String canoPath = file.getCanonicalPath();

                    // Normalize the canonical path (JDK 6 or above)
                    if (TextNormalizationHelper.isNormalizationCapable()) {
                        canoPath = TextNormalizationHelper.normalize(file.getCanonicalPath());
                    }

                    // For historical compatibility reasons, Mac file systems
                    // defaults to case-insensitive directories and files,
                    // so, we must ignore case here...
                    if (SystemInfo.isMac()) {
                        if (!absoPath.equalsIgnoreCase(canoPath)) {
                            return true; // wish this were always true...
                        }
                    } else {
                        if (!absoPath.equals(canoPath)) {
                            return true; // wish this were always true...
                        }
                    }

                } catch (IOException ioexc) {
                    Log.warn("unexpected exception in RpcPerforceFileType.isProbablySymLink(): %s",
                            ioexc.getLocalizedMessage());
                    Log.exception(ioexc);
                }
            }
        }

        return false;
    }

    private static RpcServerTypeStringSpec getAction(
            String clientPath,
            boolean overSize,
            int xlevel,
            ActionTableElement atElement,
            String forceType) {

        switch (atElement.ctActions[(xlevel >= atElement.xlevel ? 1 : 0)]) {
            case OK:
                if (forceType != null) {
                    return new RpcServerTypeStringSpec(forceType, null);
                } else {
                    return new RpcServerTypeStringSpec(atElement.type, null);
                }

            case CHKSZ:
                // If server sends a maximum size for file, check it
                if (forceType != null) {
                    return new RpcServerTypeStringSpec(forceType, null);
     		} else if (overSize) {
                    return new RpcServerTypeStringSpec(atElement.cmpType, null);
                } else {
                    return new RpcServerTypeStringSpec(atElement.type, null);
                }
            case ASS:
                if (forceType != null) {
                    return new RpcServerTypeStringSpec(forceType,
                            new RpcMessage(
                                    ClientMessageId.ASSUMING_FILE_TYPE,
                                    MessageSeverityCode.E_INFO,
                                    MessageGenericCode.EV_CLIENT,
                                    new String[]{clientPath,
                                            atElement.type,
                                            forceType
                                    }
                            ));
                } else {
                    return new RpcServerTypeStringSpec(atElement.altType,
                            new RpcMessage(
                                    ClientMessageId.ASSUMING_FILE_TYPE,
                                    MessageSeverityCode.E_INFO,
                                    MessageGenericCode.EV_CLIENT,
                                    new String[]{clientPath,
                                            atElement.type,
                                            atElement.altType
                                    }
                            ));
                }

            case SUBST:
                return new RpcServerTypeStringSpec(atElement.altType,
                        new RpcMessage(
                                ClientMessageId.SUBSTITUTING_FILE_TYPE,
                                MessageSeverityCode.E_INFO,
                                MessageGenericCode.EV_CLIENT,
                                new String[]{clientPath,
                                        atElement.altType,
                                        atElement.type,
                                }
                        ));

            case CANT:
                return new RpcServerTypeStringSpec(null,
                        new RpcMessage(
                                ClientMessageId.CANT_ADD_FILE_TYPE,
                                MessageSeverityCode.E_INFO,
                                MessageGenericCode.EV_CLIENT,
                                new String[]{clientPath,
                                        atElement.type
                                }
                        ));
        }

        return new RpcServerTypeStringSpec(null,
                new RpcMessage(
                        ClientMessageId.CANT_ADD_FILE_TYPE,
                        MessageSeverityCode.E_INFO,
                        MessageGenericCode.EV_CLIENT,
                        new String[]{clientPath,
                                atElement.type
                        }
                ));
    }

    private static RpcPerforceFileType getBinaryType(byte[] buffer, int bytesRead, boolean isExecutable) {
	    if (isKnownCBinary(buffer, bytesRead)) {
			return FST_CBINARY;
		}
		return (isExecutable ? FST_XBINARY : FST_BINARY);
    }
    
    
    /**
     * Need to look inside the file to work out from its contents what
     * type it (probably) is. This involves looking for magic numbers,
     * etc.<p>
     *
     * Much of the binary inferencing logic here is adapted fairly loosely
     * from the C++ API equivalent and may share the same errors (or lack of
     * them) that that code has.<p>
     */

    private static RpcPerforceFileType inferFileTypeFromContents(File file,
                                                                 boolean isExecutable,
                                                                 int scanSize,
                                                                 boolean isUnicodeServer,
                                                                 Charset clientCharset) {

		byte[] buffer;
		if (scanSize < 0) {
			buffer = new byte[RpcPropertyDefs.RPC_DEFAULT_FILETYPE_PEEK_SIZE];
		} else {
			buffer = new byte[scanSize];
		}
		int bytesRead = 0;
		try (FileInputStream inStream = new FileInputStream(file)) {
			if ((bytesRead = inStream.read(buffer)) < 0) {
				return FST_CANTTELL;
			}
		} catch (IOException ioexc) {
			Log.warn("Unexpected exception: %s", ioexc.getMessage());
			Log.exception(ioexc);
			return FST_CANTTELL;
		}

		if (bytesRead == 0) {
			return FST_EMPTY;
		}
		
		if(!isUnicodeServer) {
			clientCharset = null;
		}
		
		
		// Look for binary chars.

		boolean highbit = false;
		boolean controlchar = false;
		boolean zero = false;

		for (int i = 0; i < bytesRead; i++)
		{
		    highbit |= (0x80 & buffer[i]) != 0;
		    zero |= buffer[i] == 0;
		    controlchar |= (0x80 & buffer[i]) == 0 && Character.isISOControl( buffer[i] ) && !Character.isWhitespace( buffer[i] );
		}
		
		// Is it a PDF?
		if (isPDF(buffer, bytesRead)) {
			return (isExecutable ? FST_XBINARY : FST_BINARY);
		}

	    RpcPerforceFileType rettype = (isExecutable ? FST_XTEXT : FST_TEXT);
		Bom bom = readBom(buffer, bytesRead);
		boolean utf8Bom = bom != null && bom == Bom.UTF8;
		boolean utf16Bom = bom != null && bom == Bom.UTF16;
		boolean utf32Bom = bom != null && bom == Bom.UTF32;
		
		if (utf8Bom) {
			if (controlchar) {
			    return getBinaryType(buffer, bytesRead, isExecutable);
			}

			if( testValidUtf8( buffer, bytesRead ) > 0 ) {
			    rettype = (isExecutable ? FST_XUTF8 : FST_UTF8);
			}
	    }
		
	    // is there an UTF16 BOM at the start
		// second word of zero means UTF-32: readBom handles that for us!
	    if( utf16Bom ) {
			// might be utf16...
			rettype = (isExecutable ? FST_XUTF16 : FST_UTF16);
			clientCharset = Charset.forName("UTF-16");
		    if (!checkConvert( buffer, bytesRead, clientCharset, true )) {
				return getBinaryType(buffer, bytesRead, isExecutable);
			}
	    	return rettype;
	    }

	    String charsetName = "none";
	    if(clientCharset != null) {
	    	charsetName = clientCharset.name();
	    }

	    switch(charsetName) {
		    case "UTF-8":
		    	if (controlchar) {
				    return getBinaryType(buffer, bytesRead, isExecutable);
				}
				if (highbit && (rettype == FST_TEXT || rettype == FST_XTEXT)) {
				    // run special UTF_8 validator...
				    if (testValidUtf8(buffer, bytesRead) > 0) {
				    	rettype = (isExecutable ? FST_XUNICODE : FST_UNICODE);
				    }
				}
				// else?  should we do unicode if no highbit?
				break;
	
		    case "UTF-32":
		    case "UTF-32LE":
		    case "UTF-32BE":
				if ((!zero && !highbit && !controlchar) || utf8Bom ) {
				    break;
				}
	    		rettype = (isExecutable ? FST_XUNICODE : FST_UNICODE);
				// is there a BOM at the start
				if( utf32Bom ) {
				    break;
				}
				// is there a UTF16 BOM at start... consider binary...
				if( utf16Bom ) {
				    return getBinaryType(buffer, bytesRead, isExecutable);
				}
			    if (!checkConvert( buffer, bytesRead, clientCharset, true )) {
					return getBinaryType(buffer, bytesRead, isExecutable);
				}
				break;
	
		    case "UTF-16":
		    case "UTF-16LE":
		    case "UTF-16BE":
				if ((!zero && !highbit && !controlchar) || utf8Bom) {
				    break;
				}
				rettype = (isExecutable ? FST_XUNICODE : FST_UNICODE);
				// is there a BOM at the start
				if( utf16Bom ) {
				    break;
				}
				// is there a UTF32 BOM at start... consider binary...
				if( utf32Bom ) {
				    return getBinaryType(buffer, bytesRead, isExecutable);
				}
			    if (!checkConvert( buffer, bytesRead, clientCharset, true )) {
					return getBinaryType(buffer, bytesRead, isExecutable);
				}
			    break;
	
		    case "none":
		    	// non-unicode mode goes here...
		    	if (controlchar) {
				    return getBinaryType(buffer, bytesRead, isExecutable);
				}
				break;
	
	    	default:
	    		// most 8-bit charsets go here...
	    		if (controlchar) {
				    return getBinaryType(buffer, bytesRead, isExecutable);
				}
				if (utf8Bom) {
				    break;
				}
				if( highbit && checkConvert( buffer, bytesRead, clientCharset, false ) ) {
					rettype = (isExecutable ? FST_XUNICODE : FST_UNICODE);
				}
	    }

	    if (rettype == FST_UNICODE) {
    		// Is the Unicode just plain UTF8?
	    	if (highbit) {
			    if (testValidUtf8(buffer, bytesRead) > 0) {
			    	rettype = (isExecutable ? FST_XUTF8 : FST_UTF8);
			    }
			}
	    	// Option to disable unicode detection...
			/*if( rettype == FST_UNICODE &&
				!p4tunable.Get( P4TUNE_FILESYS_DETECTUNICODE ) ) {
			    rettype = FST_TEXT;
		    }*/
		}

	    return rettype;
    }

    
    private static boolean checkConvert(byte[] buffer, int bytesRead, Charset clientCharset, boolean checkSpaces) {
    	
    	byte[] bytes = null;
    	
    	try {
	    	CharsetConverter converter = new CharsetConverter(clientCharset, CharsetDefs.UTF8);
	    	ByteBuffer from = ByteBuffer.wrap(buffer, 0, bytesRead);
	    	ByteBuffer to = converter.convert(from);
	    	bytes = to.array();
    	} catch (Throwable e) {
			// XXX: we expect some thrown exceptions here, because we're testing that this works.
    		return false;
		}
    	
	    if(!checkSpaces) {
	    	return true;
	    }
	    
	    
	    // it did convert... see if it looks like utf8 text...
	    // we think it is text if there are more than
	    // 1 space character every 40 characters or so...
	    
		try {
			String str = new String(bytes, "UTF-8");
		    int cnt = 0, ccnt = 0;
		    
		    for( ; ccnt < str.length(); ccnt++ ) {
				if( Character.isWhitespace( str.charAt(ccnt) ) ) {
				    ++cnt;
				}
		    }
		    if( 40 * cnt < ccnt ) {
		    	 return false;
		    }
		} catch (UnsupportedEncodingException e) {
	    	 return false;
		}
		
		return true;
	}


	/*
     * What do these bits mean?
     *
     * 0x40	Valid single byte code
     * 0x80 Part of a multi byte sequence
     * 0x08 UTF-16 surrogate
     * 0x07 count of following bytes
     */

    static char[] validUtf8map = {
    // 0 - 0x7f
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
        0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40, 0x40,
    // 0x80 - 0x8f
        0xb0, 0xb0, 0xb0, 0xb0, 0xb0, 0xb0, 0xb0, 0xb0,
        0xb0, 0xb0, 0xb0, 0xb0, 0xb0, 0xb0, 0xb0, 0xb0,
    // 0x90 - 0x9f
        0x90, 0x90, 0x90, 0x90, 0x90, 0x90, 0x90, 0x90,
        0x90, 0x90, 0x90, 0x90, 0x90, 0x90, 0x90, 0x90,
    // 0xa0 - 0xbf
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80,
    // 0xc0, 0xc1 illegal
        0, 0,
    // 0xc2 - 0xdf
                    0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
        0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
        0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
        0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
    // 0xe0 - 0xef  ( 0xe0 and 0xed are magical )
        0x72, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42,
        0x42, 0x42, 0x42, 0x42, 0x42, 0x4a, 0x42, 0x42,
    // 0xf0 - 0xf4 ( 0xf0 and 0xf4 are magical )
        0x63, 0x43, 0x43, 0x43, 0x53,
    // 0xf5 - 0xff illegal
                                      0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0
    };

    /*
     * return values are...
     *
     * 0 not valid
     * 1 valid
     * 3 valid so far (following bytes needed to complete a multi-byte char)
     */
    
	private static int testValidUtf8(byte[] buffer, int bytesRead) {

		int followcnt = 0;
		int magic = 0;

		for (int i = 0; i < bytesRead; i++ ) {
			int chflags = validUtf8map[0xff & buffer[i]];

			if (followcnt != 0) {
				if ((chflags & 0x80) != 0x80) {
					return 0;
				}
				--followcnt;
				if (magic != 0) {
					switch (magic) {
					case 0x10: // lead is 0xf4
						if ((chflags & 0x20) != 0x20) {
							return 0;
						}
						break;
					case 0x20: // lead is 0xf0
						if ((chflags & 0x20) == 0x20) {
							return 0;
						}
						break;
					case 0x30: // lead is 0xe0
						if ((chflags & 0x10) == 0x10) {
							return 0;
						}
						break;
					case 0x08: // lead is 0xed (UTF-16 surrogates)
						if ((chflags & 0x30) == 0x00) {
							return 0;
						}
						break;
					}
					magic = 0;
				}
			} else {
				if ((chflags & 0x40) != 0x40) {
					return 0;
				}
				followcnt = chflags & 0x7;
				magic = chflags & 0x38;
			}
		}
		if (followcnt != 0) {
			return 3;
		}
		return 1;
	}

	enum Bom {
		UTF8,
		UTF16,
		UTF32
	}
	
	/**
	 * Return file type IFF the contents seem to be Unicode-encoded.
	 */
	private static Bom readBom(byte[] bytes, int bytesRead) {
			// First check for Unicode BOMs (see e.g. http://unicode.org/faq/utf_bom.html):
			if (       (bytesRead >= 4) && (bytes[0] == (byte) 0xFE) && (bytes[1] == (byte) 0xFF) && (bytes[2] == (byte) 0x00) && (bytes[3] == (byte) 0x00)) {
				return Bom.UTF32; // UTF-32-LE
			} else if ((bytesRead >= 4) && (bytes[0] == (byte) 0x00) && (bytes[1] == (byte) 0x00) && (bytes[2] == (byte) 0xFE) && (bytes[3] == (byte) 0xFF)) {
				return Bom.UTF32; // UTF-32-BE
			} else if ((bytesRead >= 3) && (bytes[0] == (byte) 0xEF) && (bytes[1] == (byte) 0xBB) && (bytes[2] == (byte) 0xBF)) {
				return Bom.UTF8; // UTF-8
			} else if ((bytesRead >= 2) && (bytes[0] == (byte) 0xFF) && (bytes[1] == (byte) 0xFE)) {
				return Bom.UTF16; // UTF-16-LE
			} else if ((bytesRead >= 2) && (bytes[0] == (byte) 0xFE) && (bytes[1] == (byte) 0xFF)) {
				return Bom.UTF16; // UTF-16-BE
			}
			return null;
	}

	/**
	 * Return true IFF the contents seem to be a PDF.
	 */
	
	private static boolean isPDF(byte[] bytes, int bytesRead) {
		if (bytesRead > pdfMagic.length) {
			int i = 0;

			for (byte b : pdfMagic) {
				if (b != bytes[i++]) {
					return false;
				}
			}

			return true;
		}

		return false;
    }

    /**
	 * Return true if the passed-in bytes look like ascii text,
	 * i.e. no high bits set, no control characters less than 0x07,
	 * not a PDF, etc.... Not entirely reliable, but Good Enough.<p>
	 * 
	 * NOTE: assumes the PDFs have been weeded out alread...
     */
	
//	private static boolean isAsciiText(byte[] bytes, int bytesRead) {
//
//		if (bytes == null) {
//			return false; // Which may be wrong, but we have no reliable way of
//							// knowing...
//		}
//
//		for (int i = 0; i < bytesRead; i++) {
//			if (bytes[i] < 7) {
//				// Relies on signed promotion here...
//				return false;
//			}
//		}
//
//		return true;
//    }

    /**
     * Return true iff the file appears to start with some known
     * magic numbers that we interpret to mean the file is
     * pre-compressed binary (FST_CBINARY). Typically things
     * like JPEGs and GIFs, etc.
     */
	
	public static boolean isKnownCBinary(byte[] bytes, int bytesRead) {
		
        if (bytesRead > 0) {
			for (byte[] magicBytes : cBinaryMagicTable) {
                if (bytesRead > magicBytes.length) {
                    int i = 0;
                    for (byte b : magicBytes) {
                        if (bytes[i] != b) {
                            break;
                        }
                        i++;
                    }

                    if (i == magicBytes.length) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
	
	public static byte[][] getBinaryMagicNumberTable() {
		return cBinaryMagicTable;
	}
}
