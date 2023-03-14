package com.sequoiacm.scmfile.serial;

import java.io.File;
import java.util.LinkedList;
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
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-126:获取ws下所有文件列表
 * @author huangxiaoni init
 * @date 2017.4.6
 */

public class ListInstanceByWS126 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String author = "scmfile126";
    private int fileSize = 1024 * 10;
    private int fileNum = 3;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIdList = new LinkedList< ScmId >();

    private long preCount = 0;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            this.cleanFile();

            for ( int i = 0; i < fileNum; i++ ) {
                ScmId fileId = ScmFileUtils.create( ws, author + "_" + i,
                        filePath );
                fileIdList.add( fileId );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.base })
    private void testListInstanceByWS() {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            ScopeType scopeType = ScopeType.SCOPE_CURRENT;
            BSONObject condition = null;
            condition = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( author + "_" + 0 ).get();
            cursor = ScmFactory.File.listInstance( ws, scopeType, condition );
            int size = 0;
            ScmFileBasicInfo file;
            while ( cursor.hasNext() ) {
                file = cursor.getNext();
                // check results
                ScmId fileId = file.getFileId();
                fileIdList.contains( fileId );
                checkFileAttributes( file, fileId );

                size++;
            }
            Assert.assertEquals( size, 1 );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            cursor.close();
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void checkFileAttributes( ScmFileBasicInfo file, ScmId fileId ) {
        try {
            Assert.assertNotNull( file.getFileId() );
            Assert.assertEquals( file.getFileId().get(), fileId.get() );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void cleanFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR )
                    .is( author + "_" + i ).get();
            preCount = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, condition );
            if ( 0 != preCount ) {
                ScmFileUtils.cleanFile( wsp, condition );
            }
        }
    }
}