package com.sequoiacm.fulltext.server.sch;

import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.schedule.client.ScheduleClient;
import com.sequoiacm.schedule.client.worker.ScheduleWorkerBuilder;
import com.sequoiacm.schedule.client.worker.ScheduleWorkerMgr;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.ScheduleDefine;
import com.sequoiacm.schedule.common.model.InternalSchStatus;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.common.model.ScheduleFullEntity;
import com.sequoiacm.schedule.common.model.ScheduleUserEntity;

@Component
public class ScheduleServiceClient {

    @Value("${eureka.client.region}")
    private String localRegion;

    @Value("${eureka.instance.metadata-map.zone}")
    private String localZone;

    @Autowired
    private ScheduleClient schClient;

    @Autowired
    public ScheduleServiceClient(ScheduleWorkerMgr schWorkerMgr,
            List<ScheduleWorkerBuilder> workerFactorys) {
        for (ScheduleWorkerBuilder f : workerFactorys) {
            schWorkerMgr.registerWorkerFactory(f);
        }
    }

    public void createFulltextSch(String schName, FulltextIdxSchJobType jobTyp,
            FulltextIdxSchJobData jobData) throws FullTextException {
        ScheduleUserEntity sch = new ScheduleUserEntity();
        sch.setCron(null);
        sch.setDesc(jobTyp.name());
        sch.setEnable(true);
        sch.setName(schName);
        sch.setType(ScheduleDefine.ScheduleType.INTERNAL_SCHEDULE);
        sch.setWorkspace(jobData.getWs());
        sch.setPreferredZone(localZone);
        sch.setPreferredRegion(localRegion);
        BSONObject content = new BasicBSONObject();
        content.put(FieldName.Schedule.FIELD_INTERNAL_JOB_TYPE, jobTyp.name());
        content.put(FieldName.Schedule.FIELD_INTERNAL_WORKER_SERVICE, "fulltext-server");
        content.put(FieldName.Schedule.FIELD_INTERNAL_JOB_DATA, jobData.toBSON());
        sch.setContent(content);
        try {
            schClient.createSchedule(sch);
        }
        catch (ScheduleException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to create fulltext index schedule:schName=" + schName + ", jobData="
                            + jobData,
                    e);
        }
    }

    public void removeInternalSch(String name, boolean stopWorker) throws FullTextException {
        try {
            ScheduleFullEntity sch = schClient.getInternalSchduleByName(name);
            if (sch == null) {
                return;
            }
            schClient.deleteSchedule(sch.getId(), true);
        }
        catch (ScheduleException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to remove internal schedule:name=" + name, e);
        }
    }

    public boolean isInternalSchExist(String name) throws FullTextException {
        ScheduleFullEntity s = getInternalSchByName(name);
        if (s == null) {
            return false;
        }
        return true;
    }

    public InternalSchStatus getInternalSchLatestStatus(String schName) throws FullTextException {
        try {
            return schClient.getInternalSchLatestStatus(schName);
        }
        catch (ScheduleException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to get schedule status from schedule-server:schName=" + schName, e);
        }
        
    }

    public ScheduleFullEntity getInternalSchByName(String name) throws FullTextException {
        ScheduleFullEntity s;
        try {
            s = schClient.getInternalSchduleByName(name);
        }
        catch (ScheduleException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to get internal-schedule job info from schedule-server:name=" + name,
                    e);
        }
        return s;
    }

}
