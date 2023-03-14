/**
 *
 */
package com.sequoiacm.sftp.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.dsutils.SftpUtils;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSftpDataLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @description SCM-4908:指定存在的存储根目录、目录分区规则为year，创建工作区
 *              SCM-4909:指定存在的存储根目录、目录分区规则为none，创建工作区
 *              SCM-4910:指定不存在的存储根目录、目录分区规则为quarter，创建工作区
 *              SCM-4911:指定不存在的存储根目录、目录分区规则为month，创建工作区
 *              SCM-4912:指定不存在的存储根目录、目录分区规则为day，创建工作区
 *              SCM-4913:指定存在的存储根目录、目录分区规则为day，创建工作区
 * @author ZhangYanan
 * @createDate 2022.7.8
 * @updateUser ZhangYanan
 * @updateDate 2022.7.8
 * @updateRemark
 * @version v1.0
 */
public class CreateWorkspaceOnSftp4908_4909_4910_4911_4912_4913
        extends TestScmBase {
    private ScmId fileId = null;
    private String wsName = "ws4908";
    private String fileName = "scmfile4908";
    private String dataPath = "/dir4908";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private String filePath = null;
    private String updatePath = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private AtomicInteger actSuccessTests = new AtomicInteger( 0 );

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "updateFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, fileSize );

        List< SiteWrapper > sites = ScmInfo
                .getSitesByType( ScmType.DatasourceType.SFTP );
        site = sites.get( 0 );

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );
        SftpUtils.deleteDirectory( site.getDataDsUrl().split( ":" )[ 0 ],
                dataPath );
    }

    @DataProvider(name = "generateRangData")
    public Object[][] generateRangData() {
        return new Object[][] {
                // SCM-4908:指定存在的存储根目录、目录分区规则为year，创建工作区
                { true, ScmShardingType.YEAR },
                // SCM-4909:指定存在的存储根目录、目录分区规则为none，创建工作区
                { true, ScmShardingType.NONE },
                // SCM-4910:指定不存在的存储根目录、目录分区规则为quarter，创建工作区
                { false, ScmShardingType.QUARTER },
                // SCM-4911:指定不存在的存储根目录、目录分区规则为month，创建工作区
                { false, ScmShardingType.MONTH },
                // SCM-4912:指定不存在的存储根目录、目录分区规则为day，创建工作区
                { false, ScmShardingType.DAY },
                // SCM-4913:指定存在的存储根目录、目录分区规则为day，创建工作区
                { true, ScmShardingType.DAY }, };
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite }, dataProvider = "generateRangData")
    private void test( boolean isDirExist,
            ScmShardingType SftpDataShardingType ) throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( isDirExist ) {
            SftpUtils.createDirectory( site.getDataDsUrl().split( ":" )[ 0 ],
                    dataPath );
        }

        ws = createWS( dataPath, SftpDataShardingType );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        fileId = file.save();

        List< ScmContentLocation > fileLocationsInfos = file
                .getContentLocations();
        String postFix = SftpUtils.getSftpPostfix( SftpDataShardingType );
        String expFile_path = dataPath + "/" + wsName + "/" + postFix + "/"
                + fileId.toString();
        if ( SftpDataShardingType == ScmShardingType.NONE ) {
            expFile_path = dataPath + "/" + wsName + "/" + fileId.toString();
        }
        Assert.assertEquals(
                fileLocationsInfos.get( 0 ).getFullData().get( "file_path" ),
                expFile_path );

        checkCURD();

        SftpUtils.deleteDirectory( site.getDataDsUrl().split( ":" )[ 0 ],
                dataPath );
        actSuccessTests.getAndIncrement();
    }

    @AfterClass
    private void tearDown() throws Exception {
        if ( actSuccessTests.get() == generateRangData().length
                || TestScmBase.forceClear ) {
            try {
                ScmWorkspaceUtil.deleteWs( wsName, session );
                SftpUtils.deleteDirectory(
                        site.getDataDsUrl().split( ":" )[ 0 ], dataPath );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    public ScmWorkspace createWS( String dataPath,
            ScmShardingType SftpDataShardingType )
            throws ScmException, InterruptedException {
        ScmWorkspace ws;
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        String domainName = TestSdbTools
                .getDomainNames( rootSite.getDataDsUrl() ).get( 0 );
        scmDataLocationList.add(
                new ScmSdbDataLocation( rootSite.getSiteName(), domainName ) );
        scmDataLocationList.add( new ScmSftpDataLocation( site.getSiteName(),
                dataPath, SftpDataShardingType ) );

        conf.setDataLocations( scmDataLocationList );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        conf.setEnableDirectory( true );
        ws = ScmWorkspaceUtil.createWS( session, conf );

        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        return ws;
    }

    public void checkCURD() throws Exception {
        int hisVersion = 1;
        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMetaAndData( wsName, fileId, expSites, localPath,
                filePath );

        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.updateContent( updatePath );

        List< ScmId > fileIdList = new ArrayList<>();
        fileIdList.add( fileId );
        ScmFileUtils.checkHistoryFileMetaAndData( wsName, fileIdList, expSites,
                localPath, filePath, hisVersion, 0 );
        ScmFileUtils.checkMetaAndData( wsName, fileId, expSites, localPath,
                updatePath );

        ScmFactory.File.deleteInstance( ws, fileId, true );
        try {
            ScmFactory.File.getInstance( ws, fileId );
            Assert.fail( "预期失败实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }
    }
}
