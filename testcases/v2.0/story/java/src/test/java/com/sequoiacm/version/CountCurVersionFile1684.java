package com.sequoiacm.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1684:指定当前版本统计文件
 * @author luweikang
 * @createDate 2018.06.12
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class CountCurVersionFile1684 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = null;
    private ScmId fileId = null;

    private String fileName = "fileVersion1684";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();

        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileIdList = new ArrayList< ScmId >();
        for ( int i = 1; i < 6; i++ ) {
            ScmId fileId = ScmFileUtils.createFileByStream( ws,
                    fileName + "_" + i, filedata, fileName );
            fileIdList.add( fileId );
            for ( int j = 0; j < i; j++ ) {
                VersionUtils.updateContentByStream( ws, fileId, updatedata );
            }
        }
        fileId = fileIdList.get( 2 );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        // 组合size/major_version
        BSONObject filter1 = new BasicBSONObject();
        BSONObject[] option1 = {
                new BasicBSONObject( "author", "fileVersion1684" ),
                new BasicBSONObject( "size",
                        new BasicBSONObject( "$gt", 102400 ) ),
                new BasicBSONObject( "major_version",
                        new BasicBSONObject( "$lt", 5 ) ) };
        filter1.put( "$and", option1 );
        String[] fileNameArr1 = { "fileVersion1684_1", "fileVersion1684_2",
                "fileVersion1684_3" };
        listInstanceByOption( ws, ScopeType.SCOPE_CURRENT, filter1,
                fileNameArr1 );

        // 组合id/major_version
        BSONObject filter2 = new BasicBSONObject();
        BSONObject[] option2 = {
                new BasicBSONObject( "author", "fileVersion1684" ),
                new BasicBSONObject( "id", fileId.get() ), new BasicBSONObject(
                        "major_version", new BasicBSONObject( "$gt", 1 ) ) };
        filter2.put( "$and", option2 );
        String[] fileNameArr2 = { "fileVersion1684_3" };
        listInstanceByOption( ws, ScopeType.SCOPE_CURRENT, filter2,
                fileNameArr2 );

        // 组合name/major_version
        BSONObject filter3 = new BasicBSONObject();
        BSONObject[] option3 = {
                new BasicBSONObject( "author", "fileVersion1684" ),
                new BasicBSONObject( "name", "fileVersion1684_1" ),
                new BasicBSONObject( "major_version",
                        new BasicBSONObject( "$lt", 6 ) ),
                new BasicBSONObject( "site_list.0.site_id",
                        site.getSiteId() ) };
        filter3.put( "$and", option3 );
        String[] fileNameArr3 = { "fileVersion1684_1" };
        listInstanceByOption( ws, ScopeType.SCOPE_CURRENT, filter3,
                fileNameArr3 );

        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileIdList.size(); i++ ) {
                    ScmFactory.File.deleteInstance( ws, fileIdList.get( i ),
                            true );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void listInstanceByOption( ScmWorkspace ws, ScopeType scopeType,
            BSONObject filter, String[] fileNameArr ) throws ScmException {
        System.out.println( "filter: " + filter.toString() );
        ScmCursor< ScmFileBasicInfo > fileCursor = ScmFactory.File
                .listInstance( ws, scopeType, filter );

        List< ScmFileBasicInfo > fileInfoList = new ArrayList< ScmFileBasicInfo >();
        while ( fileCursor.hasNext() ) {
            ScmFileBasicInfo fileInfo = fileCursor.getNext();
            fileInfoList.add( fileInfo );
        }
        fileCursor.close();
        Collections.sort( fileInfoList, new ListComparator() );
        String[] actfileNameArr = new String[ fileInfoList.size() ];
        for ( int j = 0; j < fileInfoList.size(); j++ ) {
            actfileNameArr[ j ] = fileInfoList.get( j ).getFileName();
        }
        Assert.assertEquals( fileInfoList.size(), fileNameArr.length,
                "select file number error" );
        Assert.assertEquals( actfileNameArr, fileNameArr,
                "breakpointFile name error" );
    }

    private class ListComparator implements Comparator< ScmFileBasicInfo > {

        @Override
        public int compare( ScmFileBasicInfo fileInfo1,
                ScmFileBasicInfo fileInfo2 ) {
            return fileInfo1.getFileName().compareTo( fileInfo2.getFileName() );
        }
    }

}
