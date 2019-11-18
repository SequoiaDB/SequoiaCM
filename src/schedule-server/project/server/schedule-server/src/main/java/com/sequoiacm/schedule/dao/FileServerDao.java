package com.sequoiacm.schedule.dao;

import org.bson.BSONObject;

import com.sequoiacm.schedule.entity.FileServerEntity;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;

public interface FileServerDao {
    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception;

    public FileServerEntity queryOne(String nodeName) throws Exception;
}