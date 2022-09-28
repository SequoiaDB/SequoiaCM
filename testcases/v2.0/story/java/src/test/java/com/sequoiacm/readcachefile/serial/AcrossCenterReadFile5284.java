package com.sequoiacm.readcachefile.serial;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @Descreption SCM-5284:更新工作区缓存策略，驱动测试
 * @Author YiPan
 * @CreateDate
 * @UpdateUser
 * @UpdateDate 2022/9/19
 * @UpdateRemark
 * @Version
 */
public class AcrossCenterReadFile5284 extends TestScmBase {
    private final String wsName = "ws_5284";
    private ScmSession session;
    private List< ScmDataLocation > dataLocationList;
    private ScmMetaLocation metaLocation;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        session = TestScmTools.createSession();
        ScmWorkspaceUtil.deleteWs( wsName, session );
        dataLocationList = ScmWorkspaceUtil
                .getDataLocationList( ScmInfo.getSiteNum() );
        metaLocation = ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 创建默认ScmWorkspaceConf,校验默认为ALWAYS
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( dataLocationList );
        conf.setMetaLocation( metaLocation );
        conf.setName( wsName );
        Assert.assertEquals( conf.getSiteCacheStrategy(),
                ScmSiteCacheStrategy.ALWAYS );

        // 设置为NEVER,创建工作区
        conf.setSiteCacheStrategy( ScmSiteCacheStrategy.NEVER );
        ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, conf );
        Assert.assertEquals( ws.getSiteCacheStrategy(),
                ScmSiteCacheStrategy.NEVER );

        // 使用ScmWorkspace修改为ALWAYS校验
        ws.updateSiteCacheStrategy( ScmSiteCacheStrategy.ALWAYS );
        Assert.assertEquals( ws.getSiteCacheStrategy(),
                ScmSiteCacheStrategy.ALWAYS );

        // 列取修改后的scmWorkspaceInfo校验
        ScmCursor< ScmWorkspaceInfo > cursor = ScmFactory.Workspace
                .listWorkspace( session );
        while ( cursor.hasNext() ) {
            ScmWorkspaceInfo scmWorkspaceInfo = cursor.getNext();
            if ( scmWorkspaceInfo.getName().equals( wsName ) ) {
                Assert.assertEquals( scmWorkspaceInfo.getSiteCacheStrategy(),
                        ScmSiteCacheStrategy.ALWAYS );
            }
        }
        cursor.close();
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            session.close();
        }
    }

}