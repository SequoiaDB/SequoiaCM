package com.sequoiacm.scmfile.serial;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @FileName SCM-401: ws下不存在文件，查询文件
 * @Author linsuqiang
 * @Date 2017-05-24
 * @Version 1.00
 */

/*
 * 1、ws下不存在文件，统计该ws下文件； 2、检查统计结果正确性；
 */

public class Count_whenNoFile401 extends TestScmBase {
    private static SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            site = ScmInfo.getSite();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                    session );
            cleanUpFiles( ws );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void test() {
        try {
            long actCount = ScmFactory.File.countInstance( ws,
                    ScmType.ScopeType.SCOPE_CURRENT, new BasicBSONObject() );
            ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance(
                    ws, ScmType.ScopeType.SCOPE_CURRENT,
                    new BasicBSONObject() );
            while ( cursor.hasNext() ) {
                System.out.println( cursor.getNext().toString() );
            }
            Assert.assertEquals( actCount, 0 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( session != null ) {
            session.close();
        }
    }

    private void cleanUpFiles( ScmWorkspace ws ) throws ScmException {
        BSONObject opt = ( BSONObject ) JSON.parse( "{}" );
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT, opt );
        while ( cursor.hasNext() ) {
            ScmId fileId = cursor.getNext().getFileId();
            ScmFactory.File.deleteInstance( ws, fileId, true );
        }
        cursor.close();
    }
}
