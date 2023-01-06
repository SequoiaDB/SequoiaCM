
package com.sequoiacm.metasource;

import java.util.Date;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import org.bson.BSONObject;

public interface MetaFileHistoryAccessor {
    public BSONObject delete(String fileId, int majorVersion, int minorVersion)
            throws ScmMetasourceException;

    public void delete(String fileId) throws ScmMetasourceException;

    public boolean addToSiteList(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date, int wsVersion, String tableName) throws ScmMetasourceException;

    public boolean deleteNullFromSiteList(String fileId, int majorVersion, int minorVersion)
            throws ScmMetasourceException;

    public boolean updateAccessTime(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date) throws ScmMetasourceException;

    public boolean updateMd5(String fileId, int majorVersion, int minorVersion, String md5)
            throws ScmMetasourceException;

    public boolean deleteFromSiteList(String fileId, int majorVersion, int minorVersion, int siteId)
            throws ScmMetasourceException;

    public void createFileTable(BSONObject file) throws ScmMetasourceException;
    
    // update and return new
    public BSONObject updateFileInfo(String fileId, int majorVersion, int minorVersion,
            BSONObject newFileInfo) throws ScmMetasourceException;

    public BSONObject updateFileExternalData(BSONObject matcher, BSONObject externalData)
            throws ScmMetasourceException;

    public BSONObject queryAndDelete(String fileId, BSONObject latestVersion,
            BSONObject additionalMatcher, BSONObject orderby) throws ScmMetasourceException;

    public void insert(BSONObject insertor) throws ScmMetasourceException;

    public MetaCursor query(BSONObject matcher, BSONObject orderBy, long skip,
            long limit) throws ScmMetasourceException;

    public MetaCursor query(BSONObject matcher, BSONObject orderBy, BSONObject hint, long skip,
            long limit) throws ScmMetasourceException;

    public MetaCursor query(BSONObject matcher, BSONObject orderBy) throws ScmMetasourceException;

    public BSONObject queryOne(BSONObject matcher, BSONObject orderBy)
            throws ScmMetasourceException;

    public void delete(BSONObject deletor) throws ScmMetasourceException;

    BSONObject updateAndReturnNew(BSONObject matcher, BSONObject updator) throws ScmMetasourceException;

    MetaCursor queryAndUpdate(BSONObject matcher, BSONObject updater) throws ScmMetasourceException;

    public long count(BSONObject matcher) throws ScmMetasourceException;

    public double sum(BSONObject matcher, String field) throws ScmMetasourceException;

    boolean isIndexFieldExist(String fieldName) throws SdbMetasourceException;

    public MetaCursor queryAndDeleteWithCursor(String fileId, BSONObject latestVersion,
            BSONObject additionalMatcher, BSONObject orderby) throws ScmMetasourceException;
}
