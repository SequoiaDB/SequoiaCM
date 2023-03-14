package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSftpDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5597:修改工作区下sftp数据源配置验证
 * @author ZhangYanan
 * @date 2022/12/22
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5597 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsName = "ws5597";
    private String dataPath = "dataPath5597";
    private String fileName = "file5597";
    private ScmWorkspace ws = null;
    private List< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile2_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSiteByType( ScmType.DatasourceType.SFTP );
        siteList.add( site );
        session = ScmSessionUtils.createSession( site );

        ScmWorkspaceUtil.deleteWs( wsName, session );

        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        List< ScmDataLocation > dataLocation = prepareExpWsDataLocation();
        ws.updateDataLocation( dataLocation, true );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );
        // 通过文件上传下载删除操作验证工作区是否正常
        checkWsStatus();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public List< ScmDataLocation > prepareExpWsDataLocation()
            throws ScmInvalidArgumentException {
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        ScmSftpDataLocation scmSftpDataLocation = new ScmSftpDataLocation(
                site.getSiteName() );
        scmSftpDataLocation.setDataPath( dataPath );
        scmDataLocationList.add( scmSftpDataLocation );

        return scmDataLocationList;
    }

    public void checkWsStatus() throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        ScmId fileId = file.save();

        SiteWrapper[] expSite = { site };
        ScmFileUtils.checkMetaAndData( wsName, fileId, expSite, localPath,
                filePath );

        List< ScmContentLocation > fileContentLocationsInfo = file
                .getContentLocations();
        ScmFileUtils.checkContentLocation( fileContentLocationsInfo, site,
                fileId, ws );
        file.delete( true );
        try {
            ScmFactory.File.getInstance( ws, fileId );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }

    }
}