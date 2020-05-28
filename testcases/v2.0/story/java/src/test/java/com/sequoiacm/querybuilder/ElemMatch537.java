package com.sequoiacm.querybuilder;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
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
 * @FileName SCM-537: eleMatch匹配不存在的字段
 * @Author linsuqiang
 * @Date 2017-06-25
 * @Version 1.00
 */

/*
 * 1、countInstance带查询条件查询文件，对不存在的文件属性字段做eleMatch匹配查询； 2、检查ScmQueryBuilder结果正确性；
 * 3、检查查询结果正确性；
 */

public class ElemMatch537 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private final int fileNum = 3;
    private final String authorName = "file537";
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );

            readyScmFile();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testInvalidKey() throws Exception {
        try {
            // build condition
            ScmFile file = ScmFactory.File.getInstance( ws,
                    fileIdList.get( 1 ) );
            String key = ScmAttributeName.File.TITLE;
            BSONObject value = ScmQueryBuilder
                    .start( ScmAttributeName.File.SITE_ID )
                    .is( site.getSiteId() ).get();
            BSONObject cond = ScmQueryBuilder.start( key ).elemMatch( value )
                    .and( ScmAttributeName.File.AUTHOR ).is( file.getAuthor() )
                    .get();

            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    ( "{ \"title\" : { \"$elemMatch\" : { \"site_id\" : "
                            + site.getSiteId() + "}} , \"author\" : \""
                            + authorName + "\"}" ).replaceAll( "\\s*", "" ) );

            // count
            long count = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( count, 0 );

            runSuccess1 = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testInvalidElemKey() throws Exception {
        try {
            // build condition
            ScmFile file = ScmFactory.File.getInstance( ws,
                    fileIdList.get( 1 ) );
            String key = ScmAttributeName.File.SITE_LIST;
            BSONObject value = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR )
                    .is( site.getSiteId() ).get();
            BSONObject cond = ScmQueryBuilder.start( key ).elemMatch( value )
                    .and( ScmAttributeName.File.AUTHOR ).is( file.getAuthor() )
                    .get();

            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    ( "{ \"site_list\" : { \"$elemMatch\" : { \"author\" : "
                            + site.getSiteId() + "}} , \"author\" : \""
                            + authorName + "\"}" ).replaceAll( "\\s*", "" ) );

            // count
            long count = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( count, 0 );

            runSuccess2 = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( ( runSuccess1 && runSuccess2 ) || TestScmBase.forceClear ) {
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

    private void readyScmFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( authorName + "_" + i );
            file.setAuthor( authorName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }
}