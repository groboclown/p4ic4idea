/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */

package com.perforce.p4java.impl.generic.core.file;

import com.perforce.p4java.Log;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.*;
import com.perforce.p4java.exception.*;
import com.perforce.p4java.impl.generic.core.ServerResource;
import com.perforce.p4java.impl.generic.core.file.FilePath.PathType;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.IServerMessage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Simple generic default implementation class for the IFileSpec
 * interface.
 */

public class FileSpec extends ServerResource implements IFileSpec {

	protected FileSpecOpStatus opStatus = FileSpecOpStatus.VALID;
	protected IServerMessage statusMessage = null;
	protected int genericCode = 0;
	protected int severityCode = 0;
	protected int rawCode = 0;
	protected int uniqueCode = 0;
	protected int subCode = 0;
	protected int subSystem = 0;
	
	protected FilePath originalPath = null;
	protected FilePath depotPath = null;
	protected FilePath clientPath = null;
	protected FilePath localPath = null;
	protected String fileType = null;
	protected int startRevision = NO_FILE_REVISION;
	protected int endRevision = NO_FILE_REVISION;
	protected int changeListId = IChangelist.UNKNOWN;
	protected String label = null;
	protected Date date = null;
	protected FileAction action = null;
	protected String userName = null;
	protected String clientName = null;
	protected int baseRev = NO_FILE_REVISION;
	protected String baseName = null;
	protected String baseFile = null;
	
	protected boolean unmap = false;
	
	private String fromFile = null;
	private int endFromRev = NO_FILE_REVISION;
	private int startFromRev = NO_FILE_REVISION;
	private String toFile = null;
	private int endToRev = NO_FILE_REVISION;
	private int startToRev = NO_FILE_REVISION;
	
	private int workRev = NO_FILE_REVISION;
	private String howResolved = null;
	private FileAction otherAction = null;
	
	private boolean locked = false;
	
	private String diffStatus = null;
	
	private String resolveType = null;
	private String contentResolveType = null;

	private int shelvedChange = IChangelist.UNKNOWN;
	
	protected IClient client = null;
	
	/**
	 * Default constructor. Sets all paths, labels, dates, etc. to null; revisions
	 * to IFileSpec.NO_FILE_REVISION; client and server references to null;
	 * changelist ID to IChangelist.UNKNOWN; opStatus to VALID; locked to false, etc.
	 */
	public FileSpec() {
		super(false, false);
	}
	
	/**
	 * Given a candidate path string (which may include version
	 * and changelist annotations, at least), try to construct
	 * a corresponding file spec.<p>
	 * 
	 * Effectively an alias for FileSpec(pathStr, true).
	 * 
	 * @param pathStr candidate path string
	 */
	
	public FileSpec(String pathStr) {
		this(pathStr, true);
	}
	
	/**
	 * Given a candidate path string (which may include version
	 * and changelist annotations, at least), try to construct
	 * a corresponding file spec.<p>
	 * 
	 * The motivation for the hasAnnotations parameter is to
	 * allow path strings to contain "@" and "#" characters;
	 * the downside of this that if there's any associated 
	 * annotation info, it's not parsed at all and any such
	 * information must be set up manually.
	 * 
	 * @param pathStr candidate path string
	 * @param parseAnnotations if true, attempt to parse the path string
	 * 				for revision annotations.
	 */
	public FileSpec(String pathStr, boolean parseAnnotations) {
		super(false, false);
		this.originalPath = new FilePath(PathType.ORIGINAL, pathStr);
		if (parseAnnotations && (pathStr != null) && PathAnnotations.hasPerforceAnnotations(pathStr)) {
			PathAnnotations annotations = new PathAnnotations(pathStr);
			this.startRevision = annotations.getStartRevision();
			this.endRevision = annotations.getEndRevision();
			this.changeListId = annotations.getChangelistId();
			this.label = annotations.getLabel();
			this.date = annotations.getDate();
		}
	}
	
	/**
	 * Construct a FileSpec from a specific FilePath.
	 */
	public FileSpec(FilePath path) {
		super(false, false);
		setPath(path);
	}
	
	/**
	 * Construct a filespec from an opstatus and error message pair.
	 */
	public FileSpec(FileSpecOpStatus status, IServerMessage err) {
		super(false, false);
		this.opStatus = status;
		this.statusMessage = err;
		this.genericCode = err.getGeneric();
		this.severityCode = err.getSeverity();
		this.rawCode = err.getRawCode();
		this.uniqueCode = err.getUniqueCode();
		this.subCode = err.getSubCode();
		this.subSystem = err.getSubSystem();
	}

