/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.core.file;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseInt;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.ACTIONOWNER;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.ATTR_PREFIX;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.ATTR_PROP_PREFIX;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.ATTR_TYPE_PREFIX;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CHARSET;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CLIENT;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEFAULT;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DESC;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DIGEST;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FILESIZE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HAVEREV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HEADACTION;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HEADCHANGE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HEADMODTIME;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HEADREV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HEADTIME;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HEADTYPE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HEAD_CHARSET;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.ISMAPPED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.MOVEDFILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OPEN_ACTION;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OPEN_ACTION_OWNER;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OPEN_ATTR_PREFIX;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OPEN_ATTR_PROP_PREFIX;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OPEN_ATTR_TYPE_PREFIX;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OPEN_CHANGELIST;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OPEN_TYPE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OTHERLOCK;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OTHER_ACTION;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OTHER_CHANGE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OTHER_OPEN;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.RERESOLVABLE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.RESOLVED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.RESOLVE_ACTION;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.SHELVED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.STATUS;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.UNRESOLVED;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IResolveRecord;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.server.IServer;

// p4ic4idea: allow for construction from a server message
import com.perforce.p4java.server.IServerMessage;

/**
 * Useful generic implementation class for the IExtendedFileSpec interface.
 * Fields below generally correspond exactly with the similarly-named Perforce
 * fstat call fields, and will not be documented in detail here.
 */
public class ExtendedFileSpec extends FileSpec implements IExtendedFileSpec {

    private boolean mapped = false; // set if mapped client file is synced
    private FileAction headAction = null; // action at head rev, if in depot
    private int headChange = 0; // head rev changelist#, if in depot
    private int headRev = 0; // head rev #, if in depot
    private String headType = null; // head rev type, if in depot
    private Date headTime = null; // head rev changelist time, if in depot
    private Date headModTime = null; // head rev mod time, if in depot
    private String headCharset = null; // head rev charset, if in depot
    private int haveRev = 0; // rev had on client, if on client
    private String desc = null; // change description
    private String digest = null; // MD5 digest (fingerprint)
    private long fileSize = 0; // file size
    private FileAction openAction = null; // open action, if opened
    private String openType = null; // open type, if opened
    private String openActionOwner = null; // user who opened file, if opened
    private int openChangelistId = 0; // open changelist#, if opened
    private boolean unresolved = false; // true if needs resolution
    private boolean resolved = false; // true if has been resolved
    private boolean reresolvable = false; // true if it is reresolvable
    private boolean otherLocked = false; // true if locked by another client
    private List<String> otherActionList = new ArrayList<>(); // list of other
                                                              // actions on this
                                                              // file
    private List<String> otherChangelist = new ArrayList<>(); // list of other
                                                              // change lists
                                                              // for this file
    private List<String> otherOpenList = new ArrayList<>(); // list of other
                                                            // users with file
                                                            // open
    private String actionOwner = null; // owner of the open action
    private String charset = null; // charset for this file revision
    private boolean shelved = false;
    private List<IResolveRecord> resolveRecords = new ArrayList<>();
    private String movedFile = null;
    private Map<String, byte[]> attributes = new HashMap<>(); // Leave it null
                                                              // until needed...
    private Map<String, byte[]> propagatingAttributes = new HashMap<>(); // Leave
                                                                         // it
                                                                         // null
                                                                         // until
                                                                         // needed...
    private Map<String, byte[]> attributeTypes = new HashMap<>(); // Leave it
                                                                  // null until
                                                                  // needed...
    private String verifyStatus = null;

    /**
     * Default constructor. Sets all boolean fields to false, object fields to
     * null, integers to zero.
     */
    public ExtendedFileSpec() {
        super();
    }

    /**
     * Given a candidate path string (which may include version and changelist
     * annotations, at least), try to construct a corresponding extended file
     * spec.
     * <p>
     *
     * See the corresponding FileSpec constructor for details -- this
     * constructor does not add any ExtendedFileSpec-specific semantics.
     *
     * @param pathStr
     *            candidate path string
     */
    public ExtendedFileSpec(String pathStr) {
        super(pathStr);
    }

