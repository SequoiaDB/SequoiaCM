package com.sequoiacm.scmfile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-711: 游标未关闭时操作
 * @Author linsuqiang
 * @Date 2017-07-31
 * @Version 1.00
 */

/*
 * 0、创建若干文件； 1、listInstance获取文件列表（返回游标）；
 * 2、循环用cursor.getNext().getFileId()获取fileId； 3、delete删除该文件； 4、检查结果
 */

public class OprWhenCursorOpen711 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private final int fileNum = 20;
    private final String author = "case711";
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            prepareScmFile();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void testQuery() throws Exception {
        try {
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();

            ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                    .listInstance( ws, ScopeType.SCOPE_CURRENT, cond );
            while ( cursor.hasNext() ) {
                ScmFileBasicInfo fileInfo = cursor.getNext();
                ScmId fileId = fileInfo.getFileId();
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
            cursor.close();

            // patch for JIRA: SEQUOIACM-40
            ScmCursor< ScmFileBasicInfo > cursor1 = ScmFactory.File
                    .listInstance( ws, ScopeType.SCOPE_CURRENT, cond );
            ScmCursor< ScmFileBasicInfo > cursor2 = ScmFactory.File
                    .listInstance( ws, ScopeType.SCOPE_CURRENT, cond );
            cursor2.close();
            cursor1.close();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }

    private void prepareScmFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( author + "_" + UUID.randomUUID() );
            file.setAuthor( author );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

}