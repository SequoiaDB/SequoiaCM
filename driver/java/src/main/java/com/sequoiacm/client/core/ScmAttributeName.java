package com.sequoiacm.client.core;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;

/**
 * Scm Attribute String
 *
 * @since 2.1
 */
public class ScmAttributeName {

    private ScmAttributeName() {

    }

    /**
     * Attribute of ScmWorkspace
     *
     * @since 3.1
     */
    public static class Workspace {
        private Workspace() {
        }

        /**
         * Workspace id
         */
        public static final String ID = FieldName.FIELD_CLWORKSPACE_ID;

        /**
         * Workspace name
         */
        public static final String NAME = FieldName.FIELD_CLWORKSPACE_NAME;

        /**
         * Workspace create user
         */
        public static final String CREATEUSER = FieldName.FIELD_CLWORKSPACE_CREATEUSER;

        /**
         * Workspace create time
         */
        public static final String CREATETIME = FieldName.FIELD_CLWORKSPACE_CREATETIME;

        /**
         * Workspace update user
         */
        public static final String UPDATEUSER = FieldName.FIELD_CLWORKSPACE_UPDATEUSER;

        /**
         * Workspace update time
         */
        public static final String UPDATETIME = FieldName.FIELD_CLWORKSPACE_UPDATETIME;

        /**
         * Workspace description
         */
        public static final String DESCRIPTION = FieldName.FIELD_CLWORKSPACE_DESCRIPTION;
    }

    /**
     * Attribute of ScmUser
     *
     * @since 3.0
     */
    public static class User {
        private User() {
        }

        /**
         * Password type
         */
        public static final String PASSWORD_TYPE = FieldName.User.FIELD_PASSWORD_TYPE;

        /**
         * Enabled
         */
        public static final String ENABLED = FieldName.User.FIELD_ENABLED;

        /**
         * Has role
         */
        public static final String HAS_ROLE = "has_role";
    }

    /**
     * Attribute of ScmRole
     *
     * @since 3.1
     */
    public static class Role {
        private Role() {
        }

        /**
         * Role name
         */
        public static final String ROLE_NAME = FieldName.FIELD_CLROLE_ROLENAME;

        /**
         * Role description
         */
        public static final String DESCRIPTION = FieldName.FIELD_CLROLE_DESCRIPTION;
    }

    /**
     * Attribute of ScmFile
     *
     * @since 2.1
     */
    public static class File {
        private File() {

        }

        /**
         * File id
         *
         * @since 2.1
         */
        public static final String FILE_ID = FieldName.FIELD_CLFILE_ID;

        /**
         * File name
         *
         * @since 2.1
         */
        public static final String FILE_NAME = FieldName.FIELD_CLFILE_NAME;

        /**
         * Major version
         *
         * @since 2.1
         */
        public static final String MAJOR_VERSION = FieldName.FIELD_CLFILE_MAJOR_VERSION;

        /**
         * Minor version.
         *
         * @since 2.1
         */
        public static final String MINOR_VERSION = FieldName.FIELD_CLFILE_MINOR_VERSION;

        /**
         * Property type.
         *
         * @since 2.1
         */
        public static final String PROPERTY_TYPE = FieldName.FIELD_CLFILE_FILE_CLASS_ID;

        /**
         * Batch id
         *
         * @since 2.1
         */
        public static final String BATCH_ID = FieldName.FIELD_CLFILE_BATCH_ID;

