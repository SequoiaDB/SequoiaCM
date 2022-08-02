package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:get breakpoint file attribute information testlink-case:SCM-1382
 * 
 * @author wuyan
 * @Date 2018.05.15
 * @version 1.00
 */

public class GetBreakpointFileAttr1382 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmBreakpointFile breakpointFile = null;
    private long localTime;

    private String fileName = "breakpointfile1382";
    private int fileSize = 1024 * 1024 * 1;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBDataSource();
        // scminfo
        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        createBreakpointFile();
        getFileAttrInfo();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createBreakpointFile() throws ScmException {
        // create file
        breakpointFile = ScmFactory.BreakpointFile.createInstance( ws,
                fileName );
        byte[] data = new byte[ fileSize ];
        new Random().nextBytes( data );
        breakpointFile.upload( new ByteArrayInputStream( data ) );
        localTime = new Date().getTime();
    }

    private void getFileAttrInfo() throws Exception {
        // check file's attribute
        Assert.assertEquals( breakpointFile.getFileName(), fileName );
        Assert.assertEquals( breakpointFile.getChecksumType(),
                ScmChecksumType.NONE );
        Assert.assertEquals( breakpointFile.getChecksum(), 0 );
        Assert.assertEquals( breakpointFile.getCreateUser(),
                TestScmBase.scmUserName );
        Assert.assertEquals( breakpointFile.getUploadUser(),
                TestScmBase.scmUserName );
        Assert.assertEquals( breakpointFile.getUploadSize(), fileSize );
        Assert.assertEquals( breakpointFile.getWorkspace(), ws );
        Assert.assertEquals( breakpointFile.isCompleted(), true );
        Assert.assertEquals( breakpointFile.getSiteName(), site.getSiteName() );

        // there is a difference between the results of the comparison
        long acceptableOffSet = 5000 * 1000; // unit:ms
        if ( Math.abs( breakpointFile.getCreateTime().getTime()
                - localTime ) > acceptableOffSet ) {
            Assert.fail( "time is different: createTime="
                    + breakpointFile.getCreateTime() + ", localTime="
                    + localTime );
        }

        // update time later than create time
        if ( breakpointFile.getUploadTime().getTime() < breakpointFile
                .getCreateTime().getTime() ) {
            Assert.fail( "update time : " + breakpointFile.getCreateTime()
                    + ", localTime=" + localTime );
        }
    }

}