package com.sequoiacm.bucket;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TestScmBucketCustomTag extends ScmTestMultiCenterBase {

    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmBucket bucket;
    private String bucketName = "scm-bucket-customtag";

    @BeforeClass
    public void setUp() throws ScmException, InterruptedException {
        ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getS3WorkSpaces(), ss);
    }

    @Test
    public void testCustomTag() throws ScmException {
        // create bucket
        bucket = ScmFactory.Bucket.createBucket(ws, bucketName);
        // set customTag
        Map<String, String> map = new HashMap<>();
        map.put("b", "h");
        map.put("d", "g");
        map.put("a", "f");
        map.put("c", "w");
        bucket.setCustomTag(map);
        Assert.assertEquals(bucket.getCustomTag().size(), map.size());
        Assert.assertEquals(bucket.getCustomTag(), new TreeMap<>(map));
        // get customTag
        Assert.assertEquals(bucket.getCustomTag(), map);
        
        // set customTag number more than 50
        Map<String, String> numberMap = new HashMap<>();
        for (int i = 1; i <= 51; i++) {
            numberMap.put("k" + i, "v" + i);
        }
        try {
            bucket.setCustomTag(numberMap);
            Assert.fail("set bucket customTag should failed");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.BUCKET_CUSTOMTAG_TOO_LARGE);
        }
        
        // set customTag empty
        Map<String, String> emptyMap = new HashMap<>();
        bucket.setCustomTag(emptyMap);
        Assert.assertNull(bucket.getCustomTag());
        
        // set customTag key is null
        Map<String, String> nullKeyMap = new HashMap<>();
        nullKeyMap.put(null, "b1");
        try {
            bucket.setCustomTag(nullKeyMap);
            Assert.fail("set bucket customTag should failed");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.BUCKET_INVALID_CUSTOMTAG);
        }

        // set customTag key is ""
        Map<String, String> emptyKeyMap = new HashMap<>();
        emptyKeyMap.put("", "b2");
        try {
            bucket.setCustomTag(emptyKeyMap);
            Assert.fail("set bucket customTag should failed");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.BUCKET_INVALID_CUSTOMTAG);
        }

        // set customTag value is null
        Map<String, String> nullValueMap = new HashMap<>();
        nullValueMap.put("a1", null);
        bucket.setCustomTag(nullValueMap);
        Assert.assertEquals(bucket.getCustomTag().size(), nullValueMap.size());

        // set customTag value is ""
        Map<String, String> emptyValueMap = new HashMap<>();
        emptyValueMap.put("a2", "");
        bucket.setCustomTag(emptyValueMap);
        Assert.assertEquals(bucket.getCustomTag().size(), emptyValueMap.size());
        
        // delete customTag 
        bucket.deleteCustomTag();
        Assert.assertNull(bucket.getCustomTag());
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if (null != bucketName) {
                ScmFactory.Bucket.deleteBucket(ss, bucketName);
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
