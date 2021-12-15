package com.sequoiacm.infrastructure.statistics.common;

import java.util.Arrays;
import java.util.List;

public class ScmStatisticsType {
    public static final String FILE_UPLOAD = "file_upload";

    // 文件上传(FILE_UPLOAD)包含了普通文件上传和断点文件上传(BREAKPOINT_FILE_UPLOAD)
    // 因此 BREAKPOINT_FILE_UPLOAD 不需要加入 ALL_TYPES 里
    public static final String BREAKPOINT_FILE_UPLOAD = "breakpoint_file_upload";

    public static final String FILE_DOWNLOAD = "file_download";

    public static final List<String> ALL_TYPES = Arrays.asList(FILE_DOWNLOAD, FILE_UPLOAD);
}
