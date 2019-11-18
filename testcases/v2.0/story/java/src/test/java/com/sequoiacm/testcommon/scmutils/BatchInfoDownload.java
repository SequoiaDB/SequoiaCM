package com.sequoiacm.testcommon.scmutils;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fanyu on 2019/10/11.
 */
public class BatchInfoDownload {
    public final static String ID = "ID";
    public final static String WORKSPACE = "WORKSPACE";
    public final static String BATCHNO = "BATCHNO";
    public final static String BATCHFOLDER = "BATCHFOLDER";
    public final static String CLOUDDISKFOLDER = "CLOUDDISKFOLDER";
    public final static String STATE = "STATE";
    public final static String ENDTIME = "ENDTIME";
    public final static String APPLICATIONPERSON = "APPLICATIONPERSON";
    public final static String TYPE = "TYPE";
    public final static String DOWNLOADLOCATION = "DOWNLOADLOCATION";

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String id="";
    private String workspace="";
    private String batchNO="";
    private String batchFolder="";
    private String cloudDiskFolder="";
    private String applicationPerson="";
    private int state;
    private int type;
    private int downloadLocation;
    private Date endTime;

    public String getId() {
        return id;
    }

    public BatchInfoDownload withId(String id) {
        this.id = id;
        return this;
    }

    public String getWorkspace() {
        return workspace;
    }

    public BatchInfoDownload withWorkspace(String workspace) {
        this.workspace = workspace;
        return this;
    }

    public String getBatchNO() {
        return batchNO;
    }

    public BatchInfoDownload withBatchNO(String batchNO) {
        this.batchNO = batchNO;
        return this;
    }

    public String getBatchFolder() {
        return batchFolder;
    }

    public BatchInfoDownload withBatchFolder(String batchFolder) {
        this.batchFolder = batchFolder;
        return this;
    }

    public String getCloudDiskFolder() {
        return cloudDiskFolder;
    }

    public BatchInfoDownload withCloudDiskFolder(String cloudDiskFolder) {
        this.cloudDiskFolder = cloudDiskFolder;
        return this;
    }

    public String getApplicationPerson() {
        return applicationPerson;
    }

    public BatchInfoDownload withApplicationPerson(String applicationPerson) {
        this.applicationPerson = applicationPerson;
        return this;
    }

    public int getState() {
        return state;
    }

    public BatchInfoDownload withState(int state) {
        this.state = state;
        return this;
    }

    public int getType() {
        return type;
    }

    public BatchInfoDownload withType(int type) {
        this.type = type;
        return this;
    }

    public int getDownloadLocation() {
        return downloadLocation;
    }

    public BatchInfoDownload withDownloadLocation(int downloadLocation) {
        this.downloadLocation = downloadLocation;
        return this;
    }

    public Date getEndTime() {
        return endTime;
    }

    public BatchInfoDownload withEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
    }

    public BSONObject toBson() {
        BSONObject bson = new BasicBSONObject();
        bson.put(BatchInfoDownload.ID, this.getId());
        bson.put(BatchInfoDownload.BATCHNO, this.getBatchNO());
        bson.put(BatchInfoDownload.WORKSPACE, this.getWorkspace());
        bson.put(BatchInfoDownload.BATCHFOLDER, this.getBatchFolder());
        bson.put(BatchInfoDownload.CLOUDDISKFOLDER, this.getCloudDiskFolder());
        bson.put(BatchInfoDownload.APPLICATIONPERSON, this.getApplicationPerson());
        bson.put(BatchInfoDownload.STATE, this.getState());
        bson.put(BatchInfoDownload.TYPE, this.getType());
        bson.put(BatchInfoDownload.DOWNLOADLOCATION, this.getDownloadLocation());
        if (this.getEndTime() != null) {
            bson.put(BatchInfoDownload.ENDTIME, format.format(this.getEndTime()));
        } else {
            bson.put(BatchInfoDownload.ENDTIME, this.getEndTime());
        }
        return bson;
    }

    public static BatchInfoDownload toBatchInfo(BasicBSONObject bson) throws ParseException {
        BatchInfoDownload BachInfoDownload = new BatchInfoDownload();
        BachInfoDownload.withId(bson.getString(BachInfoDownload.ID))
                .withWorkspace(bson.getString(BachInfoDownload.WORKSPACE))
                .withBatchNO(bson.getString(BachInfoDownload.BATCHNO))
                .withBatchFolder(bson.getString(BachInfoDownload.BATCHFOLDER))
                .withCloudDiskFolder(bson.getString(BachInfoDownload.CLOUDDISKFOLDER))
                .withApplicationPerson(bson.getString(BachInfoDownload.APPLICATIONPERSON))
                .withType(bson.getInt(BachInfoDownload.TYPE))
                .withDownloadLocation(bson.getInt(BachInfoDownload.TYPE))
                .withState(bson.getInt(BachInfoDownload.STATE));
        if (bson.getString(BachInfoDownload.ENDTIME) != null) {
            BachInfoDownload.withEndTime(format.parse(bson.getString(BachInfoDownload.ENDTIME)));
        } else {
            BachInfoDownload.withEndTime(null);
        }
        return BachInfoDownload;
    }

    public String toString() {
        return this.toBson().toString();
    }
}

