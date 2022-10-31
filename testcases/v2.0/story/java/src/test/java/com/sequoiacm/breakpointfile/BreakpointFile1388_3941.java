package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @description SCM-1388:删除断点文件，该文件未上传完成 SCM-3941:删除断点文件，该文件未上传完成
 * @author wuyan
 * @createDate 2018.05.18
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class BreakpointFile1388_3941 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String fileName = "breakpointfile1388";
    private int dataSize = 1024 * 1024 * 5;
    private byte[] data = new byte[ dataSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        createBreakpointFile();
        deleteUploadFile();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess || TestScmBase.forceClear ) {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createBreakpointFile() throws ScmException, IOException {
        // create breakpointfile
        ScmChecksumType checksumType = ScmChecksumType.ADLER32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, checksumType );

        new Random().nextBytes( data );
        int uploadSize = 1024 * 1024 * 5;
        byte[] datapart = new byte[ uploadSize ];
        System.arraycopy( data, 0, datapart, 0, uploadSize );
        breakpointFile.incrementalUpload( new ByteArrayInputStream( datapart ),
                false );
    }

    private void deleteUploadFile() throws ScmException {
        ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        // check the delete result
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
            }
        }
    }

}