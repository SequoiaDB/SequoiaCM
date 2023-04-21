package com.sequoiacm.contentserver.quota;

import com.sequoiacm.exception.ScmServerException;

public interface ScmRefreshScopeRefreshedListener {
    void onRefreshScopeRefreshed() throws ScmServerException;
}