    /**
     * Construct an ExtendedFileSpec object from a status, message string pair.
     * See the corresponding FileSpec constructor for details -- this
     * constructor does not add any ExtendedFileSpec-specific semantics.
     *
     * @param status
     *            FileSpecOpStatus status.
     * @param errStr
     *            error / info message string.
     */
    // p4ic4idea: use the IServerMessage instead of a String
    public ExtendedFileSpec(FileSpecOpStatus status, IServerMessage errStr) {
        super(status, errStr);
    }

    /**
     * Construct an ExtendedFileSpec object from the passed-in map. The map must
     * be (or have the same keys and semantics as) a map as returned from a
     * suitable Perforce server call; the semantics and format of this map are
     * not spelled out here.
     *
     * @param map
     *            suitable field map from Perforce server; if null, this
     *            constructor has the same semantics as the default constructor.
     * @param server
     *            non-null server object
     * @param index
     *            passed to the superclass FileSpec(Map<String, Object> map,
     *            IServer server, int index) constructor but not otherwise used
     *            here.
     */
    public ExtendedFileSpec(final Map<String, Object> map, final IServer server, final int index) {

        super(map, server, index);
        if (nonNull(map)) {
            try {
                setClient(null);
                setMapped(nonNull(map.get(ISMAPPED)));
                setHeadAction(FileAction.fromString(parseString(map, HEADACTION)));
                String cList = parseString(map, HEADCHANGE);
                try {
                    if (!DEFAULT.equalsIgnoreCase(CLIENT)) {
                        setHeadChange(Integer.parseInt(cList));
                    } else {
                        setHeadChange(IChangelist.DEFAULT);
                    }
                } catch (NumberFormatException nfe) {
                    setHeadChange(IChangelist.UNKNOWN);
                }
                setHeadRev(map.get(HEADREV) == null ? 0 : parseInt(map, HEADREV));
                setHeadType(parseString(map, HEADTYPE));
                setHeadTime(map.get(HEADTIME) == null ? null
                        : new Date(parseLong(map, HEADTIME) * 1000));
                setHeadModTime(map.get(HEADMODTIME) == null ? null
                        : new Date(parseLong(map, HEADMODTIME) * 1000));
                setHeadCharset(parseString(map, HEAD_CHARSET));
                setHaveRev(getRevFromString(parseString(map, HAVEREV)));
                setDesc(parseString(map, DESC));
                setDigest(parseString(map, DIGEST));
                setFileSize((map.get(FILESIZE) == null ? 0 : parseLong(map, FILESIZE)));
                setOpenAction(map.get(OPEN_ACTION) == null ? null
                        : FileAction.fromString(parseString(map, OPEN_ACTION)));
                setOpenType(parseString(map, OPEN_TYPE));
                setOpenActionOwner(parseString(map, OPEN_ACTION_OWNER));
                setOpenChangelistId(
                        map.get(OPEN_CHANGELIST) == null ? 0 : parseInt(map, OPEN_CHANGELIST));
                setResolved(nonNull(map.get(RESOLVED)));
                setUnresolved(nonNull(map.get(UNRESOLVED)));
                setReresolvable(nonNull(map.get(RERESOLVABLE)));
                setOtherLocked(nonNull(map.get(OTHERLOCK)));
                setOtherActionList(getStringList(map, OTHER_ACTION));
                setOtherChangelist(getStringList(map, OTHER_CHANGE));
                setOtherOpenList(getStringList(map, OTHER_OPEN));
                setActionOwner(parseString(map, ACTIONOWNER));
                setCharset(parseString(map, CHARSET));
                shelved = map.containsKey(SHELVED);
                movedFile = parseString(map, MOVEDFILE);
                setVerifyStatus(parseString(map, STATUS));

                // Pick off the resolve / integration records, if any:

                for (int i = 0; map.containsKey(RESOLVE_ACTION + i); i++) {
                    resolveRecords.add(new ResolveRecord(map, i));
                }

                // Try to get any attributes; note that these are actually
                // bytes, not a string (even though they may well be a string),
                // but
                // we put them into the map as Objects. This may change soon --
                // HR.
                // This could (obviously) be hugely optimised -- HR.

                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (nonNull(entry.getKey()) && (entry.getKey().startsWith(ATTR_PREFIX)
                            || entry.getKey().startsWith(OPEN_ATTR_PREFIX))) {
                        int i = entry.getKey().indexOf("-");
                        if (i < entry.getKey().length()) {
                            String name = entry.getKey().substring(i + 1);
                            try {
                                // Sometimes it comes across as a string,
                                // sometimes
                                // as bytes...
                                Object object = entry.getValue();
                                if (object instanceof String) {
                                    attributes.put(name, ((String) object).getBytes());
                                } else {
                                    attributes.put(name, (byte[]) object);
                                }
                            } catch (Throwable thr) {
                                Log.warn(
                                        "Unexpected exception in ExtendedFileSpec file attributes processing");
                                Log.exception(thr);
                            }
                        }
                    }
                    if (nonNull(entry.getKey()) && (entry.getKey().startsWith(ATTR_PROP_PREFIX)
                            || entry.getKey().startsWith(OPEN_ATTR_PROP_PREFIX))) {
                        int i = entry.getKey().indexOf("-");
                        if (i < entry.getKey().length()) {
                            String name = entry.getKey().substring(i + 1);
                            try {
                                // Sometimes it comes across as a string,
                                // sometimes
                                // as bytes...
                                Object object = entry.getValue();
                                if (object instanceof String) {
                                    propagatingAttributes.put(name, ((String) object).getBytes());
                                } else {
                                    propagatingAttributes.put(name, (byte[]) object);
                                }
                            } catch (Throwable thr) {
                                Log.warn(
                                        "Unexpected exception in ExtendedFileSpec file propagating attributes processing");
                                Log.exception(thr);
                            }
                        }
                    }
                    if (nonNull(entry.getKey()) && (entry.getKey().startsWith(ATTR_TYPE_PREFIX)
                            || entry.getKey().startsWith(OPEN_ATTR_TYPE_PREFIX))) {
                        int i = entry.getKey().indexOf("-");
                        if (i < entry.getKey().length()) {
                            String name = entry.getKey().substring(i + 1);
                            try {
                                // Sometimes it comes across as a string,
                                // sometimes
                                // as bytes...
                                Object object = entry.getValue();
                                if (object instanceof String) {
                                    attributeTypes.put(name, ((String) object).getBytes());
                                } else {
                                    attributeTypes.put(name, (byte[]) object);
                                }
                            } catch (Throwable thr) {
                                Log.warn(
                                        "Unexpected exception in ExtendedFileSpec file attribute types processing");
                                Log.exception(thr);
                            }
                        }
                    }
                }
            } catch (Exception exc) {
                Log.error("Unexpected exception in ExtendedFileSpec constructor"
                        + exc.getLocalizedMessage());
                Log.exception(exc);
            }
        }
    }

    private List<String> getStringList(Map<String, Object> map, String key) {
        List<String> strList = new ArrayList<>();

        if (nonNull(map) && nonNull(key)) {
            int i = 0;
            while (map.containsKey(key + i)) {
                strList.add((String) map.get(key + i));
                i++;
            }
        }

        return strList;
    }

    // p4ic4idea: these string + code based constructors are no longer used
    //public ExtendedFileSpec(FileSpecOpStatus status, String errStr, String errCodeStr) {
    //    super(status, errStr, errCodeStr);
    //}
    //
    //public ExtendedFileSpec(FileSpecOpStatus status, String errStr, int rawCode) {
    //    super(status, errStr, rawCode);
    //}


    /*
     * Construct an ExtendedFileSpec object from a status, message string,
     * generic code, severity code tuple. See the corresponding FileSpec
     * constructor for details -- this constructor does not add any
     * ExtendedFileSpec-specific semantics.
     *
     * @param status
     *            FileSpecOpStatus status.
     * @param errStr
     *            error / info message string.
     * @param genericCode
     *            Perforce generic code to use
     * @param severityCode
     *            Perforce severity code to use.
     */
    //public ExtendedFileSpec(FileSpecOpStatus status, String errStr, int genericCode,
    //        int severityCode) {
    //    super(status, errStr, genericCode, severityCode);
    //}

    public String getActionOwner() {
        return this.actionOwner;
    }

    public void setActionOwner(String actionOwner) {
        this.actionOwner = actionOwner;
    }

    public Map<String, byte[]> getAttributeTypes() {
        return attributeTypes;
    }

    public Map<String, byte[]> getAttributes() {
        return attributes;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public int getHaveRev() {
        return haveRev;
    }

    public void setHaveRev(int haveRev) {
        this.haveRev = haveRev;
    }

    @Override
    public FileAction getHeadAction() {
        return headAction;
    }

    public void setHeadAction(FileAction headAction) {
        this.headAction = headAction;
    }

    @Override
    public int getHeadChange() {
        return headChange;
    }

    public void setHeadChange(int headChange) {
        this.headChange = headChange;
    }

    @Override
    public String getHeadCharset() {
        return headCharset;
    }

    public void setHeadCharset(String headCharset) {
        this.headCharset = headCharset;
    }

    @Override
    public Date getHeadModTime() {
        return headModTime;
    }

    public void setHeadModTime(Date headModTime) {
        this.headModTime = headModTime;
    }

    @Override
    public int getHeadRev() {
        return headRev;
    }

    public void setHeadRev(int headRev) {
        this.headRev = headRev;
    }

    @Override
    public Date getHeadTime() {
        return headTime;
    }

    public void setHeadTime(Date headTime) {
        this.headTime = headTime;
    }

    @Override
    public String getHeadType() {
        return headType;
    }

    public void setHeadType(String headType) {
        this.headType = headType;
    }

    @Override
    public String getMovedFile() {
        return movedFile;
    }

    @Override
    public void setMovedFile(String movedFile) {
        this.movedFile = movedFile;
    }

    @Override
    public FileAction getOpenAction() {
        return openAction;
    }

    public void setOpenAction(FileAction openAction) {
        this.openAction = openAction;
    }

    @Override
    public String getOpenActionOwner() {
        return openActionOwner;
    }

    public void setOpenActionOwner(String openActionOwner) {
        this.openActionOwner = openActionOwner;
    }

    @Override
    public int getOpenChangelistId() {
        return openChangelistId;
    }

    public void setOpenChangelistId(int openChangelistId) {
        this.openChangelistId = openChangelistId;
    }

    @Override
    public String getOpenType() {
        return openType;
    }

    public void setOpenType(String openType) {
        this.openType = openType;
    }

    @Override
    public List<String> getOtherActionList() {
        return otherActionList;
    }

    public void setOtherActionList(List<String> otherActionList) {
        this.otherActionList = otherActionList;
    }

    @Override
    public List<String> getOtherChangelist() {
        return otherChangelist;
    }

    public void setOtherChangelist(List<String> otherChangelist) {
        this.otherChangelist = otherChangelist;
    }

    @Override
    public List<String> getOtherOpenList() {
        return otherOpenList;
    }

    public void setOtherOpenList(List<String> otherOpenList) {
        this.otherOpenList = otherOpenList;
    }

    public Map<String, byte[]> getPropagatingAttributes() {
        return this.propagatingAttributes;
    }

    @Override
    public List<IResolveRecord> getResolveRecords() {
        return resolveRecords;
    }

    @Override
    public void setResolveRecords(List<IResolveRecord> resolveRecords) {
        this.resolveRecords = resolveRecords;
    }

    public String getVerifyStatus() {
        return verifyStatus;
    }

    public void setVerifyStatus(String verifyStatus) {
        this.verifyStatus = verifyStatus;
    }

    @Override
    public boolean isMapped() {
        return mapped;
    }

    public void setMapped(boolean mapped) {
        this.mapped = mapped;
    }

    @Override
    public boolean isOtherLocked() {
        return otherLocked;
    }

    public void setOtherLocked(boolean otherLocked) {
        this.otherLocked = otherLocked;
    }

    @Override
    public boolean isReresolvable() {
        return reresolvable;
    }

    public void setReresolvable(boolean reresolvable) {
        this.reresolvable = reresolvable;
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    @Override
    public boolean isShelved() {
        return shelved;
    }

    @Override
    public boolean isUnresolved() {
        return unresolved;
    }

    public void setUnresolved(boolean unresolved) {
        this.unresolved = unresolved;
    }

    // p4ic4idea: IServerMessage construction

	/**
	 * Construct an ExtendedFileSpec object from a status, message string,
	 * generic code, severity code tuple. See the corresponding FileSpec
	 * constructor for details -- this constructor does not add any
	 * ExtendedFileSpec-specific semantics.
	 * 
	 * @param message server message
	 */
	public ExtendedFileSpec(IServerMessage message) {
		super(getStatusFor(message), message);
	}


	private static FileSpecOpStatus getStatusFor(IServerMessage message) {
		if (message.getSeverity() == MessageSeverityCode.E_INFO) {
			return FileSpecOpStatus.INFO;
		}
		return FileSpecOpStatus.ERROR;
	}
}
