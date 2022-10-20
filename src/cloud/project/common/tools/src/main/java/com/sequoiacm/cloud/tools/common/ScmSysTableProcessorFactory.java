package com.sequoiacm.cloud.tools.common;

import java.util.Properties;


import com.sequoiacm.infrastructure.tool.element.ScmNodeType;
import com.sequoiacm.infrastructure.tool.element.ScmNodeTypeEnum;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public final class ScmSysTableProcessorFactory {
    private ScmSysTableProcessorFactory() {
    }

    public static ScmSysTableCreator getSysTableCreator(ScmNodeType nodeType, Properties properties)
            throws ScmToolsException {
        String type = nodeType.getType();
        String name = nodeType.getName();
        if (type.equals(ScmNodeTypeEnum.AUTHSERVER.getTypeNum())
                || name.equals(ScmNodeTypeEnum.AUTHSERVER.getName())) {
            return new AuthServerTableCreator(properties);
        }
        else if (type.equals(ScmNodeTypeEnum.ADMINSERVER.getTypeNum())
                || name.equals(ScmNodeTypeEnum.ADMINSERVER.getName())) {
            return new AdminServerTableCreator(properties);
        }
        else {
            return null;
        }
    }

    public static ScmSysTableCleaner getSysTableCleaner(ScmNodeType nodeType, String sdbUrl,
            String sdbUser, String sdbPwdFile) throws ScmToolsException {
        String type = nodeType.getType();
        String name = nodeType.getName();
        if (type.equals(ScmNodeTypeEnum.AUTHSERVER.getTypeNum())
                || name.equals(ScmNodeTypeEnum.AUTHSERVER.getName())) {
            return new AuthServerTableCleaner(sdbUrl, sdbUser, sdbPwdFile);
        }
        else if (type.equals(ScmNodeTypeEnum.ADMINSERVER.getTypeNum())
                || name.equals(ScmNodeTypeEnum.ADMINSERVER.getName())) {
            return new AdminServerTableCleaner(sdbUrl, sdbUser, sdbPwdFile);
        }
        else if (type.equals(ScmNodeTypeEnum.SERVICECENTER.getTypeNum())
                || name.equals(ScmNodeTypeEnum.SERVICECENTER.getName())) {
            return new ServiceCenterTableCleaner(sdbUrl, sdbUser, sdbPwdFile);
        }
        else {
            return null;
        }
    }
}
