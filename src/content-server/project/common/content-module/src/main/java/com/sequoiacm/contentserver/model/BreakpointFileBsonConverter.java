package com.sequoiacm.contentserver.model;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.util.Assert;

import com.sequoiacm.common.checksum.ChecksumType;
import com.sequoiacm.contentserver.common.AbstractBsonConverter;

public class BreakpointFileBsonConverter extends AbstractBsonConverter<BreakpointFile> {

    public static final String BSON_FIELD_FILE_NAME = "file_name";
    public static final String BSON_FIELD_SITE_ID = "site_id";
    public static final String BSON_FIELD_CHECKSUM_TYPE = "checksum_type";
    public static final String BSON_FIELD_CHECKSUM = "checksum";
    public static final String BSON_FIELD_DATA_ID = "data_id";
    public static final String BSON_FIELD_COMPLETED = "completed";
    public static final String BSON_FIELD_UPLOAD_SIZE = "upload_size";
    public static final String BSON_FIELD_CREATE_USER = "create_user";
    public static final String BSON_FIELD_CREATE_TIME = "create_time";
    public static final String BSON_FIELD_UPLOAD_USER = "upload_user";
    public static final String BSON_FIELD_UPLOAD_TIME = "upload_time";
    public static final String BSON_FIELD_IS_NEED_MD5 = "is_need_md5";
    public static final String BSON_FIELD_MD5 = "md5";
    public static final String BSON_FIELD_EXTRA_CONTEXT = "extra_context";
    public static final String BSON_FIELD_WS_VERSION = "ws_version";

    @Override
    public BreakpointFile convert(BSONObject obj) {
        Assert.notNull(obj, "BSONObject object is null");
        BreakpointFile file = new BreakpointFile()
                .setFileName(getStringChecked(obj, BSON_FIELD_FILE_NAME))
                .setSiteId(getIntegerChecked(obj, BSON_FIELD_SITE_ID))
                .setChecksumType(ChecksumType.valueOf(
                        getStringOrElse(obj, BSON_FIELD_CHECKSUM_TYPE, ChecksumType.NONE.name())))
                .setChecksum(getLongOrElse(obj, BSON_FIELD_CHECKSUM, 0L))
                .setDataId(getString(obj, BSON_FIELD_DATA_ID))
                .setCompleted(getBooleanOrElse(obj, BSON_FIELD_COMPLETED, false))
                .setUploadSize(getLongOrElse(obj, BSON_FIELD_UPLOAD_SIZE, 0L))
                .setCreateUser(getString(obj, BSON_FIELD_CREATE_USER))
                .setCreateTime(getLongOrElse(obj, BSON_FIELD_CREATE_TIME, 0L))
                .setUploadUser(getString(obj, BSON_FIELD_UPLOAD_USER))
                .setUploadTime(getLongOrElse(obj, BSON_FIELD_UPLOAD_TIME, 0L))
                .setNeedMd5(getBooleanOrElse(obj, BSON_FIELD_IS_NEED_MD5, false))
                .setMd5(getString(obj, BSON_FIELD_MD5))
                .setExtraContext(BsonUtils.getBSON(obj, BSON_FIELD_EXTRA_CONTEXT));
        Object temp = obj.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION);
        if (null != temp) {
            file.setWsVersion((Integer) temp);
        }
        else {
            file.setWsVersion(1);
        }
        return file;
    }

    @Override
    public BSONObject convert(BreakpointFile value) {
        Assert.notNull(value, "BreakpointFile value is null");
        BSONObject obj = new BasicBSONObject();
        obj.put(BSON_FIELD_FILE_NAME, value.getFileName());
        obj.put(BSON_FIELD_SITE_ID, value.getSiteId());
        obj.put(BSON_FIELD_CHECKSUM_TYPE, value.getChecksumType().name());
        obj.put(BSON_FIELD_CHECKSUM, value.getChecksum());
        obj.put(BSON_FIELD_DATA_ID, value.getDataId());
        obj.put(BSON_FIELD_COMPLETED, value.isCompleted());
        obj.put(BSON_FIELD_UPLOAD_SIZE, value.getUploadSize());
        obj.put(BSON_FIELD_CREATE_USER, value.getCreateUser());
        obj.put(BSON_FIELD_CREATE_TIME, value.getCreateTime());
        obj.put(BSON_FIELD_UPLOAD_USER, value.getUploadUser());
        obj.put(BSON_FIELD_UPLOAD_TIME, value.getUploadTime());
        obj.put(BSON_FIELD_IS_NEED_MD5, value.isNeedMd5());
        obj.put(BSON_FIELD_MD5, value.getMd5());
        obj.put(BSON_FIELD_EXTRA_CONTEXT, value.getExtraContext());
        obj.put(BSON_FIELD_WS_VERSION, value.getWsVersion());
        return obj;
    }
}
