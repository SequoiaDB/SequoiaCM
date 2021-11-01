package com.sequoiacm.daemon.element;

import com.sequoiacm.daemon.common.DaemonDefine;
import com.sequoiacm.daemon.exception.ScmExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ScmNodeMatcher {
    private int flag;
    private String type;
    private int port;

    public ScmNodeMatcher() {
        this.flag = DaemonDefine.MATCH_ALL_FLAG;
    }

    public ScmNodeMatcher(String type) {
        this.flag = DaemonDefine.MATCH_TYPE_FLAG;
        this.type = type;
    }

    public ScmNodeMatcher(int port) {
        this.flag = DaemonDefine.MATCH_PORT_FLAG;
        this.port = port;
    }

    public boolean isMatch(ScmNodeInfo nodeInfo) throws ScmToolsException {
        switch (flag) {
            case DaemonDefine.MATCH_PORT_FLAG:
                return port == nodeInfo.getPort();
            case DaemonDefine.MATCH_TYPE_FLAG:
                return type.toUpperCase().equals(nodeInfo.getServerType().getType());
            case DaemonDefine.MATCH_ALL_FLAG:
                return true;
            default:
                throw new ScmToolsException(
                        "Failed to match condition, caused by no such matching condition, matcher:"
                                + toString(),
                        ScmExitCode.INVALID_ARG);
        }
    }

    public String toString() {
        StringBuilder matcherStr = new StringBuilder();
        matcherStr.append("Matcher[");
        switch (flag) {
            case DaemonDefine.MATCH_PORT_FLAG:
                matcherStr.append("port=").append(port);
                break;
            case DaemonDefine.MATCH_TYPE_FLAG:
                matcherStr.append("type=").append(type);
                break;
            case DaemonDefine.MATCH_ALL_FLAG:
                matcherStr.append("type=all");
                break;
            default:
                matcherStr.append("unknown matcher condition, flag=").append(flag);
        }
        matcherStr.append("]");
        return matcherStr.toString();
    }
}
