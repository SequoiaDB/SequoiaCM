package com.sequoiacm.config.server.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.config.metasource.EnableMetasource;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.server.core.ScmConfOperatorMgr;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@Repository
@EnableMetasource
public class ScmConfSubscriberDaoImpl implements ScmConfSubscriberDao {

    @Autowired
    Metasource metasource;

    @Autowired
    ScmConfOperatorMgr confOperatorMgr;

    @Override
    public void createSubscriber(String businessType, String serviceName) throws ScmConfigException {
        // make sure businessType is valid.
        confOperatorMgr.getConfOperator(businessType);
        try {
            TableDao dao = metasource.getSubscribersTable();
            BSONObject record = new BasicBSONObject();
            record.put(FieldName.FIELD_CLSUBSCRIBER_BUSINESS_TYPE, businessType);
            record.put(FieldName.FIELD_CLSUBSCRIBER_SERVICE_NAME, serviceName.toLowerCase());
            dao.insert(record);
        }
        catch (MetasourceException e) {
            if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                return;
            }
            throw new ScmConfigException(e.getError(), "failed to subscribe:businessType="
                    + businessType + ", serviceName=" + serviceName + ",error=" + e.getMessage(), e);
        }
    }

    @Override
    public List<ScmConfSubscriber> querySubscribers(String businessType) throws ScmConfigException {
        MetaCursor cursor = null;
        try {
            TableDao dao = metasource.getSubscribersTable();
            BSONObject matcher = new BasicBSONObject();
            if (businessType != null) {
                matcher.put(FieldName.FIELD_CLSUBSCRIBER_BUSINESS_TYPE, businessType);
            }
            cursor = dao.query(matcher, null, null);
            List<ScmConfSubscriber> subscribers = new ArrayList<>();
            while (cursor.hasNext()) {
                BSONObject subscriberObj = cursor.getNext();
                String recServiceName = (String) subscriberObj
                        .get(FieldName.FIELD_CLSUBSCRIBER_SERVICE_NAME);

                String recConfigName = (String) subscriberObj
                        .get(FieldName.FIELD_CLSUBSCRIBER_BUSINESS_TYPE);

                Assert.notNull(recServiceName, "missing serviceName:" + subscriberObj);
                ScmConfSubscriber subscriber = new ScmConfSubscriber(recConfigName, recServiceName);
                subscribers.add(subscriber);
            }
            return subscribers;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void deleteSubscriber(String businessType, String serviceName) throws ScmConfigException {
        TableDao tableDao = metasource.getSubscribersTable();
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLSUBSCRIBER_BUSINESS_TYPE, businessType);
        matcher.put(FieldName.FIELD_CLSUBSCRIBER_SERVICE_NAME, serviceName);
        tableDao.delete(matcher);
    }

}