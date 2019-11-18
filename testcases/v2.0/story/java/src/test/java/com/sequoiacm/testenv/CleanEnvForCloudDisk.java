
package com.sequoiacm.testenv;

import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiadb.base.Sequoiadb;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Description:clean cloud disk
 * @author fanyu
 * @Date:2019年05月09日
 * @version:1.0
 */
public class CleanEnvForCloudDisk extends TestScmBase {
    Logger log = LoggerFactory.getLogger(CleanEnvForCloudDisk.class);
    private final static String CS_NAME = "CLOUDDISK";
    private final static String CL_NAME = "admin";

    @BeforeClass(alwaysRun = true)
    private void setUp() {
    }

    /*
     *为了方便，假网盘的元数据和scm文件元数据部署在同一个集群上，
     * 跑用例之前，连接scm文件元数据的db地址清理环境。
     * 若假网盘的db与scm文件元数据db不在一个集群上，
     * 请修改连接db地址
     */
    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void test() throws Exception {
        deleteCS();
    }

    private void deleteCS() {
        Sequoiadb sdb = null;
        BSONObject subMatcher = new BasicBSONObject();
        subMatcher.put("$ne",1);
        BSONObject matcher = new BasicBSONObject();
        matcher.put("id",subMatcher);
        try {
            sdb = TestSdbTools.getSdb(TestScmBase.mainSdbUrl);
            sdb.getCollectionSpace(CS_NAME).getCollection(CL_NAME).delete(matcher);
        } catch (Exception e) {
            log.info("clean env failed, e = " + e.getMessage());
        } finally {
            if (sdb != null) {
                sdb.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }
}
