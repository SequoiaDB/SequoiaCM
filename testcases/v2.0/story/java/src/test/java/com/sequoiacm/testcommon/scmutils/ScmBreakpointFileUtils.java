package com.sequoiacm.testcommon.scmutils;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.*;
import org.testng.SkipException;

import java.util.ArrayList;
import java.util.List;

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

    public static List< SiteWrapper > checkDBDataSource() {
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
            throw new SkipException(
                    "breakpoint file only support sequoiadb datasourse and ceph S3 datasourse, "
                            + "skip!" );
        } else {
            return DBSites;
        }
    }

}
