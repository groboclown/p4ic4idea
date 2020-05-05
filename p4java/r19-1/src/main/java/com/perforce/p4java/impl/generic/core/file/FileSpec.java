/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */

package com.perforce.p4java.impl.generic.core.file;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseInt;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.ACTION;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.BASENAME;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.BASE_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.BASE_REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.BLOB_SHA;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.BRANCH;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CHANGE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CLIENT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.COMMIT_SHA;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CONTENT_RESOLVE_TYPE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEFAULT;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEFAULT_CHANGE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DIR;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.ENDFROMREV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.END_TO_REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FROM_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HAVEREV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HOW;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.LOCAL_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OTHERLOCK;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OTHER_ACTION;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OURLOCK;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PATH;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REPO;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REPO_NAME;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.RESOLVE_TYPE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.SHA;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.SHELVED_CHANGE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.STARTFROMREV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.START_TO_REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.STATUS;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TIME;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TO_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TYPE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TREE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.UNMAP;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.USER;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.WORKREV;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.length;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substring;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;

import com.perforce.p4java.Log;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.ServerResource;
import com.perforce.p4java.impl.generic.core.file.FilePath.PathType;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;

/**
 * Simple generic default implementation class for the IFileSpec interface.
 */

public class FileSpec extends ServerResource implements IFileSpec {

    protected FileSpecOpStatus opStatus = FileSpecOpStatus.VALID;
    protected String statusMessage = null;
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

    protected String repoName = null;
    protected String sha = null;
    protected String branch = null;
    protected String blobSha = null;
    protected String commitSha = null;
	protected String treeSha = null;

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
	private List<String> resolveTypes = null;
    private String contentResolveType = null;

    private int shelvedChange = IChangelist.UNKNOWN;

    protected IClient client = null;

    /**
     * Default constructor. Sets all paths, labels, dates, etc. to null;
     * revisions to IFileSpec.NO_FILE_REVISION; client and server references to
     * null; changelist ID to IChangelist.UNKNOWN; opStatus to VALID; locked to
     * false, etc.
     */
    public FileSpec() {
        super(false, false);
    }

    /**
     * Given a candidate path string (which may include version and changelist
     * annotations, at least), try to construct a corresponding file spec.
     * <p>
	 * <p>
     * Effectively an alias for FileSpec(pathStr, true).
     *
	 * @param pathStr candidate path string
     */

    public FileSpec(String pathStr) {
        this(pathStr, true);
    }

