package com.sequoiacm.client.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.exception.ScmError;

/**
 * The result set of set configuration in service instances.
 */
public class ScmUpdateConfResultSet {
    private List<ScmUpdateConfResult> successList;
    private List<ScmUpdateConfResult> failList;
    private Set<String> rebootConf;
    private Map<String, String> adjustConf;

    public ScmUpdateConfResultSet(BSONObject ret) throws ScmException {
        successList = new ArrayList<ScmUpdateConfResult>();
        BasicBSONList successes = BsonUtils.getArray(ret, "successes");
        if (successes == null) {
            String message = BsonUtils.getString(ret, "message");
            throw new ScmException(ScmError.HTTP_INTERNAL_SERVER_ERROR, message);
        }
        for (Object success : successes) {
            successList.add(new ScmUpdateConfResult((BSONObject) success));
        }

        failList = new ArrayList<ScmUpdateConfResult>();
        BasicBSONList failes = BsonUtils.getArray(ret, "failes");
        for (Object fail : failes) {
            failList.add(new ScmUpdateConfResult((BSONObject) fail));
        }

        rebootConf = new HashSet<String>();
        BasicBSONList rebootConfList = BsonUtils.getArray(ret, "reboot_conf");
        if (rebootConfList != null) {
            for (Object conf : rebootConfList) {
                rebootConf.add((String) conf);
            }
        }

        adjustConf = new HashMap<String, String>();
        BSONObject adjustConfBSON = BsonUtils.getBSONObject(ret, "adjust_conf");
        if (adjustConfBSON != null) {
            adjustConf.putAll(adjustConfBSON.toMap());
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

    /**
     * Gets the reboot to take effect conf.
     * 
     * @return conf.
     */
    public Set<String> getRebootConf() {
        return rebootConf;
    }

    /**
     * Gets the adjusted conf by service.
     * 
     * @return conf.
     */
    public Map<String, String> getAdjustConf() {
        return adjustConf;
    }

    @Override
    public String toString() {
        return "ScmUpdateConfResultSet{" + "successList=" + successList + ", failList=" + failList
                + ", rebootConf=" + rebootConf + ", adjustConf=" + adjustConf + '}';
    }
}
