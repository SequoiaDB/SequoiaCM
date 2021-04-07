package com.sequoiacm.infrastructure.statistics.common;

import java.util.Arrays;
import java.util.List;

public class ScmStatisticsType {
    public static final String FILE_UPLOAD = "file_upload";
    public static final String FILE_DOWNLOAD = "file_download";

    public static final List<String> ALL_TYPES = Arrays.asList(FILE_DOWNLOAD, FILE_UPLOAD);
}
