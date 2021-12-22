package com.sequoiacm.rest.serial;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.sequoiacm.client.exception.ScmException;
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
 * @description SCM-1102:提交任务，过滤条件任意拼装
 * @author fanyu
 * @createDate 2018.03.24
 * @updateUser ZhangYanan
 * @updateDate 2021.10.27
 * @updateRemark
 * @version v1.0
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
    private void setUp() throws IOException, ScmException {
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
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        JSONObject options = generateOptions();
        String response1 = rest.setApi( "tasks" )
                .setRequestMethod( HttpMethod.POST )
                .setParameter( "task_type", "2" )
                .setParameter( "workspace_name", ws.getName() )
                .setParameter( "options", options.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        taskId = JSON.parseObject( response1 ).getJSONObject( "task" )
                .getString( "id" );
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
        String response = rest.reset().setApi( "tasks/" + taskId )
                .setRequestMethod( HttpMethod.GET ).exec().getBody().toString();
        taskInfo = JSON.parseObject( response );
        return taskInfo;
    }

    private void check( JSONObject taskInfo ) {
        // checktaskInfo
        JSONObject taskDetail = taskInfo.getJSONObject( "task" );
        Assert.assertEquals( taskDetail.getString( "id" ), taskId );
        Assert.assertEquals( taskDetail.getString( "running_flag" ), "3" );
        Assert.assertEquals( taskDetail.getString( "workspace_name" ),
                ws.getName() );
    }

    private JSONObject generateOptions() {
        JSONObject filter = new JSONObject();
        JSONObject option1 = new JSONObject();
        option1.put( "$lt", "ffffffffffffffffffffffff" );
        filter.put( "id", option1 );
        JSONObject option2 = new JSONObject();
        option2.put( "$lt", author );
        filter.put( "author", option2 );
        JSONObject option3 = new JSONObject();
        option3.put( "$gte", author );
        filter.put( "title", option3 );
        JSONObject option4 = new JSONObject();
        option4.put( "$gte", "application/octet-stream" );
        filter.put( "mime_type", option4 );
        JSONObject option5 = new JSONObject();
        option5.put( "$lt", "2147483647" );
        filter.put( "major_version", option5 );
        JSONObject option6 = new JSONObject();
        option6.put( "$lt", "2147483647" );
        filter.put( "minor_version", option6 );
        JSONObject option7 = new JSONObject();
        option7.put( "$gt", TestScmBase.scmUserName );
        filter.put( "user", option7 );
        filter.put( "attrUnexist", author );
        JSONObject options = new JSONObject();
        options.put( "filter", filter );
        return options;
    }

    public String upload( String filePath, WsWrapper ws, String desc,
            RestWrapper rest )
            throws HttpClientErrorException, FileNotFoundException {
        File file = new File( filePath );
        String wResponse = rest.setApi( "files?workspace_name=" + ws.getName() )
                .setRequestMethod( HttpMethod.POST )
                .setRequestHeaders( "description", desc )
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