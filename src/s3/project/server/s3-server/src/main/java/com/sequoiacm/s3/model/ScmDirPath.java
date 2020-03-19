package com.sequoiacm.s3.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

public class ScmDirPath {
    public final static String PATH_SPILT = CommonDefine.Directory.SCM_DIR_SEP;
    private StringBuilder path;
    private List<String> names;

    // ex:
    // path = "/a/b/c/"
    // dirLevle = 4
    // names = {"/", "a", "b", "c"}
    // "/" : dirLevel = 1
    public ScmDirPath(String path) throws S3ServerException {
        this.path = createAndAmendPath(path);
        path = this.path.toString();
        // when path is root dir, name array is empty after split
        if (path.equals(CommonDefine.Directory.SCM_ROOT_DIR_NAME)) {
            names = new ArrayList<String>();
            names.add(CommonDefine.Directory.SCM_ROOT_DIR_NAME);
        }
        else {
            String[] nameArray = path.split(PATH_SPILT);
            if (nameArray.length < 1) {
                throw new S3ServerException(S3Error.INTERNAL_ERROR,
                        "path unformatted, path=" + path);
            }
            // when name array index=0, name is empty
            nameArray[0] = CommonDefine.Directory.SCM_ROOT_DIR_NAME;
            names = Arrays.asList(nameArray);
        }
    }

    public int getLevel() {
        return names.size();
    }

    public String getBaseName() {
        return names.get(getLevel() - 1);
    }

    public String getNamebyLevel(int level) throws S3ServerException {
        checkLevelBound(level);
        return names.get(level - 1);
    }

    public String getPathByLevel(int level) throws S3ServerException {
        checkLevelBound(level);
        StringBuilder newPath = new StringBuilder();
        for (int index = 0; index < level; index++) {
            newPath.append(names.get(index));
            // when index = 0, value are both dir name and separator
            if (index != 0) {
                newPath.append(PATH_SPILT);
            }
        }
        return newPath.toString();
    }

    private void checkLevelBound(int level) throws S3ServerException {
        if (level < 1) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "level less than path limit,minLevel=1, level=" + level);
        }
        if (level > names.size()) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR,
                    "level greater than path limit,maxLevel=" + names.size() + ", level=" + level);
        }
    }

    // amend path, ex: /a/b/c-->/a/b/c/
    private StringBuilder createAndAmendPath(String path) {
        if (!path.endsWith(PATH_SPILT)) {
            path += PATH_SPILT;
        }
        if (!path.startsWith(PATH_SPILT)) {
            path = PATH_SPILT + path;
        }
        return new StringBuilder(path);
    }

    public String getPath() {
        return path.toString();
    }

    public boolean isRootDir() {
        return getLevel() == 1;
    }

    public static int getLevelByPath(String path) {
        if (path.equals(CommonDefine.Directory.SCM_ROOT_DIR_NAME)) {
            return 1;
        }
        return path.split(PATH_SPILT).length;
    }

    @Override
    public String toString() {
        return "dirpath=" + path.toString() + ", level=" + getLevel();
    }

    public static void main(String[] args) throws S3ServerException {
        ScmDirPath p = new ScmDirPath("asd1");
        System.out.println(p.getLevel());
        System.out.println(p.getNamebyLevel(1));
        System.out.println(p.getNamebyLevel(2));
    }
}
