package org.sequoiacm.om.omserver.test.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sequoiacm.om.omserver.test.ClientRespChecker;
import org.sequoiacm.om.omserver.test.OmServerTest;
import org.sequoiacm.om.omserver.test.ScmOmClient;
import org.sequoiacm.om.omserver.test.ScmOmTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.module.OmBatchBasic;
import com.sequoiacm.om.omserver.module.OmBatchDetail;
import com.sequoiacm.om.omserver.module.OmFileBasic;

import feign.Response;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OmServerTest.class)
public class TestBatch {
    @Autowired
    ObjectMapper objMapper;

    @Autowired
    ScmOmClient client;

    @Autowired
    ClientRespChecker respChecker;

    @Autowired
    ScmSession session;

    @Autowired
    ScmOmTestConfig config;

    @Test
    public void test() throws Exception {
        Response resp = client.login(config.getScmUser(), config.getScmPassword());
        respChecker.check(resp);

        String sessionId = (String) resp.headers().get(RestParamDefine.X_AUTH_TOKEN).toArray()[0];
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(config.getWorkspaceName1(), session);
        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(UUID.randomUUID().toString());
        file.save();

        ScmBatch batch1 = ScmFactory.Batch.createInstance(ws);
        batch1.setName(UUID.randomUUID().toString());
        batch1.save();

        ScmBatch batch2 = ScmFactory.Batch.createInstance(ws);
        batch2.setName(UUID.randomUUID().toString());
        batch2.save();
        batch2.attachFile(file.getFileId());
        batch2 = ScmFactory.Batch.getInstance(ws, batch2.getId());

        ScmBatch batch3 = ScmFactory.Batch.createInstance(ws);
        batch3.setName(UUID.randomUUID().toString());
        batch3.save();

        BSONObject fileter = (BSONObject) JSON.parse(String.format("{id:{'$in':['%s','%s','%s']}}",
                batch1.getId().get(), batch2.getId().get(), batch3.getId().get()));
        List<ScmBatchInfo> driverBatchs = new ArrayList<ScmBatchInfo>();
        ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, fileter,
                new BasicBSONObject(ScmAttributeName.Batch.ID, 1), 0, 1000);
        while (cursor.hasNext()) {
            driverBatchs.add(cursor.getNext());
        }
        cursor.close();

        List<OmBatchBasic> omBatchList = client.getBatchList(sessionId, config.getWorkspaceName1(),
                fileter, 0, 1000);
        Assert.assertEquals(omBatchList.size(), driverBatchs.size());
        for (int i = 0; i < omBatchList.size(); i++) {
            OmBatchBasic omBatch = omBatchList.get(i);
            ScmBatchInfo driverBatch = driverBatchs.get(i);
            Assert.assertEquals(omBatch.getId(), driverBatch.getId().get());
            Assert.assertEquals(omBatch.getName(), driverBatch.getName());
            Assert.assertEquals(omBatch.getCreateTime(), driverBatch.getCreateTime());
            Assert.assertEquals(omBatch.getFileCount(), driverBatch.getFilesCount());
        }

        OmBatchDetail batch = client.getBatch(sessionId, config.getWorkspaceName1(),
                batch2.getId().get());
        Assert.assertEquals(batch.getCreateUser(), batch2.getCreateUser());
        Assert.assertEquals(batch.getId(), batch2.getId().get());
        Assert.assertEquals(batch.getName(), batch2.getName());
        Assert.assertEquals(batch.getUpdateUser(), batch2.getUpdateUser());
        Assert.assertEquals(batch.getCreateTime(), batch2.getCreateTime());
        Assert.assertEquals(batch.getUpdateTime(), batch2.getUpdateTime());

        Assert.assertEquals(batch.getFileCount(), 1);
        OmFileBasic fileBasic = batch.getFiles().get(0);
        ScmFile dirverBatchFile = batch2.listFiles().get(0);
        Assert.assertEquals(fileBasic.getId(), dirverBatchFile.getFileId().get());
        Assert.assertEquals(fileBasic.getMimeType(), dirverBatchFile.getMimeType());
        Assert.assertEquals(fileBasic.getName(), dirverBatchFile.getFileName());
        Assert.assertEquals(fileBasic.getUser(), dirverBatchFile.getUser());
        Assert.assertEquals(fileBasic.getCreateTime(), dirverBatchFile.getCreateTime());
        Assert.assertEquals(fileBasic.getMajorVersion(), dirverBatchFile.getMajorVersion());
        Assert.assertEquals(fileBasic.getMinorVersion(), dirverBatchFile.getMinorVersion());

        batch2.detachFile(file.getFileId());
        file.delete(true);
        batch1.delete();
        batch2.delete();
        batch3.delete();

        client.logout(sessionId);

    }
}
