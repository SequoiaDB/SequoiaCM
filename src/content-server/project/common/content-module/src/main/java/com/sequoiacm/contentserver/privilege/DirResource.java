package com.sequoiacm.contentserver.privilege;

import com.sequoiacm.infrastructrue.security.privilege.IResource;

public class DirResource implements IResource {

    public static final String RESOURCE_TYPE = "directory";
    private String wsName;
    private String dirName;

    public DirResource(String wsName, String dirName) {
        this.wsName = wsName;
        this.dirName = formatDir(dirName);
    }

    private static String formatDir(String directory) {
        if (null == directory || directory.isEmpty()) {
            return "/";
        }

        while (directory.length() > 1 && directory.endsWith("/")) {
            directory = directory.substring(0, directory.length() - 1);
        }

        return directory;
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getWorkspace() {
        return wsName;
    }

    public String getDirectory() {
        return dirName;
    }

    @Override
    public String toStringFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append(getWorkspace()).append(":").append(getDirectory());
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RESOURCE_TYPE).append(":").append(wsName).append(":").append(dirName);
        return sb.toString();
    }

}
