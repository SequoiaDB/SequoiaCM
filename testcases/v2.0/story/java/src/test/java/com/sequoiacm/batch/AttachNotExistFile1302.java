package com.sequoiacm.batch;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-1302: 添加不存在的文件
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class AttachNotExistFile1302 extends TestScmBase {
    private final String batchName = "batch1302";
    private String fileName = "file_1302";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        ScmId fileId = file.save();
        ScmFactory.File.deleteInstance( ws, fileId, true );
        try {
            batch.attachFile( fileId );
            Assert.fail( "attach inexistent file should not succeed" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND );
        }

        List< ScmFile > files = batch.listFiles();
        Assert.assertEquals( files.size(), 0 );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmFactory.Batch.deleteInstance( ws, batchId );
        if ( session != null ) {
            session.close();
        }
    }
}