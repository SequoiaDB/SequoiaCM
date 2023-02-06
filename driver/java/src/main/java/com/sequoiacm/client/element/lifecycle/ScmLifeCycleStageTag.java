package com.sequoiacm.client.element.lifecycle;

import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * The stage tag info.
 */
public class ScmLifeCycleStageTag {

    private String name;

    private String desc;

    public ScmLifeCycleStageTag() {
    }

    public static ScmLifeCycleStageTag fromUser(BSONObject obj) {
        ScmLifeCycleStageTag stageTag = new ScmLifeCycleStageTag();

        Object temp = null;
        temp = obj.get("Name");
        if (null != temp) {
            stageTag.name = ((String) temp);
        }

        temp = obj.get("Desc");
        if (null != temp) {
            stageTag.desc = ((String) temp);
        }

        return stageTag;
    }

    public ScmLifeCycleStageTag(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    /**
     * Return the stage tag name.
     * 
     * @return stage tag name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the stage tag name.
     * 
     * @param name
     *            stage tag name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the stage tag desc.
     * 
     * @return stage tag desc.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Set the stage tag desc.
     * 
     * @param desc
     *            stage tag desc.
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public BSONObject toBSONObject() {
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_NAME, name);
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_DESC, desc);
        return bsonObject;
    }

    public static ScmLifeCycleStageTag fromRecord(BSONObject obj) {
        ScmLifeCycleStageTag stageTag = new ScmLifeCycleStageTag();

        Object temp = null;
        temp = obj.get(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_NAME);
        if (null != temp) {
            stageTag.name = (String) temp;
        }

        temp = obj.get(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_DESC);
        if (null != temp) {
            stageTag.desc = (String) temp;
        }

        return stageTag;
    }
}
