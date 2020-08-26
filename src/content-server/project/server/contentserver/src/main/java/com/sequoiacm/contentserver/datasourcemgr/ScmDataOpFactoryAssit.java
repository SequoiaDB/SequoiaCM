package com.sequoiacm.contentserver.datasourcemgr;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.datasource.dataoperation.ScmDataOpFactory;
import com.sequoiacm.contentserver.site.ScmContentServer;

public class ScmDataOpFactoryAssit {
    public static ScmDataOpFactory getFactory() throws ScmServerException {
        return ScmContentServer.getInstance().getSiteMgr().getOpFactory();
    }
}
