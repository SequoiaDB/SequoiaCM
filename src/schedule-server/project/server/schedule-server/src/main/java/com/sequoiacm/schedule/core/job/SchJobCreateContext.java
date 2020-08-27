package com.sequoiacm.schedule.core.job;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;

public class SchJobCreateContext {
    private ScheduleJobInfo info;
    private Map<String, Object> contextData;

    public SchJobCreateContext(ScheduleJobInfo info) {
        this.info = info;
        this.contextData = new HashMap<>();
    }

    public ScheduleJobInfo getJobInfo() {
        return info;
    }

    public void set(String key, Object data) {
        contextData.put(key, data);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> dataType) throws ScheduleException {
        T data = (T) contextData.get(key);
        if (data == null) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "no such context data:key=" + key + ", contextData=" + contextData);
        }
        return data;
    }
}
