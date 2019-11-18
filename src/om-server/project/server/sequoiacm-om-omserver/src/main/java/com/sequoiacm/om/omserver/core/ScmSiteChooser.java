package com.sequoiacm.om.omserver.core;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;

public interface ScmSiteChooser {
    public String chooseSiteFromWorkspace(OmWorkspaceDetail ws)
            throws ScmInternalException, ScmOmServerException;

    public String chooseFromAllSite() throws ScmInternalException, ScmOmServerException;

    public String getRootSite() throws ScmInternalException, ScmOmServerException;

    public void refreshCacheSilence();

    // silence method
    public void onException(ScmInternalException e);
}
