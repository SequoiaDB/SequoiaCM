package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
            List<Integer> siteIdList = new ArrayList<>();
            for (ScmFileLocation fileLocation : siteList) {
                siteIdList.add(fileLocation.getSiteId());
            }
            ScmFileDataDeleter dataDeleter = new ScmFileDataDeleter(siteIdList, wsInfo,
                    fileMeta.getDataInfo());
            dataDeleter.deleteData();
        }
        catch (Exception e) {
            logger.warn("remove file data failed:fileId={},version={}.{},dataInfo={}",
                    fileMeta.getId(), fileMeta.getMajorVersion(), fileMeta.getMinorVersion(),
                    fileMeta.getDataInfo(), e);
        }
    }
}
