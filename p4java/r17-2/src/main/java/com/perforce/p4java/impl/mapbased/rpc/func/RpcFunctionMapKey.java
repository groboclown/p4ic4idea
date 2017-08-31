/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines Perforce RPC function keys and associated methods. Analogous in many
 * ways to the C++ API's tag definitions.
 * <p>
 * 
 * Function keys in this context are the strings used as keys in the map
 * arguments passed between Perforce server and client. We've implemented them
 * as a set of static strings on a class only because enums don't map well to
 * the use of lower case, etc., and, in any case, they're fundamentally
 * <i>strings</i>, dammit.
 * <p>
 * 
 * No attempt is made here to explain the individual usage or semantics of each
 * or any key. The order of definition below is not significant.
 * <p>
 * 
 * Note that this list can never be definitive, given that the various jobspec
 * and client (etc.) specs each define their own (free-form) key names which we
 * can't check here at all.
 */

public class RpcFunctionMapKey {

	// NOTE: the order of declaration below is not significant...

	public static final String ACCESS = "Access";
	public static final String ACTION = "action";
	public static final String ACTIONOWNER = "actionOwner";
	public static final String ALTROOTS = "AltRoots";
	public static final String APP = "app";
	public static final String ATTR_PREFIX = "attr-"; // Breaks the rules
														// here...
	public static final String ATTR_PROP_PREFIX = "attrProp-";
	public static final String ATTR_TYPE_PREFIX = "attrType-";
	public static final String BASE_FILE = "baseFile";
	public static final String BASE_REV = "baseRev";
	public static final String BASEDEPOTREC = "baseDepotRec"; // Server state
																// pass-through
	public static final String BASENAME = "baseName";
	public static final String BITS = "bits";
	public static final String BLOCKCOUNT = "blockCount";
	public static final String CADDR = "caddr";
	public static final String CHANGE = "change";
	public static final String CHARSET = "charset";
	public static final String CLIENT = "Client";
	public static final String CLIENT_FILE = "clientFile";
	public static final String CLIENTADDRESS = "clientAddress";
	public static final String CLIENTCWD = "clientCwd";
	public static final String CLIENTHOST = "clientHost";
	public static final String CLIENTNAME = "clientName";
	public static final String CLIENT_REC_DELETED = "clientRecDeleted";
	public static final String CLIENTROOT = "clientRoot";
	public static final String CLIENTSEND = "clientSend";
	public static final String CODE = "code";
	public static final String CODE0 = "code0";
	public static final String COMMIT = "commit";
	public static final String CONFIRM = "confirm";
	public static final String CONTENT_RESOLVE_TYPE = "contentResolveType";
	public static final String DADDR = "daddr";
	public static final String DATA = "data";
	public static final String DATA2 = "data2";
	public static final String DECLINE = "decline";
	public static final String DEFAULT = "default";
	public static final String DEFAULT_CHANGE = "default change";
	public static final String DEPOT_FILE = "depotFile";
	public static final String DEPOTREC = "depotRec"; // Server state
														// pass-through
	public static final String DESC = "desc";
	public static final String DESCRIPTION = "Description";
	public static final String DIFF_FLAGS = "diffFlags";
	public static final String DIGEST = "digest";
	public static final String DIR = "dir";
	public static final String END_TO_REV = "endToRev";
	public static final String ENDFROMREV = "endFromRev";
	public static final String FILE = "file";
	public static final String FILESIZE = "fileSize";
	public static final String FLUSH_HWM = "himark";
	public static final String FLUSH_RSEQ = "rseq";
	public static final String FLUSH_SEQ = "fseq";
	public static final String FMT = "fmt";
	public static final String FMT0 = "fmt0";
	public static final String FORCE = "force";
	public static final String FORCETYPE = "forceType";
	public static final String FROM_FILE = "fromFile";
	public static final String FUNC2 = "func2";
	public static final String FUNCTION = "func";
	public static final String HANDLE = "handle";
	public static final String HAVEREC = "haveRec"; // Server state pass-through
	public static final String HAVEREV = "haveRev";
	public static final String HEADACTION = "headAction";
	public static final String HEADCHANGE = "headChange";
	public static final String HEAD_CHARSET = "headCharset";
	public static final String HEADMODTIME = "headModTime";
	public static final String HEADREV = "headRev";
	public static final String HEADTIME = "headTime";
	public static final String HEADTYPE = "headType";
	public static final String HOST = "host";
	public static final String HOW = "how";
	public static final String IGNORE = "ignore";
	public static final String INTEGRATION_REC_ADDED = "integrationRecAdded";
	public static final String INTEGRATION_REC_DELETED = "integrationRecDeleted";
	public static final String INTEGREC = "integRec"; // Server state
														// pass-through
	public static final String IPADDR = "ipaddr";
	public static final String IS_GROUP = "isgroup";
	public static final String ISMAPPED = "isMapped";
	public static final String LABEL_REC_DELETED = "labelRecDeleted";
	public static final String LABEL = "label";
	public static final String LINEEND = "LineEnd";
	public static final String LOCAL_FILE = "localFile";
	public static final String MANGLE = "mangle";
	public static final String MAP_TABLE = "mapTable";
	public static final String MERGE_AUTO = "mergeAuto";
	public static final String MERGE_CONFIRM = "mergeConfirm";
	public static final String MERGE_DECLINE = "mergeDecline";
	public static final String MERGE_HOW = "mergeHow";
	public static final String MERGE_PERMS = "mergePerms";
	public static final String MOVEDFILE = "movedFile";
	public static final String NEW_PASSWORD = "newPassword";
	public static final String NEW_PASSWORD2 = "newPassword2";
	public static final String NOBASE = "noBase";
	public static final String NOCASE = "nocase";
	public static final String NOCLOBBER = "noclobber";
	public static final String NOECHO = "noecho";
	public static final String OFFSET = "offset";
	public static final String OLD_PASSWORD = "oldPassword";
	public static final String OPEN = "open";
	public static final String OPEN_ACTION = "openAction";
	public static final String OPEN_ACTION_OWNER = "openActionOwner";
	public static final String OPEN_ATTR_PREFIX = "openattr-";
	public static final String OPEN_ATTR_PROP_PREFIX = "openattrProp-";
	public static final String OPEN_ATTR_TYPE_PREFIX = "openattrType-";
	public static final String OPEN_CHANGELIST = "openChangelist";
	public static final String OPEN_TYPE = "openType";
	public static final String OPTIONS = "Options";
	public static final String OTHER_ACTION = "otherAction";
	public static final String OTHER_CHANGE = "otherChange";
	public static final String OTHERLOCK = "otherLock";
	public static final String OTHER_OPEN = "otherOpen";
	public static final String OURLOCK = "ourLock";
	public static final String OUTPUT = "output";
	public static final String OWNER = "Owner";
	public static final String PASSWORD = "password";
	public static final String PATH = "path";
	public static final String PATH2 = "path2";
	public static final String PEER = "peer";
	public static final String PERM = "perm";
	public static final String PERMS = "perms";
	public static final String PORT = "port";
	public static final String PROXYLOAD = "proxyload";
	public static final String PROXYVERBOSE = "proxyverbose";
	public static final String PURGE_FILE = "purgeFile";
	public static final String PURGE_REV = "purgeRev";
	public static final String RCVBUF = "rcvbuf";
	public static final String REOPEN = "reopen";
	public static final String REPORT_ONLY = "reportOnly";
	public static final String RERESOLVABLE = "reresolvable";
	public static final String RESOLVE_ACTION = "resolveAction";
	public static final String RESOLVE_TYPE = "resolveType";
	public static final String RESOLVED = "resolved";
	public static final String REV = "rev";
	public static final String REVERTUNCHANGED = "revertUnchanged";
	public static final String REVISION_REC_DELETED = "revisionRecDeleted";
	public static final String REVVER = "revver";
	public static final String RMDIR = "rmdir";
	public static final String ROOT = "Root";
	public static final String SCANSIZE = "scanSize";
	public static final String SERVER = "server";
	public static final String SERVER2 = "server2";
	public static final String SERVERADDRESS = "serverAddress";
	public static final String SERVERDATE = "serverDate";
	public static final String SERVERDIGEST = "serverDigest";
	public static final String SERVERID = "serverID";
	public static final String SERVERLICENSE = "serverLicense";
	public static final String SERVERROOT = "serverRoot";
	public static final String SERVERUPTIME = "serverUptime";
	public static final String SERVERVERSION = "serverVersion";
	public static final String SET = "set";
	public static final String SHELVED = "shelved";
	public static final String SHELVED_CHANGE = "shelvedChange";
	public static final String SHOWALL = "showAll";
	public static final String SKIP_IGNORE = "skipIgnore";
	public static final String SNDBUF = "sndbuf";
	public static final String SPECFORMATTED = "specFormatted";
	public static final String SSO = "sso";
	public static final String START_TO_REV = "startToRev";
	public static final String STARTFROMREV = "startFromRev";
	public static final String STATE = "state";
	public static final String STATUS = "status";
	public static final String SUBMITOPTIONS = "SubmitOptions";
	public static final String SVRNAME = "svrname";
	public static final String THEIRNAME = "theirName";
	public static final String TICKET = "ticket";
	public static final String TIME = "time";
	public static final String TO_FILE = "toFile";
	public static final String TOKEN = "token";
	public static final String TOTALFILECOUNT = "totalFileCount";
	public static final String TOTALFILESIZE = "totalFileSize";
	public static final String TRANS = "trans";
	public static final String TRAVERSE = "traverse";
	public static final String TRUNCATE = "truncate";
	public static final String TYPE = "type";
	public static final String TYPE2 = "type2";
	public static final String TZOFFSET = "tzoffset";
	public static final String UNMAP = "unmap";
	public static final String UNRESOLVED = "unresolved";
	public static final String UNSET = "unset";
	public static final String UPDATE = "Update";
	public static final String USER = "user";
	public static final String USERNAME = "userName";
	public static final String VALUE = "value";
	public static final String VIEW = "View";
	public static final String WORKREC = "workRec"; // Server state pass-through
	public static final String WORKREC2 = "workRec2"; // Server state
														// pass-through
	public static final String WORKING_REC_DELETED = "workingRecDeleted";
	public static final String WORKREV = "workRev";
	public static final String WRITE = "write";
	public static final String XFILES = "xfiles";
	public static final String YOURNAME = "yourName";