	/**
	 * Construct a new filespec from another filespec. In
	 * other words, effectively clone it by deep copy of local
	 * fields.<p>
	 * 
	 * @param impl non-null existing filespec.
	 */
	public FileSpec(FileSpec impl) {
		super(false, false);
		if (impl == null) {
			throw new NullPointerError("null impl passed to FileSpec constructor");
		}

		this.opStatus = impl.opStatus;
		this.statusMessage = impl.statusMessage;
		this.genericCode = impl.genericCode;
		this.severityCode = impl.severityCode;
		this.originalPath = impl.originalPath;
		this.depotPath = impl.depotPath;
		this.clientPath = impl.clientPath;
		this.localPath = impl.localPath;
		this.fileType = impl.fileType;
		this.startRevision = impl.startRevision;
		this.endRevision = impl.endRevision;
		this.changeListId = impl.changeListId;
		this.label = impl.label;
		this.date = impl.date;
		this.action = impl.action;
		this.userName = impl.userName;
		this.clientName = impl.clientName;
		this.unmap = impl.unmap;
		this.fromFile = impl.fromFile;
		this.endFromRev = impl.endFromRev;
		this.startFromRev = impl.startFromRev;
		this.toFile = impl.toFile;
		this.endToRev = impl.endToRev;
		this.startToRev = impl.startToRev;
		this.workRev = impl.workRev;
		this.howResolved = impl.howResolved;
		this.otherAction = impl.otherAction;
		this.locked = impl.locked;
		this.diffStatus = impl.diffStatus;
		this.resolveType = impl.resolveType;
		this.contentResolveType = impl.contentResolveType;
		this.shelvedChange = impl.shelvedChange;
		this.server = impl.server;
		this.client = impl.client;
		this.baseFile = impl.baseFile;
	}
	
	/**
	 * Try to construct a FileSpec from a passed-in map as returned from a
	 * Perforce server. Tuned to return values from the underlying map-based server
	 * interface, which explains the index (set this to zero for normal use).
	 */
	
