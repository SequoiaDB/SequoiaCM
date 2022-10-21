/**
 *
 */
package com.sequoiacm.sftp.serial;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.SftpUtils;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-4907:不指定存储根目录和目录分区规则，创建工作区
 * @author ZhangYanan
 * @createDate 2022.7.8
 * @updateUser ZhangYanan
 * @updateDate 2022.7.8
 * @updateRemark
 * @version v1.0
 */
public class CreateWorkspaceOnSftp4907 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private String wsName = "ws4907";
    private String fileName = "scmfile4907";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private SiteWrapper site = null;
    private SiteWrapper rootSite = null;
    private String filePath = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );

        List< SiteWrapper > sites = ScmInfo
                .getSitesByType( ScmType.DatasourceType.SFTP );
        site = sites.get( 0 );

        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    private void test() throws Exception {
        ws = SftpUtils.createWS( session, site, wsName );

        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        fileId = file.save();

        SiteWrapper[] expSites = { site };
        ScmFileUtils.checkMetaAndData( wsName, fileId, expSites, localPath,
                filePath );

        // 校验文件在sftp数据源的存储位置
        List< ScmContentLocation > fileLocationsInfos = file
                .getContentLocations();
        String postFix = SftpUtils.getSftpPostfix( ScmShardingType.DAY );
        String expFile_path = "/scmfile/" + wsName + "/" + postFix + "/"
                + fileId.toString();
        Assert.assertEquals(
                fileLocationsInfos.get( 0 ).getFullData().get( "file_path" ),
                expFile_path );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
