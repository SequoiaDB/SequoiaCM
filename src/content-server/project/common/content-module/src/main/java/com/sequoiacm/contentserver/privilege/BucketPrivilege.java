package com.sequoiacm.contentserver.privilege;

import java.util.TreeMap;

public class BucketPrivilege {
    private TreeMap<String, Integer> bucketPrivMap = new TreeMap<>();

    public boolean addResourcePriv(String bucket, int priv) {
        Integer v = bucketPrivMap.get(bucket);
        if (null != v) {
            bucketPrivMap.put(bucket, priv | v);
        }
        else {
            bucketPrivMap.put(bucket, priv);
        }

        return true;
    }

    public boolean checkResourcePriv(String bucket, int op) {
        int v = getResourcePriv(bucket);
        return (op & v) == op;
    }

    public int getResourcePriv(String bucket) {
        Integer p = bucketPrivMap.get(bucket);
        if (p == null) {
            return 0;
        }
        return p;
    }
}
