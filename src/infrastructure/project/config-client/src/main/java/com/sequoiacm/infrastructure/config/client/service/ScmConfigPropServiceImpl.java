package com.sequoiacm.infrastructure.config.client.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.stereotype.Service;

import com.sequoiacm.infrastructure.config.client.core.ScmConfPropVerifiersMgr;
import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDao;
import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDaoFactory;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@Service
public class ScmConfigPropServiceImpl implements ScmConfigPropService {

    @Autowired
    private ScmConfPropVerifiersMgr verfierMgr;

    @Autowired
    private ContextRefresher contextRefresher;

    @Autowired
    private ScmConfigPropsDaoFactory daoFactory;

    @Override
    public void updateConfigProps(Map<String, String> updateProps, List<String> deleteProps,
            boolean acceptUnknownProps) throws ScmConfigException {
        verfierMgr.checkProps(updateProps, deleteProps, acceptUnknownProps);

        ScmConfigPropsDao dao = daoFactory.createConfigPropsDao();
        try {
            boolean isDifferentFromOld = dao.modifyPropsFile(updateProps, deleteProps);
            if (isDifferentFromOld) {
                contextRefresher.refresh();
            }
        }
        catch (Exception e) {
            dao.rollback();
            throw e;
        }
    }

}
