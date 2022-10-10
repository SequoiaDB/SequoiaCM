package com.sequoiacm.scheduletask;

import com.sequoiacm.client.element.ScmScheduleSpaceRecyclingContent;
import com.sequoiacm.client.element.ScmSpaceRecycleScope;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-5252:通过驱动设置清理范围，驱动测试
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class ScmScheduleSpaceRecyclingContent5252 extends TestScmBase {
    private SiteWrapper rootSite = null;

    @BeforeClass
    public void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmSpaceRecycleScope scmSpaceRecycleScope = ScmSpaceRecycleScope
                .mothBefore( 1 );
        int maxExecTime = 120000;
        ScmScheduleSpaceRecyclingContent content = new ScmScheduleSpaceRecyclingContent(
                rootSite.getSiteName(), scmSpaceRecycleScope, maxExecTime );

        Assert.assertEquals( content.getTargetSite(), rootSite.getSiteName() );
        Assert.assertEquals( content.getSpaceRecycleScope(),
                scmSpaceRecycleScope );
        Assert.assertEquals( content.getMaxExecTime(), maxExecTime );
    }

    @AfterClass
    public void tearDown() throws Exception {
    }
}