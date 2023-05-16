package com.sequoiacm.om.omserver.module.tag;

import com.sequoiacm.client.element.tag.ScmCustomTag;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class OmCustomTagDetail extends OmTagBasic {

    private String key;
    private String value;
    private BSONObject tag = new BasicBSONObject();

    public OmCustomTagDetail(ScmCustomTag customTag) {
        super(customTag.getId(), OmTagType.CUSTOM_TAG);
        this.key = customTag.getKey();
        this.value = customTag.getValue();
        this.tag.put(this.key, this.value);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String getTagContent() {
        return tag.toString();
    }
}
