package com.sequoiacm.diagnose.task;

import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.diagnose.common.CheckLevel;
import com.sequoiacm.diagnose.common.CompareResult;
import com.sequoiacm.diagnose.common.ResultType;
import com.sequoiacm.diagnose.common.ScmFileInfo;
import com.sequoiacm.diagnose.datasource.ScmDataSourceMgr;
import com.sequoiacm.diagnose.progress.CompareProgress;
import com.sequoiacm.diagnose.utils.CommonUtils;
import com.sequoiacm.diagnose.utils.FileOperateUtils;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.tools.exception.ScmExitCode;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CompareTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CompareTask.class);
    private List<ScmFileInfo> fileList;
    private CompareProgress progress;
    private CheckLevel checkLevel;
    private boolean isFull;
    private String wsName;
    private ExecutionContext context;
    private static final int MAX_RESULT_CACHE_COUNT = 2000;

    public CompareTask(CompareProgress progress, String wsName, List<ScmFileInfo> fileList,
            CheckLevel checkLevel, boolean isFull, ExecutionContext context) {
        this.progress = progress;
        this.fileList = fileList;
        this.checkLevel = checkLevel;
        this.isFull = isFull;
        this.wsName = wsName;
        this.context = context;
    }

    @Override
    public void run() {
        for (ScmFileInfo info : fileList) {
            if (info.isDeleteMarker()) {
                progress.success(true, 1);
                continue;
            }
            if (checkLevel == CheckLevel.MD5 && !StringUtils.hasText(info.getMd5())) {
                try {
                    FileOperateUtils.appendNullMd5ToFile(info.getId(), info.getMajorVersion(),
                            info.getMinorVersion());
                }
                catch (Exception e) {
                    logger.error("Failed to write null md5 file record to file", e);
                    context.setHasException(e);
                }
            }
            List<String> resultList = new ArrayList<>();
            boolean allSame = true; // 标记是否所有数据源都一致
            boolean hasFailed = false; // 比较是否发生过异常
            BasicBSONList siteList = info.getSiteList();
            for (Object obj : siteList) {
                // 刷新结果到文件，防止结果list太大
                refreshResult(resultList);
                BSONObject siteBson = (BSONObject) obj;
                CompareResult result = compareData(info, siteBson);
                ResultType type = result.getResultType();
                if (type == ResultType.FAILED) {
                    hasFailed = true;
                    resultList.add(result.toString());
                    continue;
                }
                if (type == ResultType.SAME) {
                    if (isFull) {
                        resultList.add(result.toString());
                    }
                    continue;
                }
                allSame = false;
                resultList.add(result.toString());
            }
            if (!hasFailed) {
                progress.success(allSame, 1);
            }
            else {
                progress.failed(1);
            }
            try {
                // 写结果到文件里
                FileOperateUtils.appendCompareResult(resultList);
            }
            catch (Exception e) {
                logger.error("Failed to write result to file", e);
                context.setHasException(e);
            }
        }
        context.taskCompleted();
    }

    private void refreshResult(List<String> resultList) {
        if (null != resultList && resultList.size() >= MAX_RESULT_CACHE_COUNT) {
            try {
                FileOperateUtils.appendCompareResult(resultList);
                // 清空结果列表
                resultList.clear();
            }
            catch (Exception e) {
                logger.error("Failed to write result to file", e);
                context.setHasException(e);
            }
        }
    }

    private CompareResult compareData(ScmFileInfo info, BSONObject siteBson) {
        int siteId = (int) siteBson.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID);
        ScmSiteInfo siteInfo = ScmDataSourceMgr.getInstance().getSiteInfo(siteId);
        String siteName = siteInfo.getName();
        ScmDataReader reader = null;
        try {
            // 文件site_list下无 ws_version 时，置 1
            Object wsVersionInfo = siteBson.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION);
            int wsVersion = wsVersionInfo == null ? 1 : (int) wsVersionInfo;
            // 驱动 ScmFileImpl 是缺省类型的 class，无法用在这个包里
            ScmFileLocation fileLocation = info.getScmFileLocation(siteId);
            if (null == fileLocation) {
                throw new ScmToolsException("Can not get file location,siteId:" + siteId,
                        ScmExitCode.SYSTEM_ERROR);
            }
            ScmDataInfo dataInfo = ScmDataInfo.forOpenExistData(info.getDataType(),
                    info.getDataId(), new Date(info.getCreateTime()), wsVersion,
                    info.getScmFileLocation(siteId).getTableName());
            reader = ScmDataSourceMgr.getInstance().getReader(siteId, wsVersion, wsName, dataInfo);
            long dataSize = reader.getSize();
            // compare size
            if (info.getSize() != dataSize) {
                CompareResult result = new CompareResult(info.getId(), siteName,
                        info.getMajorVersion(), info.getMinorVersion(), ResultType.ERR_SIZE,
                        "file meta size is " + info.getSize() + ", but data size is " + dataSize);
                return result;
            }
            // compare md5
            if (checkLevel == CheckLevel.MD5) {
                if (StringUtils.hasText(info.getMd5())) {
                    String dataMd5 = CommonUtils.calcMd5(reader);
                    if (!info.getMd5().equals(dataMd5)) {
                        CompareResult result = new CompareResult(info.getId(), siteName,
                                info.getMajorVersion(), info.getMinorVersion(), ResultType.ERR_MD5,
                                "file meta md5 is " + info.getMd5() + ", but data md5 is "
                                        + dataMd5);
                        return result;
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error(
                    "compare file data failed,fileId:{},siteId:{},majorVersion:{},minorVersion:{}",
                    info.getId(), siteId, info.getMajorVersion(), info.getMinorVersion(), e);
            CompareResult result = new CompareResult(info.getId(), siteName, info.getMajorVersion(),
                    info.getMinorVersion(), ResultType.FAILED, e.getMessage());
            return result;
        }
        finally {
            if (null != reader) {
                reader.close();
            }
        }
        return new CompareResult(info.getId(), siteName, info.getMajorVersion(),
                info.getMinorVersion(), ResultType.SAME, "file data same with meta data");
    }
}
