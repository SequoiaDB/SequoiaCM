package com.sequoiacm.session.seria;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-529: User表用户存在，密码为有效密码md5值
 * @Author linsuqiang
 * @Date 2017-06-25
 * @Version 1.00
 */

/*
 * 1、SCMSYSTEM.USER表用户存在、密码为有效密码md5值； 2、登入SCM，检查登入结果； 3、业务操作，检查操作结果；
 */

public class Login529 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private String fileName = "passwdmanage529";
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void test() {
        ScmSession session = null;
        try {
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/" + site,
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            session = ScmFactory.Session
                    .createSession( SessionType.AUTH_SESSION, scOpt );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            doBusinessOperate( ws );

            runSuccess = true;
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void doBusinessOperate( ScmWorkspace ws ) {
        try {
            ScmFile writefile = ScmFactory.File.createInstance( ws );
            writefile.setContent( filePath );
            writefile.setFileName( fileName + "_" + UUID.randomUUID() );
            ScmId fileId = writefile.save();

            ScmFile readfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = localPath + File.separator + "download.txt";
            this.read( readfile, downloadPath );
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );

            ScmFactory.File.deleteInstance( ws, fileId, true );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void read( ScmFile file, String downloadPath )
            throws ScmException, IOException {
        ScmInputStream sis = null;
        OutputStream fos = null;
        try {
            sis = ScmFactory.File.createInputStream( file );
            fos = new FileOutputStream( downloadPath );
            sis.read( fos );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
        }
    }

}
