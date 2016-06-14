/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.core.file;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IResolveRecord;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.IServerMessage;

import java.util.*;

/**
 * Useful generic implementation class for the IExtendedFileSpec interface. Fields
 * below generally correspond exactly with the similarly-named Perforce fstat call
 * fields, and will not be documented in detail here.
 */
public class ExtendedFileSpec extends FileSpec implements
												IExtendedFileSpec {
	private boolean mapped = false;		// set if mapped client file is synced
	private FileAction headAction = null; // action at head rev, if in depot
	private int headChange = 0;	// head rev changelist#, if in depot
	private int headRev = 0;		// head rev #, if in depot
	private String headType = null;	// head rev type, if in depot
	private Date headTime = null;		// head rev changelist time, if in depot
	private Date headModTime = null;	// head rev mod time, if in depot
	private String headCharset = null;	// head rev charset, if in depot
    private int haveRev = 0;		// rev had on client, if on client
    private String desc = null;		// change description
    private String digest = null;		// MD5 digest (fingerprint)
    private long fileSize = 0;		// file size
    private FileAction openAction = null;	// open action, if opened
    private String openType = null;	// open type, if opened
    private String openActionOwner = null;	// user who opened file, if opened
    private int openChangelistId = 0;	// open changelist#, if opened
    private boolean unresolved = false; // true if needs resolution
    private boolean resolved = false;	// true if has been resolved
    private boolean reresolvable = false;	// true if it is reresolvable
    private boolean otherLocked = false; // true if locked by another client
    private List<String> otherActionList = null; // list of other actions on this file
    private List<String> otherChangelist = null; // list of other change lists for this file
    private List<String> otherOpenList = null; // list of other users with file open
    private String actionOwner = null;	// owner of the open action
    private String charset = null;	// charset for this file revision
    private boolean shelved = false;
    private List<IResolveRecord> resolveRecords = null;
    private String movedFile = null;
    private Map<String, byte[]> attributes = null;	// Leave it null until needed...
    private Map<String, byte[]> propagatingAttributes = null;	// Leave it null until needed...
    private Map<String, byte[]> attributeTypes = null;	// Leave it null until needed...
    private String verifyStatus = null;

    /**
     * Default constructor. Sets all boolean fields to false,
     * object fields to null, integers to zero.
     */
	public ExtendedFileSpec() {
		super();
	}

	/**
	 * Construct an ExtendedFileSpec object from the passed-in map. The map
	 * must be (or have the same keys and semantics as) a map as returned from
	 * a suitable Perforce server call; the semantics and format of this map
	 * are not spelled out here. 
	 * 
	 * @param map suitable field map from Perforce server; if null, this constructor
	 * 				has the same semantics as the default constructor.
	 * @param server non-null server object
	 * @param index passed to the superclass FileSpec(Map<String, Object> map,
	 * 				IServer server, int index) constructor but not otherwise used here.
	 */
	public ExtendedFileSpec(Map<String, Object> map, IServer server,
			int index) {
		super(map, server, index);
		if (map != null) {
			try {
				this.setClient(null);
				this.setMapped((map.get("isMapped") != null));
				this.setHeadAction(FileAction.fromString((String) map.get("headAction")));
				String cList = (String) map.get("headChange");
				if ((cList != null) && !cList.equalsIgnoreCase("default")) {
					this.setHeadChange(new Integer(cList));
				} else {
					this.setHeadChange(IChangelist.DEFAULT);
				}
				this.setHeadRev((map.get("headRev") == null ? 0 :
							(new Integer((String) map.get("headRev")))));
				this.setHeadType((String) map.get("headType"));
				this.setHeadTime((map.get("headTime") == null ?
						null : new Date(Long.parseLong((String) map.get("headTime")) * 1000)));
				this.setHeadModTime((map.get("headModTime") == null ?
						null : new Date(Long.parseLong((String) map.get("headModTime")) * 1000)));
				this.setHeadCharset((String) map.get("headCharset"));
				this.setHaveRev(getRevFromString((String) map.get("haveRev")));
				this.setDesc((String) map.get("desc"));
				this.setDigest((String) map.get("digest"));
				this.setFileSize((map.get("fileSize") == null ? 0 : new Long((String) map.get("fileSize"))));
				this.setOpenAction((map.get("openAction") == null ? null :
								FileAction.fromString((String) map.get("openAction"))));
				this.setOpenType((String) map.get("openType"));
				this.setOpenActionOwner((String) map.get("openActionOwner"));
				this.setOpenChangelistId((map.get("openChangelist") == null ? 0 :
							(new Integer((String) map.get("openChangelist")))));
				this.setResolved((map.get("resolved") == null ? false : true));
				this.setUnresolved((map.get("unresolved") == null ? false : true));
				this.setReresolvable((map.get("reresolvable") == null ? false : true));
				this.setOtherLocked((map.get("otherLock") == null ? false : true));
				this.setOtherActionList(getStringList(map, "otherAction"));
				this.setOtherChangelist(getStringList(map, "otherChange"));
				this.setOtherOpenList(getStringList(map, "otherOpen"));
				this.setActionOwner((String) map.get("actionOwner"));
				this.setCharset((String) map.get("charset"));
				this.shelved = map.containsKey("shelved");
				this.movedFile = (String) map.get("movedFile");
				this.setVerifyStatus((String)map.get("status"));
			
				// Pick off the resolve / integration records, if any:
				
				for (int i = 0; map.containsKey("resolveAction" + i); i++) {
					if (this.resolveRecords == null) {
						this.resolveRecords = new ArrayList<IResolveRecord>();
					}
					this.resolveRecords.add(new ResolveRecord(map, i));
				}
				
				// Try to get any attributes; note that these are actually
				// bytes, not a string (even though they may well be a string), but
				// we put them into the map as Objects. This may change soon -- HR.
				// This could (obviously) be hugely optimised -- HR.
				
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					if ((entry.getKey() != null) && (entry.getKey().startsWith("attr-") || entry.getKey().startsWith("openattr-"))) {
						if (this.attributes == null) {
							this.attributes = new HashMap<String, byte[]>();
						}
						int i = entry.getKey().indexOf("-");
						if (i < entry.getKey().length()) {
							String name = entry.getKey().substring(i + 1);
							try {
								// Sometimes it comes across as a string, sometimes
								// as bytes...
								Object object = entry.getValue();
								if (object instanceof String) {
									this.attributes.put(name, ((String) object).getBytes());
								} else {
									this.attributes.put(name, (byte[]) object);
								}
								
							} catch (Throwable thr) {
								Log.warn("Unexpected exception in ExtendedFileSpec file attributes processing");
								Log.exception(thr);
							}
						}
					}
					if ((entry.getKey() != null) && (entry.getKey().startsWith("attrProp-") || entry.getKey().startsWith("openattrProp-"))) {
						if (this.propagatingAttributes == null) {
							this.propagatingAttributes = new HashMap<String, byte[]>();
						}
						int i = entry.getKey().indexOf("-");
						if (i < entry.getKey().length()) {
							String name = entry.getKey().substring(i + 1);
							try {
								// Sometimes it comes across as a string, sometimes
								// as bytes...
								Object object = entry.getValue();
								if (object instanceof String) {
									this.propagatingAttributes.put(name, ((String) object).getBytes());
								} else {
									this.propagatingAttributes.put(name, (byte[]) object);
								}
								
							} catch (Throwable thr) {
								Log.warn("Unexpected exception in ExtendedFileSpec file propagating attributes processing");
								Log.exception(thr);
							}
						}
					}
					if ((entry.getKey() != null) && (entry.getKey().startsWith("attrType-") || entry.getKey().startsWith("openattrType-"))) {
						if (this.attributeTypes == null) {
							this.attributeTypes = new HashMap<String, byte[]>();
						}
						int i = entry.getKey().indexOf("-");
						if (i < entry.getKey().length()) {
							String name = entry.getKey().substring(i + 1);
							try {
								// Sometimes it comes across as a string, sometimes
								// as bytes...
								Object object = entry.getValue();
								if (object instanceof String) {
									this.attributeTypes.put(name, ((String) object).getBytes());
								} else {
									this.attributeTypes.put(name, (byte[]) object);
								}
								
							} catch (Throwable thr) {
								Log.warn("Unexpected exception in ExtendedFileSpec file attribute types processing");
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

	/*
	 * Construct an ExtendedFileSpec object from a status, message string pair.
	 * See the corresponding FileSpec constructor for details -- this constructor
	 * does not add any ExtendedFileSpec-specific semantics.
	 *  
	 * @param status FileSpecOpStatus status.
	 * @param errStr error / info message string.
	public ExtendedFileSpec(FileSpecOpStatus status, String errStr) {
		super(status, errStr);
	}
	 */

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

	/**
	 * Given a candidate path string (which may include version
	 * and changelist annotations, at least), try to construct
	 * a corresponding extended file spec.<p>
	 * 
	 * See the corresponding FileSpec constructor for details -- this constructor
	 * does not add any ExtendedFileSpec-specific semantics.
	 * 
	 * @param pathStr candidate path string
	 */
	public ExtendedFileSpec(String pathStr) {
		super(pathStr);
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getDesc()
	 */
	public String getDesc() {
		return this.desc;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getDigest()
	 */
	public String getDigest() {
		return this.digest;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getFileSize()
	 */
	public long getFileSize() {
		return this.fileSize;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getHaveRev()
	 */
	public int getHaveRev() {
		return this.haveRev;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getHeadAction()
	 */
	public FileAction getHeadAction() {
		return this.headAction;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getHeadChange()
	 */
	public int getHeadChange() {
		return this.headChange;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getHeadModTime()
	 */
	public Date getHeadModTime() {
		return this.headModTime;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getHeadCharset()
	 */
	public String getHeadCharset() {
		return this.headCharset;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getHeadRev()
	 */
	public int getHeadRev() {
		return this.headRev;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getHeadTime()
	 */
	public Date getHeadTime() {
		return this.headTime;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getHeadType()
	 */
	public String getHeadType() {
		return this.headType;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getOpenAction()
	 */
	public FileAction getOpenAction() {
		return this.openAction;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getOpenActionOwner()
	 */
	public String getOpenActionOwner() {
		return this.openActionOwner;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getOpenChangelistId()
	 */
	public int getOpenChangelistId() {
		return this.openChangelistId;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getOpenType()
	 */
	public String getOpenType() {
		return this.openType;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#isMapped()
	 */
	public boolean isMapped() {
		return this.mapped;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#isResolved()
	 */
	public boolean isResolved() {
		return this.resolved;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#isUnresolved()
	 */
	public boolean isUnresolved() {
		return this.unresolved;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#isReresolvable()
	 */
	public boolean isReresolvable() {
		return this.reresolvable;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#isOtherLocked()
	 */
	public boolean isOtherLocked() {
		return this.otherLocked;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getOtherOpenList()
	 */
	public List<String> getOtherOpenList() {
		return this.otherOpenList;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getOtherChangelist()
	 */
	public List<String> getOtherChangelist() {
		return this.otherChangelist;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getOtherActionList()
	 */
	public List<String> getOtherActionList() {
		return this.otherActionList;
	}

	public void setMapped(boolean mapped) {
		this.mapped = mapped;
	}

	public void setHeadAction(FileAction headAction) {
		this.headAction = headAction;
	}

	public void setHeadChange(int headChange) {
		this.headChange = headChange;
	}

	public void setHeadRev(int headRev) {
		this.headRev = headRev;
	}

	public void setHeadType(String headType) {
		this.headType = headType;
	}

	public void setHeadTime(Date headTime) {
		this.headTime = headTime;
	}

	public void setHeadModTime(Date headModTime) {
		this.headModTime = headModTime;
	}

	public void setHeadCharset(String headCharset) {
		this.headCharset = headCharset;
	}

	public void setHaveRev(int haveRev) {
		this.haveRev = haveRev;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public void setOpenAction(FileAction openAction) {
		this.openAction = openAction;
	}

	public void setOpenType(String openType) {
		this.openType = openType;
	}

	public void setOpenActionOwner(String openActionOwner) {
		this.openActionOwner = openActionOwner;
	}

	public void setOpenChangelistId(int openChangelistId) {
		this.openChangelistId = openChangelistId;
	}

	public void setUnresolved(boolean unresolved) {
		this.unresolved = unresolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}
	
	public void setReresolvable(boolean reresolvable) {
		this.reresolvable = reresolvable;
	}

	public void setOtherLocked(boolean otherLocked) {
		this.otherLocked = otherLocked;
	}
	
	public void setOtherOpenList(List<String> otherOpenList) {
		this.otherOpenList = otherOpenList;
	}
	
	public void setOtherChangelist(List<String> otherChangelist) {
		this.otherChangelist = otherChangelist;
	}
	
	public void setOtherActionList(List<String> otherActionList) {
		this.otherActionList = otherActionList;
	}
	
	private List<String> getStringList(Map<String, Object> map, String key) {
		List<String> strList = null;

		if ((map != null) && (key != null)) {
			int i = 0;
			while (map.containsKey(key + i)) {
				if (strList == null) {
					strList = new ArrayList<String>();
				}
				strList.add((String) map.get(key + i));
				i++;
			}
		}
		
		return strList;
	}

	public String getActionOwner() {
		return this.actionOwner;
	}

	public void setActionOwner(String actionOwner) {
		this.actionOwner = actionOwner;
	}

	public String getCharset() {
		return this.charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#isShelved()
	 */
	public boolean isShelved() {
		return this.shelved;
	}

	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#getResolveRecords()
	 */
	public List<IResolveRecord> getResolveRecords() {
		return this.resolveRecords;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IExtendedFileSpec#setResolveRecords(java.util.List)
	 */
	public void setResolveRecords(List<IResolveRecord> resolveRecords) {
		this.resolveRecords = resolveRecords;
	}

	/**
	 * @return the movedFile
	 */
	public String getMovedFile() {
		return movedFile;
	}

	/**
	 * @param movedFile the movedFile to set
	 */
	public void setMovedFile(String movedFile) {
		this.movedFile = movedFile;
	}
	
	public Map<String, byte[]> getAttributes() {
		if (this.attributes == null) {
			this.attributes = new HashMap<String, byte[]>();
		}
		return this.attributes;
	}

	public Map<String, byte[]> getPropagatingAttributes() {
		if (this.propagatingAttributes == null) {
			this.propagatingAttributes = new HashMap<String, byte[]>();
		}
		return this.propagatingAttributes;
	}

	public Map<String, byte[]> getAttributeTypes() {
		if (this.attributeTypes == null) {
			this.attributeTypes = new HashMap<String, byte[]>();
		}
		return this.attributeTypes;
	}

    public String getVerifyStatus() {
    	return this.verifyStatus;
    }

    public void setVerifyStatus(String verifyStatus) {
    	this.verifyStatus = verifyStatus;
    }


	private static FileSpecOpStatus getStatusFor(IServerMessage message) {
		if (message.getSeverity() == MessageSeverityCode.E_INFO) {
			return FileSpecOpStatus.INFO;
		}
		return FileSpecOpStatus.ERROR;
	}
}