        /**
         * Directory id.
         *
         * @since 2.1
         */
        public static final String DIRECTORY_ID = FieldName.FIELD_CLFILE_DIRECTORY_ID;
        /**
         * Properties
         *
         * @since 2.1
         */
        public static final String PROPERTIES = FieldName.FIELD_CLFILE_PROPERTIES;
        /**
         * Tags
         *
         * @since 2.1
         */
        public static final String TAGS = FieldName.FIELD_CLFILE_TAGS;
        /**
         * Author.
         *
         * @since 2.1
         */
        public static final String AUTHOR = FieldName.FIELD_CLFILE_FILE_AUTHOR;
        /**
         * User.
         *
         * @since 2.1
         */
        public static final String USER = FieldName.FIELD_CLFILE_INNER_USER;
        /**
         * Site list
         *
         * @since 2.1
         */
        public static final String SITE_LIST = FieldName.FIELD_CLFILE_FILE_SITE_LIST;
        /**
         * Site id
         *
         * @since 2.1
         */
        public static final String SITE_ID = FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID;
        /**
         * Last access time
         *
         * @since 2.1
         */
        public static final String LAST_ACCESS_TIME = FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME;
        /**
         * Create time.
         *
         * @since 2.1
         */
        public static final String CREATE_TIME = FieldName.FIELD_CLFILE_INNER_CREATE_TIME;
        /**
         * Update time.
         *
         * @since 2.1
         */
        public static final String UPDATE_TIME = FieldName.FIELD_CLFILE_INNER_UPDATE_TIME;
        /**
         * Size.
         *
         * @since 2.1
         */
        public static final String SIZE = FieldName.FIELD_CLFILE_FILE_SIZE;
        /**
         * Title.
         *
         * @since 2.1
         */
        public static final String TITLE = FieldName.FIELD_CLFILE_FILE_TITLE;
        /**
         * Mime type
         *
         * @since 2.1
         */
        public static final String MIME_TYPE = FieldName.FIELD_CLFILE_FILE_MIME_TYPE;
        /**
         * Update user.
         *
         * @since 2.1
         */
        public static final String UPDATE_USER = FieldName.FIELD_CLFILE_INNER_UPDATE_USER;
        /**
         * Create month.
         *
         * @since 2.1
         */
        public static final String CREATE_MONTH = FieldName.FIELD_CLFILE_INNER_CREATE_MONTH;

    }

    /**
     * Attribute of ScmBreakpointFile
     *
     * @since 3.0
     */
    public static class BreakpointFile {
        private BreakpointFile() {
        }

        /**
         * File name
         */
        public static final String FILE_NAME = FieldName.BreakpointFile.FIELD_FILE_NAME;

        /**
         * Checksum type
         */
        public static final String CHECKSUM_TYPE = FieldName.BreakpointFile.FIELD_CHECKSUM_TYPE;

        /**
         * Checksum
         */
        public static final String CHECKSUM = FieldName.BreakpointFile.FIELD_CHECKSUM;

        /**
         * Data id
         */
        public static final String DATA_ID = FieldName.BreakpointFile.FIELD_DATA_ID;

        /**
         * Completed
         */
        public static final String COMPLETED = FieldName.BreakpointFile.FIELD_COMPLETED;

        /**
         * Upload size
         */
        public static final String UPLOAD_SIZE = FieldName.BreakpointFile.FIELD_UPLOAD_SIZE;

        /**
         * Create user
         */
        public static final String CREATE_USER = FieldName.BreakpointFile.FIELD_CREATE_USER;

        /**
         * Create time
         */
        public static final String CREATE_TIME = FieldName.BreakpointFile.FIELD_CREATE_TIME;

        /**
         * Upload user
         */
        public static final String UPLOAD_USER = FieldName.BreakpointFile.FIELD_UPLOAD_USER;

        /**
         * Upload time
         */
        public static final String UPLOAD_TIME = FieldName.BreakpointFile.FIELD_UPLOAD_TIME;
    }

    /**
     * Attribute of ScmTask
     *
     * @since 2.1
     */
    public static class Task {
        private Task() {

        }

        /**
         * Id.
         *
         * @since 2.1
         */
        public static final String ID = FieldName.Task.FIELD_ID;
        /**
         * Type.
         *
         * @since 2.1
         */
        public static final String TYPE = FieldName.Task.FIELD_TYPE;
        /**
         * Workspace
         *
         * @since 2.1
         */
        public static final String WORKSPACE = FieldName.Task.FIELD_WORKSPACE;
        /**
         * Content.
         *
         * @since 2.1
         */
        public static final String CONTENT = FieldName.Task.FIELD_CONTENT;
        /**
         * Server id
         *
         * @since 2.1
         */
        public static final String SERVER_ID = FieldName.Task.FIELD_SERVER_ID;
        /**
         * Progress.
         *
         * @since 2.1
         */
        public static final String PROGRESS = FieldName.Task.FIELD_PROGRESS;
        /**
         * Running flag.
         *
         * @since 2.1
         */
        public static final String RUNNING_FLAG = FieldName.Task.FIELD_RUNNING_FLAG;
        /**
         * Detail.
         *
         * @since 2.1
         */
        public static final String DETAIL = FieldName.Task.FIELD_DETAIL;
        /**
         * Start time.
         *
         * @since 2.1
         */
        public static final String START_TIME = FieldName.Task.FIELD_START_TIME;
        /**
         * Stop time.
         *
         * @since 2.1
         */
        public static final String STOP_TIME = FieldName.Task.FIELD_STOP_TIME;
        /**
         * schedule id.
         *
         * @since 3.0
         */
        public static final String SCHEDULE_ID = FieldName.Task.FIELD_SCHEDULE_ID;

