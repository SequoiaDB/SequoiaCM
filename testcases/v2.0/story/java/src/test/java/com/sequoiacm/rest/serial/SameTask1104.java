package com.sequoiacm.rest.serial;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
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
 * @description SCM-1104:并发提交多个相同ws下的任务
 * @author fanyu
 * @createDate 2018.03.24
 * @updateUser ZhangYanan
 * @updateDate 2021.10.27
 * @updateRemark
 * @version v1.0
 */
public class SameTask1104 extends TestScmBase {
    private boolean runSuccess = false;
    private WsWrapper ws = null;
    private File localPath = null;
    private int fileNum = 100;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private JSONArray descs = new JSONArray();
    private String filePath = null;
    private int fileSize = 1;
    private String author = "SameTask1104";
    private String taskId1 = null;
    private String taskId2 = null;
    private RestWrapper rest1 = null;
    private RestWrapper rest2 = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;

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
            rootSite = ScmInfo.getRootSite();
            branchSite = ScmInfo.getBranchSite();
            rest1 = new RestWrapper();
            rest2 = new RestWrapper();
            rest1.connect( rootSite.getSiteServiceName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            rest2.connect( branchSite.getSiteServiceName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            ws = ScmInfo.getWs();
            for ( int i = 0; i < fileNum; i++ ) {
                JSONObject desc = new JSONObject();
                desc.put( "name", author + i );
                desc.put( "author", author );
                desc.put( "title", author + i );
                desc.put( "mime_type", "text/plain" );
                String fileId = upload( filePath, ws, desc.toString(), rest1 );
                download( fileId, rest2 );
                fileIdList.add( new ScmId( fileId ) );
                descs.add( desc );
            }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker(new TestThread(taskId1,rest1));
        t.addWorker(new TestThread(taskId2,rest2));
        t.run();
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
                    if ( taskId1 != null ) {
                        TestSdbTools.Task.deleteMeta( new ScmId( taskId1 ) );
                    }
                    if ( taskId2 != null ) {
                        TestSdbTools.Task.deleteMeta( new ScmId( taskId2 ) );
                    }
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

    class TestThread{
        private String taskId = null;
        private RestWrapper rest = null;

        public TestThread(String taskId, RestWrapper rest) {
            this.taskId = taskId;
            this.rest = rest;
        }

        @ExecuteOrder(step = 1)
        private void run(){
            JSONObject options = new JSONObject();
            JSONObject option = new JSONObject();
            option.put( "author", author );
            options.put( "filter", option );

            String response1 = rest.setApi( "tasks" )
                    .setRequestMethod( HttpMethod.POST )
                    .setParameter( "task_type", "2" )
                    .setParameter( "workspace_name", ws.getName() )
                    .setParameter( "options", options.toString() )
                    .setResponseType( String.class ).exec().getBody().toString();
            taskId = JSON.parseObject( response1 ).getJSONObject( "task" )
                    .getString( "id" );
        }
    }

    public String upload( String filePath, WsWrapper ws, String desc,
            RestWrapper rest )
            throws HttpClientErrorException, FileNotFoundException {
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

    public void download( String fileId, RestWrapper rest ) {
        rest.reset()
                .setApi( "files/" + fileId + "?workspace_name=" + ws.getName() )
                .setRequestMethod( HttpMethod.GET )
                .setResponseType( Resource.class ).exec();
    }

}
