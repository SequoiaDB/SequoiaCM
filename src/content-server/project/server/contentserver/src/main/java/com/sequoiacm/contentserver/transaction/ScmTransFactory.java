package com.sequoiacm.contentserver.transaction;

import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.common.ServiceDefine;


public class ScmTransFactory {

    public static ScmTransBase getTransaction(int siteID, String workspaceName, String transID)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentServer.getWorkspaceInfo(workspaceName);
        BSONObject transInfo = contentServer.getTransLog(workspaceName, transID);
        if (null == transInfo) {
            return null;
        }

        int type = (int)transInfo.get(FieldName.FIELD_CLTRANS_TYPE);

        ScmTransBase trans = null;
        if (ServiceDefine.TransType.DELETING_SINGLE_FILE == type) {
            trans = new TransSingleFileDeletor(siteID, wsInfo, transInfo);
        }
        else {
            assert false : "unknown type:" + type;
        }

        return trans;
    }
}
