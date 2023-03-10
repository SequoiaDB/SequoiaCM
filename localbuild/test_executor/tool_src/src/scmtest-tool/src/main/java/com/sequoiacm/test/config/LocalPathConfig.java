package com.sequoiacm.test.config;

import java.io.File;
import java.io.IOException;

public class LocalPathConfig {

    public static String BASE_PATH = System.getProperty("common.basePath", ".");
    public static String JARS_PATH = BASE_PATH + "jars" + File.separator;
    public static String CONF_PATH = BASE_PATH + "conf" + File.separator;
    public static String PRO_INFO_PATH = CONF_PATH + "scmtest-project.cfg";
    public static String TOOL_CONF_PATH = CONF_PATH + "scmtest.properties";

    // 默认的工作目录：${测试工具目录}/executor
    public static String WORK_PATH = BASE_PATH + "executor" + File.separator;
    public static String CONSOLE_OUT_PATH = WORK_PATH + "console-out" + File.separator;
    public static String TEST_OUTPUT_PATH = WORK_PATH + "test-output" + File.separator;
    public static String TMP_PATH = WORK_PATH + "tmp" + File.separator;
    public static String TEST_RESULT_PATH = TMP_PATH + "test.result";

    // 本地执行目录：${测试工具目录}/executor/local-exec
    public static String EXEC_PATH = WORK_PATH + "local-exec" + File.separator;

    public static String LOG_PATH = BASE_PATH + "log" + File.separator;
    public static String TEST_LOG_PATH = LOG_PATH + "scmtest.log";
    private static String LISTENER_JAR_PATH;


    public static String getListenerJarPath() throws IOException {
        if (LISTENER_JAR_PATH == null) {
            File[] jarFileList = new File(JARS_PATH).listFiles();
            if (jarFileList != null) {
                for (File jarFile : jarFileList) {
                    String jarFileName = jarFile.getName();
                    if (jarFileName.startsWith("sequoiacm-testng-listener") && jarFileName.endsWith(".jar")) {
                        LISTENER_JAR_PATH = JARS_PATH + jarFileName;
                        break;
                    }
                }
            }
        }

        if (LISTENER_JAR_PATH == null) {
            throw new IOException("The listener of test does not exist");
        }

        return LISTENER_JAR_PATH;
    }
}
