package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.core.*;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.om.omserver.module.OmDirectoryBasic;
import com.sequoiacm.om.omserver.module.OmDirectoryInfoWithSubDir;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmDirDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class ScmDirDaoImpl implements ScmDirDao {
    private ScmOmSession session;

    public ScmDirDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public long countDir(String wsName, BSONObject condition) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, connection);
            return ScmFactory.Directory.countInstance(ws, condition);
        }
        catch (ScmException e) {
            if (e.getError() == ScmError.DIR_FEATURE_DISABLE) {
                return 0;
            }
            throw new ScmInternalException(e.getError(), "failed to count dir, " + e.getMessage(),
                    e);
        }
    }

    @Override
    public List<OmDirectoryBasic> getDirList(String wsName, BSONObject condition,
            BSONObject orderBy, int skip, int limit) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        ScmCursor<ScmDirectory> cursor = null;
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, connection);
            List<OmDirectoryBasic> dirList = new ArrayList<>();
            cursor = ScmFactory.Directory.listInstance(ws, condition, orderBy, skip, limit);
            while (cursor.hasNext()) {
                ScmDirectory directory = cursor.getNext();
                dirList.add(transformToDirectoryBasic(directory));
            }
            return dirList;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get directory list, " + e.getMessage(), e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public List<OmDirectoryInfoWithSubDir> listSubDir(String wsName, String dirId,
            BSONObject orderBy, int skip, int limit) throws ScmInternalException {
        BSONObject condition = new BasicBSONObject();
        condition.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, dirId);
        List<OmDirectoryInfoWithSubDir> res = new ArrayList<>();
        List<OmDirectoryBasic> dirList = getDirList(wsName, condition, orderBy, skip, limit);
        for (OmDirectoryBasic directoryBasicInfo : dirList) {
            OmDirectoryInfoWithSubDir directoryInfoWithSubDir = new OmDirectoryInfoWithSubDir();
            BeanUtils.copyProperties(directoryBasicInfo, directoryInfoWithSubDir);
            res.add(directoryInfoWithSubDir);
        }
        return res;
    }

    private OmDirectoryBasic transformToDirectoryBasic(ScmDirectory directory) throws ScmException {
        OmDirectoryBasic directoryBasicInfo = new OmDirectoryBasic();
        directoryBasicInfo.setId(directory.getId());
        directoryBasicInfo.setName(directory.getName());
        directoryBasicInfo.setPath(directory.getPath());
        directoryBasicInfo.setUser(directory.getUser());
        directoryBasicInfo.setCreateDate(directory.getCreateTime());
        directoryBasicInfo.setUpdateUser(directory.getUpdateUser());
        directoryBasicInfo.setUpdateDate(directory.getUpdateTime());
        return directoryBasicInfo;
    }
}
