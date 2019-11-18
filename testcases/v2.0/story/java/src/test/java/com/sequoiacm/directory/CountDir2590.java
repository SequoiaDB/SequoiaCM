package com.sequoiacm.directory;


import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description:   SCM-2590:通过条件查询文件夹数量
 * @author fanyu
 * @Date:2019年09月03日
 * @version:1.0
 */
public class CountDir2590 extends TestScmBase{
    private AtomicInteger successCount = new AtomicInteger(0);
    private SiteWrapper site;
    private WsWrapper wsp;
    private ScmSession session;
    private ScmWorkspace ws;
    private String dirNameBase = "CreateDir1132";
    private List<String> dirNames = new ArrayList<>();
    private int dirNum = 100;
    private int totalDirNum = 100 * 2;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession(site);
        ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
        for(int i = 0; i < dirNum; i++){
            String dirName = dirNameBase + "-" + i;
            dirNames.add(dirName);
            if(!ScmFactory.Directory.isInstanceExist(ws,"/" + dirName)) {
                ScmDirectory parentDir = ScmFactory.Directory.createInstance(ws, "/" + dirName);
                parentDir.createSubdirectory(dirName);
            }
        }
    }

    @Test
    private void testAll() throws Exception {
        BSONObject filter = ScmQueryBuilder.start().get();
        long count = ScmFactory.Directory.countInstance(ws,filter);
        Assert.assertTrue(count >=  totalDirNum,filter.toString());
        successCount.getAndIncrement();
    }

    @Test
    private void testZero() throws Exception {
        BSONObject filter = ScmQueryBuilder.start(ScmAttributeName.Directory.NAME)
                .is(dirNameBase + "-inexistence").get();
        long count = ScmFactory.Directory.countInstance(ws, filter);
        Assert.assertEquals(count, 0, filter.toString());
        successCount.getAndIncrement();
    }

    @Test
    private void testPart() throws Exception {
        BSONObject filter = ScmQueryBuilder.start(ScmAttributeName.Directory.NAME)
                .in(dirNames).get();
        long count = ScmFactory.Directory.countInstance(ws, filter);
        Assert.assertEquals(count,totalDirNum, filter.toString());

        BSONObject filter1 = ScmQueryBuilder.start(ScmAttributeName.Directory.NAME)
                .in(dirNames.get(0)).get();
        long count1 = ScmFactory.Directory.countInstance(ws, filter1);
        Assert.assertEquals(count1,totalDirNum/dirNum, filter.toString());
        successCount.getAndIncrement();
    }

    @Test
    private void testInvalidWs() throws Exception {
         try{
             ScmFactory.Directory.countInstance(null,new BasicBSONObject());
             Assert.fail("exp fail but act success");
         }catch (ScmException e){
             if(e.getError() != ScmError.INVALID_ARGUMENT){
                 throw  e;
             }
         }
        successCount.getAndIncrement();
    }

    @Test
    private void testInvalidBSON() throws Exception {
        try{
            ScmFactory.Directory.countInstance(ws,null);
            Assert.fail("exp fail but act success");
        }catch (ScmException e){
            if(e.getError() != ScmError.INVALID_ARGUMENT){
                throw  e;
            }
        }
        successCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( successCount.get() == 5 || TestScmBase.forceClear) {
                for(String dirName : dirNames){
                    ScmFactory.Directory.deleteInstance(ws,"/" + dirName + "/" + dirName);
                    ScmFactory.Directory.deleteInstance(ws,"/" + dirName);
                }
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}

