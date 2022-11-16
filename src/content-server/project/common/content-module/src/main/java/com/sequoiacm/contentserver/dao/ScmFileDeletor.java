package com.sequoiacm.contentserver.dao;

import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.exception.ScmServerException;

public interface ScmFileDeletor {
    public FileMeta delete() throws ScmServerException;
}
