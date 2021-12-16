package com.sequoiacm.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @description SCM-3694:创建ScmFile对象，未执行save更新文件内容
 * @author YiPan
 * @createDate 2021.7.21
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.00
 */
public class UpdateContent3694 extends TestScmBase {
    private ScmSession session;
    private ScmWorkspace workspace;
    private String fileName = "file3694";
    private int fileSize = 1024 * 2;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    public void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        SiteWrapper rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        workspace = ScmFactory.Workspace
                .getWorkspace( ScmInfo.getWs().getName(), session );
    }

    @Test
    public void test() throws ScmException {
        try {
            ScmFile f = ScmFactory.File.createInstance( workspace );
            f.setFileName( fileName );
            f.setAuthor( fileName );
            f.updateContent( filePath );
            Assert.fail( "except fail but succeed" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                throw e;
            }
        }
    }

    @AfterClass
    public void tearDown() {
        TestTools.LocalFile.removeFile( localPath );
        session.close();
    }

}