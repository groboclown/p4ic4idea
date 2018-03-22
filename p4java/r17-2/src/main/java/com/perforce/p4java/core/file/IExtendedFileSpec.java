/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.core.file;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Extends the basic IFileSpec with methods and fields for use with the IServer
 * getExtendedFiles method and other specialized methods.<p>
 *
 * This information is typically only returned by the server.fstat() method or similarly
 * specialized method, but this interface may be returned or used by other methods as documented.
 * Note that returns from these methods may be (and often will be) null.<p>
 *
 * No documentation is given here on individual methods and usage; please consult
 * the detailed Perforce documentation for help with this.<p>
 *
 * Note that setter methods below will only affect local fields and are provided
 * mostly to help with object initialization.
 */

public interface IExtendedFileSpec extends IFileSpec {

        boolean isMapped();
        void setMapped(boolean mapped);

        FileAction getHeadAction();
        void setHeadAction(FileAction action);

        int getHeadChange();
        void setHeadChange(int change);

        int getHeadRev();
        void setHeadRev(int rev);

        String getHeadType();
        void setHeadType(String type);

        Date getHeadTime();
        void setHeadTime(Date date);

        Date getHeadModTime();
        void setHeadModTime(Date date);

        String getHeadCharset();
        void setHeadCharset(String charset);

        int getHaveRev();
        void setHaveRev(int rev);

        String getDesc();
        void setDesc(String desc);

        String getDigest();
        void setDigest(String digest);

        long getFileSize();
        void setFileSize(long size);

        FileAction getOpenAction();
        void setOpenAction(FileAction action);

        String getOpenType();
        void setOpenType(String type);

        String getOpenActionOwner();
        void setOpenActionOwner(String owner);

        String getCharset();
        void setCharset(String charset);

        int getOpenChangelistId();
        void setOpenChangelistId(int id);

        boolean isUnresolved();
        void setUnresolved(boolean unresolved);

        boolean isResolved();
        void setResolved(boolean resolved);

        boolean isReresolvable();
        void setReresolvable(boolean reresolvable);

        boolean isOtherLocked();
        void setOtherLocked(boolean otherLocked);

        List<String> getOtherOpenList();
        void setOtherOpenList(List<String> otherOpenList);

        List<String> getOtherChangelist();
        void setOtherChangelist(List<String> otherChangelist);

        List<String> getOtherActionList();
        void setOtherActionList(List<String> actionList);

        boolean isShelved();

        String getActionOwner();
        void setActionOwner(String actionOwner);

        List<IResolveRecord> getResolveRecords();
        void setResolveRecords(List<IResolveRecord> resolveRecords);

        String getMovedFile();
        void setMovedFile(String movedFile);

        String getVerifyStatus();
    	void setVerifyStatus(String status);
        
        /**
         * Get the file attributes of this file, if they're available. Attributes
         * will only be available if getExtendedFiles was called with the correct
         * FileStatAncilliaryOptions settings; see the main Perforce file attribute
         * documentation. Attributes are treated by the Perforce server as bytes
         * (they're commonly used to store raw data such as thumbnails); it is up to
         * the consumer to determine the "real" type and convert as appropriate.
         *
         * @since 2011.1
         * @return non-null but possibly-empty map of file attributes keyed
         * 				by attribute name. Individual attribute values may be null.
         */
        Map<String, byte[]> getAttributes();
}