    /**
     * Given a candidate path string (which may include version and changelist
     * annotations, at least), try to construct a corresponding file spec.
     * <p>
	 * <p>
     * The motivation for the hasAnnotations parameter is to allow path strings
     * to contain "@" and "#" characters; the downside of this that if there's
     * any associated annotation info, it's not parsed at all and any such
     * information must be set up manually.
     *
	 * @param pathStr          candidate path string
	 * @param parseAnnotations if true, attempt to parse the path string for revision
     *            annotations.
     */
    public FileSpec(final String pathStr, final boolean parseAnnotations) {
        super(false, false);
        originalPath = new FilePath(PathType.ORIGINAL, pathStr);
        if (parseAnnotations && isNotBlank(pathStr)
                && PathAnnotations.hasPerforceAnnotations(pathStr)) {
            PathAnnotations annotations = new PathAnnotations(pathStr);
            startRevision = annotations.getStartRevision();
            endRevision = annotations.getEndRevision();
            changeListId = annotations.getChangelistId();
            label = annotations.getLabel();
            date = annotations.getDate();
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

    public FileSpec(final FileSpecOpStatus status, final String errStr) {
        super(false, false);
        opStatus = status;
        statusMessage = errStr;
    }

    /**
     * Construct a FileSpec from an opstatus, error message, Perforce generic
     * code, and Perforce severity code.
     */
    public FileSpec(final FileSpecOpStatus status, final String errStr, final int genericCode,
            final int severityCode) {

        super(false, false);
        opStatus = status;
        statusMessage = errStr;
        this.genericCode = genericCode;
        this.severityCode = severityCode;
    }

    /**
     * Construct a new FileSpec given the op status, an error string, and a raw
     * code string returned from a Perforce server.
     */
    public FileSpec(final FileSpecOpStatus status, final String errStr, final String codeStr) {
        super(false, false);
        opStatus = status;
        statusMessage = errStr;
        try {
            setCodes(Integer.valueOf(codeStr));
        } catch (NumberFormatException thr) {
            Log.exception(thr);
        }
    }

    /**
     * Construct a new FileSpec given the op status, an error string, and a raw
     * code value returned from a Perforce server.
     */
    public FileSpec(final FileSpecOpStatus status, final String errStr, final int rawCode) {

        super(false, false);
        opStatus = status;
        statusMessage = errStr;
        setCodes(rawCode);
    }

    /**
     * Construct a new filespec from another filespec. In other words,
     * effectively clone it by deep copy of local fields.
     * <p>
     *
	 * @param impl non-null existing filespec.
     */
    public FileSpec(final FileSpec impl) {
        super(false, false);

        Validate.notNull(impl);

        opStatus = impl.opStatus;
        statusMessage = impl.statusMessage;
        genericCode = impl.genericCode;
        severityCode = impl.severityCode;
        originalPath = impl.originalPath;
        depotPath = impl.depotPath;
        clientPath = impl.clientPath;
        localPath = impl.localPath;
        fileType = impl.fileType;
        startRevision = impl.startRevision;
        endRevision = impl.endRevision;
        changeListId = impl.changeListId;
        label = impl.label;
        date = impl.date;
        action = impl.action;
        userName = impl.userName;
        clientName = impl.clientName;
        unmap = impl.unmap;
        fromFile = impl.fromFile;
        endFromRev = impl.endFromRev;
        startFromRev = impl.startFromRev;
        toFile = impl.toFile;
        endToRev = impl.endToRev;
        startToRev = impl.startToRev;
        workRev = impl.workRev;
        howResolved = impl.howResolved;
        otherAction = impl.otherAction;
        locked = impl.locked;
        diffStatus = impl.diffStatus;
        resolveType = impl.resolveType;
		resolveTypes = impl.resolveTypes;
        contentResolveType = impl.contentResolveType;
        shelvedChange = impl.shelvedChange;
        server = impl.server;
        client = impl.client;
        baseFile = impl.baseFile;
    }

    /**
     * Try to construct a FileSpec from a passed-in map as returned from a
     * Perforce server. Tuned to return values from the underlying map-based
     * server interface, which explains the index (set this to zero for normal
     * use).
     */

    public FileSpec(@Nullable final Map<String, Object> map, @Nonnull final IServer server,
            final int index) {

        super(false, false);
        if (nonNull(map)) {
            setOpStatus(FileSpecOpStatus.VALID);

            String indexStr = EMPTY;
            if (index >= 0) {
                indexStr += index;
            }
            setServer(server);
            if (map.containsKey(DIR + indexStr)) {
                setDepotPath(new FilePath(PathType.DEPOT, parseString(map, DIR + indexStr), true));
            }
            if (map.containsKey(DEPOT_FILE + indexStr)) {
                setDepotPath(new FilePath(PathType.DEPOT, parseString(map, DEPOT_FILE + indexStr),
                        true));
            }
            if (map.containsKey(CLIENT_FILE + indexStr)) {
                setClientPath(new FilePath(PathType.CLIENT,
                        parseString(map, CLIENT_FILE + indexStr), true));
            }
            if (map.containsKey(LOCAL_FILE + indexStr)) {
                setLocalPath(new FilePath(PathType.LOCAL, parseString(map, LOCAL_FILE + indexStr),
                        true));
            }
            if (map.containsKey(PATH + indexStr)) {
                setLocalPath(new FilePath(PathType.LOCAL, parseString(map, PATH + indexStr), true));
            }
            setFileType(parseString(map, TYPE + indexStr));
            setAction(FileAction.fromString(parseString(map, ACTION + indexStr)));
            setUserName(parseString(map, USER + indexStr));
            setClientName(parseString(map, "client" + indexStr));
            String cid = parseString(map, CHANGE + indexStr);
            String revStr = parseString(map, REV + indexStr);
            if (revStr == null) {
                // Sometimes it's the haveRev key...
                revStr = parseString(map, HAVEREV + indexStr);
            }

            // Get submit date from the 'time' (seconds).
            // Multiply by 1000 to get the milliseconds.
            if (nonNull(map.get(TIME))) {
                try {
                    long seconds = parseLong(map, TIME);
                    setDate(new Date(seconds * 1000));
                } catch (NumberFormatException nfe) {
                    Log.error("Error parsing the '%S' in the FileSpec constructor: %s", TIME,
                            nfe.getLocalizedMessage());
                    Log.exception(nfe);
                }
            }

            setLocked(nonNull(map.get(OURLOCK)) || nonNull(map.get(OTHERLOCK)));
            setEndRevision(getRevFromString(revStr));

            if (isBlank(cid)) {
                setChangelistId(IChangelist.UNKNOWN);
            } else if (DEFAULT.equalsIgnoreCase(cid) || DEFAULT_CHANGE.equalsIgnoreCase(cid)) {
                setChangelistId(IChangelist.DEFAULT);
            } else {
                // Sometimes in format "change nnnnnn", sometimes just "nnnnn".
                // Urgh...
                int i = indexOf(cid, SPACE);
                if (i < 0) {
                    setChangelistId(Integer.valueOf(cid));
                } else {
                    setChangelistId(Integer.valueOf(substring(cid, i + 1)));
                }
            }

            setEndFromRev(getRevFromString(parseString(map, ENDFROMREV + indexStr)));
            setStartFromRev(getRevFromString(parseString(map, STARTFROMREV + indexStr)));
            setWorkRev(getRevFromString(parseString(map, WORKREV + indexStr)));

            setHowResolved(parseString(map, HOW));
            setFromFile(parseString(map, FROM_FILE + indexStr));

            setEndToRev(getRevFromString(parseString(map, END_TO_REV + indexStr)));
            setStartToRev(getRevFromString(parseString(map, START_TO_REV + indexStr)));
            setToFile(parseString(map, TO_FILE + indexStr));

            setBaseRev(getRevFromString(parseString(map, BASE_REV + indexStr)));
            setBaseName(parseString(map, BASENAME + indexStr));
            setBaseFile(parseString(map, BASE_FILE + indexStr));

            setOtherAction(FileAction.fromString(parseString(map, OTHER_ACTION + indexStr)));
            setDiffStatus(parseString(map, STATUS));

            setResolveType(parseString(map, RESOLVE_TYPE));

			int r = 0;
			List<String> rTypes = new ArrayList<>();
			while(map.containsKey(RESOLVE_TYPE + r)) {
				rTypes.add(parseString(map, RESOLVE_TYPE + r));
				r++;
			}
			setResolveTypes(rTypes);

            setContentResolveType(parseString(map, CONTENT_RESOLVE_TYPE));

            if (map.containsKey(SHELVED_CHANGE)) {
                try {
                    setShelvedChange(parseInt(map, SHELVED_CHANGE));
                } catch (NumberFormatException nfe) {
                    Log.error("Error parsing the 'shelvedChange' in the FileSpec constructor: %s",
                            nfe.getLocalizedMessage());
                    Log.exception(nfe);
                }
            }
            setUnmap(nonNull(map.get(UNMAP + indexStr)));

            // Graph output
            setRepoName(parseString(map, REPO_NAME));
            setSha(parseString(map, SHA));
            setBranch(parseString(map, BRANCH));
            setBlobSha(parseString(map, BLOB_SHA));
            setCommitSha(parseString(map, COMMIT_SHA));
			setTreeSha(parseString(map, TREE));
            setRepoName(parseString(map, REPO));
        }
    }

    /**
     * Set the various error codes for this FileSpec to a value returned from
     * the server or the RPC layer. Use this if you're hand-constructing a new
     * FileSpec for an error condition and you have the raw code.
     */
    public FileSpec setCodes(final int rawCode) {
        this.rawCode = rawCode;
        subCode = (rawCode & 0x3FF);
        subSystem = ((rawCode >> 10) & 0x3F);
        uniqueCode = (rawCode & 0xFFFF);
        genericCode = ((rawCode >> 16) & 0xFF);
        severityCode = ((rawCode >> 28) & 0x00F);
        return this;
    }

    @Override
    public FilePath getPath(@Nullable final PathType pathType) {
        if (nonNull(pathType)) {
            switch (pathType) {
            case DEPOT:
                return depotPath;
            case CLIENT:
                return clientPath;
            case LOCAL:
                return localPath;
            default:
                break;
            }
        }

        return originalPath;
    }

    @Override
    public void setPath(@Nullable final FilePath filePath) {
        if (nonNull(filePath)) {
            if (nonNull(filePath.getPathType())) {
                switch (filePath.getPathType()) {
                case DEPOT:
                    depotPath = filePath;
                    return;
                case CLIENT:
                    clientPath = filePath;
                    return;
                case LOCAL:
                    localPath = filePath;
                    return;
                default:
                    break;
                }
            }
        }

        originalPath = filePath;
    }

    @Override
    public FileAction getAction() {
        return action;
    }

    @Override
    public int getChangelistId() {
        return changeListId;
    }

    @Override
    public String getClientName() {
        return clientName;
    }

    @Override
    public FilePath getClientPath() {
        return getPath(PathType.CLIENT);
    }

    @Override
    public InputStream getContents(boolean noHeaderLine)
            throws ConnectionException, RequestException, AccessException {
        checkServer();
        List<IFileSpec> fList = new ArrayList<>();
        fList.add(this);

        return server.getFileContents(fList, false, noHeaderLine);
    }

    @Override
    public InputStream getContents(GetFileContentsOptions opts) throws P4JavaException {
        checkServer();
        List<IFileSpec> fList = new ArrayList<>();
        fList.add(this);

        return ((IOptionsServer) server).getFileContents(fList, opts);
    }

    @Override
    public FilePath getDepotPath() {
        return getPath(PathType.DEPOT);
    }

    @Override
    public int getEndRevision() {
        return endRevision;
    }

    @Override
    public String getFileType() {
        return fileType;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public FilePath getLocalPath() {
        return getPath(PathType.LOCAL);
    }

    @Override
    public FileSpecOpStatus getOpStatus() {
        return opStatus;
    }

    @Override
    public FilePath getPreferredPath() {
        if (nonNull(originalPath)) {
            return originalPath;
        } else if (nonNull(depotPath)) {
            return depotPath;
        } else if (nonNull(clientPath)) {
            return clientPath;
        } else if (nonNull(localPath)) {
            return localPath;
        }

        return null;
    }

    @Override
    public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(final int maxRevs,
            final boolean contentHistory, final boolean includeInherited, final boolean longOutput,
            final boolean truncatedLongOutput)
            throws ConnectionException, RequestException, AccessException {

        checkServer();
        return server.getRevisionHistory(
                FileSpecBuilder.makeFileSpecList(getAnnotatedPreferredPathString()), maxRevs,
                contentHistory, includeInherited, longOutput, truncatedLongOutput);
    }

    @Override
    public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(
            GetRevisionHistoryOptions opts) throws P4JavaException {
        checkServer();
        return ((IOptionsServer) server).getRevisionHistory(
                FileSpecBuilder.makeFileSpecList(getAnnotatedPreferredPathString()), opts);
    }

    @Override
    public List<IFileAnnotation> getAnnotations(final DiffType wsOptions, final boolean allResults,
            final boolean useChangeNumbers, final boolean followBranches)
            throws ConnectionException, RequestException, AccessException {

        checkServer();
        List<IFileSpec> specList = new ArrayList<>();
        specList.add(this);
        return server.getFileAnnotations(specList, wsOptions, allResults, useChangeNumbers,
                followBranches);
    }

    @Override
    public List<IFileAnnotation> getAnnotations(GetFileAnnotationsOptions opts)
            throws P4JavaException {
        checkServer();
        List<IFileSpec> specList = new ArrayList<>();
        specList.add(this);
        return ((IOptionsServer) server).getFileAnnotations(specList, opts);
    }

    @Override
    public List<IFileSpec> move(final int changelistId, final boolean listOnly,
            final boolean noClientMove, final String fileType, final IFileSpec toFile)
            throws ConnectionException, RequestException, AccessException {

        checkServer();
        return server.moveFile(changelistId, listOnly, noClientMove, fileType, this, toFile);
    }

    @Override
    public List<IFileSpec> move(final IFileSpec toFile, final MoveFileOptions opts)
            throws P4JavaException {
        checkServer();
        return ((IOptionsServer) server).moveFile(this, toFile, opts);
    }

    private void checkServer() throws P4JavaError {
        Validate.notNull(server);
        if (!(server instanceof IOptionsServer)) {
            // This should be impossible, but you never know... -- HR.
            throw new P4JavaError("File specification is not associated with an options server");
        }
    }

    @Override
    public int getStartRevision() {
        return startRevision;
    }

    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public int getSeverityCode() {
        return severityCode;
    }

    @Override
    public int getGenericCode() {
        return genericCode;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public String getDiffStatus() {
        return diffStatus;
    }

    @Override
    public String getResolveType() {
        return resolveType;
    }

    @Override
    public String getContentResolveType() {
        return contentResolveType;
    }

    @Override
    public int getShelvedChange() {
        return shelvedChange;
    }

    public void setOpStatus(FileSpecOpStatus opStatus) {
        this.opStatus = opStatus;
    }

    public void setStatusMessage(String statusMessage) {
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

    @Override
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    @Override
    public void setStartRevision(int startRevision) {
        this.startRevision = startRevision;
    }

    @Override
    public void setEndRevision(int endRevision) {
        this.endRevision = endRevision;
    }

    @Override
    public void setChangelistId(int changeListId) {
        this.changeListId = changeListId;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public void setAction(FileAction action) {
        this.action = action;
    }

    @Override
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setClient(IClient client) {
        this.client = client;
    }

    @Override
    public int getEndFromRev() {
        return endFromRev;
    }

    @Override
    public int getEndToRev() {
        return endToRev;
    }

    @Override
    public String getFromFile() {
        return fromFile;
    }

    @Override
    public String getHowResolved() {
        return howResolved;
    }

    @Override
    public FileAction getOtherAction() {
        return otherAction;
    }

    @Override
    public int getStartFromRev() {
        return startFromRev;
    }

    @Override
    public int getStartToRev() {
        return startToRev;
    }

    @Override
    public String getToFile() {
        return toFile;
    }

    @Override
    public int getWorkRev() {
        return workRev;
    }

    @Override
    public boolean isUnmap() {
        return unmap;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public void setFromFile(String fromFile) {
        this.fromFile = fromFile;
    }

    @Override
    public void setEndFromRev(int endFromRev) {
        this.endFromRev = endFromRev;
    }

    @Override
    public void setStartFromRev(int startFromRev) {
        this.startFromRev = startFromRev;
    }

    @Override
    public void setToFile(String toFile) {
        this.toFile = toFile;
    }

    @Override
    public void setEndToRev(int endToRev) {
        this.endToRev = endToRev;
    }

    @Override
    public void setStartToRev(int startToRev) {
        this.startToRev = startToRev;
    }

    @Override
    public void setWorkRev(int workRev) {
        this.workRev = workRev;
    }

    @Override
    public void setHowResolved(String howResolved) {
        this.howResolved = howResolved;
    }

    @Override
    public void setOtherAction(FileAction otherAction) {
        this.otherAction = otherAction;
    }

    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public void setDiffStatus(String diffStatus) {
        this.diffStatus = diffStatus;
    }

    @Override
    public void setResolveType(String resolveType) {
        this.resolveType = resolveType;
    }

    @Override
    public void setContentResolveType(String contentResolveType) {
        this.contentResolveType = contentResolveType;
    }

    @Override
    public void setShelvedChange(int shelvedChange) {
        this.shelvedChange = shelvedChange;
    }

    @Override
    public void setUnmap(boolean unmap) {
        this.unmap = unmap;
    }

    @Override
    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    @Override
    public void setSha(String sha) {
        this.sha = sha;
    }

    @Override
    public void setBranch(String branch) {
        this.branch = branch;
    }

    @Override
    public String getBlobSha() {
        return blobSha;
    }

    @Override
    public void setBlobSha(String sha) {
        blobSha = sha;
    }

    @Override
    public String getCommitSha() {
        return commitSha;
    }

    @Override
    public void setCommitSha(String sha) {
        commitSha = sha;
    }

	@Override
	public String getTreeSha() {
		return treeSha;
	}

	@Override
	public void setTreeSha(String sha) {
		treeSha = sha;
	}

    public static int getRevFromString(String str) {
        int rev = NO_FILE_REVISION;

        if (isNotBlank(str)) {
            // can be in #rev or rev form, unfortunately...
            if (contains(str, "head")) {
                return HEAD_REVISION;
            }

            if (contains(str, "none")) {
                return NO_FILE_REVISION;
            }

            try {
                int length = length(str);
                if (startsWith(str, "#") && length > 1) {
                    rev = Integer.valueOf(substring(str, 1));
                } else if (length > 0) {
                    rev = Integer.valueOf(str);
                }
            } catch (Exception exc) {
                Log.error("Conversion error in FileSpec.getRevFromString: %s",
                        exc.getLocalizedMessage());
                Log.exception(exc);
            }
        }
        return rev;
    }

    @Override
    public String getClientPathString() {
        return getPathString(PathType.CLIENT);
    }

    @Override
    public String getDepotPathString() {
        return getPathString(PathType.DEPOT);
    }

    @Override
    public String getLocalPathString() {
        return getPathString(PathType.LOCAL);
    }

    @Override
    public FilePath getOriginalPath() {
        if (nonNull(getPath(PathType.ORIGINAL))) { // See job061945
            return getPath(PathType.ORIGINAL);
        } else { // API backward compatibility - See job070533
            return getPath(PathType.CLIENT);
        }
    }

    @Override
    public String getOriginalPathString() {
        if (nonNull(getPathString(PathType.ORIGINAL))) { // See job061945
            return getPathString(PathType.ORIGINAL);
        } else { // API backward compatibility - See job070533
            return getPathString(PathType.CLIENT);
        }
    }

    @Override
    public String getPathString(PathType pathType) {
        FilePath fPath = getPath(pathType);

        if (nonNull(fPath)) {
            return fPath.toString();
        }
        return null;
    }

    @Override
    public String getAnnotatedPathString(PathType pathType) {
        FilePath path = getPath(pathType);
        if (nonNull(path)) {
            return path.annotate(this);
        }

        return null;
    }

    @Override
    public String getPreferredPathString() {
        FilePath prefPath = getPreferredPath();

        if (nonNull(prefPath) && nonNull(prefPath.getPathString())) {
            return prefPath.toString();
        }

        return null;
    }

    @Override
    public String getAnnotatedPreferredPathString() {
        FilePath prefPath = getPreferredPath();

        if (nonNull(prefPath) && nonNull(prefPath.getPathString())) {
            return prefPath.annotate(this);
        }

        return null;
    }

    @Override
    public String toString() {
        String usefulDescription = getAnnotatedPreferredPathString();
        if ( usefulDescription == null && statusMessage != null ) {
            usefulDescription = opStatus + ": "+ statusMessage;
        }
		if (usefulDescription == null && repoName != null) {
			usefulDescription = repoName + ": " + sha;
		}
        return usefulDescription;
    }

    @Override
    public void setPathFromString(PathType pathType, String pathStr) {
        FilePath path = new FilePath(pathType, pathStr);
        setPath(path);
    }

    @Override
    public Date getDate() {
        return this.date;
    }

    @Override
    public void setClientPath(String pathStr) {
        setPath(new FilePath(PathType.CLIENT, pathStr));
    }

    @Override
    public void setDepotPath(String pathStr) {
        setPath(new FilePath(PathType.DEPOT, pathStr));
    }

    @Override
    public void setLocalPath(String pathStr) {
        setPath(new FilePath(PathType.LOCAL, pathStr));
    }

    @Override
    public void setOriginalPath(String pathStr) {
        setPath(new FilePath(PathType.ORIGINAL, pathStr));
    }

    @Override
    public int getBaseRev() {
        return baseRev;
    }

    @Override
    public void setBaseRev(int baseRev) {
        this.baseRev = baseRev;
    }

    @Override
    public String getBaseName() {
        return baseName;
    }

    @Override
    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    @Override
    public String getBaseFile() {
        return baseFile;
    }

    @Override
    public void setBaseFile(String baseFile) {
        this.baseFile = baseFile;
    }

    @Override
    public int getRawCode() {
        return rawCode;
    }

    @Override
    public int getUniqueCode() {
        return uniqueCode;
    }

    @Override
    public int getSubCode() {
        return subCode;
    }

    @Override
    public int getSubSystem() {
        return subSystem;
    }

    @Override
    public String getRepoName() {
        return repoName;
    }

    @Override
    public String getSha() {
        return sha;
    }

    @Override
    public String getBranch() {
        return branch;
    }

	@Override
	public List<String> getResolveTypes() {
		return resolveTypes;
	}

	@Override
	public void setResolveTypes(List<String> types) {
		this.resolveTypes = types;
	}
}
