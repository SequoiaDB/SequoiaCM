package com.sequoiacm.cloud.adminserver.core;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiacm.cloud.adminserver.dao.QuotaSyncDao;
import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ScmConfNotifyProcessor {

    private static Logger logger = LoggerFactory.getLogger(ScmConfNotifyProcessor.class);

    @Autowired
    private QuotaSyncDao quotaSyncDao;

    @Autowired
    private ScmConfClient confClient;

    @PostConstruct
    private void init() throws ScmConfigException {
        confClient.subscribe(ScmBusinessTypeDefine.BUCKET, new NotifyCallback() {
            @Override
            public void processNotify(EventType type, String businessName,
                    NotifyOption notification) throws Exception {
                handleBucketNotify(type, businessName, notification);
            }
        });

        confClient.subscribe(ScmBusinessTypeDefine.WORKSPACE, new NotifyCallback() {
            @Override
            public void processNotify(EventType type, String businessName,
                    NotifyOption notification) throws Exception {
                handleWorkspaceNotify(type, businessName, notification);
            }
        });
    }

    public void handleBucketNotify(EventType type, String businessName, NotifyOption notification)
            throws ScmMetasourceException {
        if (type == EventType.DELTE) {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.QuotaSync.TYPE, StatisticsDefine.QuotaType.BUCKET);
            matcher.put(FieldName.QuotaSync.NAME, businessName);
            quotaSyncDao.delete(matcher);
        }
    }

    public void handleWorkspaceNotify(EventType type, String businessName,
            NotifyOption notification)
            throws ScmMetasourceException {
        if (type == EventType.DELTE) {
            BSONObject matcher = new BasicBSONObject();
            matcher.put(FieldName.QuotaSync.TYPE, StatisticsDefine.QuotaType.BUCKET);
            matcher.put(
                    FieldName.QuotaSync.EXTRA_INFO + "." + FieldName.QuotaSync.EXTRA_INFO_WORKSPACE,
                    businessName);
            quotaSyncDao.delete(matcher);
        }
    }
}
