/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv.RpcHandler;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.rpc.RpcServer;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientMessage.ClientMessageId;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5Digester;
import com.perforce.p4java.impl.mapbased.rpc.handles.ClientFile;
import com.perforce.p4java.impl.mapbased.rpc.handles.ReconcileHandle;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherResult;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceDigestType;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFile;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.FilePathHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.mapapi.MapFlag;
import com.perforce.p4java.mapapi.MapTable;
import com.perforce.p4java.mapapi.MapTableT;
import com.perforce.p4java.mapapi.MapWrap;

/**
 * Implements the simpler lower-level file commands that typically
 * correspond to system commands such as chmod, delete, etc.
 * 
 *
 */

public class ClientSystemFileMatchCommands {
	
	public static final String TRACE_PREFIX = "ClientSystemFileCommands";
	
	public static final String DEFAULT_TMPFILE_PFX = "p4j";
	public static final String DEFAULT_TMPFILE_SFX = ".p4j";
	
	public static final String SYSTEM_TMPDIR_PROPS_KEY = "java.io.tmpdir";
	public static final String SYSTEM_TMPDIR_DEFAULT = "/tmp";
	
	public static final String PERMS_RW = "rw";
	
	// Reconcile handler map key for 'skipAdd'
	protected static final String RECONCILE_HANDLE = "skipAdd";
	
	private Properties props = null;
	private RpcServer server = null;
	private ClientIgnoreChecker checker = null;
	
	private String tmpDirName = null;
	
	protected ClientSystemFileMatchCommands(Properties props, RpcServer server) {
		this.props = props;
		this.server = server;
		this.tmpDirName = RpcPropertyDefs.getProperty(this.props,
							PropertyDefs.P4JAVA_TMP_DIR_KEY,
									System.getProperty(SYSTEM_TMPDIR_PROPS_KEY));
		
		if (tmpDirName == null) {
			// This can really only happen if someone has nuked or played with
			// the JVM's system props before we get here... the default will
			// work for most non-Windows boxes in most cases, and may not be
			// needed in many cases anyway.
			
			tmpDirName = SYSTEM_TMPDIR_DEFAULT;
			
			Log.warn("Unable to get tmp name from P4 props or System; using "
					+ tmpDirName + " instead");
			
		}
	}
	
	void openMatch(RpcConnection rpcConnection,
            CommandEnv cmdEnv, Map<String, Object> resultsMap, ClientFile cfile) throws ConnectionException {
	    
	    // Follow on from clientOpenFile, not called by server directly.

	    // Grab RPC vars and attach them to the file handle so that
	    // clientCloseMatch can use them for N-way diffing.

	    String fromFile = (String) resultsMap.get(RpcFunctionMapKey.FROM_FILE);
	    String key      = (String) resultsMap.get(RpcFunctionMapKey.KEY);
	    String flags    = (String) resultsMap.get(RpcFunctionMapKey.DIFF_FLAGS);
	    
	    if( fromFile == null || key == null ) {
	        throw new NullPointerException("Missing fromFile or key");
	    }

	    cfile.getMatchDict().put( RpcFunctionMapKey.FROM_FILE , fromFile );
	    cfile.getMatchDict().put( RpcFunctionMapKey.KEY, key );
	    if( flags != null ) {
	        cfile.getMatchDict().put( RpcFunctionMapKey.DIFF_FLAGS, flags );
	    }
	    
	    for( int i = 0 ; ; i++ ) {
	        String index = (String) resultsMap.get(RpcFunctionMapKey.INDEX + i);
	        String file  = (String) resultsMap.get(RpcFunctionMapKey.TO_FILE + i );
	        if (index == null || file == null) {
	            break;
	        }
	        cfile.getMatchDict().put( RpcFunctionMapKey.INDEX + i, index );
	        cfile.getMatchDict().put( RpcFunctionMapKey.TO_FILE + i, file );
	    }
	}
	
