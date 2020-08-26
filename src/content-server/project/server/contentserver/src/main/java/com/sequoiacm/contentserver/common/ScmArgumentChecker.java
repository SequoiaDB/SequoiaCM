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
    public static Object checkAndCorrectClass(BSONObject classValue, String fieldName)
            throws ScmServerException {
        if (null == classValue) {
            classValue = new BasicBSONObject();
        }
        else if (!(classValue instanceof Map)) {
            throw new ScmInvalidArgumentException(
                    "field [ " + fieldName + " ] is not Json format!");
        }

        // SEQUOIACM-312
        Set<String> keySet = classValue.keySet();
        for (String key : keySet) {
            MetaDataManager.getInstence().validateKeyFormat(key, fieldName);
        }

        return classValue;
    }

    public static Object checkAndCorrectTags(BSONObject tagsValue, String fieldName)
            throws ScmServerException {
        if (null == tagsValue) {
            return new BasicBSONList();
        }
        else if (!(tagsValue instanceof List)) {
            throw new ScmInvalidArgumentException("field [" + fieldName + "] is not Json format!");
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
