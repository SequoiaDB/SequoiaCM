package com.sequoiacm.testcommon.scmutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.testcommon.TestTools;

/**
 * @Description: 全文检索公共方法
 * @author fanyu
 * @Date:2020/9/14
 * @version:1.0
 */
public class FullTextUtils {
    public final static int FILE_NUM = 10;
    public final static int TIMEOUT = 1000 * 500;
    public final static int INTERVAL = 200;
    public final static TestTools.LocalFile.FileType[] fileTypes = TestTools.LocalFile.FileType
            .values();

    /**
     * @descreption 创建文件，默认创建十个
     * @param ws
     * @param fileNamePrefix
     *            文件名前缀
     * @return List< ScmId >
     * @throws Exception
     */
    public static List< ScmId > createFiles( ScmWorkspace ws,
            String fileNamePrefix ) throws Exception {
        return createFiles( ws, fileNamePrefix, FILE_NUM );
    }

    /**
     * @descreption 执行文件数量创建文件，文件类型在TEXT, DOC, DOCX, XLSX, XLS, BMP, PNG,
     *              JPEG中随机选择
     * @param ws
     * @param fileNamePrefix
     * @param fileNum
     * @return List< ScmId >
     * @throws Exception
     */
    public static List< ScmId > createFiles( ScmWorkspace ws,
            String fileNamePrefix, int fileNum ) throws Exception {
        List< ScmId > fileIdList = new ArrayList<>();
        for ( int i = 0; i < fileNum; i++ ) {
            fileIdList.add( ScmFileUtils.create( ws, fileNamePrefix + "-" + i,
                    TestTools.LocalFile.getFileByType(
                            fileTypes[ fileNum % fileTypes.length ] ) ) );
        }
        return fileIdList;
    }

    /**
     * @descreption 检查全文检索和普通查询结果
     * @param ws
     *            工作区
     * @param scope
     *            查询区域
     * @param actFileCondition
     *            es的检索条件
     * @param expFileCondition
     *            db的检索条件
     * @return
     * @throws Exception
     */
    public static void searchAndCheckResults( ScmWorkspace ws,
            ScmType.ScopeType scope, BSONObject actFileCondition,
            BSONObject expFileCondition ) throws Exception {
        waitIndexSyncToES( ws, scope, actFileCondition, expFileCondition );
        try {
            ScmCursor< ScmFulltextSearchResult > fulltextResults = ScmFactory.Fulltext
                    .simpleSeracher( ws ).fileCondition( actFileCondition )
                    .scope( scope ).notMatch( "condition" ).search();
            ScmCursor< ScmFileBasicInfo > normalResults = ScmFactory.File
                    .listInstance( ws, scope, expFileCondition );
            checkCursor( fulltextResults, normalResults );
        } catch ( AssertionError e ) {
            throw new Exception(
                    "wsName = " + ws.getName() + ",scope = " + scope
                            + ",fileCondition = " + actFileCondition.toString(),
                    e );
        }
    }

    /**
     * @descreption 比较全文检索的游标和普通查询的游标
     * @param fulltextResults
     * @param normalResults
     * @return
     * @throws Exception
     */
    public static void checkCursor(
            ScmCursor< ScmFulltextSearchResult > fulltextResults,
            ScmCursor< ScmFileBasicInfo > normalResults ) throws Exception {
        try {
            List< ScmFulltextSearchResult > fulltextList = new ArrayList<>();
            List< ScmFileBasicInfo > normalList = new ArrayList<>();
            int i = 0;
            int j = 0;
            while ( fulltextResults.hasNext() ) {
                fulltextList.add( fulltextResults.getNext() );
                i++;
            }

            while ( normalResults.hasNext() ) {
                normalList.add( normalResults.getNext() );
                j++;
            }

            try {
                Assert.assertEquals( i, j );
            } catch ( AssertionError e ) {
                throw new Exception( e );
            }

            Collections.sort( fulltextList,
                    new Comparator< ScmFulltextSearchResult >() {
                        @Override
                        public int compare( ScmFulltextSearchResult o1,
                                ScmFulltextSearchResult o2 ) {
                            return o1.getFileBasicInfo().toString().compareTo(
                                    o2.getFileBasicInfo().toString() );

                        }
                    } );

            Collections.sort( normalList, new Comparator< ScmFileBasicInfo >() {
                @Override
                public int compare( ScmFileBasicInfo o1, ScmFileBasicInfo o2 ) {
                    return o1.toString().compareTo( o2.toString() );
                }
            } );
            for ( int k = 0; k < normalList.size(); k++ ) {
                ScmFulltextSearchResult actResults = fulltextList.get( k );
                ScmFileBasicInfo act = actResults.getFileBasicInfo();
                ScmFileBasicInfo exp = normalList.get( k );
                try {
                    Assert.assertEquals( act.getFileName(), act.getFileName() );
                    Assert.assertEquals( act.getFileId(), exp.getFileId() );
                    Assert.assertEquals( act.getCreateDate(),
                            exp.getCreateDate() );
                    Assert.assertEquals( act.getUser(), exp.getUser() );
                    Assert.assertEquals( act.getMajorVersion(),
                            exp.getMajorVersion() );
                    Assert.assertEquals( act.getMinorVersion(),
                            exp.getMinorVersion() );
                    Assert.assertEquals( act.getMimeType(), exp.getMimeType() );
                    Assert.assertEquals( actResults.getHighlightTexts().size(),
                            0 );
                    Assert.assertTrue( actResults.getScore() >= 0,
                            actResults.getScore() + "" );
                } catch ( AssertionError e ) {
                    throw new Exception( "k = " + k + ",\nact = "
                            + act.toString() + ", \nexp = " + exp.toString() );
                }
            }
            fulltextList.clear();
            normalList.clear();
        } finally {
            if ( fulltextResults != null ) {
                fulltextResults.close();
            }
            if ( normalResults != null ) {
                fulltextResults.close();
            }
        }
    }

