package com.sequoiadb.infrastructure.map;

public class CommonDefine {
    public static class FieldName {
        public static final String _ID = "_id";
        public static final String KEY = "key";
        public static final String VALUE = "value";

        public static final String MAP_NAME = "name";
        public static final String MAP_CL_NAME = "cl_name";
        public static final String MAP_KEY_TYPE = "key_type";
        public static final String MAP_VALUE_TYPE = "value_type";
        public static final String MAP_GROUP_NAME = "group_name";
    }

    public static class RestArg {
        public static final String MAP_GROUP_NAME = "group_name";
        public static final String MAP_NAME = "map_name";
        public static final String MAP_KEY = "key";
        public static final String MAP_VALUE = "value";
        public static final String MAP_ENTRY = "entry";
        public static final String MAP_ENTRY_LIST = "entry_list";
        public static final String MAP_FILTER = "filter";
        public static final String MAP_ORDERBY = "orderby";
        public static final String MAP_SKIP = "skip";
        public static final String MAP_LIMIT = "limit";
        public static final String MAP_VALUE_TYPE = "value_type";
        public static final String MAP_KEY_TYPE = "key_type";

    }

    public static class Mather {
        public static final String OR = "$or";
        public static final String AND = "$and";
        public static final String NOT = "$not";
        public static final String NIN = "$nin";
        public static final String ET = "$et";
    }

    public static class Updater {
        public static final String SET = "$set";
    }
}
