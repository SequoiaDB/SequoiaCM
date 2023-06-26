package com.sequoiacm.version;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Random;

/**
 * @description SCM-3692:指定字段，排序查询历史文件表
 * @author YiPan
 * @createDate 2021.07.17
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class ListInstanceByScopeHistory3692 extends TestScmBase {
    private ScmSession session;
    private ScmWorkspace workspace;
    private String fileNameBase = "file3692_";
    private ScmId fileId1;
    private ScmId fileId2;
    private byte[] writedata = new byte[ 1024 * 1 ];
    private byte[] updatedata = new byte[ 1024 * 2 ];
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws ScmException, IOException {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        WsWrapper wsp = ScmInfo.getWs();
        workspace = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond1 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileNameBase + 1 )
                .get();
        BSONObject cond2 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileNameBase + 2 )
                .get();
        ScmFileUtils.cleanFile( wsp, cond1 );
        ScmFileUtils.cleanFile( wsp, cond2 );

        // 创建两个文件更新历史版本
        fileId1 = ScmFileUtils.createFileByStream( workspace, fileNameBase + 1,
                writedata, fileNameBase );
        fileId2 = ScmFileUtils.createFileByStream( workspace, fileNameBase + 2,
                writedata, fileNameBase );
        VersionUtils.updateContentByStream( workspace, fileId1, updatedata );
        VersionUtils.updateContentByStream( workspace, fileId2, updatedata );
    }

    // SEQUOIACM-996
    @Test(groups = { GroupTags.base }, enabled = false)
    public void test() throws ScmException {
        String valid[] = { "id", "data_id", "data_type", "major_version",
                "minor_version", "size", "create_month", "data_create_time" };
        String invalid[] = { "name", "test", "111" };
        // 随机取1个有效值1个无效值
        Random random = new Random();
        String validField = valid[ random.nextInt( valid.length ) ];
        String invalidField = invalid[ random.nextInt( invalid.length ) ];

        // 有效值查询
        ScmCursor< ScmFileBasicInfo > result = ScmFactory.File.listInstance(
                workspace, ScmType.ScopeType.SCOPE_HISTORY,
                new BasicBSONObject(), new BasicBSONObject( validField, -1 ), 0,
                -1 );
        boolean isFile1Exist = false;
        boolean isFile2Exist = false;
        while ( result.hasNext() ) {
            String fileName = result.getNext().getFileName();
            if ( fileName.equals( fileNameBase + 1 ) ) {
                isFile1Exist = true;
            }
            if ( fileName.equals( fileNameBase + 2 ) ) {
                isFile2Exist = true;
            }
            if ( isFile1Exist && isFile2Exist ) {
                break;
            }
        }
        Assert.assertTrue( isFile1Exist );
        Assert.assertTrue( isFile2Exist );

        // 无效值查询
        try {
            ScmFactory.File.listInstance( workspace,
                    ScmType.ScopeType.SCOPE_HISTORY, new BasicBSONObject(),
                    new BasicBSONObject( invalidField, 1 ), 0, -1 );
            Assert.fail( "except failure but succeed" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFactory.File.deleteInstance( workspace, fileId1, true );
                ScmFactory.File.deleteInstance( workspace, fileId2, true );
            } finally {
                session.close();
            }
        }
    }
}