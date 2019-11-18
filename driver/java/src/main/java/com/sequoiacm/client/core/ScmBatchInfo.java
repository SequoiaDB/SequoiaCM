package com.sequoiacm.client.core;

import java.util.Date;

import org.bson.BSONObject;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;

/**
 * The brief and partial information of Batch.
 */
public class ScmBatchInfo {

    private ScmId id;
    private String name;
    private Date createTime;
    private int filesCount;

    /**
     * Create a instance of ScmBatchInfo,this instance's properties is null.
     *
     */
    ScmBatchInfo() {
    }

    /**
     * Create a instance of ScmBatchInfo.
     *
     * @param bson
     *            a bson containing basic information about scm batch.
     * @throws ScmException
     *             If error happens
     */
    ScmBatchInfo(BSONObject bson) throws ScmException {
        id = new ScmId((String) bson.get(FieldName.Batch.FIELD_ID), false);
        name = (String) bson.get(FieldName.Batch.FIELD_NAME);
        long time = (Long) bson.get(FieldName.Batch.FIELD_INNER_CREATE_TIME);
        createTime = new Date(time);
        filesCount = (Integer) bson.get(CommonDefine.RestArg.BATCH_FILES_COUNT);
    }

    /**
     * Returns the id of the batch.
     * @return batch id.
     */
    public ScmId getId() {
        return id;
    }

    void setId(ScmId id) {
        this.id = id;
    }

    /**
     * Return the name of the batch.
     * @return batch name.
     */
    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    /**
     * Return the files count of the batch.
     * @return files count.
     */
    public int getFilesCount() {
        return filesCount;
    }

    void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    /**
     * Returns the create time of the batch.
     * @return create time.
     */
    public Date getCreateTime() {
        return createTime;
    }

    void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append("id : \"" + id.get() + "\" , ");
        buf.append("name : \"" + name + "\" , ");
        buf.append("createTime : \"" + createTime + "\" , ");
        buf.append("filesCount : " + filesCount);
        buf.append("}");
        return buf.toString();
    }

}