	void closeMatch(RpcConnection rpcConnection,
            CommandEnv cmdEnv, Map<String, Object> resultsMap, ClientFile cfile) throws ConnectionException {

        if (rpcConnection == null) {
            throw new NullPointerError("Null rpcConnection in convertFile().");
        }
        if (cmdEnv == null) {
            throw new NullPointerError("Null cmdEnv in convertFile().");
        }
        if (resultsMap == null) {
            throw new NullPointerError("Null resultsMap in convertFile().");
        }
        
        // Follow on from clientCloseFile, not called by server directly.

	    // Compare temp file to existing client files.  Figure out the
	    // best match, along with a quantitative measure of how good
	    // the match was (lines matched vs total lines).  Stash it
	    // in the handle so clientAckMatch can report it back.

	    //String matchFile = null;
	    //String matchIndex = null;

	    /* XXX: still to port
	    String fname = null;
	    FileSys *f2 = 0;
	    DiffFlags flags;
	    
	    String diffFlags = cfile.getMatchDict().get( RpcFunctionMapKey.DIFF_FLAGS );
	    if (diffFlags != null) {
	        flags.Init( diffFlags );
	    }

	    int bestNum = 0;
	    int bestSame = 0; 
	    int totalLines = 0;

	    for( int i = 0 ; 
	         fname = cfile.getMatchDict().get( RpcFunctionMapKey.TO_FILE + i ) ;
	         i++ ) {
	        delete f2;

	        f2 = client->GetUi()->File( f1->file->GetType() );
	        f2->SetContentCharSetPriv( f1->file->GetContentCharSetPriv() );
	        f2->Set( *fname );

	        if( e->Test() || !f2 ) {
    	        // don't care
    	        e->Clear();
    	        continue;
	        }

	        Sequence s1( f1->file, flags, e );
	        Sequence s2( f2,       flags, e );
	        if ( e->Test() )
	        {
    	        // still don't care
    	        e->Clear();
    	        continue;
	        }

	        DiffAnalyze diff( &s1, &s2 );

	        int same = 0;
	        for( Snake *s = diff.GetSnake() ; s ; s = s->next ) {
    	        same += ( s->u - s->x );
    	        if( s->u > totalLines ) {
    	            totalLines = s->u;
    	        }
	        }

	        if( same > bestSame )
	        {
    	        bestNum = i;
    	        bestSame = same;
	        }
	    }

	    delete f2;
	    f1->file->Close( e );

	    totalLines++; // snake lines start at zero

	    if( bestSame != 0 ) {
	        cfile.getMatchDict().put( RpcFunctionMapKey.INDEX,
	                cfile.getMatchDict().get( RpcFunctionMapKey.INDEX.toString() + bestNum ) );
	        cfile.getMatchDict().put( RpcFunctionMapKey.TO_FILE,
	                cfile.getMatchDict().get( RpcFunctionMapKey.TO_FILE.toString() + bestNum ) );
	        
	        cfile.getMatchDict().put( RpcFunctionMapKey.LOWER, bestSame );
	        cfile.getMatchDict().put( RpcFunctionMapKey.UPPER, totalLines );
	    }
*/
	    // clientAckMatch will send this back
	}

    protected RpcPacketDispatcherResult ackMatch(RpcConnection rpcConnection,
            CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

        if (rpcConnection == null) {
            throw new NullPointerError("Null rpcConnection in convertFile().");
        }
        if (cmdEnv == null) {
            throw new NullPointerError("Null cmdEnv in convertFile().");
        }
        if (resultsMap == null) {
            throw new NullPointerError("Null resultsMap in convertFile().");
        }

        String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
        String clientHandle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
        
        // Get handle.
    
        RpcHandler handler = cmdEnv.getHandler(clientHandle);
        ClientFile cfile = new ClientFile(handler);
    
        // Fire everything back.
    
        String fromFile = cfile.getMatchDict().get( RpcFunctionMapKey.FROM_FILE );
        String key      = cfile.getMatchDict().get( RpcFunctionMapKey.KEY );
        String toFile   = cfile.getMatchDict().get( RpcFunctionMapKey.TO_FILE );
        String index    = cfile.getMatchDict().get( RpcFunctionMapKey.INDEX );
        String lower    = cfile.getMatchDict().get( RpcFunctionMapKey.LOWER );
        String upper    = cfile.getMatchDict().get( RpcFunctionMapKey.UPPER );
    
        if( fromFile != null && key != null ) {
            resultsMap.put(RpcFunctionMapKey.FROM_FILE, fromFile);
            resultsMap.put(RpcFunctionMapKey.KEY, key);
        } else {
            throw new RuntimeException("Required parameter 'fromFile/key' not set!");
        }
        
        if( toFile != null && index != null && lower != null && upper != null ) {
            resultsMap.put( RpcFunctionMapKey.TO_FILE, toFile );
            resultsMap.put(  RpcFunctionMapKey.INDEX,  index );
            resultsMap.put(  RpcFunctionMapKey.LOWER,  lower );
            resultsMap.put(  RpcFunctionMapKey.UPPER,  upper );
        }
    
        rpcConnection.clientConfirm(confirm, resultsMap);
        
        return RpcPacketDispatcherResult.CONTINUE_LOOP;
    }

