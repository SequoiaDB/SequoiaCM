package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ScmFileDataDeleterWrapper {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileDataDeleterWrapper.class);
    private final ScmWorkspaceInfo wsInfo;
    private final BSONObject file;

    public ScmFileDataDeleterWrapper(ScmWorkspaceInfo wsInfo, BSONObject file) {
        this.file = file;
        this.wsInfo = wsInfo;
    }

    public void deleteDataSilence() {
        ScmDataInfo dataInfo = new ScmDataInfo(file);
        try {
            BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
            List<ScmFileLocation> siteList = new ArrayList<>();
            CommonHelper.getFileLocationList(sites, siteList);
            List<Integer> siteIdList = new ArrayList<>();
            for (ScmFileLocation fileLocation : siteList) {
                siteIdList.add(fileLocation.getSiteId());
            }
            ScmFileDataDeleter dataDeleter = new ScmFileDataDeleter(siteIdList, wsInfo, dataInfo);
            dataDeleter.deleteData();
        }
        catch (Exception e) {
            logger.warn("remove file data failed:fileId={},version={}.{},dataInfo={}",
                    BsonUtils.getStringChecked(file, FieldName.FIELD_CLFILE_ID),
                    file.get(FieldName.FIELD_CLFILE_MAJOR_VERSION),
                    file.get(FieldName.FIELD_CLFILE_MINOR_VERSION), dataInfo, e);
        }
    }
}
