package com.sequoiacm.contentserver.privilege;

import java.util.TreeMap;

import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;

public class DirPriv {
    private TreeMap<String, Integer> dirPrivMap = new TreeMap<>();

    public boolean addResourcePriv(String directory, int priv) {
        Integer v = dirPrivMap.get(directory);
        if (null != v) {
            dirPrivMap.put(directory, priv | v);
        }
        else {
            dirPrivMap.put(directory, priv);
        }

        return true;
    }

    public boolean checkResourcePriv(String directory, int op) {
        int v = getResourcePriv(directory);
        return (op & v) == op;
    }

    public int getResourcePriv(String directory) {
        while (true) {
            Integer v = dirPrivMap.get(directory);
            if (null != v) {
                return v;
            }

            int index = directory.lastIndexOf("/");
            if (index > 0) {
                directory = directory.substring(0, index);
                continue;
            }

            if (index == 0 && directory.length() > 1) {
                directory = "/";
                continue;
            }

            break;
        }

        return 0;
    }

    private static String arrayToString(String[] array) {
        if (null == array) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",").append(array[i]);
            }
            else {
                sb.append(array[i]);
            }
        }

        return sb.toString();
    }

}
