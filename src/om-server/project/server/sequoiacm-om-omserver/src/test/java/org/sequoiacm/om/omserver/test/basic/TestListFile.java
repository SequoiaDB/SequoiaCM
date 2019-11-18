package org.sequoiacm.om.omserver.test.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BasicBSONObject;
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
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.module.OmFileBasic;

import feign.Response;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OmServerTest.class)
public class TestListFile {
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
        file.setAuthor("TestListFile");
        file.save();

        file = ScmFactory.File.createInstance(ws);
        file.setFileName(UUID.randomUUID().toString());
        file.setAuthor("TestListFile");
        file.save();

        file = ScmFactory.File.createInstance(ws);
        file.setFileName(UUID.randomUUID().toString());
        file.setAuthor("TestListFile");
        file.save();

        List<ScmFileBasicInfo> fileList = new ArrayList<>();
        ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws,
                ScopeType.SCOPE_CURRENT,
                new BasicBSONObject(ScmAttributeName.File.AUTHOR, "TestListFile"),
                new BasicBSONObject(ScmAttributeName.File.FILE_ID, 1), 0, 1000);
        while (cursor.hasNext()) {
            fileList.add(cursor.getNext());
        }
        cursor.close();

        List<OmFileBasic> omFileList = client.getFileList(sessionId, config.getWorkspaceName1(),
                new BasicBSONObject(ScmAttributeName.File.AUTHOR, "TestListFile"), 0, 1000);
        Assert.assertEquals(omFileList.size(), fileList.size());
        for (int i = 0; i < omFileList.size(); i++) {
            OmFileBasic omFile = omFileList.get(i);
            ScmFileBasicInfo driverFile = fileList.get(i);
            Assert.assertEquals(omFile.getId(), driverFile.getFileId().get());
            Assert.assertEquals(omFile.getMimeType(), driverFile.getMimeType());
            Assert.assertEquals(omFile.getName(), driverFile.getFileName());
            Assert.assertEquals(omFile.getUser(), driverFile.getUser());
            Assert.assertEquals(omFile.getCreateTime(), driverFile.getCreateDate());
            Assert.assertEquals(omFile.getMajorVersion(), driverFile.getMajorVersion());
            Assert.assertEquals(omFile.getMinorVersion(), driverFile.getMinorVersion());
            ScmFactory.File.deleteInstance(ws, driverFile.getFileId(), true);
        }
        client.logout(sessionId);
    }

}
