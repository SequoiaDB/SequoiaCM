package com.sequoiacm.metasource;

import java.util.List;

public class IndexDef {
        private List<String> unionKeys;
        private boolean isUnique;

        public void setUnique(boolean unique) {
                isUnique = unique;
        }

        public boolean isUnique() {
                return isUnique;
        }

        public void setUnionKeys(List<String> unionKeys) {
                this.unionKeys = unionKeys;
        }

        public List<String> getUnionKeys() {
                return unionKeys;
        }
}
