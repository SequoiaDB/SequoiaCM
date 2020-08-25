package com.sequoiacm.metasource;

import java.util.Date;

import org.bson.BSONObject;

public interface MetaFileHistoryAccessor extends MetaAccessor {
    public BSONObject delete(String fileId, int majorVersion, int minorVersion)
            throws ScmMetasourceException;

    public void delete(String fileId) throws ScmMetasourceException;

    public boolean addToSiteList(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date) throws ScmMetasourceException;

    public boolean deleteNullFromSiteList(String fileId, int majorVersion, int minorVersion)
            throws ScmMetasourceException;

    public boolean updateAccessTime(String fileId, int majorVersion, int minorVersion, int siteId,
            Date date) throws ScmMetasourceException;

    public boolean updateMd5(String fileId, int majorVersion, int minorVersion, String md5)
            throws ScmMetasourceException;

    public boolean deleteFromSiteList(String fileId, int majorVersion, int minorVersion, int siteId)
            throws ScmMetasourceException;

    public void createFileTable(BSONObject file) throws ScmMetasourceException;
}
