package com.sequoiacm.om.omserver.service.impl;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.dao.ScmDirDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.factory.ScmDirDaoFactory;
import com.sequoiacm.om.omserver.module.OmDirectoryInfoWithSubDir;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.service.ScmDirService;
import com.sequoiacm.om.omserver.service.ScmWorkspaceService;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScmDirServiceImpl implements ScmDirService {

    @Autowired
    private ScmWorkspaceService wsService;

    @Autowired
    private ScmSiteChooser siteChooser;

    @Autowired
    private ScmDirDaoFactory scmDirDaoFactory;

    @Override
    public List<OmDirectoryInfoWithSubDir> listSubDir(ScmOmSession session, String wsName,
            String dirId, BSONObject orderBy, int skip, int limit)
            throws ScmOmServerException, ScmInternalException {
        OmWorkspaceDetail wsDetail = wsService.getWorkspaceDetail(session, wsName);
        String preferSite = siteChooser.chooseSiteFromWorkspace(wsDetail);
        ScmDirDao scmDirDao = scmDirDaoFactory.createScmDirDao(session);
        try {
            session.resetServiceEndpoint(preferSite);
            List<OmDirectoryInfoWithSubDir> subDirList = scmDirDao.listSubDir(wsName, dirId,
                    orderBy, skip, limit);
            // set whether to own subdirectories.
            for (OmDirectoryInfoWithSubDir directoryInfoWithSubDir : subDirList) {
                BSONObject countCondition = new BasicBSONObject();
                countCondition.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID,
                        directoryInfoWithSubDir.getId());
                long subDirCount = scmDirDao.countDir(wsName, countCondition);
                directoryInfoWithSubDir.setSubDirCount(subDirCount);
            }
            return subDirList;
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }
}