    protected RpcPacketDispatcherResult exactMatch(
            RpcConnection rpcConnection, CommandEnv cmdEnv,
            Map<String, Object> resultsMap) throws ConnectionException {

        if (rpcConnection == null) {
            throw new NullPointerError("Null rpcConnection in exactMatch().");
        }
        if (cmdEnv == null) {
            throw new NullPointerError("Null cmdEnv in exactMatch().");
        }
        if (resultsMap == null) {
            throw new NullPointerError("Null resultsMap in exactMatch().");
        }
        
        // Compare existing digest to list of
        // new client files, return match, or not.
    
        // Args:
        // type     = existing file type (clientpart)
        // digest   = existing file digest
        // fileSize = existing file size
        // charSet  = existing file charset
        // toFileN  = new file local path
        // indexN   = new file index
        // confirm  = return callback
        //
        // Return:
        // toFile   = exact match
        // index    = exact match
    
        cmdEnv.newHandler();
        String clientType = (String) resultsMap.get( RpcFunctionMapKey.TYPE );
        String digest = (String) resultsMap.get( RpcFunctionMapKey.DIGEST );
        String confirm = (String) resultsMap.get( RpcFunctionMapKey.CONFIRM );
    
        if( confirm == null ) {
            throw new NullPointerError("No confirm value.");
        }

        String matchFile = null;
        String matchIndex = null;
    
        for( int i = 0 ; 
             resultsMap.containsKey( RpcFunctionMapKey.TO_FILE + i );
             i++ ) {
    
            File f = new File((String) resultsMap.get( RpcFunctionMapKey.TO_FILE + i ));
            RpcPerforceFileType fileType = RpcPerforceFileType.decodeFromServerString(clientType);
            // If we encounter a problem with a file, we just don't return
            // it as a match.  No need to blat out lots of errors.
    
            if (f == null || digest == null) {
                continue;
            }
    
            // Skip files that are symlinks when we
            // aren't looking for symlinks.
    
            if( !( f.exists() || RpcPerforceFileType.isProbablySymLink(f) )
                || ( !RpcPerforceFileType.isProbablySymLink(f) && fileType == RpcPerforceFileType.FST_SYMLINK )  
                    || ( RpcPerforceFileType.isProbablySymLink(f) && fileType != RpcPerforceFileType.FST_SYMLINK ) ) {
                continue;
            }
    
            String localDigest = digestFile(f, fileType, rpcConnection.getClientCharset());
    
            if( localDigest == null ) {
                continue;
            }
    
            if( !localDigest.equals( digest ) ) {
                matchFile  = (String) resultsMap.get( RpcFunctionMapKey.TO_FILE + i );
                matchIndex = (String) resultsMap.get( RpcFunctionMapKey.INDEX + i );
                break; // doesn't get any better
            }
        }
    
        if( matchFile != null && matchIndex != null ) {
            resultsMap.put( RpcFunctionMapKey.TO_FILE, matchFile );
            resultsMap.put( RpcFunctionMapKey.INDEX, matchIndex );
        }

        rpcConnection.clientConfirm( confirm, resultsMap );
        
        return RpcPacketDispatcherResult.CONTINUE_LOOP;
    }
	
	/**
	 * "inquire" about file, for 'p4 reconcile' <p>
	 * 
	 * This routine performs clientCheckFile's scenario 1 checking, but also
	 * saves the list of files that are in the depot so they can be compared to
	 * the list of files on the client when reconciling later for add.
	 */
	protected RpcPacketDispatcherResult reconcileEdit(
			RpcConnection rpcConnection, CommandEnv cmdEnv,
			Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in reconcileEdit().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in reconcileEdit().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in reconcileEdit().");
		}

