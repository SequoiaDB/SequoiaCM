package com.sequoiacm.cloud.gateway.statistics.config;

import java.util.List;
import java.util.regex.Pattern;

class ScmStatisticsFileConfig {
    private List<String> workspaces;
    private String workspaceRegex;

    public List<String> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(List<String> workspaces) {
        if (workspaces != null && workspaces.size() <= 0) {
            workspaces = null;
        }
        this.workspaces = workspaces;
    }

    public String getWorkspaceRegex() {
        return workspaceRegex;
    }

    public void setWorkspaceRegex(String workspaceRegex) {
        if (workspaceRegex != null && workspaceRegex.trim().length() <= 0) {
            workspaceRegex = null;
        }
        this.workspaceRegex = workspaceRegex;

    }

    public boolean isContainWorkspace(String ws) {
        if (workspaces == null && workspaceRegex == null) {
            //没有限制工作区，返回true，表示所有工作区都需要统计
            return true;
        }
        if (workspaces != null && workspaces.contains(ws)) {
            return true;
        }
        if (workspaceRegex != null && Pattern.matches(workspaceRegex, ws)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ScmStatisticsFileConfig{" + "workspaces=" + workspaces + ", workspaceRegex='"
                + workspaceRegex + '\'' + '}';
    }
}
