/**
 *
 */
package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.testng.Assert;
import org.testng.SkipException;

import com.amazonaws.util.json.JSONException;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;

/**
 * @Description BreakpointUtil.java
 * @author luweikang
 * @date 2018年5月16日
 */
public class BreakpointUtil extends TestScmBase {

    /**
     * create file, the file content are randomly generated character
     *
     * @param filePath
     * @param size
     * @throws IOException
     */
    public static void createFile( String filePath, int size )
            throws IOException {
        FileOutputStream fos = null;
        try {
            TestTools.LocalFile.createFile( filePath );
            File file = new File( filePath );
            fos = new FileOutputStream( file );

            byte[] fileBlock = new byte[ size ];
            new Random().nextBytes( fileBlock );
            fos.write( fileBlock );
        } catch ( IOException e ) {
            System.out.println( "create file failed, file=" + filePath );
            throw e;
        } finally {
            if ( fos != null ) {
                fos.close();
            }
        }
    }

    /**
     * create breakpointFile and upload part file
     * @param ws
     * @param filePath
     * @param fileName
     * @param partFileSize
     * @param adler32
     * @throws JSONException
     * @throws ScmException
     * @throws IOException
     */
    public static void createBreakpointFile( ScmWorkspace ws, String filePath,
            String fileName, int partFileSize, ScmChecksumType adler32 )
            throws JSONException, ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, adler32 );
        InputStream inputStream = new BreakpointStream( filePath,
                partFileSize );
        breakpointFile.incrementalUpload( inputStream, false );
        inputStream.close();
    }

    /**
     * @param ws
     * @param fileName
     * @param filePath
     * @param checkFilePath
     * @throws ScmException
     * @throws IOException
     */
    public static void checkScmFile( ScmWorkspace ws, String fileName,
            String filePath, String checkFilePath )
            throws ScmException, IOException {
        TestTools.LocalFile.removeFile( checkFilePath );
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setTitle( fileName );
        ScmId fielId = file.save();

        ScmFile file1 = ScmFactory.File.getInstance( ws, fielId );
        file1.getContent( checkFilePath );
        Assert.assertEquals( TestTools.getMD5( checkFilePath ),
                TestTools.getMD5( filePath ),
                "check breakpointFile to ScmFile" );

        ScmFactory.File.deleteInstance( ws, fielId, true );
        TestTools.LocalFile.removeFile( checkFilePath );
    }

    public static void checkDBDataSource() {
        List< SiteWrapper > sites = ScmInfo.getAllSites();
        for ( SiteWrapper site : sites ) {
            DatasourceType dsType = site.getDataType();
            if ( !dsType.equals( DatasourceType.SEQUOIADB ) ) {
                throw new SkipException(
                        "breakpoint file only support sequoiadb datasourse, " +
                                "skip!" );
            }
        }
    }

}

class BreakpointStream extends InputStream {

    private FileInputStream in = null;
    private int finishByteNum = 0;
    private int breakNum;

    public BreakpointStream( String filePath, int breakNum )
            throws FileNotFoundException {
        this.in = new FileInputStream( filePath );
        this.breakNum = breakNum;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public int read() throws IOException {
        int rs = in.read();
        if ( finishByteNum >= breakNum ) {
            rs = -1;
        }
        finishByteNum++;
        return rs;
    }
}
