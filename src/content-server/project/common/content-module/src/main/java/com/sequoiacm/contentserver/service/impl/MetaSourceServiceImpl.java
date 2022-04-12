package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaSource;
import org.springframework.stereotype.Service;

@Service
public class MetaSourceServiceImpl implements MetaSourceService {
    @Override
    public MetaSource getMetaSource() throws ScmServerException {
        return ScmContentModule.getInstance().getMetaService().getMetaSource();
    }
}
