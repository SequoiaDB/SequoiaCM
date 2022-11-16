package com.sequoiacm.contentserver.common;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static Set<Object> checkAndCorrectTags(BSONObject tagsValue)
            throws ScmServerException {
        if (null == tagsValue) {
            return new HashSet<>();
        }
        else if (!(tagsValue instanceof List)) {
            throw new ScmInvalidArgumentException("tag is not json list format: " + tagsValue);
        }
        else {
            BasicBSONList tagsList = (BasicBSONList) tagsValue;
            HashSet<Object> tags = new HashSet<Object>(tagsList);
            tags.remove(null);
            tags.remove("");
            return tags;
        }
    }
}
