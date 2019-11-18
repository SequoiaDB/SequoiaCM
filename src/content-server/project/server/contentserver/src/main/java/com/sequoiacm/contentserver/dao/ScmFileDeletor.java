package com.sequoiacm.contentserver.dao;

import com.sequoiacm.contentserver.exception.ScmServerException;

public interface ScmFileDeletor {
    public void delete() throws ScmServerException;
}