        /**
         * Estimate count.
         */
        public static final String ESTIMATE_COUNT = FieldName.Task.FIELD_ESTIMATE_COUNT;

        /**
         * Actual count.
         */
        public static final String ACTUAL_COUNT = FieldName.Task.FIELD_ACTUAL_COUNT;

        /**
         * Success count.
         */
        public static final String SUCCESS_COUNT = FieldName.Task.FIELD_SUCCESS_COUNT;

        /**
         * Fail count.
         */
        public static final String FAIL_COUNT = FieldName.Task.FIELD_FAIL_COUNT;

        /**
         * Target site.
         */
        public static final String TARGET_SITE = FieldName.Task.FIELD_TARGET_SITE;

        /**
         * Scope.
         */
        public static final String SCOPE = FieldName.Task.FIELD_SCOPE;

        /**
         * Max exec time.
         */
        public static final String MAX_EXEC_TIME = FieldName.Task.FIELD_MAX_EXEC_TIME;
    }

    /**
     * Attribute of schedule.
     *
     */
    public static class Schedule {
        private Schedule() {

        }

        /**
         * Schedule id.
         */
        public static final String ID = "id";

        /**
         * Schedule name.
         */
        public static final String NAME = "name";

        /**
         * Schedule description.
         */
        public static final String DESC = "desc";

        // string
        /**
         * Schedule type.
         */
        public static final String TYPE = "type";
        // string
        /**
         * Workspace name.
         */
        public static final String WORKSPACE = "workspace";
        // bson
        /**
         * Schedule content.
         */
        public static final String CONTENT = "content";
        // string
        /**
         * Schedule cron.
         */
        public static final String CRON = "cron";
        // string
        /**
         * Created user.
         */
        public static final String CREATE_USER = "create_user";
        // long (ms)
        /**
         * Created time.
         */
        public static final String CREATE_TIME = "create_time";

        // task scope
        /**
         * File scope.
         */
        public static final String CONTENT_SCOPE = "scope";

        /**
         * Every taks max execute time.
         */
        public static final String CONTENT_MAX_EXEC_TIME = "max_exec_time";

        // string (ex. '3d')
        /**
         * File max stay time.
         */
        public static final String CONTENT_MAX_STAY_TIME = "max_stay_time";
        public static final String CONTENT_EXISTENCE_TIME = "existence_time";
        // bson
        /**
         * Extra file condition.
         */
        public static final String CONTENT_EXTRA_CONDITION = "extra_condition";

        // string
        /**
         * Data check level.
         */
        public static final String CONTENT_DATA_CHECK_LEVEL = "data_check_level";

        // boolean
        /**
         * Is quick start.
         */
        public static final String CONTENT_QUICK_START = "quick_start";

        // boolean
        /**
         * Is recycle space.
         */
        public static final String CONTENT_IS_RECYCLE_SPACE = "is_recycle_space";

        /**
         * Is disable.
         */
        public static final String ENABLE = "enable";

        /**
         * preferred region.
         */
        public static final String PREFERRED_REGION = "preferred_region";

        /**
         * preferred zone.
         */
        public static final String PREFERRED_ZONE = "preferred_zone";

        // ***************** clean job *****************
        /**
         * Clean site.
         */
        public static final String CONTENT_CLEAN_SITE = "site";

        // ***************** copy job *****************
        /**
         * Source site.
         */
        public static final String CONTENT_COPY_SOURCE_SITE = "source_site";

        /**
         * Target site.
         */
        public static final String CONTENT_COPY_TARGET_SITE = "target_site";

        // ***************** move file job *****************
        public static final String CONTENT_MOVE_SOURCE_SITE = "source_site";
        public static final String CONTENT_MOVE_TARGET_SITE = "target_site";

        // ***************** space recycle job *****************
        public static final String CONTENT_TARGET_SITE = "target_site";
        public static final String CONTENT_RECYCLE_SCOPE = "recycle_scope";
    }

    /**
     * Attribute of directory.
     */
    public static class Directory {
        /**
         * Directory name.
         */
        public static final String NAME = FieldName.FIELD_CLDIR_NAME;

        /**
         * Created time.
         */
        public static final String CREATE_TIME = FieldName.FIELD_CLDIR_CREATE_TIME;

        /**
         * Updated time.
         */
        public static final String UPDATE_TIME = FieldName.FIELD_CLDIR_UPDATE_TIME;

        /**
         * Created user.
         */
        public static final String USER = FieldName.FIELD_CLDIR_USER;

        /**
         * Updated user.
         */
        public static final String UPDATE_USER = FieldName.FIELD_CLDIR_UPDATE_USER;
    }

