package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
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
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @description SCM-1387:删除断点文件，该文件已上传完成 SCM-1391:设置断点文件为文件的内容，执行删除操作
 *              SCM-3940:删除断点文件，该文件已上传完成 SCM-3944:设置断点文件为文件的内容，执行删除操作
 * @author wuyan
 * @createDate 2018.05.11
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */

public class BreakpointFile1387_1391_3940_3944 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "breakpointfile1387";
    private ScmId fileId = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws ScmException {
        // testcase1387:delete breakpointfile
        createAndDeleteBreakpointFile();
        // testcase1391:delete breakpointfile of had created file
        DeleteBreakPointFileBySetFile();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private void createAndDeleteBreakpointFile() throws ScmException {
        // create file
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        byte[] data = new byte[ 1024 * 1024 * 5 ];
        new Random().nextBytes( data );
        breakpointFile.upload( new ByteArrayInputStream( data ) );

        // delete file,than check the result
        ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
            }
        }
    }

    private void DeleteBreakPointFileBySetFile() throws ScmException {
        // create file
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, ScmChecksumType.ADLER32 );
        byte[] data = new byte[ 1024 * 1024 * 5 ];
        new Random().nextBytes( data );
        breakpointFile.upload( new ByteArrayInputStream( data ) );

        // breakpointFile setContet file
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( breakpointFile );
        fileId = file.save();

        // delete the breakpointfile fail
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                throw e;
            }
        }
    }
}