/**
 *
 */
package com.sequoiacm.rest.serial;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1101 :: 提交清理任务（post请求）/获取任务信息
 * @author fanyu
 * @Date:2018年3月22日
 * @version:1.0
 */
public class CleanByPost1101 extends TestScmBase {
    private boolean runSuccess = false;
    private WsWrapper ws = null;
    private File localPath = null;
    private int fileNum = 10;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private JSONArray descs = new JSONArray();
    private String filePath = null;
    private int fileSize = 1024;
    private String author = "CleanByPost1101";
    private String taskId = null;
    private RestWrapper rest1 = null;
    private RestWrapper rest2 = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            ws = ScmInfo.getWs();
            rootSite = ScmInfo.getRootSite();
            branchSite = ScmInfo.getBranchSite();
            rest1 = new RestWrapper();
            rest2 = new RestWrapper();
            rest1.connect( rootSite.getSiteServiceName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            rest2.connect( branchSite.getSiteServiceName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            for ( int i = 0; i < fileNum; i++ ) {
                JSONObject desc = new JSONObject();
                desc.put( "name", author + i );
                desc.put( "author", author );
                desc.put( "title", author + i );
                desc.put( "mime_type", "text/plain" );
                String fileId = upload( filePath, ws, desc.toString(), rest1 );
                download( fileId, rest2 );
                fileIdList.add( new ScmId( fileId ) );
                descs.put( desc );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        JSONObject options = new JSONObject().put( "filter",
                new JSONObject().put( "author", author ) );
        try {
            String response1 = rest2.reset().setApi( "tasks" )
                    .setRequestMethod( HttpMethod.POST )
                    .setParameter( "task_type", "2" )
                    .setParameter( "workspace_name", ws.getName() )
                    .setParameter( "options", options.toString() )
                    .setResponseType( String.class ).exec().getBody()
                    .toString();
            taskId = new JSONObject( response1 ).getJSONObject( "task" )
                    .getString( "id" );
        } catch ( HttpClientErrorException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        waitTaskStop();
        JSONObject taskInfo = getTaskInfo( taskId );
        check( taskInfo );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    rest1.reset()
                            .setApi( "files/" + fileId.get()
                                    + "?workspace_name=" + ws.getName()
                                    + "&is_physical=true" )
                            .setRequestMethod( HttpMethod.DELETE )
                            .setResponseType( String.class ).exec();
                    TestSdbTools.Task.deleteMeta( new ScmId( taskId ) );
                    TestTools.LocalFile.removeFile( localPath );
                }
            }
        } finally {
            if ( rest1 != null ) {
                rest1.disconnect();
            }
            if ( rest2 != null ) {
                rest2.disconnect();
            }
        }
    }

    private void waitTaskStop() throws JSONException {
        String stopTime = "null";
        JSONObject taskInfo = null;
        while ( stopTime.equals( "null" ) ) {
            taskInfo = getTaskInfo( taskId );
            stopTime = taskInfo.getJSONObject( "task" )
                    .getString( "stop_time" );
        }
    }

    private JSONObject getTaskInfo( String taskId ) throws JSONException {
        JSONObject taskInfo = null;
        try {
            String response = rest1.reset().setApi( "tasks/" + taskId )
                    .setRequestMethod( HttpMethod.GET ).exec().getBody()
                    .toString();
            taskInfo = new JSONObject( response );
        } catch ( HttpClientErrorException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        return taskInfo;
    }

    private void check( JSONObject taskInfo ) throws JSONException {
        // check site
        SiteWrapper[] expSiteList = { ScmInfo.getRootSite() };
        try {
            ScmFileUtils.checkMetaAndData( ws, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // checktaskInfo
        JSONObject taskDetail = taskInfo.getJSONObject( "task" );
        Assert.assertEquals( taskDetail.getString( "id" ), taskId );
        Assert.assertEquals( taskDetail.getString( "running_flag" ), "3" );
        Assert.assertEquals( taskDetail.getString( "workspace_name" ),
                ws.getName() );
    }

    public String upload( String filePath, WsWrapper ws, String desc,
            RestWrapper rest ) throws HttpClientErrorException, JSONException,
            FileNotFoundException {
        File file = new File( filePath );
        // FileSystemResource resource = new FileSystemResource(file);
        String wResponse = rest.setApi( "files?workspace_name=" + ws.getName() )
                .setRequestMethod( HttpMethod.POST )
                // .setParameter("file", resource)
                // .setParameter("description", desc)
                .setRequestHeaders( "description", desc.toString() )
                .setInputStream( new FileInputStream( file ) )
                .setResponseType( String.class ).exec().getBody().toString();
        String fileId = new JSONObject( wResponse ).getJSONObject( "file" )
                .getString( "id" );
        return fileId;
    }

    public void download( String fileId, RestWrapper rest ) {
        rest.reset()
                .setApi( "files/" + fileId + "?workspace_name=" + ws.getName() )
                .setRequestMethod( HttpMethod.GET )
                .setResponseType( Resource.class ).exec();
    }
}