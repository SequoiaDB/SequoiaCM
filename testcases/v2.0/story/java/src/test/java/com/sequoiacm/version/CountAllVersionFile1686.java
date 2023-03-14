package com.sequoiacm.version;

import java.util.ArrayList;
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
 * @description SCM-1686:指定所有版本统计文件列表
 * @author luweikang
 * @createDate 2018.06.12
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class CountAllVersionFile1686 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private List< ScmId > fileIdList = null;
    private String fileName = "fileVersion1686";
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
            for ( int j = 1; j < i; j++ ) {
                VersionUtils.updateContentByStream( ws, fileId, updatedata );
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        // 组合major_version
        BSONObject filter1 = new BasicBSONObject();
        BSONObject[] option1 = {
                new BasicBSONObject( "id", fileIdList.get( 0 ).get() ),
                new BasicBSONObject( "major_version",
                        new BasicBSONObject( "$lt", 5 ) ) };
        filter1.put( "$and", option1 );
        listInstanceByOption( ws, filter1, 1 );

        // 组合size/major_version
        BSONObject filter2 = new BasicBSONObject();
        BSONObject[] option2 = {
                new BasicBSONObject( "id", fileIdList.get( 2 ).get() ),
                new BasicBSONObject( "major_version",
                        new BasicBSONObject( "$lt", 4 ) ),
                new BasicBSONObject( "size", 102400 ) };
        filter2.put( "$and", option2 );
        listInstanceByOption( ws, filter2, 1 );

        // 组合name/major_version
        BSONObject filter3 = new BasicBSONObject();
        BSONObject[] option3 = {
                new BasicBSONObject( "id", fileIdList.get( 4 ).get() ),
                new BasicBSONObject( "major_version",
                        new BasicBSONObject( "$gt", 2 ) ),
                new BasicBSONObject( "site_list.0.site_id",
                        site.getSiteId() ) };
        filter3.put( "$and", option3 );
        listInstanceByOption( ws, filter3, 3 );
        runSuccess = true;
    }

    @AfterClass
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

    private void listInstanceByOption( ScmWorkspace ws, BSONObject filter,
            int fileNum ) throws ScmException {
        System.out.println( "filter: " + filter.toString() );
        ScmCursor< ScmFileBasicInfo > fileCursor = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_ALL, filter );

        List< ScmFileBasicInfo > fileInfoList = new ArrayList< ScmFileBasicInfo >();
        while ( fileCursor.hasNext() ) {
            ScmFileBasicInfo fileInfo = fileCursor.getNext();
            fileInfoList.add( fileInfo );
        }
        fileCursor.close();
        Assert.assertEquals( fileInfoList.size(), fileNum,
                "select file number error" );
    }
}
