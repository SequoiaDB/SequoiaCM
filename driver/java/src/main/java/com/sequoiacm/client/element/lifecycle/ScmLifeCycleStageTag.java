package com.sequoiacm.client.element.lifecycle;

import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class ScmLifeCycleStageTag {

    private String name;

    private String desc;

    public ScmLifeCycleStageTag() {
    }

    public ScmLifeCycleStageTag(BSONObject obj) {
        Object temp = null;
        temp = obj.get("Name");
        if (null != temp) {
            setName((String) temp);
        }

        temp = obj.get("Desc");
        if (null != temp) {
            setDesc((String) temp);
        }
    }

    public ScmLifeCycleStageTag(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public BSONObject toBSONObject() {
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_NAME, name);
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_DESC, desc);
        return bsonObject;
    }

    public ScmLifeCycleStageTag fromBSONObject(BSONObject obj){
        Object temp = null;
        temp = obj.get(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_NAME);
        if (null != temp) {
            setName((String) temp);
        }

        temp = obj.get(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_DESC);
        if (null != temp) {
            setDesc((String) temp);
        }

        return this;
    }
}
