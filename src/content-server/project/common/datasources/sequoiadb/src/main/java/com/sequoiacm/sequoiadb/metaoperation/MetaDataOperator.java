package com.sequoiacm.sequoiadb.metaoperation;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.module.ScmRecyclingLog;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class MetaDataOperator {

    public static final String FIELD_LOG_INFO_ORIGINAL_COLLECTION_SPACE = "original_collectionspace";
    public static final String FIELD_LOG_INFO_RENAMED_COLLECTION_SPACE = "renamed_collectionspace";

    private String wsName;
    private String siteName;

    private int siteId;
    private ContentModuleMetaSource metaSource;

    public MetaDataOperator(MetaSource metaSource, String wsName, String siteName, int siteId) {
        this.metaSource = (ContentModuleMetaSource) metaSource;
        this.wsName = wsName;
        this.siteName = siteName;
        if (siteName == null) {
            throw new IllegalArgumentException("siteName is null");
        }
        this.siteId = siteId;
    }

    public void insertRecyclingLog(ScmRecyclingLog log) throws ScmMetasourceException {
        metaSource.getSpaceRecyclingLogAccessor().insertRecyclingLog(log);
    }

    public void removeRecyclingLog(String csName) throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLRECYCLE_SITE_ID, siteId);
        matcher.put(
                FieldName.FIELD_CLRECYCLE_LOG_INFO + "." + FIELD_LOG_INFO_ORIGINAL_COLLECTION_SPACE,
                csName);
        metaSource.getSpaceRecyclingLogAccessor().delete(matcher);
    }

    public ScmRecyclingLog queryRecyclingLog(String csName) throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLRECYCLE_SITE_ID, siteId);
        matcher.put(
                FieldName.FIELD_CLRECYCLE_LOG_INFO + "." + FIELD_LOG_INFO_ORIGINAL_COLLECTION_SPACE,
                csName);
        return metaSource.getSpaceRecyclingLogAccessor().queryOneRecyclingLog(matcher);
    }

    public void removeTableNameRecord(String csName) throws ScmMetasourceException {
        metaSource.getDataTableNameHistoryAccessor().deleteHistoryDataTableRecord(wsName, siteName,
                csName);
    }

}
