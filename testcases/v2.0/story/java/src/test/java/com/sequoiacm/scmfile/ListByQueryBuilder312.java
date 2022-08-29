package com.sequoiacm.scmfile;

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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-312:带查询条件查询文件列表，覆盖所有匹配符组合查询
 * @author huangxiaoni init
 * @date 2017.6.6
 */

public class ListByQueryBuilder312 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private List< ScmFile > fileList = new ArrayList<>();
    private int fileNum = 5;
    private String fileName = "list312_";
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
    private void test() throws Exception {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            ScmQueryBuilder builder = ScmBuilder();
            ScopeType scopeType = ScopeType.SCOPE_CURRENT;
            BSONObject cond = builder.get();
            cursor = ScmFactory.File.listInstance( ws, scopeType, cond );
            int size = 0;
            ScmFileBasicInfo fileInfo;
            while ( cursor.hasNext() ) {
                fileInfo = cursor.getNext();
                // check results
                Assert.assertEquals( fileInfo.getFileName(),
                        fileList.get( 0 ).getFileName() );
                Assert.assertEquals( fileInfo.getFileId(),
                        fileList.get( 0 ).getFileId(), fileInfo.toString() );
                Assert.assertEquals( fileInfo.getMinorVersion(),
                        fileList.get( 0 ).getMinorVersion() );
                Assert.assertEquals( fileInfo.getMajorVersion(),
                        fileList.get( 0 ).getMajorVersion() );
                size++;
            }
            Assert.assertEquals( size, 1 );

            runSuccess = true;
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( cursor != null )
                cursor.close();
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
        } catch ( Exception e ) {
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
                file.setFileName( fileName + i );
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

        // not, lessThanEquals
        BSONObject obj = ScmQueryBuilder.start( k2 ).lessThanEquals( 0 ).get();
        BSONObject not = ScmQueryBuilder.start().not( obj ).get();

        // is
        BSONObject is = ScmQueryBuilder.start( k1 )
                .is( fileList.get( 0 ).getFileName() ).get();

        // and
        BSONObject and = ScmQueryBuilder.start().and( lessThan, greaterThan )
                .get();

        ScmQueryBuilder builder = ScmQueryBuilder.start().and( or, exist,
                greaterThan, greaterThanEquals, lessThan, is, not, and );

        // System.out.println(cond.toString());
        String bsStr = "{ \"$and\" : [ { \"$or\" : [ { \"name\" : { \"$in\" : [ \""
                + fileName + "0\" , \"" + fileName
                + "1\"]}} , { \"name\" : { \"$nin\" : [ \"" + fileName
                + "1\" , \"" + fileName
                + "2\"]}}]} , { \"name\" : { \"$exists\" : 1}} , { \"major_version\" : { \"$gt\" : -1}} , { \"major_version\" : { \"$gte\" : 1}} , { \"major_version\" : { \"$lt\" : 10}} , { \"name\" : \""
                + fileName
                + "0\"} , { \"$not\" : [ { \"major_version\" : { \"$lte\" : 0}}]} , { \"$and\" : [ { \"major_version\" : { \"$lt\" : 10}} , { \"major_version\" : { \"$gt\" : -1}}]}]}";
        Assert.assertEquals( builder.get().toString().replaceAll( "\\s*", "" ),
                bsStr.replaceAll( "\\s*", "" ) );

        return builder;
    }

}