    /**
     * Attribute of batch.
     */
    public static final class Batch {
        private Batch() {
        }

        /**
         * Batch id.
         */
        public static final String ID = FieldName.Batch.FIELD_ID;

        /**
         * Batch name.
         */
        public static final String NAME = FieldName.Batch.FIELD_NAME;

        /**
         * Batch created time.
         */
        public static final String CREATE_TIME = FieldName.Batch.FIELD_INNER_CREATE_TIME;

        /**
         * Batch updated time.
         */
        public static final String UPDATE_TIME = FieldName.Batch.FIELD_INNER_UPDATE_TIME;

        /**
         * Batch created user.
         */
        public static final String CREATE_USER = FieldName.Batch.FIELD_INNER_CREATE_USER;

        /**
         * Batch updated user
         */
        public static final String UPDATE_USER = FieldName.Batch.FIELD_INNER_UPDATE_USER;

        /**
         * Batch properties.
         */
        public static final String PROPERTIES = FieldName.Batch.FIELD_CLASS_PROPERTIES;

        /**
         * Batch tags.
         */
        public static final String TAGS = FieldName.Batch.FIELD_TAGS;
    }

    /**
     * Attribute of scm class.
     */
    public static final class Class {
        private Class() {
        }

        /**
         * Class name.
         */
        public static final String NAME = FieldName.Class.FIELD_NAME;
        /**
         * Class description.
         */
        public static final String DESCRIPTION = FieldName.Class.FIELD_DESCRIPTION;

        /**
         * Class created time.
         */
        public static final String CREATE_TIME = FieldName.Class.FIELD_INNER_CREATE_TIME;

        /**
         * Class updated time.
         */
        public static final String UPDATE_TIME = FieldName.Class.FIELD_INNER_UPDATE_TIME;

        /**
         * Class created user.
         */
        public static final String CREATE_USER = FieldName.Class.FIELD_INNER_CREATE_USER;

        /**
         * Class updated user.
         */
        public static final String UPDATE_USER = FieldName.Class.FIELD_INNER_UPDATE_USER;
    }

    /**
     * Attribute of scm attribute.
     */
    public static final class Attribute {
        private Attribute() {
        }

        /**
         * Attribute name.
         */
        public static final String NAME = FieldName.Attribute.FIELD_NAME;
        /**
         * Attribute display name.
         */
        public static final String DISPLAY_NAME = FieldName.Attribute.FIELD_DISPLAY_NAME;

        /**
         * Attribute description.
         */
        public static final String DESCRIPTION = FieldName.Attribute.FIELD_DESCRIPTION;

        /**
         * Attribute type.
         */
        public static final String TYPE = FieldName.Attribute.FIELD_TYPE;

        /**
         * Is required.
         */
        public static final String REQUIRED = FieldName.Attribute.FIELD_REQUIRED;

        /**
         * Created time.
         */
        public static final String CREATE_TIME = FieldName.Attribute.FIELD_INNER_CREATE_TIME;

        /**
         * Updated time.
         */
        public static final String UPDATE_TIME = FieldName.Attribute.FIELD_INNER_UPDATE_TIME;

        /**
         * Created user.
         */
        public static final String CREATE_USER = FieldName.Attribute.FIELD_INNER_CREATE_USER;

        /**
         * Updated user.
         */
        public static final String UPDATE_USER = FieldName.Attribute.FIELD_INNER_UPDATE_USER;
    }

    /**
     * Attribute of ScmAudit
     *
     * @since 3.0
     */
    public static final class Audit {
        private Audit() {
        }

        /**
         * HOSTNAME (String)
         *
         * @since 3.0
         */
        public static final String HOSTNAME = FieldName.Audit.HOST;

        /**
         * PORT (String)
         *
         * @since 3.0
         */
        public static final String PORT = FieldName.Audit.PORT;

        /**
         * TYPE (String)
         *
         * @since 3.0
         */
        public static final String TYPE = FieldName.Audit.TYPE;

        /**
         * USERTYPE (String)
         *
         * @since 3.0
         */
        public static final String USERTYPE = FieldName.Audit.USER_TYPE;

        /**
         * USERNAME (String)
         *
         * @since 3.0
         */
        public static final String USERNAME = FieldName.Audit.USER_NAME;

        /**
         * WORKSPACENAME (String)
         *
         * @since 3.0
         */
        public static final String WORKSPACENAME = FieldName.Audit.WORK_SPACE;

        /**
         * FLAG (Int)
         *
         * @since 3.0
         */
        public static final String FLAG = FieldName.Audit.FLAG;

