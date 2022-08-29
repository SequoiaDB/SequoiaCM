package com.sequoiacm.querybuilder;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Testcase: SCM-333:所有匹配符嵌套查询
 * @author huangxiaoni init
 * @date 2017.5.26
 */

public class MthCombin333 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private static String fileName = "MthCombin333";
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private List< ScmFile > fileList = new ArrayList<>();
    private int fileNum = 5;
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
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void testQuery() throws Exception {
        try {
            ScmQueryBuilder builder = ScmBuilder();
            long count = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, builder.get() );
            Assert.assertEquals( count, 1 );

            runSuccess = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmFile file : fileList ) {
                    ScmId fileId = file.getFileId();
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

    private void readyScmFile() {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName + "_" + i );
                file.setAuthor( author );
                file.save();
                fileList.add( file );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private ScmQueryBuilder ScmBuilder() throws ScmException {
        String k1 = ScmAttributeName.File.FILE_NAME;
        String k2 = ScmAttributeName.File.MAJOR_VERSION;

        // in
        List< Object > inList = new ArrayList<>();
        inList.add( fileList.get( 0 ).getFileName() );
        inList.add( fileList.get( 1 ).getFileName() );
        BSONObject in = ScmQueryBuilder.start( k1 ).in( inList ).get();

        // nin
        List< Object > ninList = new ArrayList<>();
        ninList.add( fileList.get( 1 ).getFileName() );
        ninList.add( fileList.get( 2 ).getFileName() );
        BSONObject nin = ScmQueryBuilder.start( k1 ).notIn( ninList ).get();

        // or
        BSONObject or = ScmQueryBuilder.start().or( in, nin ).get();

        // exist
        BSONObject exist = ScmQueryBuilder.start( k1 ).exists( 1 ).get();

        // greaterThan
        BSONObject greaterThan = ScmQueryBuilder.start( k2 ).greaterThan( -1 )
                .get();

        // greaterThanEquals
        BSONObject greaterThanEquals = ScmQueryBuilder.start( k2 )
                .greaterThanEquals( 1 ).get();

        // lessThan
        BSONObject lessThan = ScmQueryBuilder.start( k2 ).lessThan( 10 ).get();

        // is
        BSONObject is = ScmQueryBuilder.start( k1 )
                .is( fileList.get( 0 ).getFileName() ).get();

        // not, lessThanEquals
        BSONObject bs = ScmQueryBuilder.start( k1 )
                .is( fileList.get( 1 ).getFileName() ).get();
        BSONObject not = ScmQueryBuilder.start().not( bs ).get();

        // and
        BSONObject and = ScmQueryBuilder.start().and( lessThan, greaterThan )
                .get();

        ScmQueryBuilder builder = ScmQueryBuilder.start().and( or, exist,
                greaterThan, greaterThanEquals, lessThan, is, not, and );

        // System.out.println(cond.toString());
        String bsStr = "{ \"$and\" : [ { \"$or\" : [ { \"name\" : { \"$in\" : [ \"MthCombin333_0\" , \"MthCombin333_1\"]}} , { \"name\" : { \"$nin\" : [ \"MthCombin333_1\" , \"MthCombin333_2\"]}}]} , { \"name\" : { \"$exists\" : 1}} , { \"major_version\" : { \"$gt\" : -1}} , { \"major_version\" : { \"$gte\" : 1}} , { \"major_version\" : { \"$lt\" : 10}} , { \"name\" : \"MthCombin333_0\"} , { \"$not\" : [ { \"name\" : \"MthCombin333_1\"}]} , { \"$and\" : [ { \"major_version\" : { \"$lt\" : 10}} , { \"major_version\" : { \"$gt\" : -1}}]}]}";
        Assert.assertEquals( builder.get().toString().replaceAll( "\\s*", "" ),
                bsStr.replaceAll( "\\s*", "" ) );

        return builder;
    }

}