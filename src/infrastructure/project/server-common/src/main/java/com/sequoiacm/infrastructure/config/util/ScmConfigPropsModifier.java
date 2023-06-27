package com.sequoiacm.infrastructure.config.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmConfigPropsModifier {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfigPropsModifier.class);

    private String configFilePath;
    private final String newAppRropsTmpFileName = "newApplication.properties.tmp";

    private final static String APP_PROPS_BACKUP_PREFIX = "application.properties.bak.";
    private final static String TIMESTAMP_FORMAT = "yyyy-MM-dd-HH.mm.ss";
    private SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT);

    private String configFileParentPath;

    private boolean isNeedRollback = false;

    private File currentOptionBackFile;

    public ScmConfigPropsModifier(String configFilePath) {
        this.configFilePath = configFilePath;
        this.configFileParentPath = new File(configFilePath).getParent();
    }

    // return true if new file is different form old
    public boolean modifyPropsFile(Map<String, String> updateProps, List<String> deleteProps) {
        // e.g: configFilePath = /scm/conf/application.properties

        // create a new props file: /scm/conf/newApplication.properties.tmp
        boolean isDifferentFromOld = createNewAppPropsTmpFile(updateProps, deleteProps);
        if (!isDifferentFromOld) {
            // new props file is the same as the old file, delete it and return.
            removeFile(configFileParentPath + File.separator + newAppRropsTmpFileName);
            return isDifferentFromOld;
        }

        // create a backup file: rename /scm/conf/application.properties to
        // /scm/conf/application.properties.bak.timestamp
        currentOptionBackFile = backupPropsFile();

        isNeedRollback = true;

        try {
            // rename /scm/conf/newApplication.properties.tmp to
            // /scm/conf/application.properties
            renameFile(new File(configFileParentPath + File.separator + newAppRropsTmpFileName),
                    new File(configFilePath));
            return true;
        }
        catch (Exception e) {
            // rollback
            rollbackSilence();
            throw e;
        }
    }

    private File backupPropsFile() {
        File propsFileDir = new File(configFileParentPath);
        // list all back file
        File[] backFiles = propsFileDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                String fileName = pathname.getName();
                if (fileName.startsWith(APP_PROPS_BACKUP_PREFIX)) {
                    try {
                        sdf.parse(fileName.substring(APP_PROPS_BACKUP_PREFIX.length())).getTime();
                        return true;

                    }
                    catch (ParseException e) {
                        logger.warn("an unexpected backup file:" + pathname);
                    }
                }
                return false;
            }
        });

        // just create new backup
        String timestamp = sdf.format(new Date());
        File backupFile = new File(
                configFileParentPath + File.separator + APP_PROPS_BACKUP_PREFIX + timestamp);
        renameFile(new File(configFilePath), backupFile);

        // (last three option backups) + (the oldest backup) = 4
        if (backFiles != null && backFiles.length >= 4) {

            // sort files by timestamp [old .. new]
            Arrays.sort(backFiles, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            // remove the second oldest backup (we need keep the oldest backup)
            removeFile(backFiles[1]);
        }

        return backupFile;
    }

    private void removeFile(String path) {
        File file = new File(path);
        removeFile(file);
    }

    private void removeFile(File file) {
        if (!file.exists()) {
            return;
        }
        if (!file.delete()) {
            logger.warn("failed to delete file:{}", file.getAbsolutePath());
        }
    }

    public void rollbackSilence() {
        try {
            rollback();
        }
        catch (Exception e) {
            logger.error("failed to rollback", e);
        }
    }

    public void rollback() {

        removeFile(configFileParentPath + File.separator + newAppRropsTmpFileName);

        if (isNeedRollback) {

            // remove /scm/conf/application.properties
            removeFile(configFilePath);

            // rename /scm/conf/application.properties.bak.time to
            // /scm/conf/application.properties
            try {
                renameFile(currentOptionBackFile, new File(configFilePath));
            }
            catch (Exception e) {
                throw new RuntimeException(String.format("failed to rollback, rename %s to %s.",
                        currentOptionBackFile, configFilePath), e);
            }

            isNeedRollback = false;
        }
    }

    private void renameFile(File oldFile, File newFile) {
        boolean isSuccess = oldFile.renameTo(newFile);
        if (!isSuccess) {
            throw new RuntimeException(
                    String.format("failed to rename application.properties, rename %s to %s.",
                            oldFile.getAbsolutePath(), newFile.getAbsolutePath()));
        }
    }

    private boolean createNewAppPropsTmpFile(Map<String, String> updateProps,
            List<String> deleteProps) {
        updateProps = new HashMap<>(updateProps);

        boolean isDifferentFromOld = false;
        BufferedWriter newTmpAppProps = null;
        BufferedReader appProps = null;
        try {
            File oldAppPropsFile = new File(configFilePath);
            appProps = new BufferedReader(
                    new InputStreamReader(new FileInputStream(oldAppPropsFile)));
            newTmpAppProps = new BufferedWriter(
                    new FileWriter(configFileParentPath + File.separator + newAppRropsTmpFileName));
            String line;
            while ((line = appProps.readLine()) != null) {
                if (line.trim().startsWith("#") || !line.contains("=")) {
                    newTmpAppProps.write(line);
                    newTmpAppProps.newLine();
                    continue;
                }

                int equalsIdx = line.indexOf("=");
                String key = line.substring(0, equalsIdx).trim();
                String oldValue = "";
                if (equalsIdx < line.length() - 1) {
                    oldValue = line.substring(equalsIdx + 1);
                }

                String newValue = updateProps.get(key);
                if (newValue != null) {
                    updateProps.remove(key);
                    if (!newValue.equals(oldValue)) {
                        line = key + "=" + newValue;
                        isDifferentFromOld = true;
                    }
                }
                else if (deleteProps.contains(key)) {
                    // we need delete this line
                    isDifferentFromOld = true;
                    continue;
                }

                newTmpAppProps.write(line);
                newTmpAppProps.newLine();
            }

            if (!updateProps.isEmpty()) {
                for (Entry<String, String> prop : updateProps.entrySet()) {
                    newTmpAppProps.write(prop.getKey() + "=" + prop.getValue());
                    newTmpAppProps.newLine();
                }
                isDifferentFromOld = true;
            }

            return isDifferentFromOld;
        }
        catch (Exception e) {
            throw new RuntimeException("failed to create new application.properties.", e);
        }
        finally {
            closeResource(newTmpAppProps);
            closeResource(appProps);
        }
    }

    private void closeResource(Closeable resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        }
        catch (IOException e) {
            logger.warn("failed to close resource:" + resource, e);
        }
    }

}
