package com.sequoiacm.metasource;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;

public interface MetaFileAccessor extends MetaAccessor {
    // delete and return old
    public BSONObject delete(String fileId, int majorVersion, int minorVersion)
            throws ScmMetasourceException;

    public boolean addToSiteList(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date, int wsVersion, String tableName) throws ScmMetasourceException;

    public boolean deleteNullFromSiteList(String fileId, int majorVersion, int minorVersion)
            throws ScmMetasourceException;

    public boolean updateAccessTime(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date) throws ScmMetasourceException;

    public boolean updateAccessHistory(String fileId, int majorVersion, int minorVersion,
            int siteId, BasicBSONList newAccessTimeLististory) throws ScmMetasourceException;

    public boolean deleteFromSiteList(String fileId, int majorVersion, int minorVersion, int siteId)
            throws ScmMetasourceException;

    // update and return old
    public BSONObject updateFileInfo(String fileId, int majorVersion, int minorVersion,
            BSONObject newFileInfo) throws ScmMetasourceException;

    // update and return old
    public BSONObject updateFileInfo(String fileId, int majorVersion, int minorVersion,
            BSONObject newFileInfo, BSONObject matcher) throws ScmMetasourceException;

    public boolean updateTransId(String fileId, int majorVersion, int minorVersion, int status,
            String transId) throws ScmMetasourceException;

    public void unmarkTransId(String fileId, int majorVersion, int minorVersion, int status)
            throws ScmMetasourceException;

    // return new
    public BSONObject updateFileExternalData(BSONObject matcher, BSONObject externalData)
            throws ScmMetasourceException;

    boolean isIndexFieldExist(String fieldName) throws SdbMetasourceException;
}
