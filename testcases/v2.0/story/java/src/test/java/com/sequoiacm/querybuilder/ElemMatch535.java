package com.sequoiacm.querybuilder;

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

/**
 * @FileName SCM-535: eleMatch多个字段
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、countInstance带查询条件查询文件，eleMatch匹配查询某个字段的多个对象， 覆盖相同相同值和不同对象不同值；
 * 2、检查ScmQueryBuilder结果正确性； 3、检查查询结果正确性；
 */

public class ElemMatch535 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private ScmId fileId = null;
    private String fileName = "EleMatch535";
    private String author = fileName;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileNum = 5;
    private List< ScmFile > fileList = new ArrayList< ScmFile >();

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

    @Test(groups = { GroupTags.base })
    private void testEleMatch() throws Exception {
        try {
            String key = ScmAttributeName.File.SITE_LIST;
            String objKey1 = ScmAttributeName.File.SITE_ID;
            String objkey2 = ScmAttributeName.File.LAST_ACCESS_TIME;
            BSONObject obj = ScmQueryBuilder.start( objKey1 )
                    .is( site.getSiteId() ).put( objkey2 ).greaterThan( 123456 )
                    .get();
            BSONObject cond = ScmQueryBuilder.start( key ).elemMatch( obj )
                    .and( ScmAttributeName.File.TITLE ).is( author ).get();
            // count
            long count = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( count, fileNum - 2 );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void readyScmFile() {
        try {
            ScmFile file = null;
            for ( int i = 0; i < fileNum - 2; i++ ) {
                file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName + "_" + UUID.randomUUID() );
                file.setTitle( author );
                file.setAuthor( author );
                fileId = file.save();
                fileList.add( file );
                fileIdList.add( fileId );
            }

            for ( int j = 0; j < 2; j++ ) {
                file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName + "_" + UUID.randomUUID() );
                file.setTitle( author + "_12" );
                file.setAuthor( author );
                fileId = file.save();
                fileList.add( file );
                fileIdList.add( fileId );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}
