package com.sequoiacm.querybuilder;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-338: exists多个不同字段，覆盖所有文件属性
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、countInstance带查询条件查询文件，对多个不同字段做exists匹配查询，覆盖所有文件属性；
 * 2、检查ScmQueryBuilder结果正确性； 3、检查查询结果正确性；
 */

public class Exists338 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileNum = 3;
    private String author = "Exists338";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );

            readyScmFile();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void testQuery() throws Exception {
        try {
            // build condition
            List< String > fileAttrs = new ArrayList< String >();
            fileAttrs.add( ScmAttributeName.File.AUTHOR );
            fileAttrs.add( ScmAttributeName.File.BATCH_ID );
            fileAttrs.add( ScmAttributeName.File.CREATE_TIME );
            fileAttrs.add( ScmAttributeName.File.FILE_ID );
            fileAttrs.add( ScmAttributeName.File.FILE_NAME );
            fileAttrs.add( ScmAttributeName.File.MAJOR_VERSION );
            fileAttrs.add( ScmAttributeName.File.MIME_TYPE );
            fileAttrs.add( ScmAttributeName.File.MINOR_VERSION );
            // fileAttrs.add(ScmAttributeName.File.PROPERTIES);
            // fileAttrs.add(ScmAttributeName.File.PROPERTY_TYPE);
            fileAttrs.add( ScmAttributeName.File.SIZE );
            fileAttrs.add( ScmAttributeName.File.TITLE );
            fileAttrs.add( ScmAttributeName.File.UPDATE_TIME );
            fileAttrs.add( ScmAttributeName.File.UPDATE_USER );
            fileAttrs.add( ScmAttributeName.File.USER );

            ScmQueryBuilder qryBuilder = ScmQueryBuilder.start();
            for ( String attr : fileAttrs ) {
                qryBuilder.put( attr ).exists( 1 );
            }
            BSONObject actCond = qryBuilder.get();

            BSONObject expCond = new BasicBSONObject();
            BSONObject existsBSON = new BasicBSONObject( "$exists", 1 );
            for ( String attr : fileAttrs ) {
                expCond.put( attr, existsBSON );
            }

            Assert.assertEquals( actCond, expCond );

            long count = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, actCond );
            if ( count < 3 ) { // if other threads have their files, file count
                // may greater than 3
                Assert.fail( "count result is wrong. expCount >= 3, actCount = "
                        + count );
            }

            runSuccess = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
            }
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void readyScmFile() {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( "a" + i );
                file.setAuthor( author );
                ScmId fileId = file.save();
                fileIdList.add( fileId );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

}
