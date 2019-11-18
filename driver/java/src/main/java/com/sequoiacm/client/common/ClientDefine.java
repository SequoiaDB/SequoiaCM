package com.sequoiacm.client.common;

public class ClientDefine {

    public static class File {
        public final static int TRANSMISSION_LEN = 1024 * 1024;
        public final static int MAX_READ_BUFFER_LEN = 64 * 1024;
    }

    public class QueryOperators {
        public static final String OR = "$or";
        public static final String AND = "$and";

        public static final String GT = "$gt";
        public static final String GTE = "$gte";
        public static final String LT = "$lt";
        public static final String LTE = "$lte";

        public static final String NE = "$ne";
        public static final String IN = "$in";
        public static final String NIN = "$nin";
        public static final String EXISTS = "$exists";

        public static final String NOR = "$nor";
        public static final String NOT = "$not";

        public static final String ELEM_MATCH = "$elemMatch";

        private QueryOperators() {
        }
    }
}
