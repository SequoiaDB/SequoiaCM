package com.sequoiacm.schedule.dao;

import java.util.List;

import com.sequoiacm.schedule.common.model.InternalSchStatus;

public interface InternalSchStatusDao {
    public List<InternalSchStatus> getStatusById(String id) throws Exception;

    public void upsertStatus(InternalSchStatus status) throws Exception;

    public InternalSchStatus getLatestStatusByName(String name) throws Exception;
}
