package com.sequoiacm.client.core.file;


import com.sequoiacm.client.core.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;
import com.sequoiacm.common.MimeType;

public class TestScmFileGetListByCondition extends ScmTestBase {

    @Test
    public void testGetExistListByCondition() throws ScmException {
        ScmSession session = null;
        ScmCursor<ScmFileBasicInfo> fbiCursor = null;
        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.MIME_TYPE)
                    .is(MimeType.PLAIN);

            fbiCursor = ScmFactory.File.listInstance(ws, ScmType.ScopeType.SCOPE_ALL, b.get());

            ScmFileBasicInfo fbi;
            int size = 0;
            while (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
                Assert.assertNotNull(fbi);
                size++;
            }
            Assert.assertEquals(fbiCursor.getNext(), null);
            Assert.assertNotEquals(size, 0);
        }
        finally {
            fbiCursor.close();
            session.close();
        }
    }

    @Test
    public void testGetNoExistListByCondition() throws ScmException {
        ScmSession session = null;
        ScmCursor<ScmFileBasicInfo> fbiCursor = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);

            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).is("");

            fbiCursor = ScmFactory.File.listInstance(ws, ScmType.ScopeType.SCOPE_ALL, b.get());

            ScmFileBasicInfo fbi;
            int size = 0;
            while (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
                Assert.assertNotNull(fbi);
                size++;
            }
            Assert.assertEquals(fbiCursor.getNext(), null);
            Assert.assertEquals(size, 0);
        }
        finally {
            fbiCursor.close();
            session.close();
        }
    }
}
