package com.sequoiacm.daemon.operator;

import com.sequoiacm.daemon.element.ScmNodeInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.File;
import java.util.List;

public interface NodeOperator {
    public boolean isNodeRunning(ScmNodeInfo node) throws ScmToolsException;

    public void startNode(ScmNodeInfo node) throws ScmToolsException;

    public List<ScmNodeInfo> getNodeInfos(File serviceDir) throws ScmToolsException;

    public int getNodePort(ScmNodeInfo node) throws ScmToolsException;
}
