package com.sequoiacm.monitor;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.trace.ScmTrace;
import com.sequoiacm.client.element.trace.ScmTraceAnnotation;
import com.sequoiacm.client.element.trace.ScmTraceSpan;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * @descreption SCM-5456:ScmTraceSpan驱动测试
 * @author YiPan
 * @date 2022/12/1
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmTrace5456 extends TestScmBase {
    private ScmSession session = null;
    private String fileName = "file5456";
    private int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private ScmWorkspace ws;
    private WsWrapper wsp;
    private BSONObject query;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        session = TestScmTools.createSession();
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        query = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, query );
        createFile();
    }

    @Test
    public void test() throws Exception {
        ScmTrace scmTrace = GetScmTrace();
        Assert.assertNotNull( scmTrace );

        ScmTraceSpan firstSpan = scmTrace.getFirstSpan();
        ScmTraceSpan nextSpan = firstSpan.getNextSpans().get( 0 );

        // 校验span各个字段
        Assert.assertEquals( firstSpan.getTraceId(), scmTrace.getTraceId() );
        Assert.assertEquals( firstSpan.getSpanId(), nextSpan.getParentId() );
        Assert.assertNotEquals( firstSpan.getDuration(), 0 );
        Assert.assertNotNull( firstSpan.getRequestUrl() );
        Assert.assertNotNull( firstSpan.getService() );
        Assert.assertNotNull( firstSpan.getIp() );
        Assert.assertNotEquals( firstSpan.getPort(), 0 );
        Assert.assertNotEquals( firstSpan.getTimestamp(), 0 );
        Assert.assertNotNull( firstSpan.getTags() );

        // 校验annotations不为空
        List< ScmTraceAnnotation > annotations = firstSpan.getAnnotations();
        Assert.assertNotEquals( annotations.size(), 0 );

        // 校验scmTraceAnnotation字段
        ScmTraceAnnotation scmTraceAnnotation = annotations.get( 0 );
        Assert.assertEquals( scmTraceAnnotation.getIp(), firstSpan.getIp() );
        Assert.assertEquals( scmTraceAnnotation.getService(),
                firstSpan.getService() );
        Assert.assertEquals( scmTraceAnnotation.getIp(), firstSpan.getIp() );
        Assert.assertEquals( scmTraceAnnotation.getPort(),
                firstSpan.getPort() );
        Assert.assertNotEquals( scmTraceAnnotation.getTimestamp(), 0 );
        runSuccess = true;
    }

    private ScmTrace GetScmTrace() throws ScmException {
        List< ScmTrace > scmTraces = ScmSystem.ServiceTrace.listTrace( session,
                1000 );
        for ( ScmTrace trace : scmTraces ) {
            if ( trace.getSpanCount() > 2 && trace.isComplete() ) {
                return trace;
            }
        }
        return null;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, query );
            }
        } finally {
            session.close();
        }
    }

    private void createFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( filePath );
        file.save();
    }
}
