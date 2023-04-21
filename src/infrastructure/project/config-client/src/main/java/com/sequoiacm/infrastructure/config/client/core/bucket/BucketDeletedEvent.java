package com.sequoiacm.infrastructure.config.client.core.bucket;

import org.springframework.context.ApplicationEvent;

public class BucketDeletedEvent extends ApplicationEvent {

    public BucketDeletedEvent(String bucketName) {
        super(bucketName);
    }

    public String getDeletedBucketName() {
        return (String) getSource();
    }
}
