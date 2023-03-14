package com.sequoiacm.batch;

import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @FileName SCM-1301: 添加已存在其它批次的文件
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class AttachFile1301 extends TestScmBase {
    private final String batchName = "batch1301";
    private final String fileName = "file1301";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private ScmId batchIdA = null;
    private ScmId batchIdB = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        fileId = file.save();
    }

    // TODO: fail for SEQUOIACM-242
    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        ScmBatch batchA = ScmFactory.Batch.createInstance( ws );
        batchA.setName( batchName );
        batchIdA = batchA.save();
        batchA.attachFile( fileId );

        ScmBatch batchB = ScmFactory.Batch.createInstance( ws );
        batchB.setName( batchName );
        batchIdB = batchB.save();
        try {
            batchB.attachFile( fileId );
            Assert.fail( "attach a file again should not succeed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.FILE_IN_ANOTHER_BATCH );
        }

        List< ScmFile > files = batchA.listFiles();
        Assert.assertEquals( files.size(), 1 );
        Assert.assertEquals( files.get( 0 ).getFileName(), fileName );

        files = batchB.listFiles();
        Assert.assertEquals( files.size(), 0 );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmFactory.Batch.deleteInstance( ws, batchIdA );
        ScmFactory.Batch.deleteInstance( ws, batchIdB );
        if ( session != null )
            session.close();
    }
}