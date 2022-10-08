package com.sequoiacm.workspace.serial;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.HbaseUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * @Description SCM-2364:不指定namespace,创建ws
 * @author fanyu
 * @date 2019年01月07日
 */
public class CreateWorkspace2364 extends TestScmBase {
    private String wsName = "ws2364";
    private String namespace = "default";
    private ScmSession session = null;
    private SiteWrapper site = null;
    private Calendar cal = null;

    private String fileName = "scmfile2358";
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        cal = Calendar.getInstance();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        List< SiteWrapper > sites = ScmInfo.getAllSites();
        for ( SiteWrapper tmpsite : sites ) {
            if ( tmpsite.getDataType()
                    .equals( ScmType.DatasourceType.HBASE ) ) {
                site = tmpsite;
                break;
            }
        }
        if ( site == null ) {
            throw new SkipException( "the site of hbase is not existed" );
        }
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        // write file in workspace
        writeAndRead( wsName );
        // only check tableName in namespace
        String tableName = null;
        if ( cal.get( Calendar.MONTH ) + 1 < 10 ) {
            tableName = wsName + "_SCMFILE_" + cal.get( Calendar.YEAR ) + "0"
                    + ( cal.get( Calendar.MONTH ) + 1 );
        } else {
            tableName = wsName + "_SCMFILE_" + cal.get( Calendar.YEAR )
                    + ( cal.get( Calendar.MONTH ) + 1 );
        }
        Assert.assertTrue( HbaseUtils.isInNS( site, namespace, tableName ),
                "expect tableName is in namespace,namespace = " + namespace
                        + "tableName = " + tableName );
    }

    @AfterClass
    private void tearDown() throws ScmException, IOException {
        ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
        TestTools.LocalFile.removeFile( localPath );
        if ( session != null ) {
            session.close();
        }
    }

    private void writeAndRead( String wsName ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setCreateTime( Calendar.getInstance().getTime() );
        file.setContent( filePath );
        ScmId fileId = file.save();
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        file1.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( downloadPath ) );
    }
}
