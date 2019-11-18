package com.sequoiacm.testcommon.scmutils;

import com.sequoiacm.testcommon.TestScmBase;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fanyu on 2019/9/10.
 */
public class BatchFileInfoUpload extends TestScmBase {
    public final static String ID = "ID";
    public final static String WORKSPACE = "WORKSPACE";
    public final static String BATCHNO = "BATCHNO";
    public final static String DIRECTORY = "DIRECTORY";
    public final static String SERVER = "SERVER";
    public final static String FILEPATH = "FILEPATH";
    public final static String FILENAME = "FILENAME";
    public final static String HASHTIME = "HASHTIME";
    public final static String STATUS = "STATUS";
    public final static String REASON = "REASON";
    public final static String UPLOADTIME = "UPLOADTIME";
    public final static String PATH = "PATH";
    public final static String FILEID = "FILEID";
    public final static String LASTUPDATETIME = "LASTUPDATETIME";
    public final static String SIZE = "SIZE";
    public final static String USER = "USER";
    public final static String UPDATEUSER = "UPDATEUSER";

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String id;
    private String workspace;
    private String batchNO;
    private String directory;
    private String server;
    private String filePath;
    private String fileName;
    private Date hashTime;
    private int status;
    private String reason;
    private Date uploadTime;
    private String path;
    private String fileId;
    private Date lastUpdateTime;
    private long size;
    private String user = TestScmBase.scmUserName;
    private String updateUser = TestScmBase.scmUserName;
    private String md5;

    public String getId() {
        return id;
    }

    public BatchFileInfoUpload withId(String id) {
        this.id = id;
        return this;
    }

    public String getWorkspace() {
        return workspace;
    }

    public BatchFileInfoUpload withWorkspace(String workspace) {
        this.workspace = workspace;
        return this;
    }

    public String getBatchNO() {
        return batchNO;
    }

    public BatchFileInfoUpload withBatchNO(String batchNO) {
        this.batchNO = batchNO;
        return this;
    }

    public String getDirectory() {
        return directory;
    }

    public BatchFileInfoUpload withDirectory(String directory) {
        this.directory = directory;
        return this;
    }

    public String getServer() {
        return server;
    }

    public BatchFileInfoUpload withServer(String server) {
        this.server = server;
        return this;
    }

    public String getFilePath() {
        return filePath;
    }

    public BatchFileInfoUpload withFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public BatchFileInfoUpload withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public Date getHashTime() {
        return hashTime;
    }

    public BatchFileInfoUpload withHashTime(Date hashTime) {
        this.hashTime = hashTime;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public BatchFileInfoUpload withStatus(int status) {
        this.status = status;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public BatchFileInfoUpload withReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public BatchFileInfoUpload withUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
        return this;
    }

    public String getPath() {
        return path;
    }

    public BatchFileInfoUpload withPath(String path) {
        this.path = path;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public BatchFileInfoUpload withFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public BatchFileInfoUpload withLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
        return this;
    }

    public long getSize() {
        return size;
    }

    public BatchFileInfoUpload withSize(long size) {
        this.size = size;
        return this;
    }

    public static String getID() {
        return ID;
    }

    public String getUser() {
        return user;
    }

    public BatchFileInfoUpload withUser(String user) {
        this.user = user;
        return this;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public BatchFileInfoUpload withUpdateUser(String updateUser) {
        this.updateUser = updateUser;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public BatchFileInfoUpload withMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public BSONObject toBson() {
        BSONObject bson = new BasicBSONObject();
        bson.put(BatchFileInfoUpload.ID, this.getId());
        bson.put(BatchFileInfoUpload.WORKSPACE, this.getWorkspace());
        bson.put(BatchFileInfoUpload.BATCHNO, this.getBatchNO());
        bson.put(BatchFileInfoUpload.DIRECTORY, this.getDirectory());
        bson.put(BatchFileInfoUpload.SERVER, this.getServer());
        bson.put(BatchFileInfoUpload.PATH, this.getPath());
        bson.put(BatchFileInfoUpload.FILEPATH, this.getFilePath());
        bson.put(BatchFileInfoUpload.FILENAME, this.getFileName());
        if (this.getHashTime() != null) {
            bson.put(BatchFileInfoUpload.HASHTIME, format.format(this.getHashTime()));
        } else {
            bson.put(BatchFileInfoUpload.HASHTIME, this.getHashTime());
        }
        bson.put(BatchFileInfoUpload.FILEID, this.getFileId());
        bson.put(BatchFileInfoUpload.UPLOADTIME, this.getUploadTime());
        if (this.getHashTime() != null) {
            bson.put(BatchFileInfoUpload.LASTUPDATETIME, format.format(this.getHashTime()));
        } else {
            bson.put(BatchFileInfoUpload.LASTUPDATETIME, this.getLastUpdateTime());
        }
        bson.put(BatchFileInfoUpload.STATUS, this.getStatus());
        bson.put(BatchFileInfoUpload.REASON, this.getReason());
        bson.put(BatchFileInfoUpload.SIZE, this.getSize());
        bson.put(BatchFileInfoUpload.USER, this.getUser());
        bson.put(BatchFileInfoUpload.UPDATEUSER, this.getUpdateUser());
        return bson;
    }

    public static BatchFileInfoUpload toBatchFileInfo(BasicBSONObject bson) throws ParseException {
        BatchFileInfoUpload batchFileInfoUpload = new BatchFileInfoUpload()
                .withBatchNO(bson.getString(BatchInfoUpload.BATCHNO))
                .withDirectory(bson.getString(BatchFileInfoUpload.DIRECTORY))
                .withFileId(bson.getString(BatchFileInfoUpload.FILEID))
                .withFileName(bson.getString(BatchFileInfoUpload.FILENAME))
                .withFilePath(bson.getString(BatchFileInfoUpload.FILEPATH))
                .withId(bson.getString(BatchFileInfoUpload.ID))
                .withHashTime(format.parse(bson.getString(BatchFileInfoUpload.HASHTIME)))
                .withLastUpdateTime(format.parse(bson.getString(BatchFileInfoUpload.LASTUPDATETIME)))
                .withPath(bson.getString(BatchFileInfoUpload.PATH))
                .withReason((String) bson.get(BatchFileInfoUpload.REASON))
                .withServer(bson.getString(BatchFileInfoUpload.SERVER))
                .withStatus(bson.getInt(BatchFileInfoUpload.STATUS))
                .withWorkspace(bson.getString(BatchFileInfoUpload.WORKSPACE))
                .withSize(bson.getLong(BatchFileInfoUpload.SIZE))
                .withUser(bson.getString(BatchFileInfoUpload.USER))
                .withUpdateUser(bson.getString(BatchFileInfoUpload.UPDATEUSER));
        if (bson.getString(BatchFileInfoUpload.UPLOADTIME) != null) {
            batchFileInfoUpload.withUploadTime(format.parse(bson.getString(BatchFileInfoUpload.UPLOADTIME)));
        } else {
            batchFileInfoUpload.withUploadTime(null);
        }
        return batchFileInfoUpload;
    }

    public String toString() {
        return toBson().toString();
    }
}
