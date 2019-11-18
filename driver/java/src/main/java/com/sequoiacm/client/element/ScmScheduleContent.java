package com.sequoiacm.client.element;

import org.bson.BSONObject;

/**
 * Schedule content.
 */
public interface ScmScheduleContent {
    /**
     * transform content to BSONObject.
     *
     * @return bson.
     */
    public BSONObject toBSONObject();
}
