package com.sequoiacm.batch;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-1305: 修改文件数后再次获取文件列表
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class ListFiles1305 extends TestScmBase {
    private final String batchName = "batch1305";
    private final int fileNum = 5;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList<>( fileNum );
    private ScmId batchId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace
                .getWorkspace( ScmInfo.getWs().getName(), session );

        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "file1305_" + i );
            file.setTitle( batchName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmBatch batch = ScmFactory.Batch.createInstance( ws );
        batch.setName( batchName );
        batchId = batch.save();

        checkFileList( batch.listFiles(), 0 );
        for ( ScmId fileId : fileIdList ) {
            batch.attachFile( fileId );
        }
        checkFileList( batch.listFiles(), fileNum );
        for ( ScmId fileId : fileIdList ) {
            batch.detachFile( fileId );
        }
        checkFileList( batch.listFiles(), 0 );
        for ( ScmId fileId : fileIdList ) {
            batch.attachFile( fileId );
        }
        checkFileList( batch.listFiles(), fileNum );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        ScmFactory.Batch.deleteInstance( ws, batchId );
        if ( session != null )
            session.close();
    }

    private void checkFileList( List< ScmFile > files, int expNum ) {
        Assert.assertEquals( files.size(), expNum );
        for ( ScmFile file : files ) {
            Assert.assertEquals( file.getTitle(), batchName );
        }
    }
}