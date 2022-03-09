package com.sequoiacm.cloud.servicecenter.dao.sequoiadb;

import com.sequoiacm.cloud.servicecenter.common.FieldDefine;
import com.sequoiacm.cloud.servicecenter.dao.InstanceDao;
import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterError;
import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterException;
import com.sequoiacm.cloud.servicecenter.exception.ScmMetasourceException;
import com.sequoiacm.cloud.servicecenter.model.ScmInstance;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.audit.ScmUserAuditType;
import com.sequoiacm.infrastructure.common.ScmModifierDefine;
import com.sequoiacm.infrastructure.metasource.MetaCursor;
import com.sequoiacm.infrastructure.metasource.template.SequoiadbTemplate;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SdbInstanceDao implements InstanceDao {

    @Autowired
    private ScmAudit scmAudit;

    private static final Logger logger = LoggerFactory.getLogger(SdbInstanceDao.class);

    private static final String CS_SCMSYSTEM = "SCMSYSTEM";
    private static final String CL_EUREKA_INSTANCE = "EUREKA_INSTANCE";
    private static final String INDEX_NAME = "idx_eureka_instance";

    @Autowired
    private SequoiadbTemplate template;

    @PostConstruct
    public void init() throws ScmServiceCenterException {
        ensureTable();
        ensureIndex();
    }

    @Override
    public void upsert(ScmInstance scmInstance) throws ScmServiceCenterException {
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldDefine.Instance.FIELD_IP_ADDR, scmInstance.getIpAddr());
        matcher.put(FieldDefine.Instance.FIELD_PORT, scmInstance.getPort());

        BasicBSONObject record = new BasicBSONObject();
        record.put(FieldDefine.Instance.FIELD_HOST_NAME, scmInstance.getHostName());
        record.put(FieldDefine.Instance.FIELD_IS_MANUAL_STOPPED, scmInstance.isManualStopped());
        record.put(FieldDefine.Instance.FIELD_HEALTH_CHECK_URL, scmInstance.getHealthCheckUrl());
        record.put(FieldDefine.Instance.FIELD_REGION, scmInstance.getRegion());
        record.put(FieldDefine.Instance.FIELD_ZONE, scmInstance.getZone());
        record.put(FieldDefine.Instance.FIELD_MANAGEMENT_PORT, scmInstance.getManagementPort());
        record.put(FieldDefine.Instance.FIELD_METADATA, scmInstance.getMetadata());
        record.put(FieldDefine.Instance.FIELD_SERVICE_NAME, scmInstance.getServiceName());

        try {
            template.collection(CS_SCMSYSTEM, CL_EUREKA_INSTANCE).upsert(matcher,
                    new BasicBSONObject(ScmModifierDefine.SEQUOIADB_MODIFIER_SET, record));
        }
        catch (Exception e) {
            throw new ScmMetasourceException("upsert failed:csName=" + CS_SCMSYSTEM + ",clName="
                    + CL_EUREKA_INSTANCE + ",record=" + record, e);
        }

    }

    @Override
    public List<ScmInstance> findAll() throws ScmServiceCenterException {
        MetaCursor metaCursor = null;
        List<ScmInstance> scmInstanceList = new ArrayList<>();
        try {
            metaCursor = template.collection(CS_SCMSYSTEM, CL_EUREKA_INSTANCE).find(null);
            while (metaCursor.hasNext()) {
                BSONObject next = metaCursor.getNext();
                ScmInstance scmInstance = new ScmInstance(next);
                scmInstanceList.add(scmInstance);
            }
        }
        catch (Exception e) {
            throw new ScmMetasourceException("failed to query instance:csName=" + CS_SCMSYSTEM
                    + ",clName=" + CL_EUREKA_INSTANCE, e);
        }
        finally {
            if (metaCursor != null) {
                metaCursor.close();
            }
        }
        return scmInstanceList;
    }

    @Override
    public void delete(String ipAddr, int port, String username, String userType)
            throws ScmServiceCenterException {
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldDefine.Instance.FIELD_IP_ADDR, ipAddr);
        matcher.put(FieldDefine.Instance.FIELD_PORT, port);
        try {
            template.collection(CS_SCMSYSTEM, CL_EUREKA_INSTANCE).delete(matcher);
            String msg = "instance deleted:ipAddr=" + ipAddr + ",port=" + port;
            scmAudit.info(ScmAuditType.MONITOR_DELETE_INSTANCE,
                    ScmUserAuditType.getScmUserAuditType(userType), username, null, 0, msg);
        }
        catch (Exception e) {
            throw new ScmMetasourceException("failed to delete instance:csName=" + CS_SCMSYSTEM
                    + ",clName=" + CL_EUREKA_INSTANCE + ",matcher=" + matcher, e);
        }

    }

    @Override
    public void stopInstance(String ipAddr, int port) throws ScmServiceCenterException {
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldDefine.Instance.FIELD_IP_ADDR, ipAddr);
        matcher.put(FieldDefine.Instance.FIELD_PORT, port);

        BasicBSONObject modifier = new BasicBSONObject(FieldDefine.Instance.FIELD_IS_MANUAL_STOPPED, true);
        try {
            long count = template.collection(CS_SCMSYSTEM, CL_EUREKA_INSTANCE).count(matcher);
            template.collection(CS_SCMSYSTEM, CL_EUREKA_INSTANCE).update(matcher,
                    new BasicBSONObject(ScmModifierDefine.SEQUOIADB_MODIFIER_SET, modifier));
            logger.info("instance stopped:ipAddr={},port={},count={}", ipAddr, port, count);
        }
        catch (Exception e) {
            throw new ScmMetasourceException("failed to stop instance:csName=" + CS_SCMSYSTEM
                    + ",clName=" + CL_EUREKA_INSTANCE + ",matcher=" + matcher, e);
        }
    }

    private void ensureIndex() throws ScmServiceCenterException {
        BasicBSONObject indexBson = new BasicBSONObject();
        indexBson.put(FieldDefine.Instance.FIELD_IP_ADDR, 1);
        indexBson.put(FieldDefine.Instance.FIELD_PORT, 1);
        try {
            template.collection(CS_SCMSYSTEM, CL_EUREKA_INSTANCE).ensureIndex(INDEX_NAME, indexBson,
                    true);
        }
        catch (Exception e) {
            throw new ScmMetasourceException("failed to create index:csName=" + CS_SCMSYSTEM
                    + ",clName=" + CL_EUREKA_INSTANCE + ",index=" + INDEX_NAME, e);
        }

    }

    private void ensureTable() throws ScmServiceCenterException {
        try {
            template.collectionSpace(CS_SCMSYSTEM).createCollection(CL_EUREKA_INSTANCE);
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_DMS_EXIST.getErrorCode()) {
                throw new ScmMetasourceException(ScmServiceCenterError.METASOURCE_ERROR,
                        "failed to create collection:" + CS_SCMSYSTEM + "." + CL_EUREKA_INSTANCE,
                        e);
            }
        }
    }
}
