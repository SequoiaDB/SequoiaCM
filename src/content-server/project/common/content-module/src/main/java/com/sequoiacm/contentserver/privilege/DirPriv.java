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

    public static void main(String[] args) {
        String d = new String("/");
        String[] arrays = d.split("/");
        System.out.println(arrayToString(arrays));

        arrays = "".split("/");
        System.out.println(arrayToString(arrays));

        arrays = "bb".split("/");
        System.out.println(arrayToString(arrays));

        DirPriv dp = new DirPriv();
        dp.addResourcePriv("/", ScmPrivilegeDefine.CREATE.getFlag());

        System.out.println("/:" + ScmPrivilegeDefine.CREATE);
        boolean flag = dp.checkResourcePriv("/a_123", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[true]");

        flag = dp.checkResourcePriv("/a_123/a", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[true]");

        flag = dp.checkResourcePriv("/a/a", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[true]");

        flag = dp.checkResourcePriv("/a", ScmPrivilegeDefine.DELETE.getFlag());
        System.out.println("flag=" + flag + ",expect[false]");

        flag = dp.checkResourcePriv("/aa", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[true]");

        System.out.println("");
        System.out.println("new test");
        dp = new DirPriv();
        dp.addResourcePriv("/a", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("/a:" + ScmPrivilegeDefine.CREATE);
        flag = dp.checkResourcePriv("/a_123", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[false]");

        flag = dp.checkResourcePriv("/a_123/a", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[false]");

        flag = dp.checkResourcePriv("/a/a", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[true]");

        flag = dp.checkResourcePriv("/a", ScmPrivilegeDefine.DELETE.getFlag());
        System.out.println("flag=" + flag + ",expect[false]");

        flag = dp.checkResourcePriv("/aa", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[false]");

        System.out.println("");
        System.out.println("new test");
        dp = new DirPriv();
        dp.addResourcePriv("/a", ScmPrivilegeDefine.CREATE.getFlag());
        dp.addResourcePriv("/a/b1", ScmPrivilegeDefine.READ.getFlag());
        dp.addResourcePriv("/a/b2", ScmPrivilegeDefine.UPDATE.getFlag());
        dp.addResourcePriv("/a/b2", ScmPrivilegeDefine.DELETE.getFlag());

        flag = dp.checkResourcePriv("/a/b2", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[false]");

        flag = dp.checkResourcePriv("/a/b3", ScmPrivilegeDefine.CREATE.getFlag());
        System.out.println("flag=" + flag + ",expect[true]");

        flag = dp.checkResourcePriv("/a/b2/c1", ScmPrivilegeDefine.UPDATE.getFlag());
        System.out.println("flag=" + flag + ",expect[true]");
    }
}
