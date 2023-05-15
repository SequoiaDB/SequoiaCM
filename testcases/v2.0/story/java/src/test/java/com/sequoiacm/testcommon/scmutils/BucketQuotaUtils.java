package com.sequoiacm.testcommon.scmutils;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.quota.ScmBucketQuotaInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmQuotaSyncStatus;
import org.testng.Assert;

public class BucketQuotaUtils {

    /**
     * @descreption 检查桶的限额状态
     * @param session
     * @param bucketName
     * @param syncStatus
     * @return
     */
    public static void checkSyncStatus( ScmSession session, String bucketName,
            ScmQuotaSyncStatus syncStatus )
            throws ScmException, InterruptedException {
        long times = 0;
        long timeout = 3 * 60; // 设置超时时间为 3 分钟
        while ( true ) {
            ScmQuotaSyncStatus status = ScmFactory.Quota
                    .getBucketQuota( session, bucketName ).getSyncStatus();
            if ( status.equals( syncStatus ) ) {
                // 等待内部放锁时间
                Thread.sleep( 4000 );
                break;
            } else {
                Thread.sleep( 1000 );
                times++;
                if ( times >= timeout ) {
                    Assert.fail( "校验任务状态超时！" );
                }
            }
        }
    }

    /**
     * @descreption 校验检查桶的限额信息
     * @param quotaInfo
     * @param bucketName
     * @param maxObjectNum
     * @param maxObjectSize
     * @param objectNum
     * @param useObjectSize
     * @return
     */
    public static void checkQuotaInfo( ScmBucketQuotaInfo quotaInfo,
            String bucketName, int maxObjectNum, long maxObjectSize,
            int objectNum, int useObjectSize ) {
        Assert.assertEquals( quotaInfo.getBucketName(), bucketName );
        Assert.assertEquals( quotaInfo.getMaxObjects(), maxObjectNum );
        Assert.assertEquals( quotaInfo.getMaxSizeBytes(), maxObjectSize );
        Assert.assertEquals( quotaInfo.getUsedObjects(), objectNum );
        Assert.assertEquals( quotaInfo.getUsedSizeBytes(), useObjectSize );
    }
}
