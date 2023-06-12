package com.sequoiacm.config.framework.common;

import java.util.List;

import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;

public interface DefaultVersionDao {
    public Integer getVersion(String businessType, String businessName) throws ScmConfigException;

    public List<Version> getVerions(VersionFilter filter) throws MetasourceException;

    public void createVersion(String businessType, String businessName, Transaction transaction)
            throws ScmConfigException;

    public void createVersion(String businessType, String businessName) throws ScmConfigException;

    public void deleteVersion(String businessType, String businessName, Transaction transaction)
            throws MetasourceException;

    public Integer updateVersion(String businessType, String businessName, int newVersion,
            Transaction transaction) throws MetasourceException;

    public Integer increaseVersion(String businessType, String businessName,
            Transaction transaction) throws MetasourceException;
}
