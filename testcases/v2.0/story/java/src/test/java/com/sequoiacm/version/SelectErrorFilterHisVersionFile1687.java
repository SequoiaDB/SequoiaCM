/**
 *
 */
package com.sequoiacm.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1687:查询条件存在历史表以外的字段
 * @author luweikang
 * @createDate 2018.06.13
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class SelectErrorFilterHisVersionFile1687 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "fileVersion1687";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();

        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = ScmFileUtils.createFileByStream( ws, fileName, filedata );
        VersionUtils.updateContentByStream( ws, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws ScmException {
        BSONObject errorFilter = new BasicBSONObject();
        errorFilter.put( "author", fileName );
        ScmCursor< ScmFileBasicInfo > fileCursor = null;
        try {
            fileCursor = ScmFactory.File.listInstance( ws, ScopeType.SCOPE_ALL,
                    errorFilter );
            long fileAllVersion = 0;
            while ( fileCursor.hasNext() ) {
                fileCursor.getNext();
                fileAllVersion++;
            }
            Assert.assertEquals( fileAllVersion, 2 );
        } finally {
            if ( fileCursor != null ) {
                fileCursor.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
