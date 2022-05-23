package com.sequoiacm.client.common;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.CommonDefine;

/**
 * Provides enum of scm type.
 *
 * @since 2.1
 */
public class ScmType {

    private ScmType() {

    }

    /**
     * Provides a set of constants to specify the version in a search query.
     */
    public enum ScopeType {
        /**
         * A constant representing latest version.
         *
         * @since 2.1
         */
        SCOPE_CURRENT(CommonDefine.Scope.SCOPE_CURRENT),

        /**
         * A constant representing history version.
         *
         * @since 2.1
         */
        SCOPE_HISTORY(CommonDefine.Scope.SCOPE_HISTORY),

        /**
         * A constant representing all version(current version + history
         * version).
         *
         * @since 2.1
         */
        SCOPE_ALL(CommonDefine.Scope.SCOPE_ALL);

        private int scope;

        private ScopeType(int scope) {
            this.scope = scope;
        }

        /**
         * Returns the internal integer value associated with a specific
         * instance of this class.
         *
         * @return integer value.
         * @since 2.1
         */
        public int getScope() {
            return scope;
        }

        public static ScopeType getScopeType(int scope) throws ScmException {
            for (ScopeType value : ScopeType.values()) {
                if (value.getScope() == scope) {
                    return value;
                }
            }
            throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
        }
    }

    /**
     * Provides a set of constants that identify a property's type which is
     * provides in system internal.
     *
     * @since 2.1
     */
    public enum PropertyType {
        VIDEO(0, "video");

        private int num;
        private String type;

        private PropertyType(int num, String type) {
            this.num = num;
            this.type = type;
        }

        /**
         * Returns the internal integer value associated with a specific num of
         * property's type.
         *
         * @return type's num
         * @since 2.1
         */
        public int getNum() {
            return num;
        }

        /**
         * Returns the internal string value associated with a specific name of
         * property's type.
         *
         * @return type's name
         * @since 2.1
         */
        public String getType() {
            return type;
        }

        /**
         * Returns an instance of the subclassable PropertyType class associated
         * with the specified num.
         *
         * @param num
         *            The constant representing propertyType.
         * @return A reference to an object of the PropertyType.
         * @since 2.1
         */
        public static PropertyType get(int num) {
            for (PropertyType prop : PropertyType.values()) {
                if (prop.num == num) {
                    return prop;
                }
            }
            return null;
        }
    }

    /**
     * Provides a set of constants that to specify the scope in reload business
     * conf.
     *
     * @since 2.1
     */
    public enum ServerScope {
        /**
         * A constants representing node scope.
         *
         * @since 2.1
         */
        NODE(CommonDefine.NodeScope.SCM_NODESCOPE_NODE),

        /**
         * A constants representing site scope.
         *
         * @since 2.1
         */
        SITE(CommonDefine.NodeScope.SCM_NODESCOPE_CENTER),
        /**
         * A constants representing all node scope.
         *
         * @since 2.1
         */
        ALL_SITE(CommonDefine.NodeScope.SCM_NODESCOPE_ALL);
        private int scope;

        private ServerScope(int scope) {
            this.scope = scope;
        }

        public int getScope() {
            return scope;
        }
    }

    /**
     * Provides a set of constants that to specify the type of session
     *
     * @since 2.1
     */
    public enum SessionType {
        /**
         * A constants representing the type of authorized session.
         *
         * @since 2.1
         */
        AUTH_SESSION,

        /**
         * A constants representing the type of unauthorized session.
         *
         * @since 2.1
         */
        NOT_AUTH_SESSION;
    }

    /**
     * Provides a set of constants to specify the inputstream type..
     */
    public enum InputStreamType {
        /**
         * A constants representing the type of seekable ScmInputStream.
         *
         * @since 2.1
         */
        SEEKABLE,

        /**
         * A constants representing the type of unseekable ScmInputStream.
         *
         * @since 2.1
         */
        UNSEEKABLE;
    }

    /**
     * Provides a set of constants to specify the datasource type.
     */
    public enum DatasourceType {
        /**
         * A constants representing the type of sequoiadb datasource.
         *
         * @since 2.1
         */
        SEQUOIADB(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SEQUOIADB_STR),

        /**
         * A constants representing the type of habse datasource.
         *
         * @since 2.1
         */
        HBASE(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HBASE_STR),

        /**
         * A constants representing the type of ceph s3 datasource.
         *
         * @since 2.1
         */
        CEPH_S3(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHS3_STR),

        /**
         * A constants representing the type of ceph swift datasource.
         *
         * @since 2.1
         */
        CEPH_SWIFT(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_CEPHSWIFT_STR),

        /**
         * A constants representing the type of hdfs datasource.
         *
         * @since 2.1
         */
        HDFS(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_HDFS_STR),

        /**
         * A constants representing the type of sftp datasource.
         *
         * @since 3.1
         */
        SFTP(CommonDefine.DataSourceType.SCM_DATASOURCE_TYPE_SFTP_STR),


        /**
         * A constants representing the type of unknown datasource.
         *
         * @since 2.1
         */
        UNKNOWN("unknown");

        private String type;

        private DatasourceType(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }

        @Override
        public String toString() {
            return this.type;
        }

        public static DatasourceType getDatasourceType(String datasource) {
            for (DatasourceType value : DatasourceType.values()) {
                if (value.getType().equals(datasource)) {
                    return value;
                }
            }

            return UNKNOWN;
        }

    }

    /**
     * Provides a set of constants that to specify the type in refresh statistic.
     *
     * @since 3.0
     */
    public enum StatisticsType {
        /**
         * A constants representing traffic type.
         *
         * @since 3.0
         */
        TRAFFIC(CommonDefine.StatisticsType.SCM_STATISTICS_TYPE_TRAFFIC),

        /**
         * A constants representing file delta type.
         *
         * @since 3.0
         */
        FILE_DELTA(CommonDefine.StatisticsType.SCM_STATISTICS_TYPE_FILE_DELTA);

        private int type;

        private StatisticsType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    /**
     * Provides a set of constants to specify the type of site strategy.
     */
    public enum SiteStrategyType {
        /**
         * A constant representing star strategy.
         *
         * @since 3.1
         */
        Star(CommonDefine.SiteStrategy.SITE_STRATEGY_STAR),

        /**
         * A constant representing network strategy.
         *
         * @since 3.1
         */
        Network(CommonDefine.SiteStrategy.SITE_STRATEGY_NETWORK),

        /**
         * A constants representing the type of unknown strategy.
         *
         * @since 3.1
         */
        UNKNOWN("unknown");

        private String strategy;

        private SiteStrategyType(String strategy) {
            this.strategy = strategy;
        }

        public String getStrategy() {
            return strategy;
        }

        public static SiteStrategyType getStrategyType(String strategy) {
            for (SiteStrategyType value : SiteStrategyType.values()) {
                if (value.getStrategy().equals(strategy)) {
                    return value;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * Provides a set of constants to specify the BreakpointFile type..
     */
    public enum BreakpointFileType {
        /**
         * A constants representing the type of buffered BreakpointFile.
         *
         * @since 3.1.3
         */
        BUFFERED,

        /**
         * A constants representing the type of directed BreakpointFile.
         *
         * @since 3.1.3
         */
        DIRECTED;

    }
}
