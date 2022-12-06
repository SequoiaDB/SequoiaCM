package com.sequoiacm.file;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TestScmFileCustomTag extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmId fileId;
    private ScmFile scmFile;
    private String workDir;
    private String filePath;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workDir);
        ScmTestTools.createDir(workDir);
        filePath = workDir + File.separator + ScmTestTools.getClassName() + ".txt";
        ScmTestTools.createFile(filePath, ScmTestTools.getClassName(), 1024);
    }
    
    @Test
    public void testCustomTag() throws ScmException {
        // create file
        scmFile = ScmFactory.File.createInstance(ws);
        scmFile.setFileName(ScmTestTools.getClassName());
        scmFile.setContent(filePath);
        fileId = scmFile.save();

        // set file customTag
        Map<String, String> map = new HashMap<>();
        map.put("b", "h");
        map.put("d", "g");
        map.put("a", "f");
        map.put("c", "w");
        scmFile.setCustomTag(map);
        // check res
        Assert.assertEquals(scmFile.getCustomTagCount(), map.size());
        Assert.assertEquals(scmFile.getCustomTag(), new TreeMap<>(map));
        
        // get file customTag
        Assert.assertEquals(map, scmFile.getCustomTag());

        // set file customTag number more than 10
        Map<String, String> numberMap = new HashMap<>();
        for (int i = 1; i <= 11; i++) {
            numberMap.put("k" + i, "v" + i);
        }
        try {
            scmFile.setCustomTag(numberMap);
            Assert.fail("set file customTag should failed");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_CUSTOMTAG_TOO_LARGE);
        }

        // set file customTag key length more than 128
        Map<String, String> keyLengthMap = new HashMap<>();
        keyLengthMap.put(genStr(129), "b1");
        try {
            scmFile.setCustomTag(keyLengthMap);
            Assert.fail( "set file customTag should failed" );
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_INVALID_CUSTOMTAG);
        }
        
        // set file customTag value length more than 256
        Map<String, String> valueLengthMap = new HashMap<>();
        valueLengthMap.put("a1", genStr(257));
        try {
            scmFile.setCustomTag(valueLengthMap);
            Assert.fail( "set file customTag should failed" );
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_INVALID_CUSTOMTAG);
        }
        
        // set file empty customTag
        Map<String, String> emptyMap = new HashMap<>();
        scmFile.setCustomTag(emptyMap);
        // check res
        Assert.assertEquals(scmFile.getCustomTag().size(), emptyMap.size());

        // set file customTag key is null
        Map<String, String> nullKeyMap = new HashMap<>();
        nullKeyMap.put(null, "b1");
        try {
            scmFile.setCustomTag(nullKeyMap);
            Assert.fail("set file customTag should failed");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_INVALID_CUSTOMTAG);
        }

        // set file customTag key is ""
        Map<String, String> emptyKeyMap = new HashMap<>();
        emptyKeyMap.put("", "b2");
        try {
            scmFile.setCustomTag(emptyKeyMap);
            Assert.fail("set file customTag should failed");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.FILE_INVALID_CUSTOMTAG);
        }

        // set file customTag value is null
        Map<String, String> nullValueMap = new HashMap<>();
        nullValueMap.put("a1", null);
        scmFile.setCustomTag(nullValueMap);
        // check res
        Assert.assertEquals(scmFile.getCustomTag().size(), nullValueMap.size());

        // set file customTag value is ""
        Map<String, String> emptyValueMap = new HashMap<>();
        emptyValueMap.put("a2", "");
        scmFile.setCustomTag(emptyValueMap);
        // check res
        Assert.assertEquals(scmFile.getCustomTag().size(), emptyValueMap.size());

        // delete file customTag
        scmFile.deleteCustomTag();
        Assert.assertEquals(scmFile.getCustomTag().size(), 0);
        
        // upload file and set customTag
        ScmFile uploadFile = ScmFactory.File.createInstance(ws);
        uploadFile.setFileName("testUploadFile");
        Map<String, String> uploadMap = new HashMap<>();
        uploadMap.put("a1", "b1");
        uploadMap.put("a2", "b2");
        uploadFile.setCustomTag(uploadMap);
        ScmId scmId = uploadFile.save();
        // check res
        Assert.assertEquals(uploadMap, new TreeMap<>(uploadFile.getCustomTag()));
        // delete file
        ScmFactory.File.deleteInstance(ws, scmId, true);
    }

    private String genStr(int num) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < num; i++) {
            sb.append("a");
        }
        return sb.toString();
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if (null != fileId) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
