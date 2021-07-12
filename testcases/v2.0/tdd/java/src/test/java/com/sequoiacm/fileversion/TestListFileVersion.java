package com.sequoiacm.fileversion;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestListFileVersion extends ScmTestMultiCenterBase {

    private ScmSession bSiteSs;

    private ScmFile file1;
    private ScmFile file2;
    private ScmFile file3;

    @BeforeClass
    public void setUp() throws ScmException {
        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));

    }

    @Test
    public void getFile() throws ScmException, IOException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);
        file1 = ScmFactory.File.createInstance(ws);
        file1.setFileName(TestListFileVersion.class.getSimpleName() + "1");
        file1.setAuthor(TestListFileVersion.class.getSimpleName());
        file1.setContent(new ByteArrayInputStream("1".getBytes()));
        file1.save();
        file1.updateContent(new ByteArrayInputStream("abc".getBytes()));

        file2 = ScmFactory.File.createInstance(ws);
        file2.setFileName(TestListFileVersion.class.getSimpleName() + "2");
        file2.setAuthor(TestListFileVersion.class.getSimpleName());
        file2.setContent(new ByteArrayInputStream("12".getBytes()));
        file2.save();
        file2.updateContent(new ByteArrayInputStream("abc".getBytes()));

        file3 = ScmFactory.File.createInstance(ws);
        file3.setFileName(TestListFileVersion.class.getSimpleName() + "3");
        file3.setAuthor(TestListFileVersion.class.getSimpleName());
        file3.setContent(new ByteArrayInputStream("123".getBytes()));
        file3.save();
        file3.updateContent(new ByteArrayInputStream("abc".getBytes()));

        BSONObject queryCondition = ScmQueryBuilder.start()
                .or(ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).is(file1.getFileId().get())
                                .get(),
                        ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                                .is(file2.getFileId().get()).get(),
                        ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                                .is(file3.getFileId().get()).get())
                .get();
        BasicBSONObject orderBy = new BasicBSONObject();

        // check sort by file size
        orderBy.put(ScmAttributeName.File.SIZE, -1);
        ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws,
                ScopeType.SCOPE_HISTORY, queryCondition, orderBy, 0, 1);
        Assert.assertEquals(cursor.hasNext(), true);
        ScmFileBasicInfo fileInfo = cursor.getNext();
        Assert.assertEquals(fileInfo.getFileId().get(), file3.getFileId().get());
        cursor.close();

        // check sort by unsupported field
        orderBy.clear();
        orderBy.put(ScmAttributeName.File.FILE_NAME, -1);
        try {
            ScmFactory.File.listInstance(ws, ScopeType.SCOPE_HISTORY, queryCondition, orderBy, 0,
                    1);
            Assert.fail("sort by unsupported field should not be successful");
        } catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
        }

    }

    @AfterClass
    public void tearDown() throws ScmException {
        file1.delete(true);
        file2.delete(true);
        file3.delete(true);
        bSiteSs.close();

    }
}
