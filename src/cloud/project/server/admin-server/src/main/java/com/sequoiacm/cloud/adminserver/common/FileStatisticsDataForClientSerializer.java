package com.sequoiacm.cloud.adminserver.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.cloud.adminserver.model.statistics.FileStatisticsDataForClient;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsDefine;

public class FileStatisticsDataForClientSerializer
        extends StdSerializer<FileStatisticsDataForClient> {

    public FileStatisticsDataForClientSerializer() {
        super(FileStatisticsDataForClient.class);
    }

    @Override
    public void serialize(FileStatisticsDataForClient value, JsonGenerator gen,
            SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField(ScmStatisticsDefine.REST_FIELD_AVG_TRAFFIC_SIZE,
                value.getStatisticsData().getAvgTrafficSize());
        gen.writeNumberField(ScmStatisticsDefine.REST_FIELD_AVG_RESP_TIME,
                value.getStatisticsData().getAvgResponseTime());
        gen.writeNumberField(ScmStatisticsDefine.REST_FIELD_REQ_COUNT,
                value.getStatisticsData().getRequestCount());
        gen.writeStringField(ScmStatisticsDefine.REST_FIELD_BEGIN, value.getCondition().getBegin());
        gen.writeStringField(ScmStatisticsDefine.REST_FIELD_END, value.getCondition().getEnd());
        if (value.getCondition().getTimeAccuracy() != null) {
            gen.writeStringField(ScmStatisticsDefine.REST_FIELD_TIME_ACCURACY,
                    value.getCondition().getTimeAccuracy().name());
        }
        if (value.getCondition().getUser() != null) {
            gen.writeStringField(ScmStatisticsDefine.REST_FIELD_USER,
                    value.getCondition().getUser());
        }
        if (value.getCondition().getWorkspace() != null) {
            gen.writeStringField(ScmStatisticsDefine.REST_FIELD_WORKSPACE,
                    value.getCondition().getWorkspace());
        }
        gen.writeEndObject();
    }
}
