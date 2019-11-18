package com.sequoiacm.client.core.file;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;
import com.sequoiacm.common.MimeType;

public class TestScmFileGetSpecificInfo extends ScmTestBase {

    @Test
    public void testGetExistSpecificInfo() throws ScmException {
        ScmSession session = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
            
            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is("BFName1");
            List<ScmFileBasicInfo> fileInfoList = new ArrayList<ScmFileBasicInfo>();

            ScmCursor<ScmFileBasicInfo> fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());

            ScmFileBasicInfo fbi;
            while (fbiCursor.hasNext()) {
                fbi = fbiCursor.getNext();
                Assert.assertEquals(fbi.getFileName(), "BFName1");
                fileInfoList.add(fbi);
            }
            fbiCursor.close();

            ScmFile file = ScmFactory.File.getInstance(ws, fileInfoList.get(0).getFileId());
            Assert.assertEquals(fileInfoList.size(), 1);

            Assert.assertEquals(file.getFileName(), "BFName1");
            Assert.assertEquals(file.getTitle(), "BFTitle1");
            Assert.assertEquals(file.getMimeType(), MimeType.PLAIN.getType());
            // Assert.assertEquals(fbi.getPropertyType(), PropertyType.VIDEO);
            // Assert.assertEquals(fbi.getFolderId(), "");
        }
        finally {
            session.close();
        }
    }

    @Test
    public void testGetNotExistSpecificInfo() throws ScmException {
        ScmSession session = null;
        ScmCursor<ScmFileBasicInfo> fbiCursor = null;

        try {
            session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, user, password));
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, session);
            
            ScmQueryBuilder b = ScmQueryBuilder.start(ScmAttributeName.File.TITLE).is("");
            
            fbiCursor = ScmFactory.File.listInstance(ws,
                    ScmType.ScopeType.SCOPE_ALL, b.get());

            int size = 0;
            while (fbiCursor.hasNext()) {
                Assert.assertNotNull(fbiCursor.getNext());
                size++;
            }

            Assert.assertEquals(fbiCursor.hasNext(), false);
            Assert.assertEquals(fbiCursor.getNext(), null);
            Assert.assertEquals(size, 0);
        }
        finally {
            fbiCursor.close();
            session.close();
        }
    }
}