package com.sequoiacm.config.framework.common;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

@Component
public class DefaultVersionDaoImpl implements DefaultVersionDao {

    @Autowired
    private Metasource metasource;

    @Override
    public Integer getVersion(String businessType, String businessName) throws ScmConfigException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_NAME, businessName);
        matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_TYPE, businessType);
        BSONObject versionrRec = metasource.getConfVersionTableDao().queryOne(matcher, null, null);
        if (versionrRec == null) {
            return null;
        }
        return (Integer) versionrRec.get(FieldName.FIELD_CLVERSION_BUSINESS_VERSION);
    }

    @Override
    public List<Version> getVerions(VersionFilter filter) throws MetasourceException {
        BSONObject matcher = new BasicBSONObject();
        if (filter.getBusinessType() != null) {
            matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_TYPE, filter.getBusinessType());
        }
        if (filter.getBusinessNames() != null) {
            List<Version> versions = new ArrayList<>();
            for (String bussinessName : filter.getBusinessNames()) {
                matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_NAME, bussinessName);
                versions.addAll(queryVersions(matcher));
            }
            return versions;
        }
        else {
            return queryVersions(matcher);
        }
    }

    private List<Version> queryVersions(BSONObject matcher) throws MetasourceException {
        List<Version> versions = new ArrayList<>();
        MetaCursor cursor = metasource.getConfVersionTableDao().query(matcher, null, null);
        try {
            while (cursor.hasNext()) {
                BSONObject versionObj = cursor.getNext();
                String bussinessType = BsonUtils.getStringChecked(versionObj,
                        FieldName.FIELD_CLVERSION_BUSINESS_TYPE);
                String bussinessName = BsonUtils.getStringChecked(versionObj,
                        FieldName.FIELD_CLVERSION_BUSINESS_NAME);
                Integer version = BsonUtils.getIntegerChecked(versionObj,
                        FieldName.FIELD_CLVERSION_BUSINESS_VERSION);
                Version basicVersion = new Version(bussinessType, bussinessName,
                        version);
                versions.add(basicVersion);
            }
            return versions;
        }
        finally {
            cursor.close();
        }
    }

    @Override
    public void createVersion(String businessType, String businessName, Transaction transaction)
            throws ScmConfigException {
        BSONObject versionRec = new BasicBSONObject();
        versionRec.put(FieldName.FIELD_CLVERSION_BUSINESS_NAME, businessName);
        versionRec.put(FieldName.FIELD_CLVERSION_BUSINESS_TYPE, businessType);
        versionRec.put(FieldName.FIELD_CLVERSION_BUSINESS_VERSION, 1);
        metasource.getConfVersionTableDao(transaction).insert(versionRec);
    }

    @Override
    public void createVersion(String businessType, String businessName) throws ScmConfigException {
        BSONObject versionRec = new BasicBSONObject();
        versionRec.put(FieldName.FIELD_CLVERSION_BUSINESS_NAME, businessName);
        versionRec.put(FieldName.FIELD_CLVERSION_BUSINESS_TYPE, businessType);
        versionRec.put(FieldName.FIELD_CLVERSION_BUSINESS_VERSION, 1);
        metasource.getConfVersionTableDao().insert(versionRec);
    }

    @Override
    public void deleteVersion(String businessType, String businessName, Transaction transaction)
            throws MetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_NAME, businessName);
        matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_TYPE, businessType);
        metasource.getConfVersionTableDao(transaction).delete(matcher);
    }

    @Override
    public Integer updateVersion(String businessType, String businessName, int newVersion,
            Transaction transaction) throws MetasourceException {
        TableDao table = transaction == null ? metasource.getConfVersionTableDao()
                : metasource.getConfVersionTableDao(transaction);
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_NAME, businessName);
        matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_TYPE, businessType);
        BasicBSONObject updator = new BasicBSONObject(FieldName.FIELD_CLVERSION_BUSINESS_VERSION,
                newVersion);
        BSONObject updatedRecord = table.updateAndCheck(matcher, updator);
        if (updatedRecord == null) {
            return null;
        }
        return BsonUtils.getIntegerChecked(updatedRecord,
                FieldName.FIELD_CLVERSION_BUSINESS_VERSION);
    }

    @Override
    public Integer increaseVersion(String businessType, String businessName,
            Transaction transaction) throws MetasourceException {
        TableDao table = metasource.getConfVersionTableDao(transaction);

        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_NAME, businessName);
        matcher.put(FieldName.FIELD_CLVERSION_BUSINESS_TYPE, businessType);

        BasicBSONObject updator = new BasicBSONObject(FieldName.FIELD_CLVERSION_BUSINESS_NAME,
                businessName);

        // update the record , for lock it
        BSONObject record = table.updateAndCheck(matcher, updator);
        if (record == null) {
            return null;
        }

        Version oldVersion = new Version(record);

        return updateVersion(businessType, businessName, oldVersion.getVersion() + 1, transaction);

    }

}
