package com.sequoiacm.querybuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
 * @FileName SCM-538: eleMatch中嵌套多种匹配符组合查询
 * @Author linsuqiang
 * @Date 2017-06-25
 * @Version 1.00
 */

/*
 * 1、countInstance带查询条件查询文件，eleMatch中嵌套多种匹配符组合查询，如： BSONObject o =
 * ScmQueryBuilder.start("site_id").is(3)
 * .and("last_access_time").greaterThan(12345).get(); BSONObject r =
 * ScmQueryBuilder.start("site_list").elemMatch(o).get(); 覆盖所有匹配符；
 * 2、检查ScmQueryBuilder结果正确性； 3、检查查询结果正确性；
 */

public class ElemMatch538 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private final int fileNum = 3;
    private final String fileName = "ElemMatch538";
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String author = fileName;

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
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testQuery1() throws Exception {
        try {
            // build condition
            String key = ScmAttributeName.File.SITE_LIST;
            String siteIdKey = ScmAttributeName.File.SITE_ID;
            String lastTimeKey = ScmAttributeName.File.LAST_ACCESS_TIME;
            String fileNameKey = ScmAttributeName.File.AUTHOR;

            BSONObject value = ScmQueryBuilder.start( siteIdKey )
                    .in( site.getSiteId() ).and( lastTimeKey )
                    .greaterThanEquals( 0 ).and( lastTimeKey )
                    .lessThan( Long.MAX_VALUE ).get();

            ScmFile file = ScmFactory.File
                    .getInstance( ws, fileIdList.get( 1 ) );
            BSONObject cond = ScmQueryBuilder.start( key ).elemMatch( value )
                    .and( fileNameKey ).is( file.getAuthor() ).get();
            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    ( "{ \"site_list\" : { \"$elemMatch\" : { " +
                            "\"site_id\" : { \"$in\" : [ " + site.getSiteId()
                            + "]} , " +
                            "\"last_access_time\" : { \"$gte\" : 0 , \"$lt\" " +
                            ": 9223372036854775807}}} , "
                            + "\"author\" : \"" + author + "\"}" )
                            .replaceAll( "\\s*", "" ) );

            // count
            long count = ScmFactory.File
                    .countInstance( ws, ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( count, 3 );

            runSuccess1 = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testQuery2() throws Exception {
        try {
            // build condition
            String key = ScmAttributeName.File.SITE_LIST;
            String siteIdKey = ScmAttributeName.File.SITE_ID;
            String lastTimeKey = ScmAttributeName.File.LAST_ACCESS_TIME;
            String fileNameKey = ScmAttributeName.File.AUTHOR;

            int notExistSiteId = 333;
            BSONObject siteIdIsOk = ScmQueryBuilder.start( siteIdKey )
                    .notIn( notExistSiteId ).get();
            BSONObject lastTimeIsOk = ScmQueryBuilder.start( lastTimeKey )
                    .greaterThan( 0 ).and( lastTimeKey )
                    .lessThanEquals( Long.MAX_VALUE ).get();
            BSONObject value = ScmQueryBuilder.start()
                    .or( siteIdIsOk, lastTimeIsOk ).get();

            ScmFile file = ScmFactory.File
                    .getInstance( ws, fileIdList.get( 1 ) );
            BSONObject cond = ScmQueryBuilder.start( key ).elemMatch( value )
                    .and( fileNameKey ).is( file.getAuthor() ).get();

            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    ( "{ \"site_list\" : { \"$elemMatch\" : { \"$or\" : [ { " +
                            "\"site_id\" : { \"$nin\" : [ "
                            + notExistSiteId + "]}} , { "
                            +
                            "\"last_access_time\" : { \"$gt\" : 0 , \"$lte\" : 9223372036854775807}}]}} , "
                            + "\"author\" : \"" + author + "\"}" )
                            .replaceAll( "\\s*", "" ) );

            // count
            long count = ScmFactory.File
                    .countInstance( ws, ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( count, 3 );

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
                    ScmFactory.File.deleteInstance( ws, fileId, true );
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
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            file.setAuthor( author );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }
}