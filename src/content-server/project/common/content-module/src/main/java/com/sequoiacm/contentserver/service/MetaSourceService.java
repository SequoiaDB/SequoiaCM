package com.sequoiacm.contentserver.service;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaSource;

public interface MetaSourceService {
    MetaSource getMetaSource() throws ScmServerException;
}
