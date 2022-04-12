package com.sequoiacm.contentserver.privilege;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;

public class DirResourceBuilder implements IResourceBuilder {
    private static final Logger logger = LoggerFactory.getLogger(DirResourceBuilder.class);

    @Override
    public IResourcePrivChecker createResourceChecker() {
        return new DirPrivChecker();
    }

    @Override
    public String toStringFormat(IResource resource) {
        return resource.toStringFormat();
    }

    @Override
    public IResource fromStringFormat(String resource) {
        return fromStringFormat(resource, false);
    }

    @Override
    public String getResourceType() {
        return DirResource.RESOURCE_TYPE;
    }

    @Override
    public IResource fromStringFormat(String resource, boolean isNeedFormat) {
        String[] parsedDir = resource.split(":");
        if (null != parsedDir && parsedDir.length >= 2) {
            if (isNeedFormat) {
                try {
                    parsedDir[1] = ScmSystemUtils.formatDirPath(parsedDir[1]);
                }
                catch (ScmInvalidArgumentException e) {
                    logger.error("format dir resource error, msg=" + e.getMessage());
                    return null;
                }
            }
            return new DirResource(parsedDir[0], parsedDir[1]);
        }
        return null;
    }
}
