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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-374: notEquals多个相同字段
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、countInstance带查询条件查询文件，对多个相同字段做notEquals匹配查询； 2、检查ScmQueryBuilder结果正确性；
 * 3、检查查询结果正确性；
 */

public class NotEquals374 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private ScmWorkspace ws = null;

    private ScmFile file = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "NotEquals374";
    private int fileNum = 3;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );

            readyScmFile();
            file = ScmFactory.File.getInstance( ws, fileIdList.get( 2 ) );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testQueryByExistCond() throws Exception {
        try {
            // build condition
            String key = ScmAttributeName.File.CREATE_TIME;
            long value = file.getCreateTime().getTime();

            BSONObject cond = ScmQueryBuilder.start( key ).notEquals( 111 )
                    .put( key ).notEquals( value )
                    .put( ScmAttributeName.File.AUTHOR ).is( file.getAuthor() )
                    .get();

            String expCond = "{ \"" + key + "\" : { \"$ne\" : " + value
                    + "} , \"author\" : \"" + authorName + "\"}";
            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    expCond.replaceAll( "\\s*", "" ) );

            // count
            long count = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( count, 2 );

            runSuccess1 = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testQueryByNotExistCond() throws Exception {
        try {
            // build condition
            String key = ScmAttributeName.File.CREATE_TIME;
            long value = file.getCreateTime().getTime();

            BSONObject cond = ScmQueryBuilder.start( key ).notEquals( value )
                    .put( "k2" ).notEquals( " " )
                    .put( ScmAttributeName.File.AUTHOR ).is( file.getAuthor() )
                    .get();

            String expCond = "{ \"" + key + "\" : { \"$ne\" : " + value + "} , "
                    + "\"k2\" : { \"$ne\" : \" \"} , \"author\" : \""
                    + authorName + "\"}";
            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    expCond.replaceAll( "\\s*", "" ) );

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

    private void readyScmFile() {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile scmfile = ScmFactory.File.createInstance( ws );
                scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
                scmfile.setAuthor( authorName );
                ScmId fileId = scmfile.save();
                fileIdList.add( fileId );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

}