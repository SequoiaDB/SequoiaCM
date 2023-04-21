package com.sequoiacm.cloud.adminserver.model;

import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

import com.sequoiacm.cloud.adminserver.common.FieldName;

public class BsonTranslator {
    public static final class ContentServer {
        public static ContentServerInfo fromBSONObject(BSONObject obj) {
            ContentServerInfo cs = new ContentServerInfo();
            cs.setId((int) obj.get(FieldName.ContentServer.FIELD_ID));
            cs.setName((String) obj.get(FieldName.ContentServer.FIELD_NAME));
            cs.setHostname((String) obj.get(FieldName.ContentServer.FIELD_HOST_NAME));
            cs.setPort((int) obj.get(FieldName.ContentServer.FIELD_PORT));
            cs.setSiteId((int) obj.get(FieldName.ContentServer.FIELD_SITE_ID));
            return cs;
        }
    }

    public static final class Traffic {
        public static TrafficInfo fromBSONObject(BSONObject obj) {
            TrafficInfo traffic = new TrafficInfo();
            traffic.setType((String) obj.get(FieldName.Traffic.FIELD_TYPE)); 
            traffic.setWorkspaceName((String) obj.get(FieldName.Traffic.FIELD_WORKSPACE_NAME));
            traffic.setTraffic((long) obj.get(FieldName.Traffic.FIELD_TRAFFIC));
            traffic.setRecordTime((long) obj.get(FieldName.Traffic.FIELD_RECORD_TIME));
            return traffic;
        }
    }
    
    public static final class FileDelta {
        public static FileDeltaInfo fromBSONObject(BSONObject obj) {
            FileDeltaInfo fileDelta = new FileDeltaInfo();
            fileDelta.setWorkspaceName((String) obj.get(FieldName.FileDelta.FIELD_WORKSPACE_NAME));
            fileDelta.setCountDelta((long) obj.get(FieldName.FileDelta.FIELD_COUNT_DELTA));
            fileDelta.setSizeDelta((long) obj.get(FieldName.FileDelta.FIELD_SIZE_DELTA));
            fileDelta.setRecordTime((long) obj.get(FieldName.FileDelta.FIELD_RECORD_TIME));
            return fileDelta;
        }
    }

    public static final class ObjectDelta {
        public static ObjectDeltaInfo fromBSONObject(BSONObject obj) {
            ObjectDeltaInfo objectDelta = new ObjectDeltaInfo();
            objectDelta.setBucketName(
                    BsonUtils.getStringChecked(obj, FieldName.ObjectDelta.FIELD_BUCKET_NAME));
            objectDelta.setCountDelta((BsonUtils
                    .getNumberChecked(obj, FieldName.ObjectDelta.FIELD_COUNT_DELTA).longValue()));
            objectDelta.setSizeDelta(BsonUtils
                    .getNumberChecked(obj, FieldName.ObjectDelta.FIELD_SIZE_DELTA).longValue());
            objectDelta.setRecordTime(BsonUtils
                    .getNumberChecked(obj, FieldName.ObjectDelta.FIELD_RECORD_TIME).longValue());
            objectDelta.setUpdateTime(BsonUtils
                    .getNumberChecked(obj, FieldName.ObjectDelta.FIELD_UPDATE_TIME).longValue());
            return objectDelta;
        }
    }

    public static final class Workspace {
        public static WorkspaceInfo fromBSONObject(BSONObject obj) {
            WorkspaceInfo workspace = new WorkspaceInfo();
            workspace.setId((int) obj.get(FieldName.Workspace.FIELD_ID));
            workspace.setName((String) obj.get(FieldName.Workspace.FIELD_NAME));

            return workspace;
        }
    }
}
