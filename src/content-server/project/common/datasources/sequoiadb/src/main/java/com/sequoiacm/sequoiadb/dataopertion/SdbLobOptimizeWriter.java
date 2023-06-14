// 屏蔽 PUT LOB 优化：SEQUOIACM-1411
//package com.sequoiacm.sequoiadb.dataopertion;
//
//import com.sequoiacm.common.memorypool.ScmPoolWrapper;
//import com.sequoiacm.datasource.dataservice.ScmService;
//import com.sequoiacm.datasource.metadata.ScmLocation;
//import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
//import com.sequoiacm.infrastructure.lock.ScmLockManager;
//import com.sequoiacm.metasource.MetaSource;
//import com.sequoiacm.sequoiadb.SequoiadbException;
//import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
//import com.sequoiacm.sequoiadb.dataservice.SequoiadbHelper;
//import com.sequoiacm.sequoiadb.metaoperation.MetaDataOperator;
//import com.sequoiadb.base.Sequoiadb;
//import com.sequoiadb.exception.SDBError;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class SdbLobOptimizeWriter implements SdbWriter {
//    private static final Logger logger = LoggerFactory.getLogger(SdbLobOptimizeWriter.class);
//    private static final int BUFFER_SIZE = 255 * 1024;
//    private int siteId;
//    private ScmLocation location;
//    private ScmService service;
//    private MetaSource metaSource;
//    private String csName;
//    private String clName;
//    private String wsName;
//    private String lobId;
//    private ScmLockManager lockManager;
//    private ScmPoolWrapper poolWrapper;
//    private byte[] buffer;
//    private int bufferDataOffset;
//    private SdbLobWriter lobWriter = null;
//    private String createdCsName;
//
//    public SdbLobOptimizeWriter(int siteId, ScmLocation location, ScmService service,
//            MetaSource metaSource, String csName, String clName, String wsName, String dataId,
//            ScmLockManager lockManager) throws SequoiadbException {
//        this.bufferDataOffset = 0;
//        this.siteId = siteId;
//        this.location = location;
//        this.service = service;
//        this.metaSource = metaSource;
//        this.csName = csName;
//        this.clName = clName;
//        this.wsName = wsName;
//        this.lobId = dataId;
//        this.lockManager = lockManager;
//        try {
//            this.poolWrapper = ScmPoolWrapper.getInstance();
//            this.buffer = poolWrapper.getBytes(BUFFER_SIZE);
//        }
//        catch (Exception e) {
//            throw new SequoiadbException("failed to acquire buffer: dataId=" + dataId, e);
//        }
//    }
//
//    @Override
//    public void write(byte[] data, int off, int len) throws SequoiadbException {
//        if (null != lobWriter) {
//            lobWriter.write(data, off, len);
//            return;
//        }
//        if (tryWriteBuffer(data, off, len)) {
//            return;
//        }
//        lobWriter = new SdbLobWriter(siteId, location, service, metaSource, csName, clName,
//                    wsName, lobId, lockManager);
//        lobWriter.write(buffer, 0, bufferDataOffset);
//        lobWriter.write(data, off, len);
//    }
//
//    @Override
//    public void cancel() {
//        releaseBuffer();
//        if (null != lobWriter) {
//            lobWriter.cancel();
//        }
//
//    }
//
//    @Override
//    public String getCreatedTableName() {
//        if (null != lobWriter) {
//            return lobWriter.getCreatedTableName();
//        }
//        return createdCsName;
//    }
//
//    @Override
//    public void close() throws SequoiadbException {
//        SdbDataService sds = (SdbDataService) service;
//        Sequoiadb sdb = null;
//        try {
//            if (null != lobWriter) {
//                lobWriter.close();
//                return;
//            }
//            sdb = sds.getSequoiadb();
//            try {
//                SequoiadbHelper.putLob(sdb, csName, clName, lobId, buffer, 0, bufferDataOffset);
//            }
//            catch (SequoiadbException e) {
//                if (e.getDatabaseError() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
//                        || e.getDatabaseError() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
//                    boolean isCreated = SdbCsRecycleHelper.createCs(sdb, csName,
//                            location.getSiteName(), (SdbDataLocation) location, lockManager,
//                            new MetaDataOperator(metaSource, wsName, location.getSiteName(),
//                                    siteId));
//                    if (isCreated) {
//                        this.createdCsName = csName;
//                    }
//                    SequoiadbHelper.createCL(sdb, csName, clName, (SdbDataLocation) location);
//                    SequoiadbHelper.putLob(sdb, csName, clName, lobId, buffer, 0, bufferDataOffset);
//                }
//                else if (e.getDatabaseError() == SDBError.SDB_FE.getErrorCode()) {
//                    CommonDataOperation.deleteResidueFileContent(siteId, wsName, lobId, location,
//                            csName, clName, service, metaSource, lockManager);
//                    SequoiadbHelper.putLob(sdb, csName, clName, lobId, buffer, 0, bufferDataOffset);
//                }
//                else {
//                    logger.error("put lob failed:siteId=" + siteId + ",cs=" + csName + ",cl="
//                            + clName + ",lobId=" + lobId, e);
//                    throw e;
//                }
//            }
//        }
//        finally {
//            sds.releaseSequoiadb(sdb);
//            releaseBuffer();
//        }
//    }
//
//    @Override
//    public long getFileSize() {
//        if (null != lobWriter) {
//            return lobWriter.getFileSize();
//        }
//        return bufferDataOffset;
//    }
//
//    private boolean tryWriteBuffer(byte[] data, int off, int len) {
//        if (!canWriteBuffer(len)) {
//            return false;
//        }
//        System.arraycopy(data, off, buffer, bufferDataOffset, len);
//        bufferDataOffset += len;
//        return true;
//    }
//
//    private boolean canWriteBuffer(int dataLen) {
//        return dataLen + bufferDataOffset <= buffer.length;
//    }
//
//    private void releaseBuffer() {
//        if (buffer != null) {
//            poolWrapper.releaseBytes(buffer);
//            buffer = null;
//        }
//    }
//
//    private void deleteResidueFileContent() throws SequoiadbException {
//        logger.warn("local site exist residue file content:localSiteId={},wsName={},dataId={}",
//                siteId, wsName, lobId);
//        try {
//            SdbDataDeletorImpl deletor = new SdbDataDeletorImpl(siteId, location.getSiteName(),
//                    csName, clName, wsName, lobId, service, metaSource, lockManager);
//            deletor.delete();
//            logger.warn("delete residue file content success:localSiteId={},wsName={},dataId={}",
//                    siteId, wsName, lobId);
//        }
//        catch (Exception e) {
//            logger.error("delete residue file content failed:localSiteId={},wsName={},dataId={}",
//                    siteId, wsName, lobId, e);
//            throw e;
//        }
//    }
//}
