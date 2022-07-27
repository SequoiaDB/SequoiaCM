package com.sequoiacm.s3.taskmanager;

import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.infrastructure.lock.ScmLockPath;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.config.MultipartUploadConfig;
import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.dao.PartDao;
import com.sequoiacm.s3.dao.UploadDao;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.lock.S3LockPathFactory;
import com.sequoiacm.s3.processor.MultipartUploadProcessor;
import com.sequoiacm.s3.processor.MultipartUploadProcessorMgr;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class UploadScanClean {
    private static final Logger logger = LoggerFactory.getLogger(UploadScanClean.class);
    public final static long CLEAN_INTERVAL = 5 * 60 * 1000;

    @Autowired
    ScmLockManager lockManger;

    @Autowired
    S3LockPathFactory lockPathFactory;

    @Autowired
    MetaSourceService metaSourceService;

    @Autowired
    MultipartUploadConfig uploadConfig;

    @Autowired
    UploadDao uploadDao;

    @Autowired
    PartDao partDao;

    @Autowired
    MultipartUploadProcessorMgr multipartUploadProcessorMgr;

    @Scheduled(initialDelay = CLEAN_INTERVAL, fixedDelay = CLEAN_INTERVAL)
    public void uploadScanClean() throws ScmServerException {
        MetaSource ms = metaSourceService.getMetaSource();
        ScmSite siteInfo = ScmContentModule.getInstance().getLocalSiteInfo();
        MetaCursor invalidUploads = null;
        try {
            // cleanCompleteTime 是给 complete 后的数据预留一点时间，queryInvalidUploads是无事务读，会读到脏数据
            // 为避免清理到正在 complete 的任务，给 complete 的数据预留一部分时间
            long cleanCompleteTime = System.currentTimeMillis()
                    - uploadConfig.getCompletereservetime() * 60 * 1000;
            BSONObject statusMatcher = new BasicBSONObject();
            statusMatcher.put(SequoiadbHelper.SEQUOIADB_MATCHER_NE,
                    S3CommonDefine.UploadStatus.UPLOAD_INIT);
            invalidUploads = uploadDao.queryUploads(statusMatcher, null);
            while (invalidUploads.hasNext()) {
                BSONObject record = invalidUploads.getNext();
                UploadMeta uploadMeta = new UploadMeta(record);
                // 只清理本站点上传的分段信息
                if (siteInfo.getId() != uploadMeta.getSiteId()) {
                    continue;
                }
                if (uploadMeta.getUploadStatus() == S3CommonDefine.UploadStatus.UPLOAD_COMPLETE
                        && uploadMeta.getLastModified() > cleanCompleteTime) {
                    continue;
                }

                long uploadId = uploadMeta.getUploadId();
                ScmLockPath lockPath = lockPathFactory.createUploadLockPath(uploadId);
                ScmLock lock = lockManger.acquiresWriteLock(lockPath, 60 * 1000);
                try {
                    uploadMeta = uploadDao.queryUpload(null, null, uploadId);
                    // 防止前面的查询列表中有脏读数据，在锁保护下再次查询记录和检查状态
                    if (null == uploadMeta) {
                        continue;
                    }
                    if (uploadMeta.getUploadStatus() != S3CommonDefine.UploadStatus.UPLOAD_INIT) {
                        MultipartUploadProcessor processor = multipartUploadProcessorMgr
                                .getProcessor(uploadMeta.getSiteType());
                        if (null == processor) {
                            logger.error("clean upload failed. wsName:{}, uploadId:{}",
                                    uploadMeta.getWsName(), uploadId);
                            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                                    "current site not support multipart upload");
                        }
                        processor.cleanInvalidUpload(uploadMeta.getWsName(), ms, uploadMeta);
                    }
                }
                catch (Exception e) {
                    logger.info("clean upload failed. wsName:{}, uploadId:{}",
                            uploadMeta.getWsName(), uploadId, e);
                }
                finally {
                    lock.unlock();
                }
            }
        }
        catch (Exception e) {
            logger.error("scan complete uploads failed", e);
        }
        finally {
            if (invalidUploads != null) {
                invalidUploads.close();
            }
        }

        MetaCursor exceedUploads = null;
        try {
            long exceedTime = System.currentTimeMillis()
                    - uploadConfig.getIncompletelifecycle() * 24 * 60 * 60 * 1000;
            exceedUploads = uploadDao.queryUploads(null, exceedTime);
            if (exceedUploads != null) {
                while (exceedUploads.hasNext()) {
                    long uploadId = (long) (exceedUploads.getNext()).get(UploadMeta.META_UPLOAD_ID);
                    ScmLockPath lockPath = lockPathFactory.createUploadLockPath(uploadId);
                    ScmLock lock = lockManger.acquiresWriteLock(lockPath, 60 * 1000);
                    try {
                        UploadMeta uploadMeta = uploadDao.queryUpload(null, null, uploadId);
                        if (uploadMeta != null && uploadMeta
                                .getUploadStatus() == S3CommonDefine.UploadStatus.UPLOAD_INIT) {
                            uploadMeta.setUploadStatus(S3CommonDefine.UploadStatus.UPLOAD_ABORT);
                            uploadDao.updateUploadMeta(null, uploadMeta);
                        }
                    }
                    catch (Exception e) {
                        logger.info("clean upload failed. uploadId:" + uploadId, e);
                    }
                    finally {
                        lock.unlock();
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("scan complete uploads failed", e);
        }
        finally {
            if (exceedUploads != null) {
                exceedUploads.close();
            }
        }

    }
}
