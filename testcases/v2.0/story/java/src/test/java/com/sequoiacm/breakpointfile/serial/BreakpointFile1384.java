/**
 *
 */
package com.sequoiacm.breakpointfile.serial;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
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
 * @Description BreakpointFile1384.java 指定条件查询断点文件信息
 * @author luweikang
 * @date 2018年5月11日
 */
public class BreakpointFile1384 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile1384";
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize;
        for ( int i = 0; i < 10; i++ ) {
            BreakpointUtil.createFile( filePath + "_" + i + ".txt",
                    fileSize * i );
        }

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws ScmException, IOException {

        // 创建10个断点文件
        this.createBreakpointFile();

        // 带$or匹配符查询
        BSONObject filter1 = new BasicBSONObject();
        BSONObject[] option1 = {
                new BasicBSONObject( "file_name", "scmfile1384_1" ),
                new BasicBSONObject( "file_name", "scmfile1384_5" ) };
        filter1.put( "$or", option1 );
        String[] fileNameArr1 = { "scmfile1384_1", "scmfile1384_5" };
        listInstanceByOption( filter1, 2, fileNameArr1 );

        // 带$and匹配符查询
        BSONObject filter2 = new BasicBSONObject();
        BSONObject[] option2 = {
                new BasicBSONObject( "upload_size",
                        new BasicBSONObject( "$gt", 3145728 ) ),
                new BasicBSONObject( "upload_size",
                        new BasicBSONObject( "$lt", 8388608 ) ) };
        filter2.put( "$and", option2 );
        String[] fileNameArr2 = { "scmfile1384_4", "scmfile1384_5",
                "scmfile1384_6", "scmfile1384_7" };
        listInstanceByOption( filter2, 4, fileNameArr2 );

        // 带$gt匹配符查询
        BSONObject filter3 = new BasicBSONObject();
        filter3.put( "upload_size", new BasicBSONObject( "$gt", 7340032 ) );
        String[] fileNameArr3 = { "scmfile1384_8", "scmfile1384_9" };
        listInstanceByOption( filter3, 2, fileNameArr3 );

        // 带$get匹配符查询
        BSONObject filter4 = new BasicBSONObject();
        filter4.put( "upload_size", new BasicBSONObject( "$gte", 7340032 ) );
        String[] fileNameArr4 = { "scmfile1384_7", "scmfile1384_8",
                "scmfile1384_9" };
        listInstanceByOption( filter4, 3, fileNameArr4 );

        // 带$lt匹配符查询
        BSONObject filter5 = new BasicBSONObject();
        filter5.put( "upload_size", new BasicBSONObject( "$lt", 3145728 ) );
        String[] fileNameArr5 = { "scmfile1384_0", "scmfile1384_1",
                "scmfile1384_2" };
        listInstanceByOption( filter5, 3, fileNameArr5 );

        // 带$lt匹配符查询
        BSONObject filter6 = new BasicBSONObject();
        filter6.put( "upload_size", new BasicBSONObject( "$lte", 3145728 ) );
        String[] fileNameArr6 = { "scmfile1384_0", "scmfile1384_1",
                "scmfile1384_2", "scmfile1384_3" };
        listInstanceByOption( filter6, 4, fileNameArr6 );

        // 带$ne匹配符查询
        BSONObject filter7 = new BasicBSONObject();
        filter7.put( "checksum_type", new BasicBSONObject( "$ne", "ADLER32" ) );
        String[] fileNameArr7 = { "scmfile1384_0", "scmfile1384_2",
                "scmfile1384_4", "scmfile1384_6", "scmfile1384_8" };
        listInstanceByOption( filter7, 5, fileNameArr7 );

        // 带$not匹配符查询
        BSONObject filter8 = new BasicBSONObject();
        BSONObject[] option8 = {
                new BasicBSONObject( "checksum_type", "CRC32" ) };
        filter8.put( "$not", option8 );
        String[] fileNameArr8 = { "scmfile1384_1", "scmfile1384_3",
                "scmfile1384_5", "scmfile1384_7", "scmfile1384_9" };
        listInstanceByOption( filter8, 5, fileNameArr8 );

    }

    @AfterClass
    private void tearDown() {
        try {
            for ( int i = 0; i < 10; i++ ) {
                ScmFactory.BreakpointFile.deleteInstance( ws,
                        fileName + "_" + i );
            }
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void createBreakpointFile() throws ScmException, IOException {
        for ( int i = 0; i < 10; i++ ) {
            ScmChecksumType checkType = null;
            if ( i % 2 == 1 ) {
                checkType = ScmChecksumType.ADLER32;
            } else {
                checkType = ScmChecksumType.CRC32;
            }
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .createInstance( ws, fileName + "_" + i, checkType );
            InputStream inputStream = new FileInputStream(
                    filePath + "_" + i + ".txt" );
            breakpointFile.upload( inputStream );
            inputStream.close();
        }
    }

    private void listInstanceByOption( BSONObject filter, int fileNum,
            String[] fileNameArr ) throws ScmException {
        System.out.println( "filter: " + filter.toString() );
        ScmCursor< ScmBreakpointFile > bFileCursor = ScmFactory.BreakpointFile
                .listInstance( ws, filter );
        List< ScmBreakpointFile > sbFileList = new ArrayList< ScmBreakpointFile >();
        while ( bFileCursor.hasNext() ) {
            ScmBreakpointFile actSBFile = bFileCursor.getNext();
            sbFileList.add( actSBFile );
        }
        bFileCursor.close();
        Collections.sort( sbFileList, new ListComparator() );
        String[] actfileNameArr = new String[ sbFileList.size() ];
        for ( int j = 0; j < sbFileList.size(); j++ ) {
            actfileNameArr[ j ] = sbFileList.get( j ).getFileName();
        }
        Assert.assertEquals( sbFileList.size(), fileNum,
                "select file number error" );
        Assert.assertEquals( actfileNameArr, fileNameArr,
                "breakpointFile name error" );
    }

    private class ListComparator implements Comparator< ScmBreakpointFile > {

        @Override
        public int compare( ScmBreakpointFile file1, ScmBreakpointFile file2 ) {
            return file1.getFileName().compareTo( file2.getFileName() );
        }
    }
}
