package com.sequoiacm.config.framework.workspace.metasource;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.springframework.util.Assert;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbTableDao;
import com.sequoiacm.infrastructure.config.core.common.FieldName;

public class SysWorkspaceTableDaoSdbImpl extends SequoiadbTableDao implements SysWorkspaceTableDao {

    public SysWorkspaceTableDaoSdbImpl(SequoiadbMetasource sdbMetasource) {
        super(sdbMetasource, MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_WORKSPACE);
    }

    public SysWorkspaceTableDaoSdbImpl(Transaction transaction) {
        super(transaction, MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_WORKSPACE);
    }

    @Override
    public BSONObject removeDataLocation(BSONObject oldWsRecord, int siteId)
            throws MetasourceException {
        BasicBSONObject matcher = new BasicBSONObject(
                FieldName.FIELD_CLWORKSPACE_DATA_LOCATION + "." + SequoiadbHelper.DOLLAR0 + "."
                        + FieldName.FIELD_CLWORKSPACE_LOCATION_SITE_ID,
                siteId);
        BasicBSONList matcherList = new BasicBSONList();
        matcherList.add(matcher);
        matcherList.add(oldWsRecord);
        BasicBSONObject andMatcher = new BasicBSONObject(SequoiadbHelper.DOLLAR_AND, matcherList);

        BasicBSONObject unsetValue = new BasicBSONObject(
                FieldName.FIELD_CLWORKSPACE_DATA_LOCATION + "." + SequoiadbHelper.DOLLAR0, "");
        BasicBSONObject unsetObj = new BasicBSONObject(SequoiadbHelper.DOLLAR_UNSET, unsetValue);

        BSONObject updatedRecord = _updateAndCheck(andMatcher, unsetObj);
        if (updatedRecord == null) {
            return updatedRecord;
        }
        updatedRecord = deleteNullDataLocation(updatedRecord);
        Assert.notNull(updatedRecord, "updatedRecord can't be null!");
        return updatedRecord;
    }

    @Override
    // TODO:控制加站点加到数组的位置
    public BSONObject addDataLocation(BSONObject oldWsRecord, BSONObject location)
            throws MetasourceException {
        BasicBSONObject locationInfo = new BasicBSONObject(
                FieldName.FIELD_CLWORKSPACE_DATA_LOCATION, location);
        BasicBSONObject pushObj = new BasicBSONObject(SequoiadbHelper.DOLLAR_PUSH, locationInfo);

        return _updateAndCheck(oldWsRecord, pushObj);
    }

    private BSONObject deleteNullDataLocation(BSONObject matcher) throws MetasourceException {
        BSONObject nullInList = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_DATA_LOCATION,
                null);
        BSONObject pull = new BasicBSONObject(SequoiadbHelper.DOLLAR_PULL, nullInList);
        return _updateAndCheck(matcher, pull);
    }

    @Override
    public BSONObject updateExternalData(BSONObject matcher, BSONObject externalData)
            throws MetasourceException {
        BSONObject updator = new BasicBSONObject();
        for (String key : externalData.keySet()) {
            updator.put(FieldName.FIELD_CLWORKSPACE_EXTERNAL_DATA + "." + key,
                    externalData.get(key));
        }
        return updateAndCheck(matcher, updator);
    }

}
