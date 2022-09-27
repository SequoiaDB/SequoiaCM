package com.sequoiacm.om.omserver.dao.impl;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmConfigPropsDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmConfPropsParam;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public class ScmConfigPropsDaoImpl implements ScmConfigPropsDao {

    private ScmOmSession session;

    public ScmConfigPropsDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public ScmUpdateConfResultSet updateProperties(ScmOmSession session, OmConfPropsParam config)
            throws ScmInternalException {
        ScmSession con = session.getConnection();
        try {
            String targetType = config.getTargetType();
            ScmConfigProperties.Builder builder = ScmConfigProperties.builder();
            if ("all".equals(targetType)) {
                builder.allInstance();
            }
            else if ("service".equals(targetType)) {
                builder.services(config.getTargets());
            }
            else if ("instance".equals(targetType)) {
                builder.instances(config.getTargets());
            }
            else {
                throw new IllegalArgumentException("unknown target type:" + targetType);
            }
            ScmConfigProperties configProperties = builder
                    .updateProperties(config.getUpdateProperties()).build();
            return ScmSystem.Configuration.setConfigProperties(con, configProperties);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to update properties, " + e.getMessage(), e);
        }
    }
}