	public FileSpec(Map<String, Object> map, IServer server, int index) {
		super(false, false);
		if (map != null) {
			this.setOpStatus(FileSpecOpStatus.VALID);

			String indexStr = "";
			if (index >= 0) {
				indexStr += index;
			}
			this.setServer(server);
			if (map.containsKey("dir" + indexStr)) {
				this.setDepotPath(new FilePath(PathType.DEPOT, (String) map.get("dir" + indexStr), true));
			}
			if (map.containsKey("depotFile" + indexStr)) {
				this.setDepotPath(new FilePath(PathType.DEPOT, (String) map.get("depotFile" + indexStr), true));
			}
			if (map.containsKey("clientFile" + indexStr)) {
				this.setClientPath(new FilePath(PathType.CLIENT, (String) map.get("clientFile" + indexStr), true));
			}
			if (map.containsKey("localFile" + indexStr)) {
				this.setLocalPath(new FilePath(PathType.LOCAL, (String) map.get("localFile" + indexStr), true));
			}
			if (map.containsKey("path" + indexStr)) {
				this.setLocalPath(new FilePath(PathType.LOCAL, (String) map.get("path" + indexStr), true));
			}
			this.setFileType((String) map.get("type" + indexStr));
			this.setAction(FileAction.fromString((String) map.get("action" + indexStr)));
			this.setUserName((String) map.get("user" + indexStr));
			this.setClientName((String) map.get("client" + indexStr));
			String cid = (String) map.get("change" + indexStr);
			String revStr = (String) map.get("rev" + indexStr);
			if (revStr == null) {
				// Sometimes it's the haveRev key...
				revStr = (String) map.get("haveRev" + indexStr);
			}

			// Get submit date from the 'time' (seconds).
			// Multiply by 1000 to get the milliseconds.
			if (map.get("time") != null) {
				try {
					long seconds = Long.parseLong((String) map.get("time"));
					this.setDate(new Date (seconds * 1000));
				} catch (NumberFormatException nfe) {
					Log.error("Error parsing the 'time' in the FileSpec constructor: "
							+ nfe.getLocalizedMessage());
					Log.exception(nfe);
				}
			}
			
			this.setLocked((((map.get("ourLock") == null) && (map.get("otherLock") == null)) ? false : true));
			this.setEndRevision(getRevFromString(revStr));
			
			if (cid == null) {
				this.setChangelistId(IChangelist.UNKNOWN);
			} else if (cid.equalsIgnoreCase("default") || cid.equalsIgnoreCase("default change")) {
				this.setChangelistId(IChangelist.DEFAULT);
			} else {
				// Sometimes in format "change nnnnnn", sometimes just "nnnnn". Urgh...
				int i = cid.indexOf(" ");
				if (i < 0) {
					this.setChangelistId(new Integer(cid));
				} else {
					this.setChangelistId(new Integer(cid.substring(i+1)));
				}
			}
			
			this.setEndFromRev(getRevFromString((String) map.get("endFromRev" + indexStr)));
			this.setStartFromRev(getRevFromString((String) map.get("startFromRev" + indexStr)));
			this.setWorkRev(getRevFromString((String) map.get("workRev" + indexStr)));
			
			this.setHowResolved((String) map.get("how"));
			this.setFromFile((String) map.get("fromFile" + indexStr));
			
			this.setEndToRev(getRevFromString((String) map.get("endToRev" + indexStr)));
			this.setStartToRev(getRevFromString((String) map.get("startToRev" + indexStr)));
			this.setToFile((String) map.get("toFile" + indexStr));
			
			this.setBaseRev(getRevFromString((String) map.get("baseRev" + indexStr)));
			this.setBaseName((String) map.get("baseName" + indexStr));
			this.setBaseFile((String) map.get("baseFile" + indexStr));

			this.setOtherAction(
							FileAction.fromString((String) map.get("otherAction" + indexStr)));
			
			this.setDiffStatus((String) map.get("status"));
			
			this.setResolveType((String) map.get("resolveType"));
			this.setContentResolveType((String) map.get("contentResolveType"));
			
			if (map.containsKey("shelvedChange")) {
				try {
					this.setShelvedChange(new Integer((String) map
							.get("shelvedChange")));
				} catch (NumberFormatException nfe) {
					Log.error("Error parsing the 'shelvedChange' in the FileSpec constructor: "
							+ nfe.getLocalizedMessage());
					Log.exception(nfe);
				}
			}
			
			if (((String) map.get("unmap" + indexStr)) != null) {
				this.unmap = true;
			}
			
			this.setUnmap(((String) map.get("unmap" + indexStr)) != null);
		}
	}
	
