package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmDirDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;

public class ScmDirDaoImpl implements ScmDirDao {
    private ScmOmSession session;

    public ScmDirDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public long countDir(String wsName) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, connection);
            return ScmFactory.Directory.countInstance(ws, new BasicBSONObject());
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to count dir, " + e.getMessage(),
                    e);
        }
    }

}
