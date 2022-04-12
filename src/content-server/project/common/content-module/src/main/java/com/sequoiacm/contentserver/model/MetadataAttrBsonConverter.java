package com.sequoiacm.contentserver.model;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.util.Assert;

import com.sequoiacm.common.AttributeType;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.AbstractBsonConverter;

public class MetadataAttrBsonConverter extends AbstractBsonConverter<MetadataAttr> {

    @Override
    public MetadataAttr convert(BSONObject obj) {
        Assert.notNull(obj, "BSONObject object is null");
        MetadataAttr attr = new MetadataAttr();
        attr.setId(getStringChecked(obj, FieldName.Attribute.FIELD_ID));
        attr.setName(getStringChecked(obj, FieldName.Attribute.FIELD_NAME));
        attr.setDisplayName(getStringOrElse(obj, FieldName.Attribute.FIELD_DISPLAY_NAME, ""));
        attr.setDescription(getStringOrElse(obj, FieldName.Attribute.FIELD_DESCRIPTION, ""));
        attr.setType(AttributeType.valueOf(getStringChecked(obj, FieldName.Attribute.FIELD_TYPE)));
        attr.setCheckRule(getBsonChecked(obj, FieldName.Attribute.FIELD_CHECK_RULE));
        attr.setRequired(getBooleanOrElse(obj, FieldName.Attribute.FIELD_REQUIRED, false));
        attr.setCreateUser(getString(obj, FieldName.Attribute.FIELD_INNER_CREATE_USER));
        attr.setCreateTime(getLongOrElse(obj, FieldName.Attribute.FIELD_INNER_CREATE_TIME, 0L));
        attr.setUpdateUser(getString(obj, FieldName.Attribute.FIELD_INNER_UPDATE_USER));
        attr.setUpdateTime(getLongOrElse(obj, FieldName.Attribute.FIELD_INNER_UPDATE_TIME, 0L));
        
        return attr;
    }
    
    @Override
    public BSONObject convert(MetadataAttr value) {
        Assert.notNull(value, "MetadataAttr value is null");
        BSONObject obj = new BasicBSONObject();
        obj.put(FieldName.Attribute.FIELD_ID, value.getId());
        obj.put(FieldName.Attribute.FIELD_NAME, value.getName());
        obj.put(FieldName.Attribute.FIELD_DISPLAY_NAME, value.getDisplayName());
        obj.put(FieldName.Attribute.FIELD_DESCRIPTION, value.getDescription());
        obj.put(FieldName.Attribute.FIELD_TYPE, value.getType().getName());
        obj.put(FieldName.Attribute.FIELD_CHECK_RULE, value.getCheckRule());
        obj.put(FieldName.Attribute.FIELD_REQUIRED, value.isRequired());
        obj.put(FieldName.Attribute.FIELD_INNER_CREATE_USER, value.getCreateUser());
        obj.put(FieldName.Attribute.FIELD_INNER_CREATE_TIME, value.getCreateTime());
        obj.put(FieldName.Attribute.FIELD_INNER_UPDATE_USER, value.getUpdateUser());
        obj.put(FieldName.Attribute.FIELD_INNER_UPDATE_TIME, value.getUpdateTime());
        return obj;
    }
}
