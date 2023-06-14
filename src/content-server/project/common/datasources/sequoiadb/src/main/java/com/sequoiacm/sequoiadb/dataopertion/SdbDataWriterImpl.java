package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;

import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiacm.sequoiadb.dataservice.SdbDatasourceConfig;
import com.sequoiacm.sequoiadb.dataservice.SdbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SdbDataWriterImpl extends ScmDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(SdbDataWriterImpl.class);
    private SdbWriter sdbWriter;

    @SlowLog(operation = "openWriter", extras = { @SlowLogExtra(name = "writeCs", data = "csName"),
            @SlowLogExtra(name = "writeCl", data = "clName"),
            @SlowLogExtra(name = "writeLobId", data = "dataId") })
    SdbDataWriterImpl(int siteId, ScmLocation location, ScmService service, MetaSource metaSource,
            String csName, String clName, String wsName, int dataType, String dataId,
            ScmLockManager lockManager) throws SequoiadbException {
// 屏蔽 PUT LOB 优化：SEQUOIACM-1411
//        if (isLobOptimizeVersion((SdbService) service)) {
//            sdbWriter = new SdbLobOptimizeWriter(siteId, location, service, metaSource, csName,
//                    clName, wsName, dataId, lockManager);
//        }
//        else {
//            sdbWriter = new SdbLobWriter(siteId, location, service, metaSource, csName, clName,
//                    wsName, dataId, lockManager);
//        }
          sdbWriter = new SdbLobWriter(siteId, location, service, metaSource, csName, clName,
                  wsName, dataId, lockManager);
    }

    @Override
    public void write(byte[] content) throws SequoiadbException {
        sdbWriter.write(content, 0, content.length);
    }

    @Override
    @SlowLog(operation = "writeData")
    public void write(byte[] content, int offset, int len) throws SequoiadbException {
        sdbWriter.write(content, offset, len);
    }

    @Override
    @SlowLog(operation = "cancelWriter")
    public void cancel() {
        sdbWriter.cancel();
    }

    @Override
    @SlowLog(operation = "closeWriter")
    public void close() throws SequoiadbException {
        sdbWriter.close();
    }

    @Override
    public long getSize() {
        return sdbWriter.getFileSize();
    }

    @Override
    public String getCreatedTableName() {
        return sdbWriter.getCreatedTableName();
    }

// 回退sdb驱动至349，不支持 getVersion：SEQUOIACM-1411
//    private boolean isLobOptimizeVersion(SdbService sdbService) {
//        try {
//            return sdbService.isCompatible(SdbDatasourceConfig.getPutLobRequiredVersionRanges());
//        }
//        catch (Exception e) {
//            logger.warn("failed to check sdb version, cannot use lob optimize writer!", e);
//            return false;
//        }
//    }
}
