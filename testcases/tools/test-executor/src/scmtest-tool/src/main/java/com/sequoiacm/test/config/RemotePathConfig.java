package com.sequoiacm.test.config;


public class RemotePathConfig {

    public static String LINUX_SEPARATOR = "/";

    public static String WORK_PATH = ScmTestToolProps.getInstance().getWorkPath();
    public static String JARS_PATH = WORK_PATH + "jars" + LINUX_SEPARATOR;

    public static String TMP_PATH = WORK_PATH + "tmp" + LINUX_SEPARATOR;
    public static String TEST_PROGRESS_PATH = TMP_PATH + "test-progress.json";

    public static String OUTPUT_PATH = WORK_PATH + "output" + LINUX_SEPARATOR;
    public static String TEST_OUTPUT_PATH = WORK_PATH + "test-output" + LINUX_SEPARATOR;

}
