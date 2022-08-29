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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-383: or多个不同字段，覆盖所有文件属性
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、countInstance带查询条件查询文件，对多个不同字段做or匹配查询，覆盖所有文件属性； 2、检查ScmQueryBuilder结果正确性；
 * 3、检查查询结果正确性；
 */

public class Or383 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private static String authorName = "Or383";
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileNum = 2;

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
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void testQuery() throws Exception {
        try {
            // build condition
            BSONObject cond = null;
            Object[][] kvs = this.kvsArr();
            ScmQueryBuilder builder = null;
            String bsStr = "{ \"$or\" : [ ";
            for ( Object[] kv : kvs ) {
                String key = ( String ) kv[ 0 ];
                Object value = kv[ 1 ];
                String subStr = null;
                if ( kv[ 1 ] instanceof String ) {
                    subStr = "{ \"" + key + "\" : \"" + value + "\"}";
                } else {
                    subStr = "{ \"" + key + "\" : " + value + "}";
                }
                BSONObject obj = ScmQueryBuilder.start( ( String ) kv[ 0 ] )
                        .is( kv[ 1 ] ).get();
                if ( null == builder ) {
                    builder = ScmQueryBuilder.start().or( obj );
                    bsStr = bsStr + subStr;
                } else {
                    builder.or( obj );
                    bsStr = bsStr + " , " + subStr;
                }
            }
            cond = builder.get();
            bsStr = bsStr + "]}";
            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    bsStr.replaceAll( "\\s*", "" ) );

            // count
            long count = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            // 考虑与其他用例并行，这里只是弱覆盖，确保bson可执行即可
            if ( count < 1 ) {
                Assert.fail( "expect count is 2 or more, but found " + count );
            }

            runSuccess = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
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
                scmfile.setFileName( authorName + "_"
                        + TestTools.getRandomString( 10 ) + i );
                scmfile.setAuthor( authorName );
                scmfile.setTitle( TestTools.getRandomString( 10 ) + i );
                scmfile.setMimeType( TestTools.getRandomString( 10 ) + i );
                ScmId fileId = scmfile.save();
                fileIdList.add( fileId );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private Object[][] kvsArr() throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileIdList.get( 0 ) );
        return new Object[][] {
                new Object[] { ScmAttributeName.File.FILE_ID,
                        file.getFileId().get() },
                new Object[] { ScmAttributeName.File.FILE_NAME,
                        file.getFileName() },
                new Object[] { ScmAttributeName.File.AUTHOR, file.getAuthor() },
                new Object[] { ScmAttributeName.File.TITLE, file.getTitle() },
                new Object[] { ScmAttributeName.File.MIME_TYPE,
                        file.getMimeType() },
                new Object[] { ScmAttributeName.File.SIZE, file.getSize() },
                new Object[] { ScmAttributeName.File.MAJOR_VERSION,
                        file.getMajorVersion() },
                new Object[] { ScmAttributeName.File.MINOR_VERSION,
                        file.getMinorVersion() },
                new Object[] { ScmAttributeName.File.USER, file.getUser() },
                new Object[] { ScmAttributeName.File.CREATE_TIME,
                        file.getCreateTime().getTime() },
                new Object[] { ScmAttributeName.File.UPDATE_USER,
                        file.getUpdateUser() },
                new Object[] { ScmAttributeName.File.UPDATE_TIME,
                        file.getUpdateTime().getTime() } };
    }

}