	public static final String TREE = "tree";
	public static final String PARENT = "parent";
	public static final String AUTHOR = "author";
	public static final String COMMITTER = "committer";
	public static final String GRAPH_DESC = "desc";
	public static final String SHA = "sha";

	public static final String REPO_NAME = "repoName";
	public static final String BRANCH = "branch";

	public static final String BLOB_SHA = "blob";
	public static final String COMMIT_SHA = "commit";
	public static final String REPO = "repo";

	public static final String MODE = "mode";
	public static final String NAME = "name";

	public static final String DATE = "date";

	public static final String AUTHOR_EMAIL = "authorEmail";
	public static final String COMMITTER_EMAIL = "committerEmail";
	public static final String COMMITTER_DATE = "committerDate";

	/**
	 * RPC keys map
	 */
	public static final Map<String, String> RPC_KEYS_MAP = new HashMap<String, String>();

	static {
		RPC_KEYS_MAP.put(null, null);
		RPC_KEYS_MAP.put("", "");
		RPC_KEYS_MAP.put(XFILES, XFILES);
		RPC_KEYS_MAP.put(SERVER, SERVER);
		RPC_KEYS_MAP.put(SERVER2, SERVER2);
		RPC_KEYS_MAP.put(SERVERID, SERVERID);
		RPC_KEYS_MAP.put(REVVER, REVVER);
		RPC_KEYS_MAP.put(TZOFFSET, TZOFFSET);
		RPC_KEYS_MAP.put(SNDBUF, SNDBUF);
		RPC_KEYS_MAP.put(RCVBUF, RCVBUF);
		RPC_KEYS_MAP.put(FUNCTION, FUNCTION);
		RPC_KEYS_MAP.put(FUNC2, FUNC2);
	}

	;
}
