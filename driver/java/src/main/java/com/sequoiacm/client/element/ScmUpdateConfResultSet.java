package com.sequoiacm.client.element;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;

/**
 * The result set of set configuration in service instances.
 */
public class ScmUpdateConfResultSet {
    private List<ScmUpdateConfResult> successList;
    private List<ScmUpdateConfResult> failList;

    public ScmUpdateConfResultSet(BSONObject ret) throws ScmException {
        successList = new ArrayList<ScmUpdateConfResult>();
        BasicBSONList successes = BsonUtils.getArray(ret, "successes");
        for (Object success : successes) {
            successList.add(new ScmUpdateConfResult((BSONObject) success));
        }

        failList = new ArrayList<ScmUpdateConfResult>();
        BasicBSONList failes = BsonUtils.getArray(ret, "failes");
        for (Object fail : failes) {
            failList.add(new ScmUpdateConfResult((BSONObject) fail));
        }

    }

    /**
     * Gets failed list.
     *
     * @return list.
     */
    public List<ScmUpdateConfResult> getFailures() {
        return failList;
    }

    /**
     * Gets the successful list.
     *
     * @return list.
     */
    public List<ScmUpdateConfResult> getSuccesses() {
        return successList;
    }

    @Override
    public String toString() {
        return "ScmUpdateConfResultSet [successes=" + successList + ", failes=" + failList + "]";
    }
}
