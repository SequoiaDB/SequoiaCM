package org.sequoiacm.om.omserver.test.basic;

import java.io.InputStream;
import java.util.UUID;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
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
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.module.OmFileDataSiteInfo;
import com.sequoiacm.om.omserver.module.OmFileDetail;

import feign.Response;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OmServerTest.class)
public class TestFile {
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
        file.setAuthor("author1");
        file.setContent("./pom.xml");
        file.setFileName(UUID.randomUUID().toString());
        file.setMimeType(MimeType.BASIC);
        file.setTitle("title1");
        file.save();

        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName("testBatch" + UUID.randomUUID().toString());
        batch.save();
        batch.attachFile(file.getFileId());
        file = ScmFactory.File.getInstance(ws, file.getFileId());

        resp = client.getFileDetail(sessionId, config.getWorkspaceName1(), file.getFileId().get(),
                1, 0);
        respChecker.check(resp);
        String fileDetailStr = (String) resp.headers().get(RestParamDefine.FILE).toArray()[0];
        OmFileDetail fileDetail = objMapper.readValue(fileDetailStr, OmFileDetail.class);

        Assert.assertEquals(fileDetail.getAuthor(), file.getAuthor());
        Assert.assertEquals(fileDetail.getBatchId(), file.getBatchId().get());
        Assert.assertEquals(fileDetail.getClassId(), "");
        Assert.assertEquals(fileDetail.getDirectoryId(), file.getDirectory().getId());
        Assert.assertEquals(fileDetail.getId(), file.getFileId().get());
        Assert.assertEquals(fileDetail.getMimeType(), file.getMimeType());
        Assert.assertEquals(fileDetail.getName(), file.getFileName());
        Assert.assertEquals(fileDetail.getSize(), file.getSize());
        Assert.assertEquals(fileDetail.getTitle(), file.getTitle());
        Assert.assertEquals(fileDetail.getUpdateUser(), file.getUpdateUser());
        Assert.assertEquals(fileDetail.getUser(), file.getUser());
        Assert.assertEquals(fileDetail.getCreateTime(), file.getCreateTime());
        Assert.assertEquals(fileDetail.getDataCreateTime(), file.getDataCreateTime());
        Assert.assertEquals(fileDetail.getMajorVersion(), file.getMajorVersion());
        Assert.assertEquals(fileDetail.getMinorVersion(), file.getMinorVersion());
        Assert.assertEquals(fileDetail.getUpdateTime(), file.getUpdateTime());
        Assert.assertEquals(fileDetail.getSites().size(), 1);
        OmFileDataSiteInfo omSite = fileDetail.getSites().get(0);
        ScmFileLocation driverSite = file.getLocationList().get(0);
        Assert.assertEquals(omSite.getCreateTime(), driverSite.getCreateDate());
        Assert.assertEquals(omSite.getLastAccessTime(), driverSite.getDate());

        resp = client.downloadFile(sessionId, config.getWorkspaceName1(), file.getFileId().get(), 1,
                0);
        respChecker.check(resp);

        ByteArrayOutputStream omFileOs = new ByteArrayOutputStream();
        ByteArrayOutputStream driverFileOs = new ByteArrayOutputStream();

        InputStream omIs = resp.body().asInputStream();
        IOUtils.copy(omIs, omFileOs);
        omIs.close();

        file.getContent(driverFileOs);

        Assert.assertArrayEquals(omFileOs.toByteArray(), driverFileOs.toByteArray());
        batch.detachFile(file.getFileId());
        file.delete(true);
        batch.delete();
        client.logout(sessionId);
    }
}