		String clientType = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String digest     = (String) resultsMap.get(RpcFunctionMapKey.DIGEST);
		String digestType = (String) resultsMap.get(RpcFunctionMapKey.DIGESTTYPE);
		String confirm    = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
        String fileSize   = (String) resultsMap.get(RpcFunctionMapKey.FILESIZE);
        String submitTime = (String) resultsMap.get(RpcFunctionMapKey.TIME);
		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH);

		ReconcileHandle recHandle = getReconcileHandle(cmdEnv);

        long checkSize = 0;
        long time = 0;
        try {
            checkSize = Long.parseLong(fileSize);
        } catch (NumberFormatException nfe) { }
        try {
            time = Long.parseLong(submitTime);
        } catch (NumberFormatException nfe) { }
		
        List<String> skipFilesMap = recHandle.getSkipFiles();
        
		String status = "exists";
		String nType = (clientType == null) ? "text" : clientType;

		RpcPerforceFileType fileType = RpcPerforceFileType.decodeFromServerString(clientType);
		boolean fstSymlink = (fileType == RpcPerforceFileType.FST_SYMLINK);

		/*
	     * If we do know the type, we want to know if it's missing.
	     * If it isn't missing and a digest is given, we want to know if
	     * it is the same.
	     */

		File file = new File(clientPath);

		if (!fileExists(file, true)) {
			status = "missing";
			recHandle.incrementDelCount();
		} else if ( ( !RpcPerforceFileType.isProbablySymLink(file) &&  fstSymlink ) ||
				    ( RpcPerforceFileType.isProbablySymLink(file)  && !fstSymlink ) ) {
			skipFilesMap.add(file.getAbsolutePath());
		} else if (digest != null) {
			// Calculate actual file digest; if same, we assume the file's
			// the same as on the server.

			if( digestType != null )
			{
				String digestStr = rpcConnection.getDigest(fileType, file, RpcPerforceDigestType.GetType(digestType));
				if ((digestStr != null) && digestStr.equals(digest)) {
					status = "same";
				}
			}
			// If file size is known and differs, skip digest.
			// If file size is known and the same, compute digest.
			// If file size is unknown, compute digest.
			else if ((checkSize == 0) || (file.length() == checkSize)) {

				// If the submit time is provided (i.e. with -m option), then
				// compare the file mtime and possibly bypass the digest.
				long fileModTime = file.lastModified() / 1000;
				if (time == 0 || time != fileModTime) {
					String digestStr = rpcConnection.getDigest(fileType, file);
					if ((digestStr != null) && digestStr.equals(digest)) {
						status = "same";
					}
				} else if (time != 0) {
					status = "same";
				}
			}
		}

		// Now construct a suitable response for the server; this
		// means copying the incoming args, appending or changing
		// "type" and "status" if necessary, and changing the
		// function type to server-ReconcileFile.
		
		resultsMap.put(RpcFunctionMapKey.TYPE, nType);
		resultsMap.put(RpcFunctionMapKey.STATUS, status);

		return rpcConnection.clientConfirm(confirm, resultsMap);
	}
	
	private ReconcileHandle getReconcileHandle(CommandEnv cmdEnv) {
	    RpcHandler handler = cmdEnv.getHandler(RECONCILE_HANDLE);
        if (handler == null) {
            handler = cmdEnv.new RpcHandler(RECONCILE_HANDLE, false, null);
            cmdEnv.addHandler(handler);
        }
        return new ReconcileHandle(handler);
    }

    /**
	 * Reconcile add confirm - scans the directory (local syntax) and returns
	 * files in the directory using the full path. This supports traversing
	 * sub-directories.<p>
	 */
	protected RpcPacketDispatcherResult reconcileAdd(
			RpcConnection rpcConnection, CommandEnv cmdEnv,
			Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in reconcileAdd().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in reconcileAdd().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in reconcileAdd().");
		}

		String dir = (String) resultsMap.get(RpcFunctionMapKey.DIR);
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		String traverse = (String) resultsMap.get(RpcFunctionMapKey.TRAVERSE);
        String summary = (String) resultsMap.get(RpcFunctionMapKey.SUMMARY);
        String skipIgnore = (String) resultsMap.get(RpcFunctionMapKey.SKIP_IGNORE);
        String skipCurrent = (String) resultsMap.get(RpcFunctionMapKey.SKIP_CURRENT);
        String sendDigest = (String) resultsMap.get(RpcFunctionMapKey.SEND_DIGEST);

		if (dir == null) {
			throw new NullPointerError("Null 'dir' in resultsMap in reconcileAdd().");
		}
		if (confirm == null) {
			throw new NullPointerError("Null 'confirm' in resultsMap in reconcileAdd().");
		}
        
        boolean isTraverse = (traverse != null && !traverse.equalsIgnoreCase("0")) ? true : false;
        boolean isSummary = (summary != null && !summary.equalsIgnoreCase("0")) ? true : false;
        boolean isSkipIgnore = (skipIgnore != null && !skipIgnore.equalsIgnoreCase("0")) ? true : false;
        boolean isSkipCurrent = (skipCurrent != null && !skipCurrent.equalsIgnoreCase("0")) ? true : false;
        boolean isSendDigest = (sendDigest != null && !sendDigest.equalsIgnoreCase("0")) ? true : false;

		MapTable map = new MapTable();
		List<String> files = new LinkedList<String>();
		Map<String,Long> sizes = new HashMap<String, Long>();
		List<String> dirs = new LinkedList<String>();
		List<String> depotFiles = new LinkedList<String>();
		Map<String,String> digests = new HashMap<String, String>();

		// Construct a MapTable object from the strings passed in by server

		for (int i = 0; resultsMap.get(RpcFunctionMapKey.MAP_TABLE + i) != null; i++) {
			String entry = (String)resultsMap.get(RpcFunctionMapKey.MAP_TABLE + i);
			if (entry != null) {
				MapFlag flag = MapFlag.MfMap;
				if( entry.startsWith( "-" ) ) {
					flag = MapFlag.MfUnmap;
				} else if( entry.startsWith( "+" ) ) {
					flag = MapFlag.MfRemap;
				} else if( entry.startsWith( "&" ) ) {
					flag = MapFlag.MfAndmap;
				}
				if( flag != MapFlag.MfMap )
					entry = entry.substring(1);
				map.insert(entry, entry, flag );
			}
		}
		
		// If we have a list of files we know are in the depot already,
	    // filter them out of our list of files to add. For -s option,
	    // we need to have this list of depot files for computing files
	    // and directories to add (even if it is an empty list).
		
		ReconcileHandle recHandle = null;
        RpcHandler handler = cmdEnv.getHandler(RECONCILE_HANDLE);
        if (handler != null) {
            //TODO: Do we need to sort the paths?
			//recHandle->pathArray->Sort( !StrBuf::CaseUsage() );
        } else if (handler == null && isSummary ) {
            handler = cmdEnv.new RpcHandler(RECONCILE_HANDLE, false, null);
            cmdEnv.addHandler(handler);
            recHandle = new ReconcileHandle(handler);
        } 
		
        // status -s also needs the list of files opened for add appended
        // to the list of depot files.

        if( isSummary )
        {
            for( int j=0; resultsMap.containsKey( RpcFunctionMapKey.DEPOT_FILES + j); j++) {
                depotFiles.add( (String) resultsMap.get( RpcFunctionMapKey.DEPOT_FILES + j) );
            }
            if( recHandle != null ) {
                for( String fname : recHandle.getSkipFiles() ) {
                    depotFiles.add( fname );
                }
            }
			//TODO: Do we need to sort the paths?
			//depotFiles->Sort( !StrBuf::CaseUsage() );
        }

        // status -s will output files in the current directory and paths
        // rather than all of the files individually. Compare against depot
        // files early so we can abort traversal early if we can.

        int hasIndex = 0;
        //TODO: still to translate
        //const char *config = client->GetEnviro()->Get( "P4CONFIG" );
        
        if( isSummary ) {
            AtomicInteger idx = new AtomicInteger(0);
			AtomicInteger ddx = new AtomicInteger(0);
            traverseShort( resultsMap, new File(dir), new File(dir), isTraverse, isSkipIgnore,
					       true, false, isSkipCurrent,
					       map, files, dirs, idx, depotFiles, ddx,
                           rpcConnection.isUnicodeServer(),
                           rpcConnection.getClientCharset(), cmdEnv);
        } else {
    		traverseDirs( new File(dir), isTraverse, isSkipIgnore, isSendDigest, map, files,
        				  sizes, digests, hasIndex, recHandle != null ? recHandle.getSkipFiles() : null,
        				  rpcConnection.isUnicodeServer(),
        				  rpcConnection.getClientCharset(), cmdEnv);
        }

		// Compare list of files on client with list of files in the depot
	    // if we have this list from ReconcileEdit. Skip this comparison
	    // if summary because it was done already.

        int j = 0;
	    if( recHandle != null && !isSummary ) {
            for (String file : files) {
    	        if( recHandle.getSkipFiles().contains(file)) {
    	            continue;
    	        }
    	        
                resultsMap.put(RpcFunctionMapKey.FILE + j, file);

	            if( !isSendDigest && recHandle.getDelCount() > 0 ) {
    	            // Deleted files?  Send filesize info so the
    	            // server can try to pair up moves.
	                
	                resultsMap.put(RpcFunctionMapKey.FILESIZE + j, "" + sizes.get(file) );
	            }
	            if( isSendDigest ) {
                    resultsMap.put(RpcFunctionMapKey.DIGEST + j, digests.get(file) );
                }
                j++;
	        }
	    } else {
	        for (String file : files) {
                resultsMap.put(RpcFunctionMapKey.FILE + j, file);

	            if( isSendDigest ) {
	                resultsMap.put(RpcFunctionMapKey.DIGEST + j, digests.get(file) );
	            }
	            j++;
	        }
	    }
		
		return rpcConnection.clientConfirm(confirm, resultsMap);
	}

	/**
	 * Reconcile flush - remove the skip add files map from the reconcile handler.
	 */
	protected RpcPacketDispatcherResult reconcileFlush(
			RpcConnection rpcConnection, CommandEnv cmdEnv,
			Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in reconcileFlush().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in reconcileFlush().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in reconcileFlush().");
		}

		RpcHandler handler = cmdEnv.getHandler(RECONCILE_HANDLE);
		
		if (handler != null) {
            ReconcileHandle recHandle = new ReconcileHandle(handler);
            recHandle.getSkipFiles().clear();
        }
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}    

	/**
	 * Recursively (optional) traverse the directory tree for files.<p>
	 * 
	 * Check MapApi; if no mapping, continue.
	 */
	private void traverseDirs(File file, boolean traverse, boolean skipIgnore, boolean sendDigest,
			MapTable map, List<String> addFilesMap, Map<String, Long> sizes, Map<String, String> digests,
			int hasIndex, List<String> skipFiles, boolean unicode, Charset charset, CommandEnv cmdEnv) {
    	
	    if (addFilesMap == null) {
    		throw new IllegalArgumentException("Must pass in a non-null 'files' list as a parameter.");
    	}

		// Return all files in dir, and optionally traverse dirs in dir,
		// while checking each file against map before returning it

		// Scan the directory.

		if (file == null || !file.exists()) {
			return;
		}

		// If this is a file, not a directory, and not to be ignored,
		// save the filename and return.

		if (file.isFile()) {
			if (skipIgnore || !isIgnore(file, charset, cmdEnv)) {
                addFilesMap.add(file.getAbsolutePath());
                sizes.put(file.getAbsolutePath(), file.length());
                if(sendDigest) {
                    String digestStr = digestFile(file, RpcPerforceFileType.FST_BINARY, charset);
                    if(digestStr != null) {
                        digests.put(file.getAbsolutePath(), digestStr);
                    }
                }
			}
			return;
		}

		// If this is a symlink to a directory, and not to be ignored,
		// save the filename and return.

		if (file.isDirectory() && RpcPerforceFileType.isProbablySymLink(file)) {
			if (skipIgnore || !isIgnore(file, charset, cmdEnv)) {
				addFilesMap.add(file.getAbsolutePath());
				sizes.put(file.getAbsolutePath(), file.length());
                if(sendDigest) {
                    String digestStr = digestFile(file, RpcPerforceFileType.FST_BINARY, charset);
                    if(digestStr != null) {
                        digests.put(file.getAbsolutePath(), digestStr);
                    }
                }
			}
			return;
		}

		if (!file.isDirectory()) {
	    	//XXX: not sure if a Java file can be neither a file or directory
	    	return;
		}
		    
		// Directory might be ignored,  bail
		if( !skipIgnore && isIgnoreDir(file, charset, cmdEnv) ) {
			return;
		}

		// This is a directory to be scanned.
		File[] files = file.listFiles();
		if (files == null) {
			return;
		}

		for (File f : files) {
			// Check mapping, ignore files before sending file or symlink back
			// Java paths are unicode: no translation needed

			String fileName = f.getAbsolutePath();

			// Do compare with array list (skip files if possible)
			int cmp = -1;

			while( skipFiles != null && hasIndex < skipFiles.size() )
			{
				//XXX: uncertain that the correct portion of the file is being compared
				cmp = fileName.compareTo(skipFiles.get( hasIndex ) );

				if( cmp < 0 )
					break;

				hasIndex++;

				if( cmp == 0 )
					break;
			}

			// Don't stat if we matched a file from the edit list

			if( cmp == 0 )
				continue;

			if (f.isDirectory()) { // Directory
				if (RpcPerforceFileType.isProbablySymLink(f)) {
					String from = fileName + "/";
					String matched = null;

					// TODO: protocol case mode
					//if( client->protocolNocase != StrBuf::CaseUsage() ) {
					//    from.SetCaseFolding( client->protocolNocase );
					//    matched = map->Translate( from, to, MapLeftRight );
					//    from.SetCaseFolding( !client->protocolNocase );
					//} else {
					MapWrap mw = map.translate( MapTableT.LHS, from );
					matched = mw == null ? null : mw.getTo();
					//}

					if( matched == null ) {
						continue;
					}

					if (skipIgnore || !isIgnore(f, charset, cmdEnv)) {
						addFilesMap.add(fileName);
						sizes.put(fileName, file.length());
						if(sendDigest) {
							String digestStr = digestFile(f, RpcPerforceFileType.FST_BINARY, charset);
							if(digestStr != null) {
								digests.put(fileName, digestStr);
							}
						}
					}
				} else if (traverse) {
					// Recursive call
					traverseDirs(f, traverse, skipIgnore, sendDigest, map, addFilesMap,
							sizes, digests, hasIndex, skipFiles,
							unicode, charset, cmdEnv);
				}
			} else { // File
				String from = fileName;
				String matched = null;

				// TODO: protocol case mode
				//if( client->protocolNocase != StrBuf::CaseUsage() ) {
				//    from.SetCaseFolding( client->protocolNocase );
				//    matched = map->Translate( from, to, MapLeftRight );
				//    from.SetCaseFolding( !client->protocolNocase );
				//} else {
				MapWrap mw = map.translate( MapTableT.LHS, from );
				matched = mw == null ? null : mw.getTo();
				//}

				if( matched == null ) {
					continue;
				}

				if (skipIgnore || !isIgnore(f, charset, cmdEnv)) {
					addFilesMap.add(fileName);
					sizes.put(fileName, file.length());
					if(sendDigest) {
						String digestStr = digestFile(f, RpcPerforceFileType.FST_BINARY, charset);
						if(digestStr != null) {
							digests.put(fileName, digestStr);
						}
					}
				}
			}
		}
	}       

	private String digestFile(File file, RpcPerforceFileType fileType, Charset charset) {
	    
	    MD5Digester digester = new MD5Digester();
        RpcPerforceFile pFile = new RpcPerforceFile(file.getName(), fileType);
        
        // Digest the file using the configured local file content
        // charset. A null digestCharset specified will cause the
        // file to be read as raw byte stream directly off disk.
        return digester.digestFileAs32ByteHex(pFile, charset);
    }

    private boolean traverseShort(
            Map<String, Object> resultsMap,
            File cwd,
            File file,
            boolean traverse,
            boolean skipIgnore, 
            boolean initial, 
            boolean skipCheck, 
            boolean skipCurrent,
	        MapTable map,
	        List<String> addFilesMap,
			List<String> dirs,
	        AtomicInteger idx,
	        List<String> depotFiles,
			AtomicInteger ddx,
	        boolean unicode, 
	        Charset charset, 
	        CommandEnv cmdEnv) {
	    
	    // Variant of traverseDirs that computes the files to be
	    // added during traversal of directories instead of at the end,
	    // and returns directories and files rather than all files.
	    // This is used by 'status -s'.

	    // Scan the directory.
        
        boolean found = false;

        if (addFilesMap == null) {
            throw new IllegalArgumentException("Must pass in a non-null 'files' list as a parameter.");
        }

        if (file == null || !file.exists()) {
            return false;
        }

        // If this is a file, not a directory, and not to be ignored,
        // save the filename and return.
        if (!file.isDirectory() && (file.isFile() || RpcPerforceFileType.isProbablySymLink(file))) {
            if (skipIgnore || !isIgnore(file, charset, cmdEnv)) {
                addFilesMap.add(file.getAbsolutePath());
                found = true;
            }
            return found;
        }

        // If this is a symlink to a directory, and not to be ignored,
        // save the filename and return.
        if (file.isDirectory() && RpcPerforceFileType.isProbablySymLink(file)) {
            if (skipIgnore || !isIgnore(file, charset, cmdEnv)) {
                addFilesMap.add(file.getAbsolutePath());
                found = true;
            }
            return found;
        }

	    // This is a directory to be scanned.

        if (!file.isDirectory()) {
            return false;
        }
            
        // Directory might be ignored,  bail
        
        if( !skipIgnore && isIgnoreDir(file, charset, cmdEnv) ) {
            return false;
        }
        
	    // If directory is unknown to p4, we don't need to check that files
	    // are in depot (they aren't), so just return after the first file
	    // is found and bypass checking. 

	    boolean doSkipCheck = skipCheck;

	    int dddx = 0;
	    List<String> depotDirs = new LinkedList<String>();

	    // First time through we save depot dirs

	    if( initial ) {
	        for( int j=0; resultsMap.containsKey( RpcFunctionMapKey.DEPOT_DIRS + j); j++) {
    	        depotDirs.add((String) resultsMap.get( RpcFunctionMapKey.DEPOT_DIRS + j ));
	        }
	    }

	    // For each directory entry.
	    
	    File[] files = file.listFiles();
        if (files == null) {
            return found;
        }
        
        for (File f : files) {
            
	        boolean isDir = false;
	        boolean isSymDir = false;
	        String fileName = f.getAbsolutePath();
	        
            // Attach path delimiter to dirs so Sort() works correctly, and also to
            // save relevant Stat() information.
	        
            if( f.isDirectory() && !RpcPerforceFileType.isProbablySymLink(f) ) {
                isDir = true;
            } else if( f.isDirectory() && RpcPerforceFileType.isProbablySymLink(f) ) {
                isDir = true;
                isSymDir = true;
            } else if( !f.exists() && !RpcPerforceFileType.isProbablySymLink(f) ) {
                continue;
            }

        
	        // Check mapping, ignore files before sending file or symlink back

	        boolean checkFile = false;

	        if( isDir ) {
    	        if( isSymDir ) {
    	            String from = fileName + "/";
					String matched = null;

    	            // TODO: protocol case mode
	                //if( client->protocolNocase != StrBuf::CaseUsage() ) {
	                //    from.SetCaseFolding( client->protocolNocase );
	                //    matched = map->Translate( from, to, MapLeftRight );
	                //    from.SetCaseFolding( !client->protocolNocase );
	                //} else {
						MapWrap mw = map.translate( MapTableT.LHS, from );
	                    matched = mw == null ? null : mw.getTo();
	                //}
    
    	            if( matched == null ) {
    	                continue;
    	            }
    
    	            if( skipIgnore || !isIgnore(f, charset, cmdEnv) ) {
        	            if( doSkipCheck ){
        	                String alt = sendDir( f, cwd, dirs, idx, skipCurrent );
        	                addFilesMap.add( alt != null ? alt : f.getAbsolutePath());
        	                found = true;
        	                break;
        	            } else {
        	               checkFile = true;
        	            }
    	            }
    	        } else if( traverse ) {
    	            if( initial ) {
        	            dirs.add( fileName );
        	            boolean foundOne = false;
        	            int l = 0;
        
        	            // If this directory is unknown to the depot, we don't
        	            // need to compare against depot files. 
        
        	            for( ; dddx < depotDirs.size() && !foundOne; dddx++)
        	            {
        	                String depotDir = depotDirs.get( dddx );
        	                //p->SetLocal( *cwd, *ddir );
        	                l =  sysCompare(fileName, depotDir);
        	                if( l == 0 ) {
        	                    foundOne = true;
        	                } else if( l < 0 ) {
        	                    break;
        	                }
        	            }
        	            skipCheck = !foundOne;
    	            }
    
    	            found = traverseShort( resultsMap, cwd, f,
    	                        traverse, skipIgnore, false,
    	                        skipCheck, skipCurrent, map,
    	                        addFilesMap, dirs, idx, depotFiles,
    	                        ddx, unicode, charset, cmdEnv);
    
    	            // Stop traversing directories when we have a file to
    	            // to add, unless we are at the top and need to check
    	            // for files in the current directory.
    
    	            if( found && !initial ) {
    	                break;
    	            } else if( found && initial && !skipCurrent ) {
    	                found = false;
    	            }
    	            if( found ) {
    	                break;
    	            }
    	        }
	        } else {
				String matched = null;

				// TODO: protocol case mode
				//if( client->protocolNocase != StrBuf::CaseUsage() ) {
				//    from.SetCaseFolding( client->protocolNocase );
				//    matched = map->Translate( from, to, MapLeftRight );
				//    from.SetCaseFolding( !client->protocolNocase );
				//} else {
					MapWrap mw = map.translate( MapTableT.LHS, fileName );
					matched = mw == null ? null : mw.getTo();
				//}

				if( matched == null ) {
					continue;
				}
	            
	            if( skipIgnore || !isIgnore(f, charset, cmdEnv) ) {
    	            if( doSkipCheck ) {
        	            String alt = sendDir( f, cwd, dirs, idx, skipCurrent );
                        addFilesMap.add( alt != null ? alt : f.getAbsolutePath());
        	            found = true;
        	            break;
    	            } else {
    	                checkFile = true;
    	            }
    	        }
	        }

	        // See if file is in depot and if not, either set the file
	        // or directory to be reported back to the server.

	        if( checkFile ) {
    	        int l = 0;
    	        boolean finished = false;
    	        while ( !finished ) {
    	            if( ddx.get() >= depotFiles.size()) {
    	                l = -1;
    	            } else {
    	                l = sysCompare( fileName, depotFiles.get(ddx.get()));
    	            }
    
    	            if( l == 0 ) {
						ddx.incrementAndGet();
        	            finished = true;
    	            } else if( l < 0 ) {
        	            if( initial && skipCurrent ) {
        	                addFilesMap.add(FilePathHelper.getLocal(f.getParentFile().getAbsolutePath(), "..."));
        	            } else {
        	                String alt = sendDir( f, cwd, dirs, idx, skipCurrent );
                            addFilesMap.add(alt != null ? alt : f.getAbsolutePath());
    	                }
        	            found = true;
        	            break;
    	            } else {
    	                ddx.incrementAndGet();
    	            }
    	        }
    	        if( ( !initial || skipCurrent ) && found ) {
    	            break;
    	        }
	        }
	    }

	    return found;
	}

    /*
     * SendDir - utility method used by clientTraverseShort to decide if a
     *       filename should be output as a file or as a directory (status -s)
     */
    private String sendDir( File file, File cwd, List<String> dirs, AtomicInteger idx, boolean skip )
    {
        // Skip printing file in current directory and just report subdirectory
        if( skip ) {
            return FilePathHelper.getLocal( cwd.getAbsolutePath(), "..." );
        }
    
        // If file is in the current directory: isDirs is unset so that our
        // caller will send back the original file.
    
        file = file.getParentFile();
    
        if( sysCompare( file.getAbsolutePath(), cwd.getAbsolutePath() ) != 0 ) {
            return null;
        }
    
        // Set path to the directory under cwd containing this file.
        // 'dirs' is the list of dirs in cwd on workspace.

        boolean isDir = false;
        for( ; idx.get() < dirs.size() && !isDir; idx.getAndIncrement() ) {
            if( file.getAbsolutePath().startsWith( new File( dirs.get( idx.get() ) ).getAbsolutePath() ) ) {
                return FilePathHelper.getLocal( dirs.get(idx.get()), "..." );
            }
        }
    
        return null;
    }

	/**
	 * Check if the file or symbolic link exists.
	 */
	private boolean fileExists(File file, boolean fstSymlink) {

		if (file != null) {
			if (file.exists()) {
				return true;
			} else if (fstSymlink) {
				return SymbolicLinkHelper.exists(file.getPath());
			}
		}

		return false;
	}
	
	/**
	 * Check if the file should be ignored.
	 */
	boolean isIgnore(File file, Charset charset, CommandEnv cmdEnv) {
		// Do ignore checking, reject file matching ignore patterns
		if (getChecker(charset) != null) {
			try {
				if (checker.match(file)) {
					cmdEnv.handleResult(new RpcMessage(
							ClientMessageId.CANT_ADD_FILE_TYPE,
							MessageSeverityCode.E_INFO,
							MessageGenericCode.EV_CLIENT, new String[] {
									file.getAbsolutePath(), "ignored" }).toMap());
					return true;
				}
			} catch (FileNotFoundException e) {
				Log.error("Exception occurred during ignore files checking: "
						+ e);
			} catch (IOException e) {
				Log.error("Exception occurred during ignore files checking: "
						+ e);
			}
		}
		
		return false;
	}

    /**
     * Check if the file should be ignored.
     */
    boolean isIgnoreDir(File file, Charset charset, CommandEnv cmdEnv) {
        // Do ignore checking, reject file matching ignore patterns
        if (getChecker(charset) != null) {
            try {
                if (checker.match(file)) {
                    cmdEnv.handleResult(new RpcMessage(
                            ClientMessageId.CANT_ADD_FILE_TYPE,
                            MessageSeverityCode.E_INFO,
                            MessageGenericCode.EV_CLIENT, new String[] {
                                    file.getAbsolutePath(), "ignored" }).toMap());
                    return true;
                }
            } catch (FileNotFoundException e) {
                Log.error("Exception occurred during ignore files checking: "
                        + e);
            } catch (IOException e) {
                Log.error("Exception occurred during ignore files checking: "
                        + e);
            }
        }
        
        return false;
    }
    
    int sysCompare(String a, String b){
        String os = System.getProperty("os.name").toLowerCase();
        if( os.contains("windows") || os.contains("mac os") ) {
            return a.compareToIgnoreCase(b);
        }
        return a.compareTo(b);
    }

    /**
     * Recursively get all files in a directory.<p>
     * 
     * Note: must pass in a non-null 'files' list as a parameter.
     */
    public static void getFiles(File dir, FilenameFilter filter, List<File> files) {
    	if (files == null) {
    		throw new IllegalArgumentException("Must pass in a non-null 'files' list as a parameter.");
    	}
    	if (dir != null) {
	        if (dir.isDirectory()) {
	            String[] children = dir.list(filter);
	            if (children != null) {
		            for (int i=0; i<children.length; i++) {
		            	getFiles(new File(dir, children[i]), filter, files);
		            }
	            }
	        } else {
	        	files.add(dir);
	        }
    	}
    }        
	
    /**
     * Return the client ignore checker; create a new one if it doesn't exist.
     */
	private ClientIgnoreChecker getChecker(Charset charset) {
		if (this.checker == null) {
			if (this.server != null) {
				if (this.server.getCurrentClient() != null
						&& this.server.getIgnoreFileName() != null) {
					this.checker = new ClientIgnoreChecker(this.server
							.getCurrentClient().getRoot(),
							this.server.getIgnoreFileName(), charset);
				}
			}
		}
		return this.checker;
	}
	
}
