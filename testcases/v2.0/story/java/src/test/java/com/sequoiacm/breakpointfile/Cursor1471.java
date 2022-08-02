package com.sequoiacm.breakpointfile;

import java.io.File;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:operator when cursonr open testlink case:seqDB-1471
 * 
 * @author wuyan
 * @Date 2018.05.11
 * @version 1.00
 */

public class Cursor1471 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "breakpointfile1471";
    private int fileSize = 1024 * 3;;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = DBSites.get( new Random().nextInt( DBSites.size() ) );
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        createBreakpointFile();
        BSONObject condition = new BasicBSONObject();
        condition.put( "file_name", fileName );
        ScmCursor< ScmBreakpointFile > cursor = ScmFactory.BreakpointFile
                .listInstance( ws, condition );
        ScmBreakpointFile fileInfo;

        while ( cursor.hasNext() ) {
            fileInfo = cursor.getNext();
            String getfileName = fileInfo.getFileName();
            ScmFactory.BreakpointFile.deleteInstance( ws, getfileName );
        }
        cursor.close();

        // check result,the file is not exist
        ScmCursor< ScmBreakpointFile > cursor1 = ScmFactory.BreakpointFile
                .listInstance( ws, condition );
        Assert.assertEquals( cursor1.hasNext(), false );
        cursor1.close();
    }

    @AfterClass
    private void tearDown() {
        try {
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createBreakpointFile() throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        breakpointFile.upload( new File( filePath ) );
    }

}