	/**
	 * Set the various error codes for this FileSpec to a value returned
	 * from the server or the RPC layer. Use this if you're hand-constructing
	 * a new FileSpec for an error condition and you have the raw code.
	 *
	 * @deprecated use the IServerMessage constructor instead
	 */
	public FileSpec setCodes(int rawCode) {
		this.rawCode = rawCode;
		this.subCode = ((rawCode >> 0) & 0x3FF);
		this.subSystem = ((rawCode >> 10) & 0x3F);
		this.uniqueCode = (rawCode & 0xFFFF);
		this.genericCode = ((rawCode >> 16) & 0xFF);
		this.severityCode = ((rawCode >> 28) & 0x00F);
		return this;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getPath(com.perforce.p4java.impl.generic.core.file.FilePath.PathType)
	 */
	public FilePath getPath(PathType pathType) {
		if (pathType != null) {
			switch (pathType) {
				case DEPOT:
					return this.depotPath;
				case CLIENT:
					return this.clientPath;
				case LOCAL:
					return this.localPath;
				default:
					break;
			}
		}
		
		return this.originalPath;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#setPath(com.perforce.p4java.impl.generic.core.file.FilePath)
	 */
	public void setPath(FilePath filePath) {
		if (filePath != null) {
			if (filePath.getPathType() != null) {
				switch (filePath.getPathType()) {
					case DEPOT:
						this.depotPath = filePath;
						return;
					case CLIENT:
						this.clientPath = filePath;
						return;
					case LOCAL:
						this.localPath = filePath;
						return;
					default:
						break;
				}
			}
		}
		
		this.originalPath = filePath;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getAction()
	 */
	public FileAction getAction() {
		return this.action;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getChangelistId()
	 */
	public int getChangelistId() {
		return this.changeListId;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getClientName()
	 */
	public String getClientName() {
		return this.clientName;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getClientPath()
	 */
	public FilePath getClientPath() {
		return this.getPath(PathType.CLIENT);
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getContents(boolean)
	 */
	public InputStream getContents(boolean noHeaderLine)
			throws ConnectionException, RequestException,
			AccessException {
		checkServer();
		List<IFileSpec> fList = new ArrayList<IFileSpec>();
		fList.add(this);
		
		return this.server.getFileContents(fList, false, noHeaderLine);
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getContents(com.perforce.p4java.option.server.GetFileContentsOptions)
	 */
	public InputStream getContents(GetFileContentsOptions opts) throws P4JavaException {
		checkServer();
		List<IFileSpec> fList = new ArrayList<IFileSpec>();
		fList.add(this);
		
		return ((IOptionsServer) this.server).getFileContents(fList, opts);
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getDepotPath()
	 */
	public FilePath getDepotPath() {
		return this.getPath(PathType.DEPOT);
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getEndRevision()
	 */
	public int getEndRevision() {
		return this.endRevision;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getFileType()
	 */
	public String getFileType() {
		return this.fileType;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getLabel()
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getLocalPath()
	 */
	public FilePath getLocalPath() {
		return this.getPath(PathType.LOCAL);
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getOpStatus()
	 */
	public FileSpecOpStatus getOpStatus() {
		return this.opStatus;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getPreferredPath()
	 */
	public FilePath getPreferredPath() {
		// groboclown: extra null checking
		if (this.originalPath != null && this.originalPath.getPathString() != null) {
			return this.originalPath;
		} else if (this.depotPath != null && this.depotPath.getPathString() != null) {
			return this.depotPath;
		} else if (this.clientPath != null && this.clientPath.getPathString() != null) {
			return this.clientPath;
		} else if (this.localPath != null && this.localPath.getPathString() != null) {
			return this.localPath;
		}
		
		return null;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getRevisionHistory(int, boolean, boolean, boolean, boolean)
	 */
	public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(int maxRevs,
						boolean contentHistory, boolean includeInherited,
						boolean longOutput, boolean truncatedLongOutput)
			throws ConnectionException, RequestException, AccessException {
		
		checkServer();
		return this.server.getRevisionHistory(
				FileSpecBuilder.makeFileSpecList(new String[] { this.getAnnotatedPreferredPathString() }),
						maxRevs, contentHistory, includeInherited, longOutput, truncatedLongOutput);
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getRevisionHistory(com.perforce.p4java.option.server.GetRevisionHistoryOptions)
	 */
	public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(GetRevisionHistoryOptions opts)
						throws P4JavaException {
		checkServer();		
		return ((IOptionsServer) this.server).getRevisionHistory(
				FileSpecBuilder.makeFileSpecList(new String[] { this.getAnnotatedPreferredPathString() }), opts);
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getAnnotations(com.perforce.p4java.core.file.DiffType, boolean, boolean, boolean)
	 */
	public List<IFileAnnotation> getAnnotations(DiffType wsOptions, boolean allResults,
						boolean useChangeNumbers, boolean followBranches)
			throws ConnectionException, RequestException, AccessException {
		checkServer();
		List<IFileSpec> specList = new ArrayList<IFileSpec>();
		specList.add(this);
		return this.server.getFileAnnotations(specList, wsOptions, allResults, useChangeNumbers, followBranches);
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getAnnotations(com.perforce.p4java.option.server.GetFileAnnotationsOptions)
	 */
	public List<IFileAnnotation> getAnnotations(GetFileAnnotationsOptions opts) throws P4JavaException {
		checkServer();
		List<IFileSpec> specList = new ArrayList<IFileSpec>();
		specList.add(this);
		return ((IOptionsServer) this.server).getFileAnnotations(specList, opts);
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#move(int, boolean, boolean, java.lang.String, com.perforce.p4java.core.file.IFileSpec)
	 */
	public List<IFileSpec> move(int changelistId, boolean listOnly, boolean noClientMove, String fileType, IFileSpec toFile)
					throws ConnectionException, RequestException, AccessException {
		checkServer();
		return this.server.moveFile(changelistId, listOnly, noClientMove, fileType, this, toFile);
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#move(com.perforce.p4java.core.file.IFileSpec, com.perforce.p4java.option.server.MoveFileOptions)
	 */
	public List<IFileSpec> move(IFileSpec toFile, MoveFileOptions opts) throws P4JavaException {
		checkServer();
		return ((IOptionsServer) this.server).moveFile(this, toFile, opts);
	}
	
	private void checkServer() throws P4JavaError {
		if (this.server == null) {
			throw new P4JavaError("File specification is not associated with any server");
		}
		
		if (!(this.server instanceof IOptionsServer)) {
			// This should be impossible, but you never know... -- HR.
			throw new P4JavaError("File specification is not associated with an options server");
		}
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getStartRevision()
	 */
	public int getStartRevision() {
		return this.startRevision;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getStatusMessage()
	 */
	public IServerMessage getStatusMessage() {
		return this.statusMessage;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getSeverityCode()
	 */
	public int getSeverityCode() {
		return this.severityCode;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getGenericCode()
	 */
	public int getGenericCode() {
		return this.genericCode;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getUserName()
	 */
	public String getUserName() {
		return this.userName;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#isLocked()
	 */
	public boolean isLocked() {
		return this.locked;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getDiffStatus()
	 */
	public String getDiffStatus() {
		return this.diffStatus;
	}

	public String getResolveType() {
		return this.resolveType;
	}

	public String getContentResolveType() {
		return this.contentResolveType;
	}

	public int getShelvedChange() {
		return this.shelvedChange;
	}

	public void setOpStatus(FileSpecOpStatus opStatus) {
		this.opStatus = opStatus;
	}

	public void setStatusMessage(IServerMessage statusMessage) {
		this.statusMessage = statusMessage;
	}

	public void setOriginalPath(FilePath path) {
		this.originalPath = path;
	}

	public void setDepotPath(FilePath depotPath) {
		this.depotPath = depotPath;
	}

	public void setClientPath(FilePath clientPath) {
		this.clientPath = clientPath;
	}

	public void setLocalPath(FilePath localPath) {
		this.localPath = localPath;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public void setStartRevision(int startRevision) {
		this.startRevision = startRevision;
	}

	public void setEndRevision(int endRevision) {
		this.endRevision = endRevision;
	}

	public void setChangelistId(int changeListId) {
		this.changeListId = changeListId;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setAction(FileAction action) {
		this.action = action;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public void setClient(IClient client) {
		this.client = client;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getEndFromRev()
	 */
	public int getEndFromRev() {
		return this.endFromRev;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getEndToRev()
	 */
	public int getEndToRev() {
		return this.endToRev;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getFromFile()
	 */
	public String getFromFile() {
		return this.fromFile;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getHowResolved()
	 */
	public String getHowResolved() {
		return this.howResolved;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getOtherAction()
	 */
	public FileAction getOtherAction() {
		return this.otherAction;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getStartFromRev()
	 */
	public int getStartFromRev() {
		return this.startFromRev;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getStartToRev()
	 */
	public int getStartToRev() {
		return this.startToRev;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getToFile()
	 */
	public String getToFile() {
		return this.toFile;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getWorkRev()
	 */
	public int getWorkRev() {
		return this.workRev;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#isUnmap()
	 */
	public boolean isUnmap() {
		return this.unmap;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setFromFile(String fromFile) {
		this.fromFile = fromFile;
	}

	public void setEndFromRev(int endFromRev) {
		this.endFromRev = endFromRev;
	}

	public void setStartFromRev(int startFromRev) {
		this.startFromRev = startFromRev;
	}

	public void setToFile(String toFile) {
		this.toFile = toFile;
	}

	public void setEndToRev(int endToRev) {
		this.endToRev = endToRev;
	}

	public void setStartToRev(int startToRev) {
		this.startToRev = startToRev;
	}

	public void setWorkRev(int workRev) {
		this.workRev = workRev;
	}

	public void setHowResolved(String howResolved) {
		this.howResolved = howResolved;
	}

	public void setOtherAction(FileAction otherAction) {
		this.otherAction = otherAction;
	}
	
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	public void setDiffStatus(String diffStatus) {
		this.diffStatus = diffStatus;
	}
	
	public void setResolveType(String resolveType) {
		this.resolveType = resolveType;
	}

	public void setContentResolveType(String contentResolveType) {
		this.contentResolveType = contentResolveType;
	}

	public void setShelvedChange(int shelvedChange) {
		this.shelvedChange = shelvedChange;
	}

	public void setUnmap(boolean unmap) {
		this.unmap = unmap;
	}

	public static int getRevFromString(String str) {
		int rev = NO_FILE_REVISION;

		if (str != null) {
			// can be in #rev or rev form, unfortunately...

			if (str.contains("head")) {
				return HEAD_REVISION;
			}
			
			if (str.contains("none")) {
				return NO_FILE_REVISION;
			}
			
			try {
				if (str.startsWith("#") && (str.length() > 1)) {
					rev = new Integer(str.substring(1));
				} else if (str.length() > 0){
					rev = new Integer(str);
				}
			} catch (Exception exc) {
				Log.error("Conversion error in FileSpec.getRevFromString: "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}

		}
		return rev;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getClientPathString()
	 */
	public String getClientPathString() {
		return getPathString(PathType.CLIENT);
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getDepotPathString()
	 */
	public String getDepotPathString() {
		return getPathString(PathType.DEPOT);
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getLocalPathString()
	 */
	public String getLocalPathString() {
		return getPathString(PathType.LOCAL);
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getOriginalPath()
	 */
	public FilePath getOriginalPath() {
		if (getPath(PathType.ORIGINAL) != null) { // See job061945
			return getPath(PathType.ORIGINAL);
		} else { // API backward compatibility - See job070533
			return getPath(PathType.CLIENT);
		}
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getOriginalPathString()
	 */
	public String getOriginalPathString() {
		if (getPathString(PathType.ORIGINAL) != null) { // See job061945
			return getPathString(PathType.ORIGINAL);
		} else { // API backward compatibility - See job070533
			return getPathString(PathType.CLIENT);
		}
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getPathString(com.perforce.p4java.impl.generic.core.file.FilePath.PathType)
	 */
	public String getPathString(PathType pathType) {
		FilePath fPath = getPath(pathType);
		
		if (fPath != null) {
			return fPath.toString();
		}
		return null;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getAnnotatedPathString(com.perforce.p4java.impl.generic.core.file.FilePath.PathType)
	 */
	public String getAnnotatedPathString(PathType pathType) {
		FilePath path = getPath(pathType);
		if (path != null) {
			return path.annotate(this);
		}
		
		return null;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getPreferredPathString()
	 */
	public String getPreferredPathString() {
		FilePath prefPath = getPreferredPath();
		
		if ((prefPath != null) && (prefPath.getPathString() != null)) {
			return prefPath.toString();
		}
		
		return null;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getAnnotatedPreferredPathString()
	 */
	public String getAnnotatedPreferredPathString() {
		FilePath prefPath = getPreferredPath();
		
		if ((prefPath != null) && (prefPath.getPathString() != null)) {
			return prefPath.annotate(this);
		}
		
		return null;
	}
	
	/**
	 * Alias for getAnnotatedPreferredPathString().
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getAnnotatedPreferredPathString();
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#setPathFromString(com.perforce.p4java.impl.generic.core.file.FilePath.PathType, java.lang.String)
	 */
	public void setPathFromString(PathType pathType, String pathStr) {
		FilePath path = new FilePath(pathType, pathStr);
		setPath(path);
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getDate()
	 */
	public Date getDate() {
		return this.date;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#setClientPath(java.lang.String)
	 */
	public void setClientPath(String pathStr) {
		setPath(new FilePath(PathType.CLIENT, pathStr));
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#setDepotPath(java.lang.String)
	 */
	public void setDepotPath(String pathStr) {
		setPath(new FilePath(PathType.DEPOT, pathStr));
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#setLocalPath(java.lang.String)
	 */
	public void setLocalPath(String pathStr) {
		// groboclown: fix how nulls work
		if (pathStr == null) {
			localPath = null;
		} else {
			setPath(new FilePath(PathType.LOCAL, pathStr));
		}
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#setOriginalPath(java.lang.String)
	 */
	public void setOriginalPath(String pathStr) {
		setPath(new FilePath(PathType.ORIGINAL, pathStr));
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getBaseRev()
	 */
	public int getBaseRev() {
		return baseRev;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#setBaseRev(int)
	 */
	public void setBaseRev(int baseRev) {
		this.baseRev = baseRev;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getBaseName()
	 */
	public String getBaseName() {
		return baseName;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#setBaseName(java.lang.String)
	 */
	public void setBaseName(String baseName) {
		this.baseName = baseName;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#getBaseFile()
	 */
	public String getBaseFile() {
		return baseFile;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileSpec#setBaseFile(java.lang.String)
	 */
	public void setBaseFile(String baseFile) {
		this.baseFile = baseFile;
	}

	public int getRawCode() {
		return rawCode;
	}

	public int getUniqueCode() {
		return uniqueCode;
	}

	public int getSubCode() {
		return subCode;
	}

	public int getSubSystem() {
		return subSystem;
	}
}
