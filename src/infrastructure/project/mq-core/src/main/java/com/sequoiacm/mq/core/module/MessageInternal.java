package com.sequoiacm.mq.core.module;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;

public class MessageInternal extends Message<BSONObject> {

    public MessageInternal() {
    }

    public MessageInternal(BSONObject record) {
        key = BsonUtils.getStringChecked(record, FIELD_KEY);
        id = BsonUtils.getNumberChecked(record, FIELD_ID).longValue();
        topic = BsonUtils.getStringChecked(record, FIELD_TOPIC);
        partition = BsonUtils.getNumberChecked(record, FIELD_PARTITION_NUM).intValue();
        createTime = BsonUtils.getNumberChecked(record, FIELD_CREATE_TIME).longValue();
        msgContent = BsonUtils.getBSONChecked(record, FIELD_MSG_CONTENT);
        msgProducer = BsonUtils.getString(record, FIELD_MSG_PRODUCER);
    }

}
