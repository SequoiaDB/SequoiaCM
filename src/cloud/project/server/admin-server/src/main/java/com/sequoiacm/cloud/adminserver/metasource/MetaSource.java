package com.sequoiacm.cloud.adminserver.metasource;

import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;

public interface MetaSource {

    public MetaAccessor getContentServerAccessor() throws ScmMetasourceException;

    public MetaAccessor getWorkspaceAccessor() throws ScmMetasourceException;

    public MetaAccessor getSiteAccessor() throws ScmMetasourceException;

    public MetaAccessor getTrafficAccessor() throws ScmMetasourceException;

    public MetaAccessor getFileDeltaAccessor() throws ScmMetasourceException;

    public MetaAccessor getFileStatisticsAccessor() throws ScmMetasourceException;
}
