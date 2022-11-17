package com.sequoiacm.testcommon;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

public class FileLoader {

    private static final String DIR_FILE = "file";

    public static String loadAndGetFilePath(String workPath, String project) throws IOException {
        if (!isRunByJar()) {
            URL fileDir = FileLoader.class.getClassLoader().getResource(DIR_FILE);
            return fileDir == null ? null : fileDir.getPath() + File.separator;
        }

        String targetPath = workPath + File.separator + project + File.separator;
        reLoadFileResource(DIR_FILE, targetPath);
        return targetPath;
    }

    private static void reLoadFileResource(String srcPath, String targetPath) throws IOException {
        File targetDir = new File(targetPath);
        if (targetDir.exists()) {
            FileUtils.cleanDirectory(targetDir);
        }
        targetDir.mkdirs();

        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources(srcPath + "/*");
        for (Resource resource : resources) {
            InputStream inputStream = resource.getInputStream();
            File targetFile = new File(targetPath + File.separator + resource.getFilename());
            Files.copy(inputStream, targetFile.toPath());
        }
    }

    private static boolean isRunByJar() {
        String protocol = FileLoader.class.getResource("").getProtocol();
        return Objects.equals(protocol, ResourceUtils.URL_PROTOCOL_JAR);
    }
}
