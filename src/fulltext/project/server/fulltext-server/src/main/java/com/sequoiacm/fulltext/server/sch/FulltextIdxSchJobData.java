package com.sequoiacm.fulltext.server.sch;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;

public class FulltextIdxSchJobData {
    public static final String KEY_JOB_DATA_FILE_MATCHER = "file_matcher";
    public static final String KEY_JOB_DATA_INDEX_LOCATION = "index_location";
    public static final String KEY_JOB_DATA_WORKSPACE = "workspace";
    public static final String KEY_JOB_DATA_LATEST_MSG_ID = "latest_msg_id";
    private BSONObject fileMatcher;
    private String indexDataLocation;
    private String ws;
    private long latestMsgId = -1;

    public FulltextIdxSchJobData() {

    }

    public FulltextIdxSchJobData(BSONObject bson) {
        ws = BsonUtils.getStringChecked(bson, KEY_JOB_DATA_WORKSPACE);
        indexDataLocation = BsonUtils.getStringChecked(bson, KEY_JOB_DATA_INDEX_LOCATION);
        fileMatcher = BsonUtils.getBSON(bson, KEY_JOB_DATA_FILE_MATCHER);
        if (fileMatcher == null) {
            fileMatcher = new BasicBSONObject();
        }
        latestMsgId = BsonUtils.getNumberOrElse(bson, KEY_JOB_DATA_LATEST_MSG_ID, -1).longValue();
    }

    public BSONObject toBSON() {
        BSONObject jobDataBson = new BasicBSONObject();
        jobDataBson.put(FulltextIdxSchJobData.KEY_JOB_DATA_FILE_MATCHER, fileMatcher);
        jobDataBson.put(FulltextIdxSchJobData.KEY_JOB_DATA_INDEX_LOCATION, indexDataLocation);
        jobDataBson.put(FulltextIdxSchJobData.KEY_JOB_DATA_WORKSPACE, ws);
        jobDataBson.put(FulltextIdxSchJobData.KEY_JOB_DATA_LATEST_MSG_ID, latestMsgId);
        return jobDataBson;
    }

    public long getLatestMsgId() {
        return latestMsgId;
    }

    public void setLatestMsgId(long latestMsgId) {
        this.latestMsgId = latestMsgId;
    }

    public String getWs() {
        return ws;
    }

    public void setWs(String ws) {
        this.ws = ws;
    }

    public BSONObject getFileMatcher() {
        return fileMatcher;
    }

    public void setFileMatcher(BSONObject fileMatcher) {
        this.fileMatcher = fileMatcher;
    }

    public String getIndexDataLocation() {
        return indexDataLocation;
    }

    public void setIndexDataLocation(String indexDataLocation) {
        this.indexDataLocation = indexDataLocation;
    }

}
