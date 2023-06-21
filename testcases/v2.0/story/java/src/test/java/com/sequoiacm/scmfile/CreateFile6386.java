package com.sequoiacm.scmfile;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Random;

/**
 * @Descreption SCM-6386 : 校验文件名
 * @Author yangjianbo
 * @CreateDate 2023/6/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class CreateFile6386 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmSession session = null;
    private WsWrapper wsp = null;
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;
    private SiteWrapper rootSite = null;
    private int fileSize = 1024 * 1024;
    private String fileName = "file6386";
    private String authName = "auth6386";
    private ScmWorkspace ws = null;
    private String randomChar = "\\%;|:*?\"<>qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789";
    private Random random = new Random();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        for ( int i = 0; i < 10; i++ ) {
            String randomFileName = fileName + getRandomFileName()
                    + System.currentTimeMillis();
            uploadAndDownloadFile( randomFileName );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void uploadAndDownloadFile( String name ) throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( name );
        file.setAuthor( authName );
        file.setContent( filePath );
        ScmId fileId = file.save();

        // get file's content, and check results
        ScmFile instance = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        instance.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ) );
    }

    public String getRandomFileName() {
        int len = random.nextInt( 3 ) + 3;
        StringBuilder fileName = new StringBuilder();
        for ( int i = 0; i < len; i++ ) {
            int randomIndex = random.nextInt( randomChar.length() );
            fileName.append( randomChar.charAt( randomIndex ) );
        }
        return fileName.toString();
    }

}
