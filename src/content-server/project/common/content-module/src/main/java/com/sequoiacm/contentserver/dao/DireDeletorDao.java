package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;

public class DireDeletorDao {
    private ScmWorkspaceInfo ws;
    private ScmMetaService metaService;
    private String id;
    private String path;
    private DirOperator dirOperator;

    public DireDeletorDao(String wsName, String dirId, String path) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        this.id = dirId;
        this.path = path;
        this.ws = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        this.metaService = contentModule.getMetaService();
        this.dirOperator = DirOperator.getInstance();
    }

    public void delete() throws ScmServerException {
        if (id == null) {
            if (path == null) {
                throw new ScmInvalidArgumentException("missing required field:id=null,path=null");
            }
            BSONObject dir = dirOperator.getDirByPath(ws, path);
            if (dir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "directory not exists:ws=" + ws.getName() + ",path=" + path);
            }
            id = (String) dir.get(FieldName.FIELD_CLDIR_ID);
        }
        else {
            BSONObject dir = metaService.getDirInfo(ws.getName(), id);
            if (dir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "directory not exists:ws=" + ws.getName() + ",id=" + id);
            }
        }

        if (id.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            throw new ScmOperationUnsupportedException("can not delete root directory:id=" + id);
        }

        dirOperator.delete(ws, id, path);
    }
}
