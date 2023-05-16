package com.sequoiacm.contentserver.common;

import java.util.*;

import com.sequoiacm.common.ScmArgChecker;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.metadata.MetaDataManager;

public class ScmArgumentChecker {
    public static BSONObject checkAndCorrectClass(BSONObject classValue, String fieldName)
            throws ScmServerException {
        if (null == classValue) {
            classValue = new BasicBSONObject();
        }
        else if (!(classValue instanceof Map)) {
            throw new ScmInvalidArgumentException(
                    "class properties is not json format: " + classValue);
        }

        // SEQUOIACM-312
        Set<String> keySet = classValue.keySet();
        for (String key : keySet) {
            MetaDataManager.getInstence().validateKeyFormat(key, fieldName);
        }

        return classValue;
    }

    public static BasicBSONList checkAndCorrectTagsAsBson(BSONObject tagsValue)
            throws ScmServerException {
        BasicBSONList ret = new BasicBSONList();
        ret.addAll(checkAndCorrectTagsAsSet(tagsValue));
        return ret;
    }

    public static Set<String> checkAndCorrectTagsAsSet(BSONObject tagsValue)
            throws ScmServerException {
        if (null == tagsValue) {
            return new HashSet<>();
        }
        else if (!(tagsValue instanceof List)) {
            throw new ScmInvalidArgumentException("tag is not json list format: " + tagsValue);
        }
        else {
            HashSet<String> tags = new HashSet<>();
            BasicBSONList tagsList = (BasicBSONList) tagsValue;
            for (Object tag : tagsList) {
                if (!(tag instanceof String)) {
                    throw new ScmInvalidArgumentException("tag is not string format: " + tag);
                }
                if (!Objects.equals("", tag)) {
                    tags.add((String) tag);
                }
            }
            return tags;
        }
    }

    public static Map<String, String> checkAndCorrectCustomTag(BSONObject customTagValue)
            throws ScmServerException {
        if (customTagValue == null) {
            return Collections.emptyMap();
        }

        Map<String, String> ret = new HashMap<>();
        for (String key : customTagValue.keySet()) {
            Object value = customTagValue.get(key);
            if (value == null) {
                continue;
            }
            if (!(value instanceof String)) {
                throw new ScmInvalidArgumentException(
                        "tag is not string format: " + customTagValue);
            }
            ret.put(key, (String) value);
        }

        ScmArgChecker.File.checkScmCustomTag(ret);

        return ret;
    }

}
