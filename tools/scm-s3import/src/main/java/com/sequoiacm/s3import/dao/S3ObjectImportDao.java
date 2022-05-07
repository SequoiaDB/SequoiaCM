package com.sequoiacm.s3import.dao;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public interface S3ObjectImportDao {

    void create() throws ScmToolsException;

    void delete() throws ScmToolsException;

}
