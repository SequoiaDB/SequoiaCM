package com.sequoiacm.tools.serial;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.Ssh;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.net.InetAddress;

/**
 * @Descreption SCM-5929:指定单个版本工作区，使用核验工具以md5级别检测数据一致性
 * @Author ZhangYanan
 * @CreateDate 2023/4/18
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class DiagnoseToolTest5929 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String filePath1 = null;
    private String filePath2 = null;
    private String filePath3 = null;
    private File localPath = null;
    private int fileSize1 = 1024;
    private int fileSize2 = 1024 * 1024;
    private String wsName = "ws5929";
    private String fileName = "file5929_";
    private ArrayList< ScmId > fileIds = new ArrayList<>();
    private int fileNum = 100;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private WsWrapper wsp = null;
    private int diffMd5Num = 10;
    private int diffSizeNum = 10;
    private int noDataNum = 10;
    private int noMd5Num = 10;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile_" + fileSize1
                + ".txt";
        filePath2 = localPath + File.separator + "localFile_sameSize"
                + fileSize1 + ".txt";
        filePath3 = localPath + File.separator + "localFile_diffSize"
                + fileSize2 + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize1 );
        TestTools.LocalFile.createFile( filePath2, fileSize1 );
        TestTools.LocalFile.createFile( filePath3, fileSize2 );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );

        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        wsp = getWsWrapper( session, wsName );
        createFile();
    }

    @Test
    public void test() throws Exception {
        compareTest();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public void createFile() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setContent( filePath1 );
            fileIds.add( file.save() );
            // 十个文件元数据无md5
            if ( i < fileNum - noMd5Num ) {
                file.calcMd5();
            }
        }

        // 构造十个文件数据md5与元数据不一致
        for ( int i = 0; i < diffMd5Num; i++ ) {
            TestSdbTools.Lob.removeLob( site, wsp, fileIds.get( i ) );
            TestSdbTools.Lob.putLob( site, wsp, fileIds.get( i ), filePath2 );
        }

        // 构造十个文件数据大小与元数据不一致
        for ( int i = diffMd5Num; i < diffMd5Num + diffSizeNum; i++ ) {
            TestSdbTools.Lob.removeLob( site, wsp, fileIds.get( i ) );
            TestSdbTools.Lob.putLob( site, wsp, fileIds.get( i ), filePath3 );
        }

        // 构造十个文件只存在元数据
        for ( int i = diffMd5Num + diffSizeNum; i < diffMd5Num + diffSizeNum
                + noDataNum; i++ ) {
            TestSdbTools.Lob.removeLob( site, wsp, fileIds.get( i ) );
        }
    }

    private static WsWrapper getWsWrapper( ScmSession session, String wsName )
            throws ScmException {
        ScmCursor< ScmWorkspaceInfo > cursor = null;
        ScmWorkspaceInfo wsInfo = null;
        try {
            cursor = ScmFactory.Workspace.listWorkspace( session );
            while ( cursor.hasNext() ) {
                ScmWorkspaceInfo info = cursor.getNext();
                if ( wsName.equals( info.getName() ) ) {
                    wsInfo = info;
                }
            }
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
        }
        WsWrapper ws = new WsWrapper( wsInfo );
        return ws;
    }

    private void compareTest() throws Exception {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMdd" );
        String formatDate = dateFormat.format( date );
        String url = gateWayList.get( 0 ) + "/"
                + ScmInfo.getRootSite().getSiteName();

        Ssh ssh = null;
        String host = site.getNode().getHost();
        System.out.println( "----host = " + host );
        String diagnoseToolPath = "tools/sequoiacm-scm-diagnose/bin";
        try {
            ssh = new Ssh( host, sshUserName, sshPassword, 22 );
            String scmInstallDir = ssh.getScmInstallDir();

            String javaHome = ssh.getJavaHome();

            ssh.exec( "cd " + scmInstallDir + "/../" + diagnoseToolPath );
            String cmd = "env JAVA_HOME=" + javaHome + " " + scmInstallDir
                    + "/../" + diagnoseToolPath
                    + "/scmdiagnose.sh compare --workspace " + wsName
                    + " --work-path /opt/compare " + " --url " + url
                    + " --user " + scmUserName + " --passwd " + scmPassword
                    + " --begin-time " + formatDate + " --end-time "
                    + formatDate;
            System.out.println( "----cmd =" + cmd );
            ssh.exec( cmd );
            String stdout = ssh.getStdout();
            checkCompare( stdout );
        } finally {
            if ( ssh != null ) {
                ssh.disconnect();
            }
        }
    }

    public void checkCompare( String str ) {
        String[] split = str.split( "Finish!" );
        String[] subSplit = split[ 1 ].split( "," );
        String ws = subSplit[ 0 ].substring( subSplit[ 0 ].indexOf( ":" ) + 2 );
        String process = subSplit[ 1 ]
                .substring( subSplit[ 1 ].indexOf( ":" ) + 2 );
        String fail = subSplit[ 2 ]
                .substring( subSplit[ 2 ].indexOf( ":" ) + 2 );
        String same = subSplit[ 3 ]
                .substring( subSplit[ 3 ].indexOf( ":" ) + 2 );
        String different = subSplit[ 4 ]
                .substring( subSplit[ 4 ].indexOf( ":" ) + 2 );

        Assert.assertEquals( ws, wsName );
        Assert.assertEquals( Integer.parseInt( process ), fileNum );
        Assert.assertEquals( Integer.parseInt( fail ), noDataNum );
        Assert.assertEquals( Integer.parseInt( same ),
                fileNum - noDataNum - diffMd5Num - diffSizeNum );
        Assert.assertEquals( Integer.parseInt( different ),
                diffMd5Num + diffSizeNum );
    }
}
