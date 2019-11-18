package com.sequoiacm.fileversion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

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
import com.sequoiacm.testcommon.ScmTestTools;

public class TestListFileVersion extends ScmTestMultiCenterBase {

    private ScmSession bSiteSs;

    @BeforeClass
    public void setUp() throws ScmException {
        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();

        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));

    }

    @Test
    public void getFile() throws ScmException, IOException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);
        ScmFile file1 = ScmFactory.File.createInstance(ws);
        file1.setFileName(TestListFileVersion.class.getSimpleName() + "1");
        file1.setAuthor(TestListFileVersion.class.getSimpleName());
        file1.save();
        file1.updateContent(new ByteArrayInputStream("ads".getBytes()));

        ScmFile file2 = ScmFactory.File.createInstance(ws);
        file2.setFileName(TestListFileVersion.class.getSimpleName() + "2");
        file2.setAuthor(TestListFileVersion.class.getSimpleName());
        file2.save();
        file2.updateContent(new ByteArrayInputStream("ads".getBytes()));

        ScmFile file3 = ScmFactory.File.createInstance(ws);
        file3.setFileName(TestListFileVersion.class.getSimpleName() + "3");
        file3.setAuthor(TestListFileVersion.class.getSimpleName());
        file3.save();
        file3.updateContent(new ByteArrayInputStream("ads".getBytes()));

        ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws,
                ScopeType.SCOPE_HISTORY,
                ScmQueryBuilder.start()
                        .or(ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                                .is(file1.getFileId().get()).get(),
                                ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                                        .is(file2.getFileId().get()).get(),
                                ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID)
                                        .is(file3.getFileId().get()).get())
                        .get(),
                ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(1).get(), 1, 1);

        Assert.assertEquals(cursor.hasNext(), true);
        ScmFileBasicInfo fileInfo = cursor.getNext();
        Assert.assertEquals(cursor.hasNext(), false);
        cursor.close();

        Assert.assertEquals(fileInfo.getFileName(),
                TestListFileVersion.class.getSimpleName() + "2");
        Assert.assertEquals(fileInfo.getFileId().get(), file2.getFileId().get());

        file1.delete(true);
        file2.delete(true);
        file3.delete(true);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        bSiteSs.close();
    }
}
