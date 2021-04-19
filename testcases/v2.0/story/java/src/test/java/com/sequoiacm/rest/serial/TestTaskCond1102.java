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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-1102 :: 提交任务，过滤条件任意拼装
 * @author fanyu
 * @Date:2018年3月24日
 * @version:1.0
 */
public class TestTaskCond1102 extends TestScmBase {
    private boolean runSuccess = false;
    private WsWrapper ws = null;
    private File localPath = null;
    private int fileNum = 1;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private JSONArray descs = new JSONArray();
    private String filePath = null;
    private int fileSize = 1024 * 200;
    private String author = "TestTaskCond1102";
    private String taskId = null;
    private RestWrapper rest = null;
    private SiteWrapper site = null;

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
            site = ScmInfo.getBranchSite();
            ws = ScmInfo.getWs();
            rest = new RestWrapper();
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            for ( int i = 0; i < fileNum; i++ ) {
                JSONObject desc = new JSONObject();
                desc.put( "name", author + i );
                desc.put( "author", author );
                desc.put( "title", author + i );
                desc.put( "mime_type", "text/plain" );
                String fileId = upload( filePath, ws, desc.toString(), rest );
                download( fileId, rest, ws );
                fileIdList.add( new ScmId( fileId ) );
                descs.add( desc );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        JSONObject options = generateOptions();
        try {
            String response1 = rest.setApi( "tasks" )
                    .setRequestMethod( HttpMethod.POST )
                    .setParameter( "task_type", "2" )
                    .setParameter( "workspace_name", ws.getName() )
                    .setParameter( "options", options.toString() )
                    .setResponseType( String.class ).exec().getBody()
                    .toString();
            taskId = JSON.parseObject( response1 ).getJSONObject( "task" )
                    .getString( "id" );
        } catch ( HttpClientErrorException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        waitTaskStop( taskId, rest );
        JSONObject taskInfo = getTaskInfo( taskId, rest );
        check( taskInfo );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    rest.reset()
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
            if ( rest != null ) {
                rest.disconnect();
            }
        }
    }

    private void waitTaskStop( String taskId, RestWrapper rest ) {
        String stopTime = null;
        JSONObject taskInfo = null;
        while ( stopTime == null ) {
            taskInfo = getTaskInfo( taskId, rest );
            stopTime = taskInfo.getJSONObject( "task" )
                    .getString( "stop_time" );
        }
    }

    private JSONObject getTaskInfo( String taskId, RestWrapper rest ) {
        JSONObject taskInfo = null;
        try {
            String response = rest.reset().setApi( "tasks/" + taskId )
                    .setRequestMethod( HttpMethod.GET ).exec().getBody()
                    .toString();
            taskInfo = JSON.parseObject(  response );
        } catch ( HttpClientErrorException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        return taskInfo;
    }

    private void check( JSONObject taskInfo ){
        // checktaskInfo
        JSONObject taskDetail = taskInfo.getJSONObject( "task" );
        Assert.assertEquals( taskDetail.getString( "id" ), taskId );
        Assert.assertEquals( taskDetail.getString( "running_flag" ), "3" );
        Assert.assertEquals( taskDetail.getString( "workspace_name" ),
                ws.getName() );
    }

    private JSONObject generateOptions()  {
        JSONObject filter = new JSONObject();
        filter.put( "id",
                new JSONObject().put( "$lt", "ffffffffffffffffffffffff" ) );
        filter.put( "author", new JSONObject().put( "$lt", author ) );
        filter.put( "title", new JSONObject().put( "$gte", author ) );
        filter.put( "mime_type",
                new JSONObject().put( "$gte", "application/octet-stream" ) );
        filter.put( "major_version",
                new JSONObject().put( "$lt", "2147483647" ) );
        filter.put( "minor_version",
                new JSONObject().put( "$lt", "2147483647" ) );
        filter.put( "user",
                new JSONObject().put( "$gt", TestScmBase.scmUserName ) );
        filter.put( "attrUnexist", author );
        JSONObject options = new JSONObject();
        options.put( "filter", filter );
        return options;
    }

    public String upload( String filePath, WsWrapper ws, String desc,
            RestWrapper rest ) throws HttpClientErrorException,FileNotFoundException {
        File file = new File( filePath );
        // FileSystemResource resource = new FileSystemResource(file);
        String wResponse = rest.setApi( "files?workspace_name=" + ws.getName() )
                .setRequestMethod( HttpMethod.POST )
                // .setParameter("file", resource)
                // .setParameter("description", desc)
                .setRequestHeaders( "description", desc.toString() )
                .setInputStream( new FileInputStream( file ) )
                .setResponseType( String.class ).exec().getBody().toString();
        String fileId = JSON.parseObject( wResponse ).getJSONObject( "file" )
                .getString( "id" );
        return fileId;
    }

    public void download( String fileId, RestWrapper rest, WsWrapper ws ) {
        rest.reset()
                .setApi( "files/" + fileId + "?workspace_name=" + ws.getName() )
                .setRequestMethod( HttpMethod.GET )
                .setResponseType( Resource.class ).exec();
    }
}