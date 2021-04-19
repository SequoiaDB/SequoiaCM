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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1103 :: 并发提交多个不同ws下的任务/获取任务列表
 * @author fanyu
 * @Date:2018年3月22日
 * @version:1.0
 */
public class TasksWithMutilWs1103 extends TestScmBase {
    private boolean runSuccess = false;
    private List< WsWrapper > wsList = new ArrayList< WsWrapper >();
    private File localPath = null;
    private int fileNum = 10;
    private List< ScmId > fileIdList1 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList2 = new ArrayList< ScmId >();
    private JSONArray descs = new JSONArray();
    private String filePath = null;
    private int fileSize = 1024;
    private String author = "TasksWithMutilWs1103";
    private String taskId1 = null;
    private String taskId2 = null;
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
            wsList = ScmInfo.getAllWorkspaces();
            rootSite = ScmInfo.getRootSite();
            branchSite = ScmInfo.getBranchSite();
            rest1 = new RestWrapper();
            rest2 = new RestWrapper();
            rest1.connect( rootSite.getSiteServiceName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            rest2.connect( branchSite.getSiteServiceName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            if ( wsList.size() < 2 ) {
                wsList.add( wsList.get( 0 ) );
            }
            for ( int i = 0; i < fileNum; i++ ) {
                JSONObject desc = new JSONObject();
                desc.put( "name", author + i );
                desc.put( "author", author );
                desc.put( "title", author + i );
                desc.put( "mime_type", "text/plain" );
                // for clean
                String fileId1 = upload( filePath, wsList.get( 0 ),
                        desc.toString(), rest1 );
                download( fileId1, rest2, wsList.get( 0 ) );
                fileIdList1.add( new ScmId( fileId1 ) );
                // for transfer
                String fileId2 = upload( filePath, wsList.get( 1 ),
                        desc.toString(), rest2 );
                fileIdList2.add( new ScmId( fileId2 ) );
                descs.add( desc );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        transferAndCheck();
        cleanAnCheck();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileNum; i++ ) {
                    rest1.reset().setApi( "files/" + fileIdList1.get( i ).get()
                            + "?workspace_name=" + wsList.get( 0 ).getName()
                            + "&is_physical=true" )
                            .setRequestMethod( HttpMethod.DELETE )
                            .setResponseType( String.class ).exec();
                    rest2.reset().setApi( "files/" + fileIdList2.get( i ).get()
                            + "?workspace_name=" + wsList.get( 1 ).getName()
                            + "&is_physical=true" )
                            .setRequestMethod( HttpMethod.DELETE )
                            .setResponseType( String.class ).exec();
                    TestSdbTools.Task.deleteMeta( new ScmId( taskId1 ) );
                    TestSdbTools.Task.deleteMeta( new ScmId( taskId2 ) );
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

    private void transferAndCheck() {
        JSONObject options = new JSONObject();
        options.put( "filter", new JSONObject().put( "author", author ) );
        String response1 = rest2.setApi( "tasks" )
                .setRequestMethod( HttpMethod.POST )
                .setParameter( "task_type", "1" )
                .setParameter( "workspace_name", wsList.get( 1 ).getName() )
                .setParameter( "options", options.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        taskId1 = JSON.parseObject( response1 ).getJSONObject( "task" )
                .getString( "id" );
        waitTaskStop( taskId1, rest1 );
    }

    private void cleanAnCheck(){
        JSONObject options = new JSONObject();
        options.put( "filter", new JSONObject().put( "author", author ) );
        try {
            String response1 = rest2.setApi( "tasks" )
                    .setRequestMethod( HttpMethod.PUT )
                    .setParameter( "task_type", "2" )
                    .setParameter( "workspace_name", wsList.get( 0 ).getName() )
                    .setParameter( "options", options.toString() )
                    .setResponseType( String.class ).exec().getBody()
                    .toString();
            taskId2 = JSON.parseObject(  response1 ).getJSONObject( "task" )
                    .getString( "id" );
        } catch ( HttpClientErrorException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        waitTaskStop( taskId2, rest1 );
        JSONObject taskInfo = getTaskInfo( taskId2, rest2 );
        SiteWrapper[] expSites = { ScmInfo.getRootSite() };
        check( taskInfo, wsList.get( 0 ), fileIdList1, expSites );
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
            taskInfo =JSON.parseObject( response );
        } catch ( HttpClientErrorException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        return taskInfo;
    }

    private void check( JSONObject taskInfo, WsWrapper ws,
            List< ScmId > fileIdList, SiteWrapper[] expSiteList ) {
        // check site
        try {
            ScmFileUtils.checkMetaAndData( ws, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        // checktaskInfo
        JSONObject taskDetail = taskInfo.getJSONObject( "task" );
        Assert.assertEquals( taskDetail.getString( "running_flag" ), "3" );
        Assert.assertEquals( taskDetail.getString( "workspace_name" ),
                ws.getName() );
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
