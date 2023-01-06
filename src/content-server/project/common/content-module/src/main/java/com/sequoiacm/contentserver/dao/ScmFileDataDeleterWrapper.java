package com.sequoiacm.contentserver.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;

public class ScmFileDataDeleterWrapper {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileDataDeleterWrapper.class);
    private final ScmWorkspaceInfo wsInfo;
    private FileMeta fileMeta;

    public ScmFileDataDeleterWrapper(ScmWorkspaceInfo wsInfo, FileMeta fileMeta) {
        this.fileMeta = fileMeta;
        this.wsInfo = wsInfo;
    }

    public void deleteDataSilence() {
        if (fileMeta.isDeleteMarker()) {
            return;
        }
        try {
            BasicBSONList sites = fileMeta.getSiteList();
            List<ScmFileLocation> siteList = new ArrayList<>();
            CommonHelper.getFileLocationList(sites, siteList);
            ScmFileDataDeleter dataDeleter = new ScmFileDataDeleter(siteList, wsInfo,
                    fileMeta.getDataId(), fileMeta.getDataType(),
                    new Date(fileMeta.getDataCreateTime()));
            dataDeleter.deleteData();
        }
        catch (Exception e) {
            logger.warn(
                    "remove file data failed:fileId={},version={}.{},dataId={},dataCreateTime={}",
                    fileMeta.getId(), fileMeta.getMajorVersion(), fileMeta.getMinorVersion(),
                    fileMeta.getDataId(), fileMeta.getDataCreateTime(), e);
        }
    }
}
