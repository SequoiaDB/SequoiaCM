/**
 *
 */
package com.sequoiacm.workspace.serial;

import java.io.File;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5907:创建工作区，指定元数据集合参数时指定互相冲突配置项
 * @author YiPan
 * @date 2023/2/3
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class CreateWorkspace5907 extends TestScmBase {
    private String ws1Name = "ws5907-1";
    private String ws2Name = "ws5907-2";
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private BasicBSONObject clOptions = new BasicBSONObject();
    private int fileSize = 1024;
    private String fileName = "file5907";
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "updateFile" + fileSize / 2
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, fileSize / 2 );
        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( ws1Name, session );
        ScmWorkspaceUtil.deleteWs( ws2Name, session );
    }

    @Test
    private void test() throws Exception {
        // 同时指定连个参数
        clOptions.put( "Compressed", false );
        clOptions.put( "CompressionType", "lzw" );

        createWs( ws1Name, clOptions );
        ScmWorkspaceUtil.wsSetPriority( session, ws1Name );
        checkWs( ws1Name );

        // 仅指定Compressed，CompressionType使用默认值
        clOptions.clear();
        clOptions.put( "Compressed", false );
        createWs( ws2Name, clOptions );
        ScmWorkspaceUtil.wsSetPriority( session, ws2Name );
        checkWs( ws2Name );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            ScmWorkspaceUtil.deleteWs( ws1Name, session );
            ScmWorkspaceUtil.deleteWs( ws2Name, session );
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createWs( String wsName, BSONObject clOptions )
            throws ScmException {
        ScmSdbMetaLocation scmSdbMetaLocation = new ScmSdbMetaLocation(
                rootSite.getSiteName(), ScmShardingType.YEAR, TestSdbTools
                        .getDomainNames( rootSite.getMetaDsUrl() ).get( 0 ) );
        scmSdbMetaLocation.setClOptions( clOptions );
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations(
                ScmWorkspaceUtil.getDataLocationList( ScmInfo.getSiteNum() ) );
        conf.setMetaLocation( scmSdbMetaLocation );
        conf.setName( wsName );
        conf.setEnableDirectory( true );
        ScmFactory.Workspace.createWorkspace( session, conf );
    }

    private void checkWs( String wsName ) throws Exception {
        SiteWrapper[] sites = { rootSite };
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + wsName );
        file.setContent( filePath );
        ScmId id = file.save();
        ScmFileUtils.checkMetaAndData( wsName, id, sites, localPath, filePath );
        file.updateContent( updatePath );
        ScmFileUtils.checkMetaAndData( wsName, id, sites, localPath,
                updatePath );
    }
}
