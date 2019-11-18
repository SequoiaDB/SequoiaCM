package com.sequoiacm.testcommon.scmutils;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fanyu on 2019/10/11.
 */
public class BatchFileInfoDownload {
    public final static String ID = "ID";
    public final static String WORKSPACE = "WORKSPACE";
    public final static String BATCHNO = "BATCHNO";
    public final static String LOCALPATH = "LOCALPATH";
    public final static String CLOUDDISKPATH = "CLOUDDISKPATH";
    public final static String FILEPATH = "FILEPATH";
    public final static String FILENAME = "FILENAME";
    public final static String FILEID = "FILEID";
    public final static String TYPE = "TYPE";
    public final static String DOWNLOADLOCATION = "DOWNLOADLOCATION";
    public final static String STAUS = "STATUS";
    public final static String READSON = "REASON";
    public final static String ENDTIME = "ENDTIME";
    public final static String LOCALFILEPATH = "LOCALFILEPATH";
    public final static String EXPSCMDIRPATH = "EXPSCMDIRPATH";
    public final static String EXPSCMFILENAME = "EXPSCMFILENAME";
    public final static String EXPSCMID= "EXPSCMID";

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String id;
    private String workspace;
    private String batchNO;
    private String localPath;
    private String cloudDiskPath;
    private String filePath;
    private String fileName;
    private String fileId;
    private int type;
    private int downloadLocation;
    private int status;
    private String reason;
    private Date endTime;
    private String localFilePath;
    private String expScmDirPath;
    private String expScmFileName;
    private String expScmId;

    public String getId() {
        return id;
    }

    public BatchFileInfoDownload withId(String id) {
        this.id = id;
        return this;
    }

    public String getWorkspace() {
        return workspace;
    }

    public BatchFileInfoDownload withWorkspace(String workspace) {
        this.workspace = workspace;
        return this;
    }

    public String getBatchNO() {
        return batchNO;
    }

    public BatchFileInfoDownload withBatchNO(String batchNO) {
        this.batchNO = batchNO;
        return this;
    }

    public String getLocalPath() {
        return localPath;
    }

    public BatchFileInfoDownload withLocalPath(String localPath) {
        this.localPath = localPath;
        return this;
    }

    public String getCloudDiskPath() {
        return cloudDiskPath;
    }

    public BatchFileInfoDownload withCloudDiskPath(String cloudDiskPath) {
        this.cloudDiskPath = cloudDiskPath;
        return this;
    }

    public String getFilePath() {
        return filePath;
    }

    public BatchFileInfoDownload withFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public BatchFileInfoDownload withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public BatchFileInfoDownload withFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public int getType() {
        return type;
    }

    public int getDownloadLocation() {
        return downloadLocation;
    }

    public BatchFileInfoDownload withDownloadLocation(int downloadLocation) {
        this.downloadLocation = downloadLocation;
        return this;
    }

    public BatchFileInfoDownload withType(int type) {
        this.type = type;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public BatchFileInfoDownload withStatus(int status) {
        this.status = status;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public BatchFileInfoDownload withReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Date getEndTime() {
        return endTime;
    }

    public BatchFileInfoDownload withEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public BatchFileInfoDownload withLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
        return this;
    }

    public String getExpScmDirPath() {
        return expScmDirPath;
    }

    public BatchFileInfoDownload withExpScmDirPath(String expScmDirPath) {
        this.expScmDirPath = expScmDirPath;
        return this;
    }

    public String getExpScmFileName() {
        return expScmFileName;
    }

    public BatchFileInfoDownload withExpScmFileName(String expScmFileName) {
        this.expScmFileName = expScmFileName;
        return this;
    }

    public String getExpScmId() {
        return expScmId;
    }

    public BatchFileInfoDownload withExpScmId(String expScmId) {
        this.expScmId = expScmId;
        return this;
    }

    public BSONObject toBson() {
        BSONObject bson = new BasicBSONObject();
        bson.put(BatchFileInfoDownload.ID, this.getId());
        bson.put(BatchFileInfoDownload.WORKSPACE, this.getWorkspace());
        bson.put(BatchFileInfoDownload.BATCHNO, this.getBatchNO());
        bson.put(BatchFileInfoDownload.STAUS, this.getStatus());
        bson.put(BatchFileInfoDownload.LOCALPATH, this.getLocalPath());
        bson.put(BatchFileInfoDownload.CLOUDDISKPATH, this.getCloudDiskPath());
        bson.put(BatchFileInfoDownload.FILEPATH, this.getFilePath());
        bson.put(BatchFileInfoDownload.FILEID, this.getFileId());
        bson.put(BatchFileInfoDownload.FILENAME, this.getFileName());
        bson.put(BatchFileInfoDownload.EXPSCMDIRPATH,this.getExpScmDirPath());
        bson.put(BatchFileInfoDownload.LOCALFILEPATH,this.getLocalFilePath());
        bson.put(BatchFileInfoDownload.EXPSCMFILENAME,this.getExpScmFileName());
        bson.put(BatchFileInfoDownload.EXPSCMID,this.getExpScmId());
        if (this.getEndTime() != null) {
            bson.put(BatchFileInfoDownload.ENDTIME, format.format(this.getEndTime()));
        } else {
            bson.put(BatchFileInfoDownload.ENDTIME, this.getEndTime());
        }
        bson.put(BatchFileInfoDownload.TYPE, this.getType());
        bson.put(BatchFileInfoDownload.DOWNLOADLOCATION, this.getDownloadLocation());
        bson.put(BatchFileInfoDownload.READSON, this.getReason());
        return bson;
    }

    public static BatchFileInfoDownload toBatchFileInfo(BasicBSONObject bson) throws ParseException {
        BatchFileInfoDownload batchFileInfoDownload = new BatchFileInfoDownload()
                .withId(bson.getString(BatchFileInfoDownload.ID))
                .withBatchNO(bson.getString(BatchFileInfoDownload.BATCHNO))
                .withLocalPath(bson.getString(BatchFileInfoDownload.LOCALPATH))
                .withCloudDiskPath(bson.getString(BatchFileInfoDownload.CLOUDDISKPATH))
                .withFilePath(bson.getString(BatchFileInfoDownload.FILEPATH))
                .withFileName(bson.getString(BatchFileInfoDownload.FILENAME))
                .withFileId(bson.getString(BatchFileInfoDownload.FILEID))
                .withType(bson.getInt(BatchFileInfoDownload.TYPE))
                .withDownloadLocation(bson.getInt(BatchFileInfoDownload.DOWNLOADLOCATION))
                .withStatus(bson.getInt(BatchFileInfoDownload.STAUS))
                .withExpScmId(bson.getString(BatchFileInfoDownload.EXPSCMID))
                .withExpScmDirPath(bson.getString(BatchFileInfoDownload.EXPSCMDIRPATH))
                .withExpScmFileName(bson.getString(BatchFileInfoDownload.EXPSCMFILENAME))
                .withLocalFilePath(bson.getString(BatchFileInfoDownload.LOCALFILEPATH))
                .withReason(bson.getString(BatchFileInfoDownload.READSON));
        if (bson.getString(BatchFileInfoDownload.ENDTIME) != null) {
            batchFileInfoDownload.withEndTime(format.parse(bson.getString(BatchFileInfoDownload.ENDTIME)));
        } else {
            batchFileInfoDownload.withEndTime(null);
        }
        return batchFileInfoDownload;
    }

    public String toString() {
        return toBson().toString();
    }
}
