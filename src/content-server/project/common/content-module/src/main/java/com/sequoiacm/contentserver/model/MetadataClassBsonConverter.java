package com.sequoiacm.contentserver.model;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.util.Assert;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.AbstractBsonConverter;

public class MetadataClassBsonConverter extends AbstractBsonConverter<MetadataClass> {

    @Override
    public MetadataClass convert(BSONObject obj) {
        Assert.notNull(obj, "BSONObject object is null");
        MetadataClass metaClass = new MetadataClass();
        metaClass.setId(getStringChecked(obj, FieldName.Class.FIELD_ID));
        metaClass.setName(getStringChecked(obj, FieldName.Class.FIELD_NAME));
        metaClass.setDescription(getStringOrElse(obj, FieldName.Class.FIELD_DESCRIPTION, ""));
        metaClass.setCreateUser(getString(obj, FieldName.Class.FIELD_INNER_CREATE_USER));
        metaClass.setCreateTime(getLongOrElse(obj, FieldName.Class.FIELD_INNER_CREATE_TIME, 0L));
        metaClass.setUpdateUser(getString(obj, FieldName.Class.FIELD_INNER_UPDATE_USER));
        metaClass.setUpdateTime(getLongOrElse(obj, FieldName.Class.FIELD_INNER_UPDATE_TIME, 0L));
        
        
        return metaClass;
    }

    @Override
    public BSONObject convert(MetadataClass value) {
        Assert.notNull(value, "MetadataAttr value is null");
        BSONObject obj = new BasicBSONObject();
        obj.put(FieldName.Class.FIELD_ID, value.getId());
        obj.put(FieldName.Class.FIELD_NAME, value.getName());
        obj.put(FieldName.Class.FIELD_DESCRIPTION, value.getDescription());
        obj.put(FieldName.Class.FIELD_INNER_CREATE_USER, value.getCreateUser());
        obj.put(FieldName.Class.FIELD_INNER_CREATE_TIME, value.getCreateTime());
        obj.put(FieldName.Class.FIELD_INNER_UPDATE_USER, value.getUpdateUser());
        obj.put(FieldName.Class.FIELD_INNER_UPDATE_TIME, value.getUpdateTime());
        return obj;
    }
}
