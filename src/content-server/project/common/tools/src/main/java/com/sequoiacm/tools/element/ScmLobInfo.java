package com.sequoiacm.tools.element;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import org.bson.BSONObject;
import org.bson.types.BSONTimestamp;
import org.bson.types.ObjectId;

import com.sequoiacm.tools.common.ScmFiledDefine;
import com.sequoiacm.tools.exception.ScmExitCode;

import java.util.Date;

public class ScmLobInfo {

    private ObjectId oid;
    private long size;
    private boolean available;
    private Date createTime;
    private Date modificationTime;

    public ScmLobInfo(BSONObject obj) throws ScmToolsException {
        Long size = (Long) obj.get(ScmFiledDefine.LOB_SIZE);
        checkNotNull(size, obj, ScmFiledDefine.LOB_SIZE);
        this.size = size;
        oid = (ObjectId) obj.get(ScmFiledDefine.LOB_OID);
        checkNotNull(oid, obj, ScmFiledDefine.LOB_OID);
        Boolean available = (Boolean) obj.get(ScmFiledDefine.LOB_AVAILABLE);
        checkNotNull(available, obj, ScmFiledDefine.LOB_AVAILABLE);
        this.available = available;
        createTime = ((BSONTimestamp) obj.get(ScmFiledDefine.LOB_CREATE_TIME)).toDate();
        checkNotNull(createTime, obj, ScmFiledDefine.LOB_CREATE_TIME);
        BSONTimestamp modTime = (BSONTimestamp) obj.get(ScmFiledDefine.LOB_MODIFICATION_TIME);
        if (modTime == null) {
            modificationTime = createTime;
        } else {
            modificationTime = modTime.toDate();
        }
    }

    public ObjectId getOid() {
        return oid;
    }

    public void setOid(ObjectId oid) {
        this.oid = oid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;

    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(Date modificationTime) {
        this.modificationTime = modificationTime;
    }

    private void checkNotNull(Object obj, BSONObject bsonobj, String field)
            throws ScmToolsException {
        if (obj == null) {
            throw new ScmToolsException(
                    "record missing field( " + field + "):" + bsonobj.toString(),
                    ScmExitCode.SCM_META_RECORD_ERROR);
        }
    }

}
