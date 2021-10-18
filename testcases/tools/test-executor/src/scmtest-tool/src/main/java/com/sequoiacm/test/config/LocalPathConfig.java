package com.sequoiacm.test.config;

import java.io.File;
import java.io.IOException;

public class LocalPathConfig {

    public static String BASE_PATH = System.getProperty("common.basePath", ".");
    public static String JARS_PATH = BASE_PATH + "jars" + File.separator;
    private static String LISTENER_JAR_PATH;

    public static String TMP_PATH = BASE_PATH + "tmp" + File.separator;
    public static String LOCAL_TEST_PROGRESS_PATH = TMP_PATH + "test-progress.json";

    public static String OUTPUT_PATH = BASE_PATH + "output" + File.separator;
    public static String TEST_OUTPUT_PATH = BASE_PATH + "test-output" + File.separator;

    public static String LOG_PATH = BASE_PATH + "log" + File.separator;
    public static String TEST_LOG_PATH = LOG_PATH + "scmtest.log";

    public static String TOOL_CONF_PATH = BASE_PATH + "conf" + File.separator + "scmtest.properties";
    public static String PRO_INFO_PATH = BASE_PATH + "conf" + File.separator + "scmtest-project.cfg";

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
