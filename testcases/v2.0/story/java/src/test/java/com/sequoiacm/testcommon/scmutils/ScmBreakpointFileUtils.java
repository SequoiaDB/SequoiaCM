package com.sequoiacm.testcommon.scmutils;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testresource.SkipTestException;

/**
 * @Description ScmBreakpointFileUtils.java
 * @author zhangYanan
 * @date 2022.08.08
 */
public class ScmBreakpointFileUtils extends TestScmBase {

    /**
     * checkDBDataSource ,if dataSource type not have SEQUOIADB or CEPH_S3
     * ,skip!
     *
     * @throws Exception
     * @return SEQUOIADB and CEPH_S3 dataSource sites
     */

    public static List< SiteWrapper > checkDBAndCephS3DataSource() {
        List< SiteWrapper > sites = ScmInfo.getAllSites();
        List< SiteWrapper > DBSites = new ArrayList<>();
        for ( SiteWrapper site : sites ) {
            ScmType.DatasourceType dsType = site.getDataType();
            if ( dsType.equals( ScmType.DatasourceType.SEQUOIADB )
                    || dsType.equals( ScmType.DatasourceType.CEPH_S3 ) ) {
                DBSites.add( site );
            }
        }
        if ( DBSites.size() == 0 ) {
            throw new SkipTestException(
                    "breakpoint file only support sequoiadb datasourse and ceph S3 datasourse, "
                            + "skip!" );
        } else {
            return DBSites;
        }
    }

}
