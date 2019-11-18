package com.sequoiacm.testcommon.scmutils;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fanyu on 2019/9/10.
 */
public class BatchInfoUpload {
    public final static String ID = "ID";
    public final static String WORKSPACE = "WORKSPACE";
    public final static String BATCHNO = "BATCHNO";
    public final static String BANTCHPATH = "BANTCHPATH";
    public final static String STATE = "STATE";
    public final static String CREATETIME = "CREATETIME";
    public final static String STARTTIME = "STARTTIME";
    public final static String ENDTIME = "ENDTIME";

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String id;
    private String workspace;
    private String batchNO;
    private String batchPath;
    private int state;
    private Date createTime;
    private Date startTime;
    private Date endTime;

    public String getId() {
        return id;
    }

    public BatchInfoUpload withId(String id) {
        this.id = id;
        return this;
    }

    public String getWorkspace() {
        return workspace;
    }

    public BatchInfoUpload withWorkspace(String workspace) {
        this.workspace = workspace;
        return this;
    }

    public String getBatchNO() {
        return batchNO;
    }

    public BatchInfoUpload withBatchNO(String batchNO) {
        this.batchNO = batchNO;
        return this;
    }

    public String getBatchPath() {
        return batchPath;
    }

    public BatchInfoUpload withBatchPath(String batchPath) {
        this.batchPath = batchPath;
        return this;
    }

    public int getState() {
        return state;
    }

    public BatchInfoUpload withState(int state) {
        this.state = state;
        return this;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public BatchInfoUpload withCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public Date getStartTime() {
        return startTime;
    }

    public BatchInfoUpload withStartTime(Date startTime) {
        this.startTime = startTime;
        return this;
    }

    public Date getEndTime() {
        return endTime;
    }

    public BatchInfoUpload withEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
    }

    public BSONObject toBson() {
        BSONObject bson = new BasicBSONObject();
        bson.put(BatchInfoUpload.ID, this.getId());
        bson.put(BatchInfoUpload.BANTCHPATH, this.getBatchPath());
        bson.put(BatchInfoUpload.BATCHNO, this.getBatchNO());
        bson.put(BatchInfoUpload.WORKSPACE, this.getWorkspace());
        if (this.getCreateTime() != null) {
            bson.put(BatchInfoUpload.CREATETIME, format.format(this.getCreateTime()));
        } else {
            bson.put(BatchInfoUpload.CREATETIME, this.getCreateTime());
        }
        if (this.getStartTime() != null) {
            bson.put(BatchInfoUpload.STARTTIME, format.format(this.getCreateTime()));
        } else {
            bson.put(BatchInfoUpload.STARTTIME, this.getStartTime());
        }
        if (this.getEndTime() != null) {
            bson.put(BatchInfoUpload.ENDTIME, format.format(this.getEndTime()));
        } else {
            bson.put(BatchInfoUpload.ENDTIME, this.getEndTime());
        }
        bson.put(BatchInfoUpload.STATE, this.getState());
        return bson;
    }

    public static BatchInfoUpload toBatchInfo(BasicBSONObject bson) throws ParseException {
        BatchInfoUpload batchInfoUpload = new BatchInfoUpload();
        batchInfoUpload.withId(bson.getString(BatchInfoUpload.ID))
                .withWorkspace(bson.getString(BatchInfoUpload.WORKSPACE))
                .withBatchNO(bson.getString(BatchInfoUpload.BATCHNO))
                .withBatchPath(bson.getString(BatchInfoUpload.BANTCHPATH))
                .withState(bson.getInt(BatchInfoUpload.STATE))
                .withCreateTime(format.parse(bson.getString(BatchInfoUpload.CREATETIME)))
                .withStartTime(format.parse(bson.getString(BatchInfoUpload.STARTTIME)));
        if (bson.getString(BatchInfoUpload.ENDTIME) != null) {
            batchInfoUpload.withEndTime(format.parse(bson.getString(BatchInfoUpload.ENDTIME)));
        } else {
            batchInfoUpload.withEndTime(null);
        }
        return batchInfoUpload;
    }

    public String toString() {
        return toBson().toString();
    }
}