    /**
     * @descreption 等待索引同步到ES
     * @param ws
     * @param scope
     * @param actFileCondition
     * @param expFileCondition
     * @return
     * @throws Exception
     */
    public static void waitIndexSyncToES( ScmWorkspace ws,
            ScmType.ScopeType scope, BSONObject actFileCondition,
            BSONObject expFileCondition ) throws Exception {
        int tryNum = TIMEOUT / INTERVAL;
        long expCount = 0;
        int actCount = 0;
        while ( tryNum-- > 0 ) {
            ScmCursor< ScmFulltextSearchResult > fulltextResults = ScmFactory.Fulltext
                    .simpleSeracher( ws ).fileCondition( actFileCondition )
                    .scope( scope ).notMatch( "condition" ).search();
            expCount = ScmFactory.File.countInstance( ws, scope,
                    expFileCondition );
            actCount = 0;
            while ( fulltextResults.hasNext() ) {
                fulltextResults.getNext();
                actCount++;
            }
            if ( actCount == expCount ) {
                return;
            }
            Thread.sleep( INTERVAL );
        }
        throw new Exception( "time out, ws = " + ws.getName() + ",scope = "
                + scope + ",actFileCondition = " + actFileCondition.toString()
                + ",expFileCondition = " + expFileCondition.toString()
                + ",actCount = " + actCount + ",expCount = " + expCount );
    }

    /**
     * @descreption限时等待工作区达到status
     * @param ws
     * @param status
     * @return
     * @throws Exception
     */
    public static void waitWorkSpaceIndexStatus( ScmWorkspace ws,
            ScmFulltextStatus status ) throws Exception {
        waitWorkSpaceIndexStatus( ws, status, TIMEOUT, INTERVAL );
    }

    /**
     * @descreption 限时等待工作区达到status，超时会抛异常
     * @param ws
     * @param status
     * @param timeout
     * @param interval
     * @return
     * @throws Exception
     */
    public static void waitWorkSpaceIndexStatus( ScmWorkspace ws,
            ScmFulltextStatus status, int timeout, int interval )
            throws Exception {
        int tryNum = timeout / interval;
        while ( tryNum-- > 0 ) {
            if ( ScmFactory.Fulltext.getIndexInfo( ws ).getStatus()
                    .equals( status ) ) {
                System.out.println( "wsName = " + ws.getName() + ", status = "
                        + ScmFactory.Fulltext.getIndexInfo( ws ).getStatus() );
                return;
            }
            Thread.sleep( interval );
        }
        throw new Exception( "time out,wsName = " + ws.getName()
                + "indexInfo = " + ScmFactory.Fulltext.getIndexInfo( ws ) );
    }

    /**
     * @descreption 限时等待文件索引状态达到status
     * @param ws
     * @param status
     * @param expCount
     * @return
     * @throws Exception
     */
    public static void waitFilesStatus( ScmWorkspace ws,
            ScmFileFulltextStatus status, int expCount ) throws Exception {
        waitFilesStatus( ws, status, expCount, TIMEOUT, INTERVAL );
    }

    /**
     * @descreption 限时等待文件索引状态达到status
     * @param ws
     * @param status
     * @param expCount
     * @param timeout
     * @param interval
     * @return
     * @throws Exception
     */
    public static void waitFilesStatus( ScmWorkspace ws,
            ScmFileFulltextStatus status, int expCount, int timeout,
            int interval ) throws Exception {
        int tryNum = timeout / interval;
        long actCount = 0;
        while ( tryNum-- > 0 ) {
            actCount = ScmFactory.File.countInstance( ws,
                    ScmType.ScopeType.SCOPE_ALL,
                    new BasicBSONObject( "external_data.fulltext_status",
                            status.toString() ) );
            if ( actCount == expCount ) {
                return;
            }
            Thread.sleep( interval );
        }
        throw new Exception( "time out,wsName = " + ws.getName()
                + " actCount = " + actCount + ",expCount = " + expCount );
    }

    /**
     * @descreption 限时等待指定单个文件索引状态达到status
     * @param ws
     * @param status
     * @param fileId
     * @return
     * @throws Exception
     */
    public static void waitFileStatus( ScmWorkspace ws,
            ScmFileFulltextStatus status, ScmId fileId ) throws Exception {
        waitFileStatus( ws, status, fileId, TIMEOUT, INTERVAL );
    }

    /**
     * @descreption 限时等待指定单个文件索引状态达到status
     * @param ws
     * @param status
     * @param fileId
     * @param timeout
     * @param interval
     * @return
     * @throws Exception
     */
    public static void waitFileStatus( ScmWorkspace ws,
            ScmFileFulltextStatus status, ScmId fileId, int timeout,
            int interval ) throws Exception {
        int tryNum = timeout / interval;
        ScmFileFulltextStatus actStatus = null;
        while ( tryNum-- > 0 ) {
            actStatus = ScmFactory.Fulltext.getFileIndexInfo( ws, fileId )
                    .getStatus();
            if ( status.equals( actStatus ) ) {
                return;
            }
            Thread.sleep( interval );
        }
        throw new Exception( "time out,  wsName = " + ws.getName()
                + "fileId is " + fileId + ",actStatus = " + actStatus
                + ",expStatus = " + status );
    }
}
