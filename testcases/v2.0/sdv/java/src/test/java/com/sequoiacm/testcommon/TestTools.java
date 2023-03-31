package com.sequoiacm.testcommon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Random;

import org.apache.log4j.Logger;

public class TestTools {
    private static final Logger logger = Logger.getLogger( TestSdbTools.class );

    /**
     * get file's md5
     *
     * @param pathName
     * @return
     * @throws IOException
     */
    public static String getMD5( String pathName ) throws IOException {
        File file = new File( pathName );
        try ( FileInputStream fis = new FileInputStream( file )) {
            byte[] buffer = new byte[ ( int ) file.length() ];
            fis.read( buffer );
            return getMD5( buffer );
        }
    }

    public static String getMD5( Object buffer ) {
        String value = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance( "MD5" );
            if ( buffer instanceof ByteBuffer ) {
                md5.update( ( ByteBuffer ) buffer );
            } else if ( buffer instanceof byte[] ) {
                md5.update( ( byte[] ) buffer );
            } else {
                throw new IllegalArgumentException( "invalid type of buffer" );
            }
            BigInteger bi = new BigInteger( 1, md5.digest() );
            value = bi.toString( 16 );
        } catch ( NoSuchAlgorithmException e ) {
            e.printStackTrace();
            throw new RuntimeException( "fail to get md5!" + e.getMessage() );
        }
        return value;
    }

    /**
     * random generate string
     *
     * @param length
     * @return character string
     */
    public static String getRandomString( int length ) {
        String str = "adcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < length; i++ ) {
            int number = random.nextInt( str.length() );
            sb.append( str.charAt( number ) );
        }
        return sb.toString();
    }

    /**
     * get buffer
     *
     * @param filePath
     * @return byte[]
     * @throws IOException
     */
    public static byte[] getBuffer( String filePath ) throws IOException {
        File file = new File( filePath );
        long fileSize = file.length();
        if ( fileSize > Integer.MAX_VALUE ) {
            System.out.println( "file too big..." );
            return null;
        }
        FileInputStream fi = new FileInputStream( file );
        byte[] buffer = new byte[ ( int ) fileSize ];
        int offset = 0;
        int numRead = 0;
        while ( offset < buffer.length && ( numRead = fi.read( buffer, offset,
                buffer.length - offset ) ) >= 0 ) {
            offset += numRead;
        }
        // 确保所有数据均被读取
        if ( offset != buffer.length ) {
            extracted( file );
        }
        fi.close();
        return buffer;
    }

    private static void extracted( File file ) throws IOException {
        throw new IOException( "failed to get buffer, file=" + file.getName() );
    }

    /**
     * get method... name
     */
    public static String getMethodName() {
        return Thread.currentThread().getStackTrace()[ 2 ].getMethodName();
    }

    public static String getClassName() {
        String fullClassName = Thread.currentThread().getStackTrace()[ 2 ]
                .getClassName();
        int index = fullClassName.lastIndexOf( "." );
        return fullClassName.substring( index + 1 );
    }

    public static void setSystemTime( String host, Long date )
            throws Exception {
        String time = new SimpleDateFormat( "\"yyyy-MM-dd HH:mm:ss\"" )
                .format( date );
        setSystemTime( host, time );
    }

    /**
     * set the system time for the host
     *
     * @param host
     * @param dateStr,
     *            e.g: yyyyMMdd
     * @throws Exception
     */
    public static void setSystemTime( String host, String dateStr )
            throws Exception {
        Ssh ssh = null;
        try {
            ssh = new Ssh( host );

            // set date
            String cmd = "date -s " + dateStr;
            ssh.exec( cmd );

            // print local date after set date
            ssh.exec( "date" );
            String localDate = ssh.getStdout().split( "\n" )[ 0 ];
            System.out.println( "host = " + host + ", localDate = " + localDate
                    + ", after set system time" );
        } finally {
            if ( null != ssh ) {
                ssh.disconnect();
            }
        }
    }

    /**
     * restore the system time for the host
     *
     * @param host
     * @throws Exception
     */
    public static void restoreSystemTime( String host ) throws Exception {
        Ssh ssh = null;
        try {
            ssh = new Ssh( host );
            String cmd = "ntpdate " + TestScmBase.ntpServer;

            // in case of time server not usable, retry in 1 min
            int times = 60;
            int intervalSec = 1;
            boolean restoreOk = false;
            Exception lastException = null;
            for ( int i = 0; i < times; ++i ) {
                try {
                    ssh.exec( cmd );
                    restoreOk = true;
                    break;
                } catch ( Exception e ) {
                    lastException = e;
                    Thread.sleep( intervalSec );
                }
            }

            // print local date after set date
            ssh.exec( "date" );
            String localDate = ssh.getStdout().split( "\n" )[ 0 ];
            System.out.println( "host = " + host + ", localDate = " + localDate
                    + ", after restore system time" );

            if ( !restoreOk ) {
                throw lastException;
            }
        } finally {
            if ( null != ssh ) {
                ssh.disconnect();
            }
        }
    }

    public static class LocalFile {

        /**
         * read the specify file length after seek, to compare the read results
         * with SCM
         *
         * @param filePath
         * @param size
         * @param len
         * @param downloadPath
         * @throws FileNotFoundException
         * @throws IOException
         */
        public static void readFile( String filePath, int size, int len,
                String downloadPath )
                throws FileNotFoundException, IOException {
            RandomAccessFile raf = null;
            OutputStream fos = null;
            try {
                raf = new RandomAccessFile( filePath, "rw" );
                fos = new FileOutputStream( downloadPath );
                raf.seek( size );
                int off = 0;
                int readSize = 0;
                byte[] buf = new byte[ off + len ];
                readSize = raf.read( buf, off, len );
                fos.write( buf, off, readSize );
            } finally {
                if ( raf != null )
                    raf.close();
                if ( fos != null )
                    fos.close();
            }
        }

        /**
         * read the entire file length after the seek, to compare the read
         * results with SCM
         *
         * @param sourceFile
         * @param size
         * @param outputFile
         * @throws FileNotFoundException
         * @throws IOException
         */
        public static void readFile( String sourceFile, int size,
                String outputFile ) throws FileNotFoundException, IOException {
            RandomAccessFile raf = null;
            OutputStream fos = null;
            try {
                raf = new RandomAccessFile( sourceFile, "rw" );
                fos = new FileOutputStream( outputFile );
                raf.seek( size );
                int readSize = 0;
                int off = 0;
                int len = 1024 * 1024;
                byte[] buf = new byte[ off + len ];
                while ( true ) {
                    readSize = raf.read( buf, off, len );
                    if ( readSize <= 0 ) {
                        break;
                    }
                    fos.write( buf, off, readSize );
                }
            } finally {
                if ( raf != null )
                    raf.close();
                if ( fos != null )
                    fos.close();
            }
        }

        /**
         * create local directory
         *
         * @param dir
         */
        public static void createDir( String dir ) {
            mkdir( new File( dir ) );
        }

        private static void mkdir( File filePath ) {
            if ( !filePath.getParentFile().exists() ) {
                mkdir( filePath.getParentFile() );
            }
            boolean mkSuccess = filePath.mkdir();
            if ( !mkSuccess && !filePath.exists() ) {
                mkdir( filePath );
            }
        }

        /**
         * remove directory including directories and sub files
         *
         * @param filePath
         */
        public static void removeFile( String filePath ) {
            File file = new File( filePath );
            if ( file.exists() ) {
                file.delete();
            }
        }

        public static void removeFile( File file ) {
            if ( file.exists() ) {
                if ( file.isFile() ) {
                    file.delete();
                } else {
                    File[] files = file.listFiles();
                    for ( File subFile : files ) {
                        removeFile( subFile );
                    }

                    file.delete();
                }
            }
        }

        /**
         * create empty file
         *
         * @param filePath
         * @throws IOException
         */
        public static void createFile( String filePath ) throws IOException {
            File file = new File( filePath );
            if ( file.exists() ) {
                file.delete();
            }
            mkdir( file.getParentFile() );
            file.createNewFile();
        }

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
                createFile( filePath );
                File file = new File( filePath );
                fos = new FileOutputStream( file );

                int written = 0;
                byte[] fileBlock = new byte[ 1024 ];
                new Random().nextBytes( fileBlock );
                while ( written < size ) {
                    int toWrite = size - written;
                    int len = fileBlock.length < toWrite ? fileBlock.length
                            : toWrite;
                    fos.write( fileBlock, 0, len );
                    written += len;
                }
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
         * create file, the file content are specify characters
         *
         * @param filePath
         * @param content
         * @param size
         * @throws IOException
         */
        public static void createFile( String filePath, String content,
                int size ) throws IOException {
            FileOutputStream fos = null;
            byte[] contentBytes = content.getBytes();
            int written = 0;
            try {
                File file = new File( filePath );
                fos = new FileOutputStream( file );
                while ( written < size ) {
                    int toWrite = size - written;
                    int len = contentBytes.length < toWrite
                            ? contentBytes.length
                            : toWrite;
                    fos.write( contentBytes, 0, len );
                    written += len;
                }
            } catch ( IOException e ) {
                System.out.println( "create file failed:file=" + filePath );
                e.printStackTrace();
            } finally {
                if ( fos != null )
                    fos.close();
            }
        }

        /**
         * create download path and file, by methodName and threadId
         */
        public static String initDownloadPath( File localPath,
                String methodName, long threadId ) throws Exception {
            String downloadPath = null;
            try {
                int randomId = new Random().nextInt( 10000 );
                String downLoadDir = localPath + File.separator + methodName;
                createDir( downLoadDir );
                downloadPath = downLoadDir + File.separator + "thread-"
                        + threadId + "_" + System.currentTimeMillis() + "_"
                        + randomId + ".lob";
            } catch ( Exception e ) {
                logger.info( "downloadPath\n" + downloadPath );
                throw e;
            }
            return downloadPath;
        }
    }

}
