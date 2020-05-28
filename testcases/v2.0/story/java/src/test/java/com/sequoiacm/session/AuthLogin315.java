package com.sequoiacm.session;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-315: 鉴权登入
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、调用session接口，指定sessionType为鉴权，并设置主机名、端口号、用户、密码登入，
 * 覆盖：ScmConfigOption类下面的所有set、get接口； 2、登入成功后做业务操作，检查登入以及业务操作结果正确性； 注：
 * ScmConfigOption接口覆盖，并覆盖所有set、get方法： ScmConfigOption(); ScmConfigOption(String
 * host, int port); ScmConfigOption(String host, int port, String user, String
 * passwd);
 */

public class AuthLogin315 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static String fileName = "AuthLogin315";
    private boolean runSuccess = false;
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

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

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            String user = TestScmBase.scmUserName;
            String passwd = TestScmBase.scmPassword;

            // ScmConfigOption()
            ScmConfigOption scOpt = new ScmConfigOption();
            // scOpt.getUrls(); //BUG 355
            // not set before get
            // Assert.assertEquals(scOpt.getUrls().toString(), "[]");
            // Assert.assertNull(scOpt.getUrls());
            scOpt.getUser();
            Assert.assertEquals( scOpt.getUser(), null );
            Assert.assertEquals( scOpt.getPasswd(), null );
            // set
            scOpt.addUrl( TestScmBase.gateWayList.get( 0 ) + "/"
                    + site.getSiteServiceName() );
            scOpt.setUser( user );
            scOpt.setPasswd( passwd );
            // get
            Assert.assertEquals( scOpt.getUrls().toString(),
                    "[" + TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName() + "]" );
            Assert.assertEquals( scOpt.getUser(), TestScmBase.scmUserName );
            Assert.assertEquals( scOpt.getPasswd(), TestScmBase.scmPassword );
            // bizOper
            loginAndOperate( scOpt );

            // ScmConfigOption(String host, int port)
            ScmConfigOption scOpt2 = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName() );
            scOpt2.setUser( user );
            scOpt2.setPasswd( passwd );
            loginAndOperate( scOpt2 );

            // ScmConfigOption(String host, int port)
            ScmConfigOption scOpt3 = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName(),
                    user, passwd );
            loginAndOperate( scOpt3 );

            runSuccess = true;
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                    TestTools.LocalFile.removeFile( localPath );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void loginAndOperate( ScmConfigOption scOpt ) throws ScmException {
        ScmSession session = ScmFactory.Session
                .createSession( SessionType.AUTH_SESSION, scOpt );
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                session );
        doBusinessOperate( ws );
        session.close();
    }

    private void doBusinessOperate( ScmWorkspace ws ) {
        try {
            ScmFile writefile = ScmFactory.File.createInstance( ws );
            writefile.setContent( filePath );
            writefile.setFileName( fileName + "_" + UUID.randomUUID() );
            ScmId fileId = writefile.save();
            fileIdList.add( fileId );
            ScmFile readfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = localPath + File.separator + "download.txt";
            this.read( readfile, downloadPath );

            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
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
