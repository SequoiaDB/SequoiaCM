package com.sequoiacm.tools.element;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.tools.common.ScmCommon;

public class ScmSimpleFileInfo {

    // DEFAULT VALUE
    private final String DEFAULT_MIME_TYPE = "unknown";
    private final int DEFAULT_MAJOR_VERSION = 1;
    private final int DEFAULT_MINOR_VERSION = 0;
    private final int DEFAULT_TYPE = 1;
    private final int DEFAULT_DATA_TYPE = 1;
    private final int DEFAULT_STATUS = 0;

    private BSONObject fileObj = new BasicBSONObject();
    private String id;

    public ScmSimpleFileInfo(long size, long metaCreateSec, long lobCreateMill, String id,
            int mainSiteId, String user) {
        long metaCreateMill = ((long) metaCreateSec) * 1000;
        Date metaCreateDate = new Date(metaCreateMill);

        this.id = id;
        fileObj.put(FieldName.FIELD_CLFILE_NAME, "");
        fileObj.put(FieldName.FIELD_CLFILE_FILE_AUTHOR, "");
        fileObj.put(FieldName.FIELD_CLFILE_FILE_TITLE, "");
        fileObj.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, DEFAULT_MIME_TYPE);
        fileObj.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, "");
        fileObj.put(FieldName.FIELD_CLFILE_FILE_CLASS_ID, 0);
        fileObj.put(FieldName.FIELD_CLFILE_PROPERTIES, null);

        fileObj.put(FieldName.FIELD_CLFILE_ID, id);

        fileObj.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, DEFAULT_MAJOR_VERSION);
        fileObj.put(FieldName.FIELD_CLFILE_MINOR_VERSION, DEFAULT_MINOR_VERSION);
        fileObj.put(FieldName.FIELD_CLFILE_TYPE, DEFAULT_TYPE);
        fileObj.put(FieldName.FIELD_CLFILE_BATCH_ID, "");
        fileObj.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, id);
        fileObj.put(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, lobCreateMill);
        fileObj.put(FieldName.FIELD_CLFILE_FILE_DATA_TYPE, DEFAULT_DATA_TYPE);

        BSONObject siteList = (BSONObject) JSON.parse(String.format("{%s:[{%s:%d,%s:%d,%s:%d}]}",
                FieldName.FIELD_CLFILE_FILE_SITE_LIST, FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID,
                mainSiteId, FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME, metaCreateMill,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME, metaCreateMill));
        fileObj.putAll(siteList);

        fileObj.put(FieldName.FIELD_CLFILE_INNER_USER, user);
        fileObj.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER, user);
        fileObj.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, metaCreateMill);
        fileObj.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH,
                ScmCommon.DateUtil.getCurrentYearMonth(metaCreateDate));
        fileObj.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, metaCreateMill);
        fileObj.put(FieldName.FIELD_CLFILE_EXTRA_STATUS, DEFAULT_STATUS);
        fileObj.put(FieldName.FIELD_CLFILE_EXTRA_TRANS_ID, "");
        fileObj.put(FieldName.FIELD_CLFILE_FILE_SIZE, size);
    }

    public BSONObject get() {
        return fileObj;
    }

    public String getId() {
        return id;
    }
}