        /**
         * TIME (BSONTimestamp)
         *
         * @since 3.0
         */
        public static final String TIME = FieldName.Audit.TIME;

        /**
         * THREAD (String)
         *
         * @since 3.0
         */
        public static final String THREAD = FieldName.Audit.THREAD;

        /**
         * LEVEL (String)
         *
         * @since 3.0
         */
        public static final String LEVEL = FieldName.Audit.LEVEL;

        /**
         * MESSAGE (String)
         *
         * @since 3.0
         */
        public static final String MESSAGE = FieldName.Audit.MESSAGE;
    }

    /**
     * Attribute of traffic.
     *
     */
    public static final class Traffic {
        private Traffic() {
        }

        /**
         * Traffic type.
         */
        public static final String TYPE = FieldName.Traffic.FIELD_TYPE;

        /**
         * Workspace name.
         */
        public static final String WORKSPACE_NAME = FieldName.Traffic.FIELD_WORKSPACE_NAME;

        /**
         * Traffic
         */
        public static final String TRAFFIC = FieldName.Traffic.FIELD_TRAFFIC;

        /**
         * Record time.
         */
        public static final String RECORD_TIME = FieldName.Traffic.FIELD_RECORD_TIME;
    }

    /**
     * Attribute of file delta.
     *
     */
    public static final class FileDelta {
        private FileDelta() {
        }

        /**
         * Workspace name.
         */
        public static final String WORKSPACE_NAME = FieldName.FileDelta.FIELD_WORKSPACE_NAME;

        /**
         * Count delta
         */
        public static final String COUNT_DELTA = FieldName.FileDelta.FIELD_COUNT_DELTA;

        /**
         * Size delta.
         */
        public static final String SIZE_DELTA = FieldName.FileDelta.FIELD_SIZE_DELTA;

        /**
         * Record time.
         */
        public static final String RECORD_TIME = FieldName.FileDelta.FIELD_RECORD_TIME;
    }

    /**
     * Attribute of object delta.
     *
     */
    public static final class ObjectDelta {
        private ObjectDelta() {
        }

        /**
         * Bucket name.
         */
        public static final String BUCKET_NAME = FieldName.ObjectDelta.FIELD_BUCKET_NAME;

        /**
         * Count delta
         */
        public static final String COUNT_DELTA = FieldName.ObjectDelta.FIELD_COUNT_DELTA;

        /**
         * Size delta.
         */
        public static final String SIZE_DELTA = FieldName.ObjectDelta.FIELD_SIZE_DELTA;

        /**
         * Record time.
         */
        public static final String RECORD_TIME = FieldName.ObjectDelta.FIELD_RECORD_TIME;

        /**
         * Update time
         */
        public static final String UPDATE_TIME = FieldName.ObjectDelta.FIELD_UPDATE_TIME;
    }

    /**
     * Attribute of bucket.
     *
     */
    public static class Bucket {
        private Bucket() {
        }

        /**
         * Bucket name.
         */
        public static final String NAME = FieldName.Bucket.NAME;

        /**
         * Bucket id.
         */
        public static final String ID = FieldName.Bucket.ID;

        /**
         * Bucket workspace.
         */
        public static final String WORKSPACE = FieldName.Bucket.WORKSPACE;

        /**
         * Bucket creation user.
         */
        public static final String CREATE_USER = FieldName.Bucket.CREATE_USER;

        /**
         * Bucket creation time.
         */
        public static final String CREATE_TIME = FieldName.Bucket.CREATE_TIME;
    }

    /**
     * Attribute of bucket attach failure external info.
     *
     */
    public static class BucketAttachFailureExt {
        private BucketAttachFailureExt() {
        }

        /**
         * Bucket name.
         */
        public static final String BUCKET_NAME = CommonDefine.RestArg.ATTACH_FAILURE_EXT_INFO_BUCKET_NAME;
    }
//    屏蔽标签功能：SEQUOIACM-1411
//    /**
//     * Attribute of tag lib.
//     */
//    public static class TagLib {
//
//        /**
//         * Tags name.
//         */
//        public static final String TAG = FieldName.TagLib.TAG;
//
//        /**
//         * Tag id.
//         */
//        public static final String TAG_ID = FieldName.TagLib.TAG_ID;
//
//        /**
//         * Custom tag key.
//         */
//        public static final String CUSTOM_TAG_KEY = FieldName.TagLib.CUSTOM_TAG_TAG_KEY;
//
//        /**
//         * Custom tag value.
//         */
//        public static final String CUSTOM_TAG_VALUE = FieldName.TagLib.CUSTOM_TAG_TAG_VALUE;
//    }
}
