package com.sequoiacm.infrastructure.monitor.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sequoiacm.infrastructure.monitor.model.WorkspaceFlow;

public class WorkspaceFlowJsonSerializer extends StdSerializer<WorkspaceFlow> {

    private static final long serialVersionUID = 3771458247952729145L;

    public WorkspaceFlowJsonSerializer() {
        super(WorkspaceFlow.class);
    }

    @Override
    public void serialize(WorkspaceFlow value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField(WorkspaceFlow.WS_NAME, value.getWorkspaceName());
        gen.writeNumberField(WorkspaceFlow.UPLOAD, value.getUpload().longValue());
        gen.writeNumberField(WorkspaceFlow.DOWNLOAD, value.getDownload().longValue());
        gen.writeEndObject();
    }
}
